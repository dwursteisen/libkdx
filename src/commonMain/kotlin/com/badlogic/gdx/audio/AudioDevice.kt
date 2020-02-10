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
package com.badlogic.gdx.audio

/** Encapsulates an audio device in mono or stereo mode. Use the [.writeSamples] and
 * [.writeSamples] methods to write float or 16-bit signed short PCM data directly to the audio device.
 * Stereo samples are interleaved in the order left channel sample, right channel sample. The [.dispose] method must be
 * called when this AudioDevice is no longer needed.
 *
 * @author badlogicgames@gmail.com
 */
interface AudioDevice : com.badlogic.gdx.utils.Disposable {

    /** @return whether this AudioDevice is in mono or stereo mode.
     */
    val isMono: Boolean

    /** Writes the array of 16-bit signed PCM samples to the audio device and blocks until they have been processed.
     *
     * @param samples The samples.
     * @param offset The offset into the samples array
     * @param numSamples the number of samples to write to the device
     */
    fun writeSamples(samples: ShortArray?, offset: Int, numSamples: Int)

    /** Writes the array of float PCM samples to the audio device and blocks until they have been processed.
     *
     * @param samples The samples.
     * @param offset The offset into the samples array
     * @param numSamples the number of samples to write to the device
     */
    fun writeSamples(samples: FloatArray?, offset: Int, numSamples: Int)

    /** @return the latency in samples.
     */
    val latency: Int

    /** Frees all resources associated with this AudioDevice. Needs to be called when the device is no longer needed.  */
    override fun dispose()

    /** Sets the volume in the range [0,1].  */
    fun setVolume(volume: Float)
}
