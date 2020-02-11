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

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.assets.loaders.FileHandleResolver
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution
import com.badlogic.gdx.files.FileHandle

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
open class ResolutionFileResolver(
    protected val baseResolver: FileHandleResolver,
    protected vararg val descriptors: Resolution
) : FileHandleResolver {

    /** Constructs a `Resolution`.
     * @param portraitWidth This resolution's width.
     * @param portraitHeight This resolution's height.
     * @param folder The name of the folder, where the assets which fit this resolution, are located.
     */
    class Resolution(
        val portraitWidth: Int,
        val portraitHeight: Int,
        /** The name of the folder, where the assets which fit this resolution, are located.  */
        val folder: String
    )


    /** Creates a `ResolutionFileResolver` based on a given [FileHandleResolver] and a list of [Resolution]s.
     * @param baseResolver The [FileHandleResolver] that will ultimately used to resolve the file.
     * @param descriptors A list of [Resolution]s. At least one has to be supplied.
     */
    init {
        require(descriptors.isNotEmpty())  { "At least one Resolution needs to be supplied." }
    }

    override fun resolve(fileName: String): FileHandle {
        val bestResolution = choose(*descriptors!!)
        val originalHandle: FileHandle = FileHandle(fileName)
        var handle: FileHandle = baseResolver!!.resolve(resolve(originalHandle, bestResolution!!.folder))
        if (!handle.exists()) handle = baseResolver!!.resolve(fileName)
        return handle
    }

    protected fun resolve(originalHandle: FileHandle, suffix: String): String {
        var parentString = ""
        val parent: FileHandle = originalHandle.parent()
        if (parent.name() != "") {
            parentString = "$parent/"
        }
        return parentString + suffix + "/" + originalHandle.name()
    }

    companion object {
        fun choose(vararg descriptors: Resolution?): Resolution? {
            val w: Int = Gdx.graphics.width
            val h: Int = Gdx.graphics.height
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
}
