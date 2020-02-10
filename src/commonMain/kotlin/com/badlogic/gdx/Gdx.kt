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

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.GL30

/** Environment class holding references to the [Application], [Graphics], [Audio], [Files] and
 * [Input] instances. The references are held in public static fields which allows static access to all sub systems. Do not
 * use Graphics in a thread that is not the rendering thread.
 *
 *
 * This is normally a design faux pas but in this case is better than the alternatives.
 * @author mzechner
 */
object Gdx {

    lateinit var app: Application
    lateinit var graphics: Graphics
    lateinit var audio: Audio
    lateinit var input: Input
    lateinit var files: Files
    lateinit var net: Net
    lateinit var gl: GL20
    lateinit var gl20: GL20
    lateinit var gl30: GL30
}
