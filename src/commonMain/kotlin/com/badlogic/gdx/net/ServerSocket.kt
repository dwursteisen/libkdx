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
package com.badlogic.gdx.net

import com.badlogic.gdx.Net
import com.badlogic.gdx.utils.Disposable

/**
 * A server socket that accepts new incoming connections, returning [Socket] instances. The [.accept]
 * method should preferably be called in a separate thread as it is blocking.
 *
 * @author mzechner
 * @author noblemaster
 */
interface ServerSocket : Disposable {

    /**
     * @return the Protocol used by this socket
     */
    val protocol: Net.Protocol?

    /**
     * Accepts a new incoming connection from a client [Socket]. The given hints will be applied to the accepted socket.
     * Blocking, call on a separate thread.
     *
     * @param hints additional [SocketHints] applied to the accepted [Socket]. Input null to use the default setting
     * provided by the system.
     * @return the accepted [Socket]
     * @throws GdxRuntimeException in case an error occurred
     */
    fun accept(hints: SocketHints?): Socket?
}
