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

import com.badlogic.gdx.Application.ApplicationType
import com.badlogic.gdx.Net.HttpMethods
import com.badlogic.gdx.Net.HttpRequest
import com.badlogic.gdx.Net.HttpResponse
import com.badlogic.gdx.Net.HttpResponseListener
import com.badlogic.gdx.net.HttpRequestHeader
import com.badlogic.gdx.net.HttpResponseHeader
import com.badlogic.gdx.net.HttpStatus
import com.badlogic.gdx.net.ServerSocket
import com.badlogic.gdx.net.ServerSocketHints
import com.badlogic.gdx.net.Socket
import com.badlogic.gdx.net.SocketHints
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool.Poolable

/** Provides methods to perform networking operations, such as simple HTTP get and post requests, and TCP server/client socket
 * communication.
 *
 * To perform an HTTP request create a [HttpRequest] with the HTTP method (see [HttpMethods] for common methods) and
 * invoke [.sendHttpRequest] with it and a [HttpResponseListener]. After the HTTP
 * request was processed, the [HttpResponseListener] is called with a [HttpResponse] with the HTTP response values and
 * an status code to determine if the request was successful or not.
 *
 * To create a TCP client socket to communicate with a remote TCP server, invoke the
 * [.newClientSocket] method. The returned [Socket] offers an [InputStream]
 * and [OutputStream] to communicate with the end point.
 *
 * To create a TCP server socket that waits for incoming connections, invoke the
 * [.newServerSocket] method. The returned [ServerSocket] offers an
 * [ServerSocket.accept] method that waits for an incoming connection.
 *
 * @author mzechner
 * @author noblemaster
 * @author arielsan
 */
interface Net {

    /** HTTP response interface with methods to get the response data as a byte[], a [String] or an [InputStream].  */
    interface HttpResponse {

        /** Returns the data of the HTTP response as a byte[].
         *
         *
         * **Note**: This method may only be called once per response.
         *
         * @return the result as a byte[] or null in case of a timeout or if the operation was canceled/terminated abnormally. The
         * timeout is specified when creating the HTTP request, with [HttpRequest.setTimeOut]
         */
        val result: ByteArray?

        /** Returns the data of the HTTP response as a [String].
         *
         *
         * **Note**: This method may only be called once per response.
         *
         * @return the result as a string or null in case of a timeout or if the operation was canceled/terminated abnormally. The
         * timeout is specified when creating the HTTP request, with [HttpRequest.setTimeOut]
         */
        val resultAsString: String?

        /** Returns the data of the HTTP response as an [InputStream]. **<br></br>
         * Warning:** Do not store a reference to this InputStream outside of
         * [HttpResponseListener.handleHttpResponse]. The underlying HTTP connection will be closed after that
         * callback finishes executing. Reading from the InputStream after it's connection has been closed will lead to exception.
         * @return An [InputStream] with the [HttpResponse] data.
         */
        val resultAsStream: java.io.InputStream?

        /** Returns the [HttpStatus] containing the statusCode of the HTTP response.  */
        val status: HttpStatus?

        /** Returns the value of the header with the given name as a [String], or null if the header is not set. See
         * [HttpResponseHeader].  */
        fun getHeader(name: String?): String?

        /** Returns a Map of the headers. The keys are Strings that represent the header name. Each values is a List of Strings that
         * represent the corresponding header values. See [HttpResponseHeader].  */
        val headers: Map<String?, List<String?>?>?
    }

    /** Provides common HTTP methods to use when creating a [HttpRequest].
     *
     *  * GET
     *  * POST
     *  * PUT
     *  * DELETE
     *  * PATCH
     *   */
    interface HttpMethods {

        companion object {
            const val GET = "GET"
            const val POST = "POST"
            const val PUT = "PUT"
            const val DELETE = "DELETE"
            const val PATCH = "PATCH"
        }
    }

