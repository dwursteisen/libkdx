/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.SharedLibraryLoader
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.RuntimeException
import java.util.HashSet
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.jvm.Synchronized
import kotlin.jvm.Throws

/**
 * Loads shared libraries from a natives jar file (desktop) or arm folders (Android). For desktop projects, have the natives jar
 * in the classpath, for Android projects put the shared libraries in the libs/armeabi and libs/armeabi-v7a folders.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
class SharedLibraryLoader {

    companion object {
        var isWindows: Boolean = java.lang.System.getProperty("os.name").contains("Windows")
        var isLinux: Boolean = java.lang.System.getProperty("os.name").contains("Linux")
        var isMac: Boolean = java.lang.System.getProperty("os.name").contains("Mac")
        var isIos = false
        var isAndroid = false
        var isARM: Boolean = java.lang.System.getProperty("os.arch").startsWith("arm")
        var is64Bit = java.lang.System.getProperty("os.arch") == "amd64" || java.lang.System.getProperty("os.arch") == "x86_64"

        // JDK 8 only.
        var abi = if (java.lang.System.getProperty("sun.arch.abi") != null) java.lang.System.getProperty("sun.arch.abi") else ""
        private val loadedLibraries: HashSet<String> = HashSet()

        /**
         * Sets the library as loaded, for when application code wants to handle libary loading itself.
         */
        @Synchronized
        fun setLoaded(libraryName: String) {
            loadedLibraries.add(libraryName)
        }

        @Synchronized
        fun isLoaded(libraryName: String): Boolean {
            return loadedLibraries.contains(libraryName)
        }

        init {
            val isMOEiOS = "iOS" == java.lang.System.getProperty("moe.platform.name")
            val vm: String = java.lang.System.getProperty("java.runtime.name")
            if (com.badlogic.gdx.utils.vm != null && com.badlogic.gdx.utils.vm.contains("Android Runtime")) {
                isAndroid = true
                isWindows = false
                isLinux = false
                isMac = false
                is64Bit = false
            }
            if (com.badlogic.gdx.utils.isMOEiOS || !isAndroid && !isWindows && !isLinux && !isMac) {
                isIos = true
                isAndroid = false
                isWindows = false
                isLinux = false
                isMac = false
                is64Bit = false
            }
        }
    }

    private var nativesJar: String? = null

    constructor() {}

    /**
     * Fetches the natives from the given natives jar file. Used for testing a shared lib on the fly.
     *
     * @param nativesJar
     */
    constructor(nativesJar: String?) {
        this.nativesJar = nativesJar
    }

    /**
     * Returns a CRC of the remaining bytes in the stream.
     */
    fun crc(input: java.io.InputStream?): String {
        if (input == null) throw IllegalArgumentException("input cannot be null.")
        val crc = CRC32()
        val buffer = ByteArray(4096)
        try {
            while (true) {
                val length: Int = input.read(buffer)
                if (length == -1) break
                crc.update(buffer, 0, length)
            }
        } catch (ex: java.lang.Exception) {
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(input)
        }
        return java.lang.Long.toString(crc.getValue(), 16)
    }

    /**
     * Maps a platform independent library name to a platform dependent name.
     */
    fun mapLibraryName(libraryName: String): String {
        if (isWindows) return libraryName + if (is64Bit) "64.dll" else ".dll"
        if (isLinux) return "lib" + libraryName + (if (isARM) "arm$abi" else "") + if (is64Bit) "64.so" else ".so"
        return if (isMac) "lib" + libraryName + (if (is64Bit) "64.dylib" else ".dylib") else libraryName
    }

    /**
     * Loads a shared library for the platform the application is running on.
     *
     * @param libraryName The platform independent library name. If not contain a prefix (eg lib) or suffix (eg .dll).
     */
    fun load(libraryName: String) {
        // in case of iOS, things have been linked statically to the executable, bail out.
        if (isIos) return
        synchronized(SharedLibraryLoader::class.java) {
            if (isLoaded(libraryName)) return
            val platformName = mapLibraryName(libraryName)
            try {
                if (isAndroid) java.lang.System.loadLibrary(platformName) else loadFile(platformName)
                setLoaded(libraryName)
            } catch (ex: Throwable) {
                throw GdxRuntimeException("Couldn't load shared library '" + platformName + "' for target: "
                    + java.lang.System.getProperty("os.name") + if (is64Bit) ", 64-bit" else ", 32-bit", ex)
            }
        }
    }

    private fun readFile(path: String): java.io.InputStream? {
        return if (nativesJar == null) {
            SharedLibraryLoader::class.java.getResourceAsStream("/$path")
                ?: throw GdxRuntimeException("Unable to read file for extraction: $path")
        } else try {
            val file = ZipFile(nativesJar)
            val entry: ZipEntry = file.getEntry(path)
                ?: throw GdxRuntimeException("Couldn't find '$path' in JAR: $nativesJar")
            file.getInputStream(entry)
        } catch (ex: IOException) {
            throw GdxRuntimeException("Error reading '$path' in JAR: $nativesJar", ex)
        }

        // Read from JAR.
    }

    /**
     * Extracts the specified file to the specified directory if it does not already exist or the CRC does not match. If file
     * extraction fails and the file exists at java.library.path, that file is returned.
     *
     * @param sourcePath The file to extract from the classpath or JAR.
     * @param dirName    The name of the subdirectory where the file will be extracted. If null, the file's CRC will be used.
     * @return The extracted file.
     */
    @Throws(IOException::class)
    fun extractFile(sourcePath: String, dirName: String?): java.io.File {
        var dirName = dirName
        return try {
            val sourceCrc = crc(readFile(sourcePath))
            if (dirName == null) dirName = sourceCrc
            var extractedFile: java.io.File? = getExtractedFile(dirName, java.io.File(sourcePath).getName())
            if (extractedFile == null) {
                extractedFile = getExtractedFile(UUID.randomUUID().toString(), java.io.File(sourcePath).getName())
                if (extractedFile == null) throw GdxRuntimeException(
                    "Unable to find writable path to extract file. Is the user home directory writable?")
            }
            extractFile(sourcePath, sourceCrc, extractedFile)
        } catch (ex: RuntimeException) {
            // Fallback to file at java.library.path location, eg for applets.
            val file: java.io.File = java.io.File(java.lang.System.getProperty("java.library.path"), sourcePath)
            if (file.exists()) return file
            throw ex
        }
    }

    /**
     * Extracts the specified file into the temp directory if it does not already exist or the CRC does not match. If file
     * extraction fails and the file exists at java.library.path, that file is returned.
     *
     * @param sourcePath The file to extract from the classpath or JAR.
     * @param dir        The location where the extracted file will be written.
     */
    @Throws(IOException::class)
    fun extractFileTo(sourcePath: String, dir: java.io.File?) {
        extractFile(sourcePath, crc(readFile(sourcePath)), java.io.File(dir, java.io.File(sourcePath).getName()))
    }

    /**
     * Returns a path to a file that can be written. Tries multiple locations and verifies writing succeeds.
     *
     * @return null if a writable path could not be found.
     */
    private fun getExtractedFile(dirName: String?, fileName: String): java.io.File? {
        // Temp directory with username in path.
        val idealFile: java.io.File = java.io.File(
            java.lang.System.getProperty("java.io.tmpdir") + "/libgdx" + java.lang.System.getProperty("user.name") + "/" + dirName, fileName)
        if (canWrite(idealFile)) return idealFile

        // System provided temp directory.
        try {
            var file: java.io.File = java.io.File.createTempFile(dirName, null)
            if (file.delete()) {
                file = java.io.File(file, fileName)
                if (canWrite(file)) return file
            }
        } catch (ignored: IOException) {
        }

        // User home.
        var file: java.io.File = java.io.File(java.lang.System.getProperty("user.home") + "/.libgdx/" + dirName, fileName)
        if (canWrite(file)) return file

        // Relative directory.
        file = java.io.File(".temp/$dirName", fileName)
        if (canWrite(file)) return file

        // We are running in the OS X sandbox.
        return if (java.lang.System.getenv("APP_SANDBOX_CONTAINER_ID") != null) idealFile else null
    }

    /**
     * Returns true if the parent directories of the file can be created and the file can be written.
     */
    private fun canWrite(file: java.io.File): Boolean {
        val parent: java.io.File = file.getParentFile()
        val testFile: java.io.File
        testFile = if (file.exists()) {
            if (!file.canWrite() || !canExecute(file)) return false
            // Don't overwrite existing file just to check if we can write to directory.
            java.io.File(parent, UUID.randomUUID().toString())
        } else {
            parent.mkdirs()
            if (!parent.isDirectory()) return false
            file
        }
        return try {
            FileOutputStream(testFile).close()
            if (!canExecute(testFile)) false else true
        } catch (ex: Throwable) {
            false
        } finally {
            testFile.delete()
        }
    }

    private fun canExecute(file: java.io.File): Boolean {
        try {
            val canExecute: java.lang.reflect.Method = java.io.File::class.java.getMethod("canExecute")
            if (canExecute.invoke(file)) return true
            val setExecutable: java.lang.reflect.Method = java.io.File::class.java.getMethod("setExecutable", Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
            setExecutable.invoke(file, true, false)
            return canExecute.invoke(file)
        } catch (ignored: java.lang.Exception) {
        }
        return false
    }

    @Throws(IOException::class)
    private fun extractFile(sourcePath: String, sourceCrc: String, extractedFile: java.io.File): java.io.File {
        var extractedCrc: String? = null
        if (extractedFile.exists()) {
            try {
                extractedCrc = crc(FileInputStream(extractedFile))
            } catch (ignored: FileNotFoundException) {
            }
        }

        // If file doesn't exist or the CRC doesn't match, extract it to the temp dir.
        if (extractedCrc == null || extractedCrc != sourceCrc) {
            var input: java.io.InputStream? = null
            var output: FileOutputStream? = null
            try {
                input = readFile(sourcePath)
                extractedFile.getParentFile().mkdirs()
                output = FileOutputStream(extractedFile)
                val buffer = ByteArray(4096)
                while (true) {
                    val length: Int = input.read(buffer)
                    if (length == -1) break
                    output.write(buffer, 0, length)
                }
            } catch (ex: IOException) {
                throw GdxRuntimeException("""
    Error extracting file: $sourcePath
    To: ${extractedFile.getAbsolutePath()}
    """.trimIndent(),
                    ex)
            } finally {
                com.badlogic.gdx.utils.StreamUtils.closeQuietly(input)
                com.badlogic.gdx.utils.StreamUtils.closeQuietly(output)
            }
        }
        return extractedFile
    }

    /**
     * Extracts the source file and calls System.load. Attemps to extract and load from multiple locations. Throws runtime
     * exception if all fail.
     */
    private fun loadFile(sourcePath: String) {
        val sourceCrc = crc(readFile(sourcePath))
        val fileName: String = java.io.File(sourcePath).getName()

        // Temp directory with username in path.
        var file: java.io.File = java.io.File(java.lang.System.getProperty("java.io.tmpdir") + "/libgdx" + java.lang.System.getProperty("user.name") + "/" + sourceCrc,
            fileName)
        val ex = loadFile(sourcePath, sourceCrc, file) ?: return

        // System provided temp directory.
        try {
            file = java.io.File.createTempFile(sourceCrc, null)
            if (file.delete() && loadFile(sourcePath, sourceCrc, file) == null) return
        } catch (ignored: Throwable) {
        }

        // User home.
        file = java.io.File(java.lang.System.getProperty("user.home") + "/.libgdx/" + sourceCrc, fileName)
        if (loadFile(sourcePath, sourceCrc, file) == null) return

        // Relative directory.
        file = java.io.File(".temp/$sourceCrc", fileName)
        if (loadFile(sourcePath, sourceCrc, file) == null) return

        // Fallback to java.library.path location, eg for applets.
        file = java.io.File(java.lang.System.getProperty("java.library.path"), sourcePath)
        if (file.exists()) {
            java.lang.System.load(file.getAbsolutePath())
            return
        }
        throw GdxRuntimeException(ex)
    }

    /**
     * @return null if the file was extracted and loaded.
     */
    private fun loadFile(sourcePath: String, sourceCrc: String, extractedFile: java.io.File): Throwable? {
        return try {
            java.lang.System.load(extractFile(sourcePath, sourceCrc, extractedFile).getAbsolutePath())
            null
        } catch (ex: Throwable) {
            ex
        }
    }
}
