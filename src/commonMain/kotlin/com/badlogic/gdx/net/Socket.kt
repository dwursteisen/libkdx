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

import com.badlogic.gdx.utils.Disposable

/**
 * A client socket that talks to a server socket via some [Protocol]. See
 * [Net.newClientSocket] and
 * [Net.newServerSocket].
 *
 *
 * A socket has an [InputStream] used to send data to the other end of the connection, and an [OutputStream] to
 * receive data from the other end of the connection.
 *
 *
 * A socket needs to be disposed if it is no longer used. Disposing also closes the connection.
 *
 * @author mzechner
 */
interface Socket : Disposable {

    /**
     * @return whether the socket is connected
     */
    val isConnected: Boolean

    /**
     * @return the [InputStream] used to read data from the other end of the connection.
     */
    val inputStream: java.io.InputStream?

    /**
     * @return the [OutputStream] used to write data to the other end of the connection.
     */
    val outputStream: java.io.OutputStream?

    /**
     * @return the RemoteAddress of the Socket as String
     */
    val remoteAddress: String?
}
