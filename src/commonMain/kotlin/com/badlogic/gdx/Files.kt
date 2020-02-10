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
package com.badlogic.gdx

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException

/** Provides standard access to the filesystem, classpath, Android SD card, and Android assets directory.
 * @author mzechner
 * @author Nathan Sweet
 */
interface Files {

    /** Indicates how to resolve a path to a file.
     * @author mzechner
     * @author Nathan Sweet
     */
    enum class FileType {

        /** Path relative to the root of the classpath. Classpath files are always readonly. Note that classpath files are not
         * compatible with some functionality on Android, such as [Audio.newSound] and
         * [Audio.newMusic].  */
        Classpath,

        /** Path relative to the asset directory on Android and to the application's root directory on the desktop. On the desktop,
         * if the file is not found, then the classpath is checked. This enables files to be found when using JWS or applets.
         * Internal files are always readonly.  */
        Internal,

        /** Path relative to the root of the SD card on Android and to the home directory of the current user on the desktop.  */
        External,

        /** Path that is a fully qualified, absolute filesystem path. To ensure portability across platforms use absolute files only
         * when absolutely (heh) necessary.  */
        Absolute,

        /** Path relative to the private files directory on Android and to the application's root directory on the desktop.  */
        Local
    }

    /** Returns a handle representing a file or directory.
     * @param type Determines how the path is resolved.
     * @throws GdxRuntimeException if the type is classpath or internal and the file does not exist.
     * @see FileType
     */
    fun getFileHandle(path: String?, type: FileType?): FileHandle?

    /** Convenience method that returns a [FileType.Classpath] file handle.  */
    fun classpath(path: String?): FileHandle?

    /** Convenience method that returns a [FileType.Internal] file handle.  */
    fun internal(path: String?): FileHandle?

    /** Convenience method that returns a [FileType.External] file handle.  */
    fun external(path: String?): FileHandle?

    /** Convenience method that returns a [FileType.Absolute] file handle.  */
    fun absolute(path: String?): FileHandle?

    /** Convenience method that returns a [FileType.Local] file handle.  */
    fun local(path: String?): FileHandle?

    /** Returns the external storage path directory. This is the SD card on Android and the home directory of the current user on
     * the desktop.  */
    val externalStoragePath: String?

    /** Returns true if the external storage is ready for file IO. Eg, on Android, the SD card is not available when mounted for use
     * with a PC.  */
    val isExternalStorageAvailable: Boolean

    /** Returns the local storage path directory. This is the private files directory on Android and the directory of the jar on the
     * desktop.  */
    val localStoragePath: String?

    /** Returns true if the local storage is ready for file IO.  */
    val isLocalStorageAvailable: Boolean
}
