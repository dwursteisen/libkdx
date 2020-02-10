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
 * A list of common request header constants of the HTTP protocol. See http://en.wikipedia.org/wiki/List_of_HTTP_header_fields.
 *
 * @author Daniel Holderbaum
 */
interface HttpRequestHeader {

    companion object {
        /**
         * Content-Types that are acceptable for the response.
         *
         *
         * Example: Accept: text/plain
         */
        const val Accept = "Accept"

        /**
         * Character sets that are acceptable.
         *
         *
         * Example: Accept-Charset: utf-8
         */
        const val AcceptCharset = "Accept-Charset"

        /**
         * List of acceptable encodings.
         *
         *
         * Example: Accept-Encoding: gzip, deflate
         */
        const val AcceptEncoding = "Accept-Encoding"

        /**
         * List of acceptable human languages for response.
         *
         *
         * Example: Accept-Language: en-US
         */
        const val AcceptLanguage = "Accept-Language"

        /**
         * Acceptable version in time.
         *
         *
         * Example: Accept-Datetime: Thu, 31 May 2007 20:35:00 GMT
         */
        const val AcceptDatetime = "Accept-Datetime"

        /**
         * Authentication credentials for HTTP authentication.
         *
         *
         * Example: Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
         */
        const val Authorization = "Authorization"

        /**
         * Used to specify directives that must be obeyed by all caching mechanisms along the request-response chain.
         *
         *
         * Example: Cache-Control: no-cache
         */
        const val CacheControl = "Cache-Control"

        /**
         * What type of connection the user-agent would prefer.
         *
         *
         * Example: Connection: keep-alive
         */
        const val Connection = "Connection"

        /**
         * An HTTP cookie previously sent by the server with Set-Cookie (below).
         *
         *
         * Example: Cookie: $Version=1=""; Skin=new="";
         */
        const val Cookie = "Cookie"

        /**
         * The length of the request body in octets (8-bit bytes).
         *
         *
         * Example: Content-Length: 348
         */
        const val ContentLength = "Content-Length"

        /**
         * A Base64-encoded binary MD5 sum of the content of the request body.
         *
         *
         * Example: Content-MD5: Q2hlY2sgSW50ZWdyaXR5IQ==
         */
        const val ContentMD5 = "Content-MD5"

        /**
         * The MIME type of the body of the request (used with POST and PUT requests).
         *
         *
         * Example: Content-Type: application/x-www-form-urlencoded
         */
        const val ContentType = "Content-Type"

        /**
         * The date and time that the message was sent (in "HTTP-date" format as defined by RFC 7231).
         *
         *
         * Example: Date: Tue, 15 Nov 1994 08:12:31 GMT
         */
        const val Date = "Date"

        /**
         * Indicates that particular server behaviors are required by the client.
         *
         *
         * Example: Expect: 100-continue
         */
        const val Expect = "Expect"

        /**
         * The email address of the user making the request.
         *
         *
         * Example: From: user@example.com
         */
        const val From = "From"

        /**
         * The domain name of the server (for virtual hosting), and the TCP port number on which the server is listening. The port
         * number may be omitted if the port is the standard port for the service requested.
         *
         *
         * Example: en.wikipedia.org
         */
        const val Host = "Host"

        /**
         * Only perform the action if the client supplied entity matches the same entity on the server. This is mainly for methods like
         * PUT to only update a resource if it has not been modified since the user last updated it.
         *
         *
         * Example: If-Match: "737060cd8c284d8af7ad3082f209582d"
         */
        const val IfMatch = "If-Match"

        /**
         * Allows a 304 Not Modified to be returned if content is unchanged.
         *
         *
         * Example: If-Modified-Since: Sat, 29 Oct 1994 19:43:31 GMT
         */
        const val IfModifiedSince = "If-Modified-Since"

        /**
         * Allows a 304 Not Modified to be returned if content is unchanged, see HTTP ETag.
         *
         *
         * Example: If-None-Match: "737060cd8c284d8af7ad3082f209582d"
         */
        const val IfNoneMatch = "If-None-Match"

        /**
         * If the entity is unchanged, send me the part(s) that I am missing=""; otherwise, send me the entire new entity.
         *
         *
         * Example: If-Range: "737060cd8c284d8af7ad3082f209582d"
         */
        const val IfRange = "If-Range"

        /**
         * Only send the response if the entity has not been modified since a specific time.
         *
         *
         * Example: If-Unmodified-Since: Sat, 29 Oct 1994 19:43:31 GMT
         */
        const val IfUnmodifiedSince = "If-Unmodified-Since"

        /**
         * Limit the number of times the message can be forwarded through proxies or gateways.
         *
         *
         * Example: Max-Forwards: 10
         */
        const val MaxForwards = "Max-Forwards"

        /**
         * Initiates a request for cross-origin resource sharing (asks server for an 'Access-Control-Allow-Origin' response field).
         *
         *
         * Example: Origin: http://www.example-social-network.com
         */
        const val Origin = "Origin"

        /**
         * Implementation-specific fields that may have various effects anywhere along the request-response chain.
         *
         *
         * Example: Pragma: no-cache
         */
        const val Pragma = "Pragma"

        /**
         * Authorization credentials for connecting to a proxy.
         *
         *
         * Example: Proxy-Authorization: Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==
         */
        const val ProxyAuthorization = "Proxy-Authorization"

        /**
         * Request only part of an entity. Bytes are numbered from 0.
         *
         *
         * Example: Range: bytes=500-999
         */
        const val Range = "Range"

        /**
         * This is the address of the previous web page from which a link to the currently requested page was followed. (The word
         * "referrer" has been misspelled in the RFC as well as in most implementations to the point that it has become standard usage
         * and is considered correct terminology).
         *
         *
         * Example: Referer: http://en.wikipedia.org/wiki/Main_Page
         */
        const val Referer = "Referer"

        /**
         * The transfer encodings the user agent is willing to accept: the same values as for the response header field
         * Transfer-Encoding can be used, plus the "trailers" value (related to the "chunked" transfer method) to notify the server it
         * expects to receive additional fields in the trailer after the last, zero-sized, chunk.
         *
         *
         * Example: TE: trailers, deflate
         */
        const val TE = "TE"

        /**
         * The user agent string of the user agent.
         *
         *
         * Example: User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:12.0) Gecko/20100101 Firefox/21.0
         */
        const val UserAgent = "User-Agent"

        /**
         * Ask the server to upgrade to another protocol.
         *
         *
         * Example: Upgrade: HTTP/2.0, SHTTP/1.3, IRC/6.9, RTA/x11
         */
        const val Upgrade = "Upgrade"

        /**
         * Informs the server of proxies through which the request was sent.
         *
         *
         * Example: Via: 1.0 fred, 1.1 example.com (Apache/1.1)
         */
        const val Via = "Via"

        /**
         * A general warning about possible problems with the entity body.
         *
         *
         * Example: Warning: 199 Miscellaneous warning
         */
        const val Warning = "Warning"
    }
}
