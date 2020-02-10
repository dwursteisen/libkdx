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
 * Socket implementation using java.net.Socket.
 *
 * @author noblemaster
 */
class NetJavaSocketImpl : Socket {

    /**
     * Our socket or null for disposed, aka closed.
     */
    private var socket: java.net.Socket? = null

    constructor(protocol: Net.Protocol?, host: String, port: Int, hints: SocketHints?) {
        try {
            // create the socket
            socket = java.net.Socket()
            applyHints(hints) // better to call BEFORE socket is connected!

            // and connect...
            val address = InetSocketAddress(host, port)
            if (hints != null) {
                socket.connect(address, hints.connectTimeout)
            } else {
                socket.connect(address)
            }
        } catch (e: java.lang.Exception) {
            throw GdxRuntimeException("Error making a socket connection to $host:$port", e)
        }
    }

    constructor(socket: java.net.Socket?, hints: SocketHints?) {
        this.socket = socket
        applyHints(hints)
    }

    private fun applyHints(hints: SocketHints?) {
        if (hints != null) {
            try {
                socket.setPerformancePreferences(hints.performancePrefConnectionTime, hints.performancePrefLatency,
                    hints.performancePrefBandwidth)
                socket.setTrafficClass(hints.trafficClass)
                socket.setTcpNoDelay(hints.tcpNoDelay)
                socket.setKeepAlive(hints.keepAlive)
                socket.setSendBufferSize(hints.sendBufferSize)
                socket.setReceiveBufferSize(hints.receiveBufferSize)
                socket.setSoLinger(hints.linger, hints.lingerDuration)
                socket.setSoTimeout(hints.socketTimeout)
            } catch (e: java.lang.Exception) {
                throw GdxRuntimeException("Error setting socket hints.", e)
            }
        }
    }

    override val isConnected: Boolean
        get() = if (socket != null) {
            socket.isConnected()
        } else {
            false
        }

    override val inputStream: java.io.InputStream
        get() = try {
            socket.getInputStream()
        } catch (e: java.lang.Exception) {
            throw GdxRuntimeException("Error getting input stream from socket.", e)
        }

    override val outputStream: java.io.OutputStream
        get() = try {
            socket.getOutputStream()
        } catch (e: java.lang.Exception) {
            throw GdxRuntimeException("Error getting output stream from socket.", e)
        }

    override val remoteAddress: String
        get() = socket.getRemoteSocketAddress().toString()

    override fun dispose() {
        if (socket != null) {
            socket = try {
                socket.close()
                null
            } catch (e: java.lang.Exception) {
                throw GdxRuntimeException("Error closing socket.", e)
            }
        }
    }
}
