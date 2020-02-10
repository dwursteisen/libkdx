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

import com.badlogic.gdx.net.HttpParametersUtils.convertHttpParameters
import java.lang.IllegalStateException

/**
 * A builder for [HttpRequest]s.
 *
 *
 * Make sure to call [.newRequest] first, then set the request up and obtain it via [.build] when you are done.
 *
 *
 * It also offers a few utility methods to deal with content encoding and HTTP headers.
 *
 * @author Daniel Holderbaum
 */
class HttpRequestBuilder {

    private var httpRequest: HttpRequest? = null

    /**
     * Initializes the builder and sets it up to build a new [HttpRequest] .
     */
    fun newRequest(): HttpRequestBuilder {
        check(httpRequest == null) { "A new request has already been started. Call HttpRequestBuilder.build() first." }
        httpRequest = Pools.obtain(HttpRequest::class.java)
        httpRequest.setTimeOut(defaultTimeout)
        return this
    }

    /**
     * @see HttpRequest.setMethod
     */
    fun method(httpMethod: String?): HttpRequestBuilder {
        validate()
        httpRequest.setMethod(httpMethod)
        return this
    }

    /**
     * The [.baseUrl] will automatically be added as a prefix to the given URL.
     *
     * @see HttpRequest.setUrl
     */
    fun url(url: String): HttpRequestBuilder {
        validate()
        httpRequest.setUrl(baseUrl + url)
        return this
    }

    /**
     * If this method is not called, the [.defaultTimeout] will be used.
     *
     * @see HttpRequest.setTimeOut
     */
    fun timeout(timeOut: Int): HttpRequestBuilder {
        validate()
        httpRequest.setTimeOut(timeOut)
        return this
    }

    /**
     * @see HttpRequest.setFollowRedirects
     */
    fun followRedirects(followRedirects: Boolean): HttpRequestBuilder {
        validate()
        httpRequest.setFollowRedirects(followRedirects)
        return this
    }

    /**
     * @see HttpRequest.setIncludeCredentials
     */
    fun includeCredentials(includeCredentials: Boolean): HttpRequestBuilder {
        validate()
        httpRequest.setIncludeCredentials(includeCredentials)
        return this
    }

    /**
     * @see HttpRequest.setHeader
     */
    fun header(name: String?, value: String?): HttpRequestBuilder {
        validate()
        httpRequest.setHeader(name, value)
        return this
    }

    /**
     * @see HttpRequest.setContent
     */
    fun content(content: String?): HttpRequestBuilder {
        validate()
        httpRequest.setContent(content)
        return this
    }

    /**
     * @see HttpRequest.setContent
     */
    fun content(contentStream: InputStream?, contentLength: Long): HttpRequestBuilder {
        validate()
        httpRequest.setContent(contentStream, contentLength)
        return this
    }

    /**
     * Sets the correct `ContentType` and encodes the given parameter map, then sets it as the content.
     */
    fun formEncodedContent(content: Map<String?, String?>?): HttpRequestBuilder {
        validate()
        httpRequest.setHeader(HttpRequestHeader.ContentType, "application/x-www-form-urlencoded")
        val formEncodedContent = convertHttpParameters(content)
        httpRequest.setContent(formEncodedContent)
        return this
    }

    /**
     * Sets the correct `ContentType` and encodes the given content object via [.json], then sets it as the content.
     */
    fun jsonContent(content: Any?): HttpRequestBuilder {
        validate()
        httpRequest.setHeader(HttpRequestHeader.ContentType, "application/json")
        val jsonContent: String = json.toJson(content)
        httpRequest.setContent(jsonContent)
        return this
    }

    /**
     * Sets the `Authorization` header via the Base64 encoded username and password.
     */
    fun basicAuthentication(username: String, password: String): HttpRequestBuilder {
        validate()
        httpRequest.setHeader(HttpRequestHeader.Authorization, "Basic " + Base64Coder.encodeString("$username:$password"))
        return this
    }

    /**
     * Returns the [HttpRequest] that has been setup by this builder so far. After using the request, it should be returned
     * to the pool via `Pools.free(request)`.
     */
    fun build(): HttpRequest? {
        validate()
        val request: HttpRequest? = httpRequest
        httpRequest = null
        return request
    }

    private fun validate() {
        checkNotNull(httpRequest) { "A new request has not been started yet. Call HttpRequestBuilder.newRequest() first." }
    }

    companion object {
        /**
         * Will be added as a prefix to each URL when [.url] is called. Empty by default.
         */
        var baseUrl = ""

        /**
         * Will be set for each new HttpRequest. By default set to `1000`. Can be overwritten via [.timeout].
         */
        var defaultTimeout = 1000

        /**
         * Will be used for the object serialization in case [.jsonContent] is called.
         */
        var json: Json = Json()
    }
}
