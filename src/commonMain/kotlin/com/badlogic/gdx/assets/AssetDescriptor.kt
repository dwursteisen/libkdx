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
package com.badlogic.gdx.assets

import com.badlogic.gdx.files.FileHandle
import kotlin.jvm.JvmField

/** Describes an asset to be loaded by its filename, type and [AssetLoaderParameters]. Instances of this are used in
 * [AssetLoadingTask] to load the actual asset.
 * @author mzechner
 */
class AssetDescriptor<T> {

    @JvmField
    val fileName: String?
    @JvmField
    val type: java.lang.Class<T?>?
    @JvmField
    val params: AssetLoaderParameters<*>?
    /** The resolved file. May be null if the fileName has not been resolved yet.  */
    @JvmField
    var file: FileHandle? = null

    @JvmOverloads
    constructor(fileName: String?, assetType: java.lang.Class<T?>?, params: AssetLoaderParameters<T?>? = null) {
        this.fileName = fileName!!.replace("\\\\".toRegex(), "/")
        type = assetType
        this.params = params
    }
    /** Creates an AssetDescriptor with an already resolved name.  */
    /** Creates an AssetDescriptor with an already resolved name.  */
    @JvmOverloads
    constructor(file: FileHandle?, assetType: java.lang.Class<T?>?, params: AssetLoaderParameters<T?>? = null) {
        fileName = file!!.path().replace("\\\\".toRegex(), "/")
        this.file = file
        type = assetType
        this.params = params
    }

    override fun toString(): String {
        val sb: java.lang.StringBuilder = java.lang.StringBuilder()
        sb.append(fileName)
        sb.append(", ")
        sb.append(type.getName())
        return sb.toString()
    }
}
