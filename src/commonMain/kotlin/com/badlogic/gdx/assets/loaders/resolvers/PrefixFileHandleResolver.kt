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
package com.badlogic.gdx.assets.loaders.resolvers

import java.util.Locale

/** [FileHandleResolver] that adds a prefix to the filename before passing it to the base resolver. Can be used e.g. to use a
 * given subfolder from the base resolver. The prefix is added as is, you have to include any trailing '/' character if needed.
 * @author Xoppa
 */
class PrefixFileHandleResolver(baseResolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?, prefix: String?) : com.badlogic.gdx.assets.loaders.FileHandleResolver {

    var prefix: String?
    private var baseResolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?
    fun setBaseResolver(baseResolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?) {
        this.baseResolver = baseResolver
    }

    fun getBaseResolver(): com.badlogic.gdx.assets.loaders.FileHandleResolver? {
        return baseResolver
    }

    override fun resolve(fileName: String?): com.badlogic.gdx.files.FileHandle? {
        return baseResolver!!.resolve(prefix + fileName)
    }

    init {
        this.baseResolver = baseResolver
        this.prefix = prefix
    }
}
