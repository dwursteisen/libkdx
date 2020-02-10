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
package com.badlogic.gdx.files

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.RandomAccessFile
import java.io.UnsupportedEncodingException
import java.lang.UnsupportedOperationException
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.channels.FileChannel.MapMode

/** Represents a file or directory on the filesystem, classpath, Android SD card, or Android assets directory. FileHandles are
 * created via a [Files] instance.
 *
 * Because some of the file types are backed by composite files and may be compressed (for example, if they are in an Android .apk
 * or are found via the classpath), the methods for extracting a [.path] or [.file] may not be appropriate for all
 * types. Use the Reader or Stream methods here to hide these dependencies from your platform independent code.
 *
 * @author mzechner
 * @author Nathan Sweet
 */
open class FileHandle {

    protected var file: java.io.File? = null
    protected var type: com.badlogic.gdx.Files.FileType? = null

    protected constructor() {}
    /** Creates a new absolute FileHandle for the file name. Use this for tools on the desktop that don't need any of the backends.
     * Do not use this constructor in case you write something cross-platform. Use the [Files] interface instead.
     * @param fileName the filename.
     */
    constructor(fileName: String?) {
        file = java.io.File(fileName)
        type = com.badlogic.gdx.Files.FileType.Absolute
    }

    /** Creates a new absolute FileHandle for the [File]. Use this for tools on the desktop that don't need any of the
     * backends. Do not use this constructor in case you write something cross-platform. Use the [Files] interface instead.
     * @param file the file.
     */
    constructor(file: java.io.File?) {
        this.file = file
        type = com.badlogic.gdx.Files.FileType.Absolute
    }

    protected constructor(fileName: String?, type: com.badlogic.gdx.Files.FileType?) {
        this.type = type
        file = java.io.File(fileName)
    }

    protected constructor(file: java.io.File?, type: com.badlogic.gdx.Files.FileType?) {
        this.file = file
        this.type = type
    }

    /** @return the path of the file as specified on construction, e.g. Gdx.files.internal("dir/file.png") -> dir/file.png.
     * backward slashes will be replaced by forward slashes.
     */
    fun path(): String {
        return file.getPath().replace('\\', '/')
    }

    /** @return the name of the file, without any parent paths.
     */
    fun name(): String {
        return file.getName()
    }

