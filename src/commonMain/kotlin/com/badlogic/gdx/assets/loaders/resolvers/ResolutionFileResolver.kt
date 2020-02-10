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

import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution
import java.util.Locale

/** This [FileHandleResolver] uses a given list of [Resolution]s to determine the best match based on the current
 * Screen size. An example of how this resolver works:
 *
 *
 *
 * Let's assume that we have only a single [Resolution] added to this resolver. This resolution has the following
 * properties:
 *
 *
 *
 *  * `portraitWidth = 1920`
 *  * `portraitHeight = 1080`
 *  * `folder = "1920x1080"`
 *
 *
 *
 *
 * One would now supply a file to be found to the resolver. For this example, we assume it is "`textures/walls/brick.png`".
 * Since there is only a single [Resolution], this will be the best match for any screen size. The resolver will now try to
 * find the file in the following ways:
 *
 *
 *
 *  * `"textures/walls/1920x1080/brick.png"`
 *  * `"textures/walls/brick.png"`
 *
 *
 *
 *
 * The files are ultimately resolved via the given {[.baseResolver]. In case the first version cannot be resolved, the
 * fallback will try to search for the file without the resolution folder.
 *   */
class ResolutionFileResolver(baseResolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?, vararg descriptors: Resolution?) : com.badlogic.gdx.assets.loaders.FileHandleResolver {

    class Resolution
    /** Constructs a `Resolution`.
     * @param portraitWidth This resolution's width.
     * @param portraitHeight This resolution's height.
     * @param folder The name of the folder, where the assets which fit this resolution, are located.
     */(val portraitWidth: Int, val portraitHeight: Int,
        /** The name of the folder, where the assets which fit this resolution, are located.  */
        val folder: String?)

    protected val baseResolver: com.badlogic.gdx.assets.loaders.FileHandleResolver?
    protected val descriptors: Array<Resolution?>?
    override fun resolve(fileName: String?): com.badlogic.gdx.files.FileHandle? {
        val bestResolution = choose(*descriptors!!)
        val originalHandle: com.badlogic.gdx.files.FileHandle = com.badlogic.gdx.files.FileHandle(fileName)
        var handle: com.badlogic.gdx.files.FileHandle = baseResolver!!.resolve(resolve(originalHandle, bestResolution!!.folder))
        if (!handle.exists()) handle = baseResolver!!.resolve(fileName)
        return handle
    }

    protected fun resolve(originalHandle: com.badlogic.gdx.files.FileHandle?, suffix: String?): String? {
        var parentString = ""
        val parent: com.badlogic.gdx.files.FileHandle = originalHandle.parent()
        if (parent != null && parent.name() != "") {
            parentString = parent.toString() + "/"
        }
        return parentString + suffix + "/" + originalHandle.name()
    }

    companion object {
        fun choose(vararg descriptors: Resolution?): Resolution? {
            val w: Int = com.badlogic.gdx.Gdx.graphics.getWidth()
            val h: Int = com.badlogic.gdx.Gdx.graphics.getHeight()
            // Prefer the shortest side.
            var best = descriptors[0]
            if (w < h) {
                var i = 0
                val n = descriptors.size
                while (i < n) {
                    val other = descriptors[i]
                    if (w >= other!!.portraitWidth && other.portraitWidth >= best!!.portraitWidth && h >= other.portraitHeight && other.portraitHeight >= best.portraitHeight) best = descriptors[i]
                    i++
                }
            } else {
                var i = 0
                val n = descriptors.size
                while (i < n) {
                    val other = descriptors[i]
                    if (w >= other!!.portraitHeight && other.portraitHeight >= best!!.portraitHeight && h >= other.portraitWidth && other.portraitWidth >= best.portraitWidth) best = descriptors[i]
                    i++
                }
            }
            return best
        }
    }

    /** Creates a `ResolutionFileResolver` based on a given [FileHandleResolver] and a list of [Resolution]s.
     * @param baseResolver The [FileHandleResolver] that will ultimately used to resolve the file.
     * @param descriptors A list of [Resolution]s. At least one has to be supplied.
     */
    init {
        if (descriptors.size == 0) throw java.lang.IllegalArgumentException("At least one Resolution needs to be supplied.")
        this.baseResolver = baseResolver
        this.descriptors = descriptors
    }
}
