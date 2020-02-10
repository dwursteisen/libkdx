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
 * Options for [Socket] instances.
 *
 * @author mzechner
 * @author noblemaster
 */
class SocketHints {

    /**
     * The connection timeout in milliseconds. Not used for sockets created via server.accept().
     */
    var connectTimeout = 5000

    /**
     * Performance preferences are described by three integers whose values indicate the relative importance of short connection
     * time, low latency, and high bandwidth. The absolute values of the integers are irrelevant; in order to choose a protocol the
     * values are simply compared, with larger values indicating stronger preferences. Negative values represent a lower priority
     * than positive values. If the application prefers short connection time over both low latency and high bandwidth, for
     * example, then it could invoke this method with the values (1, 0, 0). If the application prefers high bandwidth above low
     * latency, and low latency above short connection time, then it could invoke this method with the values (0, 1, 2).
     */
    var performancePrefConnectionTime = 0
    var performancePrefLatency = 1 // low latency
    var performancePrefBandwidth = 0

    /**
     * The traffic class describes the type of connection that shall be established. The traffic class must be in the range 0 <=
     * trafficClass <= 255.
     *
     *
     * The traffic class is bitset created by bitwise-or'ing values such the following :
     *
     *  * IPTOS_LOWCOST (0x02) - cheap!
     *  * IPTOS_RELIABILITY (0x04) - reliable connection with little package loss.
     *  * IPTOS_THROUGHPUT (0x08) - lots of data being sent.
     *  * IPTOS_LOWDELAY (0x10) - low delay.
     *
     */
    var trafficClass = 0x14 // low delay + reliable

    /**
     * True to enable SO_KEEPALIVE.
     */
    var keepAlive = true

    /**
     * True to enable TCP_NODELAY (disable/enable Nagle's algorithm).
     */
    var tcpNoDelay = true

    /**
     * The SO_SNDBUF (send buffer) size in bytes.
     */
    var sendBufferSize = 4096

    /**
     * The SO_RCVBUF (receive buffer) size in bytes.
     */
    var receiveBufferSize = 4096

    /**
     * Enable/disable SO_LINGER with the specified linger time in seconds. Only affects socket close.
     */
    var linger = false

    /**
     * The linger duration in seconds (NOT milliseconds!). Only used if linger is true!
     */
    var lingerDuration = 0

    /**
     * Enable/disable SO_TIMEOUT with the specified timeout, in milliseconds. With this option set to a non-zero timeout, a read()
     * call on the InputStream associated with this Socket will block for only this amount of time
     */
    var socketTimeout = 0
}
