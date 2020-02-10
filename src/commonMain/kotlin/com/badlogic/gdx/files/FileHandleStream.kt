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

/** A FileHandle intended to be subclassed for the purpose of implementing [.read] and/or [.write]. Methods
 * that would manipulate the file instead throw UnsupportedOperationException.
 * @author Nathan Sweet
 */
abstract class FileHandleStream
/** Create an [FileType.Absolute] file at the given location.  */(path: String?) : com.badlogic.gdx.files.FileHandle(java.io.File(path), com.badlogic.gdx.Files.FileType.Absolute) {

    override val isDirectory: Boolean
        get() = false

    override fun length(): Long {
        return 0
    }

    override fun exists(): Boolean {
        return true
    }

    override fun child(name: String?): com.badlogic.gdx.files.FileHandle {
        throw UnsupportedOperationException()
    }

    override fun sibling(name: String?): com.badlogic.gdx.files.FileHandle? {
        throw UnsupportedOperationException()
    }

    override fun parent(): com.badlogic.gdx.files.FileHandle {
        throw UnsupportedOperationException()
    }

    override fun read(): java.io.InputStream {
        throw UnsupportedOperationException()
    }

    override fun write(overwrite: Boolean): java.io.OutputStream {
        throw UnsupportedOperationException()
    }

    override fun list(): Array<com.badlogic.gdx.files.FileHandle?> {
        throw UnsupportedOperationException()
    }

    override fun mkdirs() {
        throw UnsupportedOperationException()
    }

    override fun delete(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun deleteDirectory(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun copyTo(dest: com.badlogic.gdx.files.FileHandle) {
        throw UnsupportedOperationException()
    }

    override fun moveTo(dest: com.badlogic.gdx.files.FileHandle) {
        throw UnsupportedOperationException()
    }
}
