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
package com.badlogic.gdx.utils.compression

import java.io.IOException
import java.lang.RuntimeException
import kotlin.jvm.Throws

/** Adapted from LZMA SDK version 9.22.
 *
 * This was modified to be used directly on streams, rather than via the command line as in the LZMA SDK.
 *
 * We only currently allow the default LZMA options to be used, as we know it works on for our target usage.  */
object Lzma {

    /** Compresses the given [InputStream] into the given [OutputStream].
     *
     * @param in the [InputStream] to compress
     * @param out the [OutputStream] to compress to
     * @throws IOException
     */
    @Throws(IOException::class)
    fun compress(`in`: java.io.InputStream, out: java.io.OutputStream) {
        val params = CommandLine()
        var eos = false
        if (params.Eos) eos = true
        val encoder: com.badlogic.gdx.utils.compression.lzma.Encoder = com.badlogic.gdx.utils.compression.lzma.Encoder()
        if (!encoder.SetAlgorithm(params.Algorithm)) throw RuntimeException("Incorrect compression mode")
        if (!encoder.SetDictionarySize(params.DictionarySize)) throw RuntimeException("Incorrect dictionary size")
        if (!encoder.SetNumFastBytes(params.Fb)) throw RuntimeException("Incorrect -fb value")
        if (!encoder.SetMatchFinder(params.MatchFinder)) throw RuntimeException("Incorrect -mf value")
        if (!encoder.SetLcLpPb(params.Lc, params.Lp, params.Pb)) throw RuntimeException("Incorrect -lc or -lp or -pb value")
        encoder.SetEndMarkerMode(eos)
        encoder.WriteCoderProperties(out)
        var fileSize: Long
        if (eos) {
            fileSize = -1
        } else {
            if (`in`.available().also({ fileSize = it }) == 0L) {
                fileSize = -1
            }
        }
        for (i in 0..7) {
            out.write((fileSize ushr 8 * i).toInt() and 0xFF)
        }
        encoder.Code(`in`, out, -1, -1, null)
    }

    /** Decompresses the given [InputStream] into the given [OutputStream].
     *
     * @param in the [InputStream] to decompress
     * @param out the [OutputStream] to decompress to
     * @throws IOException
     */
    @Throws(IOException::class)
    fun decompress(`in`: java.io.InputStream, out: java.io.OutputStream?) {
        val propertiesSize = 5
        val properties = ByteArray(propertiesSize)
        if (`in`.read(properties, 0, propertiesSize) != propertiesSize) throw RuntimeException("input .lzma file is too short")
        val decoder: com.badlogic.gdx.utils.compression.lzma.Decoder = com.badlogic.gdx.utils.compression.lzma.Decoder()
        if (!decoder.SetDecoderProperties(properties)) throw RuntimeException("Incorrect stream properties")
        var outSize: Long = 0
        for (i in 0..7) {
            val v: Int = `in`.read()
            if (v < 0) {
                throw RuntimeException("Can't read stream size")
            }
            outSize = outSize or v.toLong() shl 8 * i
        }
        if (!decoder.Code(`in`, out, outSize)) {
            throw RuntimeException("Error in data stream")
        }
    }

    internal class CommandLine {
        var Command = -1
        var NumBenchmarkPasses = 10
        var DictionarySize = 1 shl 23
        var DictionarySizeIsDefined = false
        var Lc = 3
        var Lp = 0
        var Pb = 2
        var Fb = 128
        var FbIsDefined = false
        var Eos = false
        var Algorithm = 2
        var MatchFinder = 1
        var InFile: String? = null
        var OutFile: String? = null

        companion object {
            const val kEncode = 0
            const val kDecode = 1
            const val kBenchmak = 2
        }
    }
}
