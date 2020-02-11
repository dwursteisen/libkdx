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
package com.badlogic.gdx.graphics.g3d.particles

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.Channel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ChannelDescriptor
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.IntChannel
import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.ObjectChannel
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ColorInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Rotation2dInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.Rotation3dInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.ScaleInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels.TextureRegionInitializer
import com.badlogic.gdx.graphics.g3d.particles.ParticleController
import com.badlogic.gdx.graphics.g3d.particles.ParticleControllerComponent
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectLoadParameter
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffectLoader.ParticleEffectSaveParameter
import kotlin.jvm.Throws

/**
 * This class represents an group of elements like an array, but the properties of the elements are stored as separate arrays.
 * These arrays are called [Channel] and are represented by [ChannelDescriptor]. It's not necessary to store primitive
 * types in the channels but doing so will "exploit" data locality in the JVM, which is ensured for primitive types. Use
 * [FloatChannel], [IntChannel], [ObjectChannel] to store the data.
 *
 * @author inferno
 */
class ParallelArray(capacity: Int) {

    /**
     * This class describes the content of a [Channel]
     */
    class ChannelDescriptor(var id: Int, type: java.lang.Class<*>?, count: Int) {

        var type: java.lang.Class<*>?
        var count: Int

        init {
            this.type = type
            this.count = count
        }
    }

    /**
     * This class represents a container of values for all the elements for a given property
     */
    abstract inner class Channel(var id: Int, var data: Any?, var strideSize: Int) {

        abstract fun add(index: Int, vararg objects: Any?)
        abstract fun swap(i: Int, k: Int)
        abstract fun setCapacity(requiredCapacity: Int)
    }

    /**
     * This interface is used to provide custom initialization of the [Channel] data
     */
    interface ChannelInitializer<T : Channel?> {

        fun init(channel: T?)
    }

    inner class FloatChannel(id: Int, strideSize: Int, size: Int) : Channel(id, FloatArray(size * strideSize), strideSize) {
        override var data: FloatArray?
        override fun add(index: Int, vararg objects: Any?) {
            var i = strideSize * size
            val c = i + strideSize
            var k = 0
            while (i < c) {
                data!![i] = (objects[k] as Float?)!!
                ++i
                ++k
            }
        }

        override fun swap(i: Int, k: Int) {
            var i = i
            var k = k
            var t: Float
            i = strideSize * i
            k = strideSize * k
            val c = i + strideSize
            while (i < c) {
                t = data!![i]
                data!![i] = data!![k]
                data!![k] = t
                ++i
                ++k
            }
        }

        override fun setCapacity(requiredCapacity: Int) {
            val newData = FloatArray(strideSize * requiredCapacity)
            java.lang.System.arraycopy(data, 0, newData, 0, java.lang.Math.min(data!!.size, newData.size))
            data = newData
            super.data = data
        }

        init {
            data = super.data as FloatArray?
        }
    }

    inner class IntChannel(id: Int, strideSize: Int, size: Int) : Channel(id, IntArray(size * strideSize), strideSize) {
        override var data: IntArray?
        override fun add(index: Int, vararg objects: Any?) {
            var i = strideSize * size
            val c = i + strideSize
            var k = 0
            while (i < c) {
                data!![i] = (objects[k] as Int?)!!
                ++i
                ++k
            }
        }

        override fun swap(i: Int, k: Int) {
            var i = i
            var k = k
            var t: Int
            i = strideSize * i
            k = strideSize * k
            val c = i + strideSize
            while (i < c) {
                t = data!![i]
                data!![i] = data!![k]
                data!![k] = t
                ++i
                ++k
            }
        }

        override fun setCapacity(requiredCapacity: Int) {
            val newData = IntArray(strideSize * requiredCapacity)
            java.lang.System.arraycopy(data, 0, newData, 0, java.lang.Math.min(data!!.size, newData.size))
            data = newData
            super.data = data
        }

        init {
            data = super.data as IntArray?
        }
    }