    /** Contains getters and setters for the following parameters:
     *
     *  * **httpMethod:** GET or POST are most common, can use [HttpMethods][Net.HttpMethods] for static
     * references
     *  * **url:** the url
     *  * **headers:** a map of the headers, setter can be called multiple times
     *  * **timeout:** time spent trying to connect before giving up
     *  * **content:** A string containing the data to be used when processing the HTTP request.
     *
     *
     * Abstracts the concept of a HTTP Request:
     *
     * <pre>
     * Map<String></String>, String> parameters = new HashMap<String></String>, String>();
     * parameters.put("user", "myuser");
     *
     * HttpRequest httpGet = new HttpRequest(HttpMethods.Get);
     * httpGet.setUrl("http://somewhere.net");
     * httpGet.setContent(HttpParametersUtils.convertHttpParameters(parameters));
     * ...
     * Gdx.net.sendHttpRequest (httpGet, new HttpResponseListener() {
     * public void handleHttpResponse(HttpResponse httpResponse) {
     * status = httpResponse.getResultAsString();
     * //do stuff here based on response
     * }
     *
     * public void failed(Throwable t) {
     * status = "failed";
     * //do stuff here based on the failed attempt
     * }
     * });
    </pre> *   */
    class HttpRequest() : Poolable {

        /** Returns the HTTP method of the HttpRequest.  */
        /** Sets the HTTP method of the HttpRequest.  */
        var method: String? = null
        /** Returns the URL of the HTTP request.  */
        /** Sets the URL of the HTTP request.
         * @param url The URL to set.
         */
        var url: String? = null
        private val headers: MutableMap<String, String>
        /** Returns the timeOut of the HTTP request.
         * @return the timeOut.
         */
        /** Sets the time to wait for the HTTP request to be processed, use 0 block until it is done. The timeout is used for both
         * the timeout when establishing TCP connection, and the timeout until the first byte of data is received.
         * @param timeOut the number of milliseconds to wait before giving up, 0 or negative to block until the operation is done
         */
        var timeOut = 0
        /** Returns the content string to be used for the HTTP request.  */
        /** Sets the content to be used in the HTTP request.
         * @param content A string encoded in the corresponding Content-Encoding set in the headers, with the data to send with the
         * HTTP request. For example, in case of HTTP GET, the content is used as the query string of the GET while on a
         * HTTP POST it is used to send the POST data.
         */
        var content: String? = null
        private var contentStream: java.io.InputStream? = null
        /** Returns the content length in case content is a stream.  */
        var contentLength: Long = 0
            private set
        private var followRedirects = true
        /** Returns whether a cross-origin request will include credentials. By default false.  */
        /** Sets whether a cross-origin request will include credentials. Only used on GWT backend to allow cross-origin requests
         * to include credentials such as cookies, authorization headers, etc...  */
        var includeCredentials = false

        /** Creates a new HTTP request with the specified HTTP method, see [HttpMethods].
         * @param httpMethod This is the HTTP method for the request, see [HttpMethods]
         */
        constructor(httpMethod: String?) : this() {
            method = httpMethod
        }

        /** Sets a header to this HTTP request, see [HttpRequestHeader].
         * @param name the name of the header.
         * @param value the value of the header.
         */
        fun setHeader(name: String, value: String) {
            headers[name] = value
        }

        /** Sets the content as a stream to be used for a POST for example, to transmit custom data.
         * @param contentStream The stream with the content data.
         */
        fun setContent(contentStream: java.io.InputStream?, contentLength: Long) {
            this.contentStream = contentStream
            this.contentLength = contentLength
        }

        /** Sets whether 301 and 302 redirects are followed. By default true. Can't be changed in the GWT backend because this uses
         * XmlHttpRequests which always redirect.
         * @param followRedirects whether to follow redirects.
         * @exception IllegalArgumentException if redirection is disabled on the GWT backend.
         */
        fun setFollowRedirects(followRedirects: Boolean) {
            if (followRedirects || Gdx.app.type != ApplicationType.WebGL) {
                this.followRedirects = followRedirects
            } else {
                throw IllegalArgumentException("Following redirects can't be disabled using the GWT/WebGL backend!")
            }
        }

        /** Returns the content stream.  */
        fun getContentStream(): java.io.InputStream? {
            return contentStream
        }

        /** Returns a Map<String></String>, String> with the headers of the HTTP request.  */
        fun getHeaders(): Map<String, String> {
            return headers
        }

        /** Returns whether 301 and 302 redirects are followed. By default true. Whether to follow redirects.  */
        fun getFollowRedirects(): Boolean {
            return followRedirects
        }

