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
package com.badlogic.gdx.assets.loaders

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.assets.loaders.ShaderProgramLoader.ShaderProgramParameter
import java.util.Locale

/** [AssetLoader] for [ShaderProgram] instances loaded from text files. If the file suffix is ".vert", it is assumed
 * to be a vertex shader, and a fragment shader is found using the same file name with a ".frag" suffix. And vice versa if the
 * file suffix is ".frag". These default suffixes can be changed in the ShaderProgramLoader constructor.
 *
 *
 * For all other file suffixes, the same file is used for both (and therefore should internally distinguish between the programs
 * using preprocessor directives and [ShaderProgram.prependVertexCode] and [ShaderProgram.prependFragmentCode]).
 *
 *
 * The above default behavior for finding the files can be overridden by explicitly setting the file names in a
 * [ShaderProgramParameter]. The parameter can also be used to prepend code to the programs.
 * @author cypherdare
 */
class ShaderProgramLoader : com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader<com.badlogic.gdx.graphics.glutils.ShaderProgram?, ShaderProgramParameter?> {

    private var vertexFileSuffix: String? = ".vert"
    private var fragmentFileSuffix: String? = ".frag"

    constructor(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) : super(resolver) {}
    constructor(resolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?, vertexFileSuffix: String?, fragmentFileSuffix: String?) : super(resolver) {
        this.vertexFileSuffix = vertexFileSuffix
        this.fragmentFileSuffix = fragmentFileSuffix
    }

    override fun getDependencies(fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: ShaderProgramParameter?): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>?>? {
        return null
    }

    override fun loadAsync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: ShaderProgramParameter?) {}
    override fun loadSync(manager: com.badlogic.gdx.assets.AssetManager?, fileName: String?, file: com.badlogic.gdx.files.FileHandle?, parameter: ShaderProgramParameter?): com.badlogic.gdx.graphics.glutils.ShaderProgram? {
        var vertFileName: String? = null
        var fragFileName: String? = null
        if (parameter != null) {
            if (parameter.vertexFile != null) vertFileName = parameter.vertexFile
            if (parameter.fragmentFile != null) fragFileName = parameter.fragmentFile
        }
        if (vertFileName == null && fileName!!.endsWith(fragmentFileSuffix!!)) {
            vertFileName = fileName.substring(0, fileName.length - fragmentFileSuffix!!.length) + vertexFileSuffix
        }
        if (fragFileName == null && fileName!!.endsWith(vertexFileSuffix!!)) {
            fragFileName = fileName.substring(0, fileName.length - vertexFileSuffix!!.length) + fragmentFileSuffix
        }
        val vertexFile: com.badlogic.gdx.files.FileHandle = vertFileName?.let { resolve(it) } ?: file
        val fragmentFile: com.badlogic.gdx.files.FileHandle = fragFileName?.let { resolve(it) } ?: file
        var vertexCode: String = vertexFile.readString()
        var fragmentCode = if (vertexFile == fragmentFile) vertexCode else fragmentFile.readString()
        if (parameter != null) {
            if (parameter.prependVertexCode != null) vertexCode = parameter.prependVertexCode + vertexCode
            if (parameter.prependFragmentCode != null) fragmentCode = parameter.prependFragmentCode + fragmentCode
        }
        val shaderProgram: com.badlogic.gdx.graphics.glutils.ShaderProgram = com.badlogic.gdx.graphics.glutils.ShaderProgram(vertexCode, fragmentCode)
        if ((parameter == null || parameter.logOnCompileFailure) && !shaderProgram.isCompiled()) {
            manager.getLogger().error("ShaderProgram " + fileName + " failed to compile:\n" + shaderProgram.getLog())
        }
        return shaderProgram
    }

    class ShaderProgramParameter : AssetLoaderParameters<com.badlogic.gdx.graphics.glutils.ShaderProgram?>() {
        /** File name to be used for the vertex program instead of the default determined by the file name used to submit this asset
         * to AssetManager.  */
        var vertexFile: String? = null
        /** File name to be used for the fragment program instead of the default determined by the file name used to submit this
         * asset to AssetManager.  */
        var fragmentFile: String? = null
        /** Whether to log (at the error level) the shader's log if it fails to compile. Default true.  */
        var logOnCompileFailure = true
        /** Code that is always added to the vertex shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. [ShaderProgram.prependVertexCode] is placed before this code.  */
        var prependVertexCode: String? = null
        /** Code that is always added to the fragment shader code. This is added as-is, and you should include a newline (`\n`) if
         * needed. [ShaderProgram.prependFragmentCode] is placed before this code.  */
        var prependFragmentCode: String? = null
    }
}