    inner class ObjectChannel<T>(id: Int, strideSize: Int, size: Int, type: java.lang.Class<T?>?) : Channel(id, ArrayReflection.newInstance(type, size * strideSize), strideSize) {
        var componentType: java.lang.Class<T?>?
        override var data: Array<T?>?
        override fun add(index: Int, vararg objects: Any?) {
            var i = strideSize * size
            val c = i + strideSize
            var k = 0
            while (i < c) {
                data!![i] = objects[k] as T?
                ++i
                ++k
            }
        }

        override fun swap(i: Int, k: Int) {
            var i = i
            var k = k
            var t: T?
            i = strideSize * i
            k = strideSize * k
            val c = i + strideSize
            while (i < c) {
                t = data!![i]
                data!![i] = data!![k]
                data!![k] = t
                ++i
                ++k
            }
        }

        override fun setCapacity(requiredCapacity: Int) {
            val newData = ArrayReflection.newInstance(componentType, strideSize * requiredCapacity) as Array<T?>
            java.lang.System.arraycopy(data, 0, newData, 0, java.lang.Math.min(data!!.size, newData.size))
            data = newData
            super.data = data
        }

        init {
            componentType = type
            data = super.data as Array<T?>?
        }
    }

    /**
     * the channels added to the array
     */
    var arrays: Array<Channel?>?

    /**
     * the maximum amount of elements that this array can hold
     */
    var capacity: Int

    /**
     * the current amount of defined elements, do not change manually unless you know what you are doing.
     */
    var size: Int

    /**
     * Adds and returns a channel described by the channel descriptor parameter. If a channel with the same id already exists, no
     * allocation is performed and that channel is returned.
     */
    fun <T : Channel?> addChannel(channelDescriptor: ChannelDescriptor?): T? {
        return addChannel(channelDescriptor, null)
    }

    /**
     * Adds and returns a channel described by the channel descriptor parameter. If a channel with the same id already exists, no
     * allocation is performed and that channel is returned. Otherwise a new channel is allocated and initialized with the
     * initializer.
     */
    fun <T : Channel?> addChannel(channelDescriptor: ChannelDescriptor?, initializer: ChannelInitializer<T?>?): T? {
        var channel: T? = getChannel(channelDescriptor)
        if (channel == null) {
            channel = allocateChannel(channelDescriptor)
            initializer?.init(channel)
            arrays.add(channel)
        }
        return channel
    }

    private fun <T : Channel?> allocateChannel(channelDescriptor: ChannelDescriptor?): T? {
        return if (channelDescriptor!!.type == Float::class.javaPrimitiveType) {
            FloatChannel(channelDescriptor.id, channelDescriptor.count, capacity) as T
        } else if (channelDescriptor.type == Int::class.javaPrimitiveType) {
            IntChannel(channelDescriptor.id, channelDescriptor.count, capacity) as T
        } else {
            ObjectChannel<Any?>(channelDescriptor.id, channelDescriptor.count, capacity, channelDescriptor.type) as T
        }
    }

    /**
     * Removes the channel with the given id
     */
    fun <T> removeArray(id: Int) {
        arrays.removeIndex(findIndex(id))
    }

    private fun findIndex(id: Int): Int {
        for (i in 0 until arrays!!.size) {
            val array: Channel = arrays.items.get(i)
            if (array.id == id) return i
        }
        return -1
    }

    /**
     * Adds an element considering the values in the same order as the current channels in the array. The n_th value must have the
     * same type and stride of the given channel at position n
     */
    fun addElement(vararg values: Any?) {
        /* FIXME make it grow... */
        if (size == capacity) throw GdxRuntimeException("Capacity reached, cannot add other elements")
        var k = 0
        for (strideArray in arrays!!) {
            strideArray!!.add(k, *values)
            k += strideArray.strideSize
        }
        ++size
    }

    /**
     * Removes the element at the given index and swaps it with the last available element
     */
    fun removeElement(index: Int) {
        val last = size - 1
        // Swap
        for (strideArray in arrays!!) {
            strideArray!!.swap(index, last)
        }
        size = last
    }

    /**
     * @return the channel with the same id as the one in the descriptor
     */
    fun <T : Channel?> getChannel(descriptor: ChannelDescriptor?): T? {
        for (array in arrays!!) {
            if (array!!.id == descriptor!!.id) return array as T?
        }
        return null
    }

    /**
     * Removes all the channels and sets size to 0
     */
    fun clear() {
        arrays.clear()
        size = 0
    }

    /**
     * Sets the capacity. Each contained channel will be resized to match the required capacity and the current data will be
     * preserved.
     */
    fun setCapacity(requiredCapacity: Int) {
        if (capacity != requiredCapacity) {
            for (channel in arrays!!) {
                channel!!.setCapacity(requiredCapacity)
            }
            capacity = requiredCapacity
        }
    }

    init {
        arrays = Array(false, 2, Channel::class.java)
        this.capacity = capacity
        size = 0
    }
}