        override fun reset() {
            method = null
            url = null
            headers.clear()
            timeOut = 0
            content = null
            contentStream = null
            contentLength = 0
            followRedirects = true
        }

        init {
            headers = HashMap()
        }
    }

    /** Listener to be able to do custom logic once the [HttpResponse] is ready to be processed, register it with
     * [Net.sendHttpRequest].  */
    interface HttpResponseListener {

        /** Called when the [HttpRequest] has been processed and there is a [HttpResponse] ready. Passing data to the
         * rendering thread should be done using [Application.postRunnable] [HttpResponse]
         * contains the [HttpStatus] and should be used to determine if the request was successful or not (see more info at
         * [HttpStatus.getStatusCode]). For example:
         *
         * <pre>
         * HttpResponseListener listener = new HttpResponseListener() {
         * public void handleHttpResponse (HttpResponse httpResponse) {
         * HttpStatus status = httpResponse.getStatus();
         * if (status.getStatusCode() >= 200 && status.getStatusCode() < 300) {
         * // it was successful
         * } else {
         * // do something else
         * }
         * }
         * }
        </pre> *
         *
         * @param httpResponse The [HttpResponse] with the HTTP response values.
         */
        fun handleHttpResponse(httpResponse: HttpResponse?)

        /** Called if the [HttpRequest] failed because an exception when processing the HTTP request, could be a timeout any
         * other reason (not an HTTP error).
         * @param t If the HTTP request failed because an Exception, t encapsulates it to give more information.
         */
        fun failed(t: Throwable?)
        fun cancelled()
    }

    /** Process the specified [HttpRequest] and reports the [HttpResponse] to the specified [HttpResponseListener]
     * .
     * @param httpRequest The [HttpRequest] to be performed.
     * @param httpResponseListener The [HttpResponseListener] to call once the HTTP response is ready to be processed. Could
     * be null, in that case no listener is called.
     */
    fun sendHttpRequest(httpRequest: HttpRequest?, httpResponseListener: HttpResponseListener?)
    fun cancelHttpRequest(httpRequest: HttpRequest?)

    /** Protocol used by [Net.newServerSocket] and
     * [Net.newClientSocket].
     * @author mzechner
     */
    enum class Protocol {

        TCP
    }

    /** Creates a new server socket on the given address and port, using the given [Protocol], waiting for incoming connections.
     *
     * @param hostname the hostname or ip address to bind the socket to
     * @param port the port to listen on
     * @param hints additional [ServerSocketHints] used to create the socket. Input null to use the default setting provided
     * by the system.
     * @return the [ServerSocket]
     * @throws GdxRuntimeException in case the socket couldn't be opened
     */
    fun newServerSocket(protocol: Protocol?, hostname: String?, port: Int, hints: ServerSocketHints?): ServerSocket?

    /** Creates a new server socket on the given port, using the given [Protocol], waiting for incoming connections.
     *
     * @param port the port to listen on
     * @param hints additional [ServerSocketHints] used to create the socket. Input null to use the default setting provided
     * by the system.
     * @return the [ServerSocket]
     * @throws GdxRuntimeException in case the socket couldn't be opened
     */
    fun newServerSocket(protocol: Protocol?, port: Int, hints: ServerSocketHints?): ServerSocket?

    /** Creates a new TCP client socket that connects to the given host and port.
     *
     * @param host the host address
     * @param port the port
     * @param hints additional [SocketHints] used to create the socket. Input null to use the default setting provided by the
     * system.
     * @return GdxRuntimeException in case the socket couldn't be opened
     */
    fun newClientSocket(protocol: Protocol?, host: String?, port: Int, hints: SocketHints?): Socket?

    /** Launches the default browser to display a URI. If the default browser is not able to handle the specified URI, the
     * application registered for handling URIs of the specified type is invoked. The application is determined from the protocol
     * and path of the URI. A best effort is made to open the given URI; however, since external applications are involved, no guarantee
     * can be made as to whether the URI was actually opened. If it is known that the URI was not opened, false will be returned;
     * otherwise, true will be returned.
     *
     * @param URI the URI to be opened.
     * @return false if it is known the uri was not opened, true otherwise.
     */
    fun openURI(URI: String?): Boolean
}
