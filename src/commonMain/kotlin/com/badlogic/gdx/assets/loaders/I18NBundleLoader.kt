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
import com.badlogic.gdx.files.FileHandle

/** [AssetLoader] for [I18NBundle] instances. The I18NBundle is loaded asynchronously.
 *
 *
 * Notice that you can't load two bundles with the same base name and different locale or encoding using the same [AssetManager].
 * For example, if you try to load the 2 bundles below
 *
 * <pre>
 * manager.load(&quot;i18n/message&quot;, I18NBundle.class, new I18NBundleParameter(Locale.ITALIAN));
 * manager.load(&quot;i18n/message&quot;, I18NBundle.class, new I18NBundleParameter(Locale.ENGLISH));
</pre> *
 *
 * the English bundle won't be loaded because the asset manager thinks they are the same bundle since they have the same name.
 * There are 2 use cases:
 *
 *  * If you want to load the English bundle so to replace the Italian bundle you have to unload the Italian bundle first.
 *  * If you want to load the English bundle without replacing the Italian bundle you should use another asset manager.
 *
 * @author davebaol
 */
class I18NBundleLoader(resolver: FileHandleResolver?) : AsynchronousAssetLoader<com.badlogic.gdx.utils.I18NBundle, I18NBundleLoader.I18NBundleParameter>(resolver) {

    var bundle: com.badlogic.gdx.utils.I18NBundle? = null

    override fun loadAsync(
        manager: com.badlogic.gdx.assets.AssetManager?,
        fileName: String,
        file: FileHandle,
        parameter: I18NBundleParameter?
    ) {
        bundle = null
        val locale: Locale
        val encoding: String?
        if (parameter == null) {
            locale = Locale.getDefault()
            encoding = null
        } else {
            locale = if (parameter.locale == null) Locale.getDefault() else parameter.locale
            encoding = parameter.encoding
        }
        if (encoding == null) {
            bundle = com.badlogic.gdx.utils.I18NBundle.createBundle(file, locale)
        } else {
            bundle = com.badlogic.gdx.utils.I18NBundle.createBundle(file, locale, encoding)
        }
    }

    override fun loadSync(
        manager: com.badlogic.gdx.assets.AssetManager,
        fileName: String,
        file: FileHandle,
        parameter: I18NBundleParameter?
    ): com.badlogic.gdx.utils.I18NBundle {
        val bundle: com.badlogic.gdx.utils.I18NBundle? = bundle
        this.bundle = null
        return bundle
    }

    override fun getDependencies(
        fileName: String,
        file: FileHandle,
        parameter: I18NBundleParameter?
    ): com.badlogic.gdx.utils.Array<com.badlogic.gdx.assets.AssetDescriptor<*>>? {
        return null
    }

    class I18NBundleParameter @JvmOverloads constructor(locale: Locale? = null, encoding: String? = null) : AssetLoaderParameters<com.badlogic.gdx.utils.I18NBundle?>() {
        val locale: Locale?
        val encoding: String?

        init {
            this.locale = locale
            this.encoding = encoding
        }
    }
}
