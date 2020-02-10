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

import com.badlogic.gdx.Net.HttpResponseListener
import com.badlogic.gdx.Net.Protocol
import java.util.HashMap
import kotlin.jvm.Throws

/** A LifecycleListener can be added to an [Application] via [Application.addLifecycleListener]. It
 * will receive notification of pause, resume and dispose events. This is mainly meant to be used by extensions that need to
 * manage resources based on the life-cycle. Normal, application level development should rely on the [ApplicationListener]
 * interface.
 *
 * The methods will be invoked on the rendering thread. The methods will be executed before the [ApplicationListener]
 * methods are executed.
 *
 * @author mzechner
 */
interface LifecycleListener {

    /** Called when the [Application] is about to pause  */
    fun pause()

    /** Called when the Application is about to be resumed  */
    fun resume()

    /** Called when the [Application] is about to be disposed  */
    fun dispose()
}
