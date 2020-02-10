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
import com.badlogic.gdx.utils.GdxRuntimeException
import java.net.InetSocketAddress

/**
 * Server socket implementation using java.net.ServerSocket.
 *
 * @author noblemaster
 */
class NetJavaServerSocketImpl(override val protocol: Net.Protocol?, hostname: String?, port: Int, hints: ServerSocketHints?) : ServerSocket {

    /**
     * Our server or null for disposed, aka closed.
     */
    private var server: java.net.ServerSocket? = null

    constructor(protocol: Net.Protocol?, port: Int, hints: ServerSocketHints?) : this(protocol, null, port, hints) {}

    override fun accept(hints: SocketHints?): Socket? {
        return try {
            NetJavaSocketImpl(server.accept(), hints)
        } catch (e: java.lang.Exception) {
            throw GdxRuntimeException("Error accepting socket.", e)
        }
    }

    override fun dispose() {
        if (server != null) {
            server = try {
                server.close()
                null
            } catch (e: java.lang.Exception) {
                throw GdxRuntimeException("Error closing server.", e)
            }
        }
    }

    init {

        // create the server socket
        try {
            // initialize
            server = java.net.ServerSocket()
            if (hints != null) {
                server.setPerformancePreferences(hints.performancePrefConnectionTime, hints.performancePrefLatency,
                    hints.performancePrefBandwidth)
                server.setReuseAddress(hints.reuseAddress)
                server.setSoTimeout(hints.acceptTimeout)
                server.setReceiveBufferSize(hints.receiveBufferSize)
            }

            // and bind the server...
            val address: InetSocketAddress
            if (hostname != null) {
                address = InetSocketAddress(hostname, port)
            } else {
                address = InetSocketAddress(port)
            }
            if (hints != null) {
                server.bind(address, hints.backlog)
            } else {
                server.bind(address)
            }
        } catch (e: java.lang.Exception) {
            throw GdxRuntimeException("Cannot create a server socket at port $port.", e)
        }
    }
}