    /** Returns the file extension (without the dot) or an empty string if the file name doesn't contain a dot.  */
    fun extension(): String {
        val name: String = file.getName()
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex == -1) "" else name.substring(dotIndex + 1)
    }

    /** @return the name of the file, without parent paths or the extension.
     */
    fun nameWithoutExtension(): String {
        val name: String = file.getName()
        val dotIndex = name.lastIndexOf('.')
        return if (dotIndex == -1) name else name.substring(0, dotIndex)
    }

    /** @return the path and filename without the extension, e.g. dir/dir2/file.png -> dir/dir2/file. backward slashes will be
     * returned as forward slashes.
     */
    fun pathWithoutExtension(): String {
        val path: String = file.getPath().replace('\\', '/')
        val dotIndex = path.lastIndexOf('.')
        return if (dotIndex == -1) path else path.substring(0, dotIndex)
    }

    fun type(): com.badlogic.gdx.Files.FileType? {
        return type
    }

    /** Returns a java.io.File that represents this file handle. Note the returned file will only be usable for
     * [FileType.Absolute] and [FileType.External] file handles.  */
    fun file(): java.io.File? {
        return if (type == com.badlogic.gdx.Files.FileType.External) java.io.File(com.badlogic.gdx.Gdx.files.getExternalStoragePath(), file.getPath()) else file
    }

    /** Returns a stream for reading this file as bytes.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    open fun read(): java.io.InputStream {
        return if (type == com.badlogic.gdx.Files.FileType.Classpath || type == com.badlogic.gdx.Files.FileType.Internal && !file().exists()
            || type == com.badlogic.gdx.Files.FileType.Local && !file().exists()) {
            FileHandle::class.java.getResourceAsStream("/" + file.getPath().replace('\\', '/'))
                ?: throw com.badlogic.gdx.utils.GdxRuntimeException("File not found: $file ($type)")
        } else try {
            FileInputStream(file())
        } catch (ex: java.lang.Exception) {
            if (file().isDirectory()) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error reading file: $file ($type)", ex)
        }
    }

    /** Returns a buffered stream for reading this file as bytes.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    fun read(bufferSize: Int): BufferedInputStream {
        return BufferedInputStream(read(), bufferSize)
    }

    /** Returns a reader for reading this file as characters the platform's default charset.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    fun reader(): java.io.Reader {
        return InputStreamReader(read())
    }

    /** Returns a reader for reading this file as characters.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    fun reader(charset: String?): java.io.Reader {
        val stream: java.io.InputStream = read()
        return try {
            InputStreamReader(stream, charset)
        } catch (ex: UnsupportedEncodingException) {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(stream)
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error reading file: $this", ex)
        }
    }

    /** Returns a buffered reader for reading this file as characters using the platform's default charset.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    fun reader(bufferSize: Int): BufferedReader {
        return BufferedReader(InputStreamReader(read()), bufferSize)
    }

    /** Returns a buffered reader for reading this file as characters.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    fun reader(bufferSize: Int, charset: String?): BufferedReader {
        return try {
            BufferedReader(InputStreamReader(read(), charset), bufferSize)
        } catch (ex: UnsupportedEncodingException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error reading file: $this", ex)
        }
    }
    /** Reads the entire file into a string using the specified charset.
     * @param charset If null the default charset is used.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    /** Reads the entire file into a string using the platform's default charset.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    @JvmOverloads
    fun readString(charset: String? = null): String {
        val output: java.lang.StringBuilder = java.lang.StringBuilder(estimateLength())
        var reader: InputStreamReader? = null
        try {
            if (charset == null) reader = InputStreamReader(read()) else reader = InputStreamReader(read(), charset)
            val buffer = CharArray(256)
            while (true) {
                val length: Int = reader.read(buffer)
                if (length == -1) break
                output.append(buffer, 0, length)
            }
        } catch (ex: IOException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error reading layout file: $this", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(reader)
        }
        return output.toString()
    }

    /** Reads the entire file into a byte array.
     * @throws GdxRuntimeException if the file handle represents a directory, doesn't exist, or could not be read.
     */
    fun readBytes(): ByteArray {
        val input: java.io.InputStream = read()
        return try {
            com.badlogic.gdx.utils.StreamUtils.copyStreamToByteArray(input, estimateLength())
        } catch (ex: IOException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error reading file: $this", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(input)
        }
    }

    private fun estimateLength(): Int {
        val length = length().toInt()
        return if (length != 0) length else 512
    }

    /** Reads the entire file into the byte array. The byte array must be big enough to hold the file's data.
     * @param bytes the array to load the file into
     * @param offset the offset to start writing bytes
     * @param size the number of bytes to read, see [.length]
     * @return the number of read bytes
     */
    fun readBytes(bytes: ByteArray?, offset: Int, size: Int): Int {
        val input: java.io.InputStream = read()
        var position = 0
        try {
            while (true) {
                val count: Int = input.read(bytes, offset + position, size - position)
                if (count <= 0) break
                position += count
            }
        } catch (ex: IOException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error reading file: $this", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(input)
        }
        return position - offset
    }
    /** Attempts to memory map this file. Android files must not be compressed.
     * @throws GdxRuntimeException if this file handle represents a directory, doesn't exist, or could not be read, or memory mapping fails, or is a [FileType.Classpath] file.
     */
    /** Attempts to memory map this file in READ_ONLY mode. Android files must not be compressed.
     * @throws GdxRuntimeException if this file handle represents a directory, doesn't exist, or could not be read, or memory mapping fails, or is a [FileType.Classpath] file.
     */
    @JvmOverloads
    fun map(mode: MapMode = MapMode.READ_ONLY): java.nio.ByteBuffer {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot map a classpath file: $this")
        var raf: RandomAccessFile? = null
        return try {
            raf = RandomAccessFile(file, if (mode === MapMode.READ_ONLY) "r" else "rw")
            val fileChannel: FileChannel = raf.getChannel()
            val map: java.nio.ByteBuffer = fileChannel.map(mode, 0, file.length())
            map.order(ByteOrder.nativeOrder())
            map
        } catch (ex: java.lang.Exception) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error memory mapping file: $this ($type)", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(raf)
        }
    }

    /** Returns a stream for writing to this file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    open fun write(append: Boolean): java.io.OutputStream {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot write to a classpath file: $file")
        if (type == com.badlogic.gdx.Files.FileType.Internal) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot write to an internal file: $file")
        parent().mkdirs()
        return try {
            FileOutputStream(file(), append)
        } catch (ex: java.lang.Exception) {
            if (file().isDirectory()) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error writing file: $file ($type)", ex)
        }
    }

    /** Returns a buffered stream for writing to this file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @param bufferSize The size of the buffer.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    fun write(append: Boolean, bufferSize: Int): java.io.OutputStream {
        return BufferedOutputStream(write(append), bufferSize)
    }

    /** Reads the remaining bytes from the specified stream and writes them to this file. The stream is closed. Parent directories
     * will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    fun write(input: java.io.InputStream?, append: Boolean) {
        var output: java.io.OutputStream? = null
        try {
            output = write(append)
            com.badlogic.gdx.utils.StreamUtils.copyStream(input, output)
        } catch (ex: java.lang.Exception) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error stream writing to file: $file ($type)", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(input)
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(output)
        }
    }
    /** Returns a writer for writing to this file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @param charset May be null to use the default charset.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    /** Returns a writer for writing to this file using the default charset. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    @JvmOverloads
    fun writer(append: Boolean, charset: String? = null): java.io.Writer {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot write to a classpath file: $file")
        if (type == com.badlogic.gdx.Files.FileType.Internal) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot write to an internal file: $file")
        parent().mkdirs()
        return try {
            val output = FileOutputStream(file(), append)
            charset?.let { OutputStreamWriter(output, it) } ?: OutputStreamWriter(output)
        } catch (ex: IOException) {
            if (file().isDirectory()) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot open a stream to a directory: $file ($type)", ex)
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error writing file: $file ($type)", ex)
        }
    }
    /** Writes the specified string to the file using the specified charset. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @param charset May be null to use the default charset.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    /** Writes the specified string to the file using the default charset. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    @JvmOverloads
    fun writeString(string: String?, append: Boolean, charset: String? = null) {
        var writer: java.io.Writer? = null
        try {
            writer = writer(append, charset)
            writer.write(string)
        } catch (ex: java.lang.Exception) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error writing file: $file ($type)", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(writer)
        }
    }

    /** Writes the specified bytes to the file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    fun writeBytes(bytes: ByteArray?, append: Boolean) {
        val output: java.io.OutputStream = write(append)
        try {
            output.write(bytes)
        } catch (ex: IOException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error writing file: $file ($type)", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(output)
        }
    }

    /** Writes the specified bytes to the file. Parent directories will be created if necessary.
     * @param append If false, this file will be overwritten if it exists, otherwise it will be appended.
     * @throws GdxRuntimeException if this file handle represents a directory, if it is a [FileType.Classpath] or
     * [FileType.Internal] file, or if it could not be written.
     */
    fun writeBytes(bytes: ByteArray?, offset: Int, length: Int, append: Boolean) {
        val output: java.io.OutputStream = write(append)
        try {
            output.write(bytes, offset, length)
        } catch (ex: IOException) {
            throw com.badlogic.gdx.utils.GdxRuntimeException("Error writing file: $file ($type)", ex)
        } finally {
            com.badlogic.gdx.utils.StreamUtils.closeQuietly(output)
        }
    }

    /** Returns the paths to the children of this directory. Returns an empty list if this file handle represents a file and not a
     * directory. On the desktop, an [FileType.Internal] handle to a directory on the classpath will return a zero length
     * array.
     * @throws GdxRuntimeException if this file is an [FileType.Classpath] file.
     */
    open fun list(): Array<FileHandle?> {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot list a classpath directory: $file")
        val relativePaths: Array<String> = file().list() ?: return arrayOfNulls(0)
        val handles = arrayOfNulls<FileHandle>(relativePaths.size)
        var i = 0
        val n = relativePaths.size
        while (i < n) {
            handles[i] = child(relativePaths[i])
            i++
        }
        return handles
    }

    /** Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file
     * handle represents a file and not a directory. On the desktop, an [FileType.Internal] handle to a directory on the
     * classpath will return a zero length array.
     * @param filter the [FileFilter] to filter files
     * @throws GdxRuntimeException if this file is an [FileType.Classpath] file.
     */
    fun list(filter: java.io.FileFilter): Array<FileHandle?> {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot list a classpath directory: $file")
        val file: java.io.File = file()
        val relativePaths: Array<String> = file.list() ?: return arrayOfNulls(0)
        var handles = arrayOfNulls<FileHandle>(relativePaths.size)
        var count = 0
        var i = 0
        val n = relativePaths.size
        while (i < n) {
            val path = relativePaths[i]
            val child = child(path)
            if (!filter.accept(child.file())) {
                i++
                continue
            }
            handles[count] = child
            count++
            i++
        }
        if (count < relativePaths.size) {
            val newHandles = arrayOfNulls<FileHandle>(count)
            java.lang.System.arraycopy(handles, 0, newHandles, 0, count)
            handles = newHandles
        }
        return handles
    }

    /** Returns the paths to the children of this directory that satisfy the specified filter. Returns an empty list if this file
     * handle represents a file and not a directory. On the desktop, an [FileType.Internal] handle to a directory on the
     * classpath will return a zero length array.
     * @param filter the [FilenameFilter] to filter files
     * @throws GdxRuntimeException if this file is an [FileType.Classpath] file.
     */
    fun list(filter: FilenameFilter): Array<FileHandle?> {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot list a classpath directory: $file")
        val file: java.io.File = file()
        val relativePaths: Array<String> = file.list() ?: return arrayOfNulls(0)
        var handles = arrayOfNulls<FileHandle>(relativePaths.size)
        var count = 0
        var i = 0
        val n = relativePaths.size
        while (i < n) {
            val path = relativePaths[i]
            if (!filter.accept(file, path)) {
                i++
                continue
            }
            handles[count] = child(path)
            count++
            i++
        }
        if (count < relativePaths.size) {
            val newHandles = arrayOfNulls<FileHandle>(count)
            java.lang.System.arraycopy(handles, 0, newHandles, 0, count)
            handles = newHandles
        }
        return handles
    }

    /** Returns the paths to the children of this directory with the specified suffix. Returns an empty list if this file handle
     * represents a file and not a directory. On the desktop, an [FileType.Internal] handle to a directory on the classpath
     * will return a zero length array.
     * @throws GdxRuntimeException if this file is an [FileType.Classpath] file.
     */
    fun list(suffix: String?): Array<FileHandle?> {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot list a classpath directory: $file")
        val relativePaths: Array<String> = file().list() ?: return arrayOfNulls(0)
        var handles = arrayOfNulls<FileHandle>(relativePaths.size)
        var count = 0
        var i = 0
        val n = relativePaths.size
        while (i < n) {
            val path = relativePaths[i]
            if (!path.endsWith(suffix!!)) {
                i++
                continue
            }
            handles[count] = child(path)
            count++
            i++
        }
        if (count < relativePaths.size) {
            val newHandles = arrayOfNulls<FileHandle>(count)
            java.lang.System.arraycopy(handles, 0, newHandles, 0, count)
            handles = newHandles
        }
        return handles
    }

    /** Returns true if this file is a directory. Always returns false for classpath files. On Android, an
     * [FileType.Internal] handle to an empty directory will return false. On the desktop, an [FileType.Internal]
     * handle to a directory on the classpath will return false.  */
    open val isDirectory: Boolean
        get() = if (type == com.badlogic.gdx.Files.FileType.Classpath) false else file().isDirectory()

    /** Returns a handle to the child with the specified name.  */
    open fun child(name: String?): FileHandle {
        return if (file.getPath().length == 0) FileHandle(java.io.File(name), type) else FileHandle(java.io.File(file, name), type)
    }

    /** Returns a handle to the sibling with the specified name.
     * @throws GdxRuntimeException if this file is the root.
     */
    open fun sibling(name: String?): FileHandle? {
        if (file.getPath().length == 0) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot get the sibling of the root.")
        return FileHandle(java.io.File(file.getParent(), name), type)
    }

    open fun parent(): FileHandle {
        var parent: java.io.File = file.getParentFile()
        if (parent == null) {
            parent = if (type == com.badlogic.gdx.Files.FileType.Absolute) java.io.File("/") else java.io.File("")
        }
        return FileHandle(parent, type)
    }

    /** @throws GdxRuntimeException if this file handle is a [FileType.Classpath] or [FileType.Internal] file.
     */
    open fun mkdirs() {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot mkdirs with a classpath file: $file")
        if (type == com.badlogic.gdx.Files.FileType.Internal) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot mkdirs with an internal file: $file")
        file().mkdirs()
    }

    /** Returns true if the file exists. On Android, a [FileType.Classpath] or [FileType.Internal] handle to a
     * directory will always return false. Note that this can be very slow for internal files on Android!  */
    open fun exists(): Boolean {
        when (type) {
            com.badlogic.gdx.Files.FileType.Internal -> {
                return if (file().exists()) true else FileHandle::class.java.getResource("/" + file.getPath().replace('\\', '/')) != null
            }
            com.badlogic.gdx.Files.FileType.Classpath -> return FileHandle::class.java.getResource("/" + file.getPath().replace('\\', '/')) != null
        }
        return file().exists()
    }

    /** Deletes this file or empty directory and returns success. Will not delete a directory that has children.
     * @throws GdxRuntimeException if this file handle is a [FileType.Classpath] or [FileType.Internal] file.
     */
    open fun delete(): Boolean {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot delete a classpath file: $file")
        if (type == com.badlogic.gdx.Files.FileType.Internal) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot delete an internal file: $file")
        return file().delete()
    }

    /** Deletes this file or directory and all children, recursively.
     * @throws GdxRuntimeException if this file handle is a [FileType.Classpath] or [FileType.Internal] file.
     */
    open fun deleteDirectory(): Boolean {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot delete a classpath file: $file")
        if (type == com.badlogic.gdx.Files.FileType.Internal) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot delete an internal file: $file")
        return deleteDirectory(file())
    }
    /** Deletes all children of this directory, recursively. Optionally preserving the folder structure.
     * @throws GdxRuntimeException if this file handle is a [FileType.Classpath] or [FileType.Internal] file.
     */
    /** Deletes all children of this directory, recursively.
     * @throws GdxRuntimeException if this file handle is a [FileType.Classpath] or [FileType.Internal] file.
     */
    @JvmOverloads
    fun emptyDirectory(preserveTree: Boolean = false) {
        if (type == com.badlogic.gdx.Files.FileType.Classpath) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot delete a classpath file: $file")
        if (type == com.badlogic.gdx.Files.FileType.Internal) throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot delete an internal file: $file")
        emptyDirectory(file(), preserveTree)
    }

    /** Copies this file or directory to the specified file or directory. If this handle is a file, then 1) if the destination is a
     * file, it is overwritten, or 2) if the destination is a directory, this file is copied into it, or 3) if the destination
     * doesn't exist, [.mkdirs] is called on the destination's parent and this file is copied into it with a new name. If
     * this handle is a directory, then 1) if the destination is a file, GdxRuntimeException is thrown, or 2) if the destination is
     * a directory, this directory is copied into it recursively, overwriting existing files, or 3) if the destination doesn't
     * exist, [.mkdirs] is called on the destination and this directory is copied into it recursively.
     * @throws GdxRuntimeException if the destination file handle is a [FileType.Classpath] or [FileType.Internal]
     * file, or copying failed.
     */
    open fun copyTo(dest: FileHandle) {
        var dest = dest
        if (!isDirectory) {
            if (dest.isDirectory) dest = dest.child(name())
            copyFile(this, dest)
            return
        }
        if (dest.exists()) {
            if (!dest.isDirectory) throw com.badlogic.gdx.utils.GdxRuntimeException("Destination exists but is not a directory: $dest")
        } else {
            dest.mkdirs()
            if (!dest.isDirectory) throw com.badlogic.gdx.utils.GdxRuntimeException("Destination directory cannot be created: $dest")
        }
        copyDirectory(this, dest.child(name()))
    }

    /** Moves this file to the specified file, overwriting the file if it already exists.
     * @throws GdxRuntimeException if the source or destination file handle is a [FileType.Classpath] or
     * [FileType.Internal] file.
     */
    open fun moveTo(dest: FileHandle) {
        when (type) {
            com.badlogic.gdx.Files.FileType.Classpath -> throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot move a classpath file: $file")
            com.badlogic.gdx.Files.FileType.Internal -> throw com.badlogic.gdx.utils.GdxRuntimeException("Cannot move an internal file: $file")
            com.badlogic.gdx.Files.FileType.Absolute, com.badlogic.gdx.Files.FileType.External ->  // Try rename for efficiency and to change case on case-insensitive file systems.
                if (file().renameTo(dest.file())) return
        }
        copyTo(dest)
        delete()
        if (exists() && isDirectory) deleteDirectory()
    }

    /** Returns the length in bytes of this file, or 0 if this file is a directory, does not exist, or the size cannot otherwise be
     * determined.  */
    open fun length(): Long {
        if (type == com.badlogic.gdx.Files.FileType.Classpath || type == com.badlogic.gdx.Files.FileType.Internal && !file.exists()) {
            val input: java.io.InputStream = read()
            try {
                return input.available()
            } catch (ignored: java.lang.Exception) {
            } finally {
                com.badlogic.gdx.utils.StreamUtils.closeQuietly(input)
            }
            return 0
        }
        return file().length()
    }

    /** Returns the last modified time in milliseconds for this file. Zero is returned if the file doesn't exist. Zero is returned
     * for [FileType.Classpath] files. On Android, zero is returned for [FileType.Internal] files. On the desktop, zero
     * is returned for [FileType.Internal] files on the classpath.  */
    fun lastModified(): Long {
        return file().lastModified()
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is FileHandle) return false
        val other = obj
        return type == other.type && path() == other.path()
    }

    override fun hashCode(): Int {
        var hash = 1
        hash = hash * 37 + type.hashCode()
        hash = hash * 67 + path().hashCode()
        return hash
    }

    override fun toString(): String {
        return file.getPath().replace('\\', '/')
    }

    companion object {
        fun tempFile(prefix: String?): FileHandle {
            return try {
                FileHandle(java.io.File.createTempFile(prefix, null))
            } catch (ex: IOException) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Unable to create temp file.", ex)
            }
        }

        fun tempDirectory(prefix: String?): FileHandle {
            return try {
                val file: java.io.File = java.io.File.createTempFile(prefix, null)
                if (!file.delete()) throw IOException("Unable to delete temp file: $file")
                if (!file.mkdir()) throw IOException("Unable to create temp directory: $file")
                FileHandle(file)
            } catch (ex: IOException) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Unable to create temp file.", ex)
            }
        }

        private fun emptyDirectory(file: java.io.File?, preserveTree: Boolean) {
            if (file.exists()) {
                val files: Array<java.io.File> = file.listFiles()
                if (files != null) {
                    var i = 0
                    val n = files.size
                    while (i < n) {
                        if (!files[i].isDirectory()) files[i].delete() else if (preserveTree) emptyDirectory(files[i], true) else deleteDirectory(files[i])
                        i++
                    }
                }
            }
        }

        private fun deleteDirectory(file: java.io.File?): Boolean {
            emptyDirectory(file, false)
            return file.delete()
        }

        private fun copyFile(source: FileHandle?, dest: FileHandle) {
            try {
                dest.write(source!!.read(), false)
            } catch (ex: java.lang.Exception) {
                throw com.badlogic.gdx.utils.GdxRuntimeException("Error copying source file: " + source!!.file + " (" + source.type + ")\n" //
                    + "To destination: " + dest.file + " (" + dest.type + ")", ex)
            }
        }

        private fun copyDirectory(sourceDir: FileHandle?, destDir: FileHandle) {
            destDir.mkdirs()
            val files = sourceDir!!.list()
            var i = 0
            val n = files.size
            while (i < n) {
                val srcFile = files[i]
                val destFile = destDir.child(srcFile!!.name())
                if (srcFile.isDirectory) copyDirectory(srcFile, destFile) else copyFile(srcFile, destFile)
                i++
            }
        }
    }
}
