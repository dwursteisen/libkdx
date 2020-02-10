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

/**
 * Options for [ServerSocket] instances.
 *
 * @author mzechner
 * @author noblemaster
 */
class ServerSocketHints {

    /**
     * The listen backlog length. Needs to be greater than 0, otherwise the system default is used. backlog is the maximum queue
     * length for incoming connection, i.e. maximum number of connections waiting for accept(...). If a connection indication
     * arrives when the queue is full, the connection is refused.
     */
    var backlog = 16

    /**
     * Performance preferences are described by three integers whose values indicate the relative importance of short connection
     * time, low latency, and high bandwidth. The absolute values of the integers are irrelevant; in order to choose a protocol the
     * values are simply compared, with larger values indicating stronger preferences. Negative values represent a lower priority
     * than positive values. If the application prefers short connection time over both low latency and high bandwidth, for
     * example, then it could invoke this method with the values (1, 0, 0). If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could invoke this method with the values (0, 1, 2).
     */
    var performancePrefConnectionTime = 0

    /**
     * See performancePrefConnectionTime for details.
     */
    var performancePrefLatency = 1 // low latency

    /**
     * See performancePrefConnectionTime for details.
     */
    var performancePrefBandwidth = 0

    /**
     * Enable/disable the SO_REUSEADDR socket option.
     */
    var reuseAddress = true

    /**
     * The SO_TIMEOUT in milliseconds for how long to wait during server.accept(). Enter 0 for infinite wait.
     */
    var acceptTimeout = 5000

    /**
     * The SO_RCVBUF (receive buffer) size in bytes for server.accept().
     */
    var receiveBufferSize = 4096
}
