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

import com.badlogic.gdx.assets.AssetDescriptor
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g3d.particles.ResourceData.Configurable
import com.badlogic.gdx.graphics.g3d.particles.ResourceData.SaveData
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.IntArray
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonValue
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectMap.Entry
import com.badlogic.gdx.utils.reflect.ClassReflection.forName
import com.badlogic.gdx.utils.reflect.ReflectionException
import java.lang.RuntimeException

/**
 * This class handles the assets and configurations required by a given resource when de/serialized. It's handy when a given
 * object or one of its members requires some assets to be loaded to work properly after being deserialized. To save the assets,
 * the object should implement the [Configurable] interface and obtain a [SaveData] object to store every required
 * asset or information which will be used during the loading phase. The passed in [AssetManager] is generally used to find
 * the asset file name for a given resource of a given type. The class can also store global configurations, this is useful when
 * dealing with objects which should be allocated once (i.e singleton). The deserialization process must happen in the same order
 * of serialization, because the per object [SaveData] blocks are stored as an [Array] within the [ResourceData]
 * , while the global [SaveData] instances can be accessed in any order because require a unique [String] and are
 * stored in an [ObjectMap].
 *
 * @author Inferno
 */
class ResourceData<T>() : Json.Serializable {

    /**
     * This interface must be implemented by any class requiring additional assets to be loaded/saved
     */
    interface Configurable<T> {

        fun save(manager: AssetManager?, resources: ResourceData<T?>?)
        fun load(manager: AssetManager?, resources: ResourceData<T?>?)
    }

    /**
     * Contains all the saved data. [.data] is a map which link an asset name to its instance. [.assets] is an array of
     * indices addressing a given [com.badlogic.gdx.graphics.g3d.particles.ResourceData.AssetData] in the
     * [ResourceData]
     */
    class SaveData : Json.Serializable {

        var data: ObjectMap<String?, Any?>?
        var assets: IntArray?
        private var loadIndex: Int
        var resources: ResourceData<*>? = null

        constructor() {
            data = ObjectMap<String?, Any?>()
            assets = IntArray()
            loadIndex = 0
        }

        constructor(resources: ResourceData<*>?) {
            data = ObjectMap<String?, Any?>()
            assets = IntArray()
            loadIndex = 0
            this.resources = resources
        }

        fun <K> saveAsset(filename: String?, type: java.lang.Class<K?>?) {
            var i = resources!!.getAssetData(filename, type)
            if (i == -1) {
                resources!!.assets.add(AssetData<Any?>(filename, type))
                i = resources!!.assets!!.size - 1
            }
            assets.add(i)
        }

        fun save(key: String?, value: Any?) {
            data.put(key, value)
        }

        fun loadAsset(): AssetDescriptor<*>? {
            if (loadIndex == assets!!.size) return null
            val data = resources!!.assets!![assets!![loadIndex++]]
            return AssetDescriptor<Any?>(data!!.filename, data.type)
        }

        fun <K> load(key: String?): K? {
            return data.get(key)
        }

        fun write(json: Json?) {
            json.writeValue("data", data, ObjectMap::class.java)
            json.writeValue("indices", assets.toArray(), IntArray::class.java)
        }

        fun read(json: Json?, jsonData: JsonValue?) {
            data = json.readValue("data", ObjectMap::class.java, jsonData)
            assets.addAll(json.readValue("indices", IntArray::class.java, jsonData))
        }
    }

    /**
     * This class contains all the information related to a given asset
     */
    class AssetData<T> : Json.Serializable {

        var filename: String? = null
        var type: java.lang.Class<T?>? = null

        constructor() {}
        constructor(filename: String?, type: java.lang.Class<T?>?) {
            this.filename = filename
            this.type = type
        }

        fun write(json: Json?) {
            json.writeValue("filename", filename)
            json.writeValue("type", type.getName())
        }

        fun read(json: Json?, jsonData: JsonValue?) {
            filename = json.readValue("filename", String::class.java, jsonData)
            val className: String = json.readValue("type", String::class.java, jsonData)
            type = try {
                forName(className) as java.lang.Class<T?>
            } catch (e: ReflectionException) {
                throw GdxRuntimeException("Class not found: $className", e)
            }
        }
    }

    /**
     * Unique data, can be used to save/load generic data which is not always loaded back after saving. Must be used to store data
     * which is uniquely addressable by a given string (i.e a system configuration).
     */
    private var uniqueData: ObjectMap<String?, SaveData?>?

    /**
     * Objects save data, must be loaded in the same saving order
     */
    private var data: Array<SaveData?>?

    /**
     * Shared assets among all the configurable objects
     */
    var assets: Array<AssetData<*>?>?
    private var currentLoadIndex: Int
    var resource: T? = null

    constructor(resource: T?) : this() {
        this.resource = resource
    }

    fun <K> getAssetData(filename: String?, type: java.lang.Class<K?>?): Int {
        var i = 0
        for (data in assets!!) {
            if (data!!.filename == filename && data.type == type) {
                return i
            }
            ++i
        }
        return -1
    }

    val assetDescriptors: Array<AssetDescriptor<*>?>?
        get() {
            val descriptors = Array<AssetDescriptor<*>?>()
            for (data in assets!!) {
                descriptors.add(AssetDescriptor<T?>(data!!.filename, data.type))
            }
            return descriptors
        }

    /**
     * Creates and adds a new SaveData object to the save data list
     */
    fun createSaveData(): SaveData? {
        val saveData = SaveData(this)
        data.add(saveData)
        return saveData
    }

    /**
     * Creates and adds a new and unique SaveData object to the save data map
     */
    fun createSaveData(key: String?): SaveData? {
        val saveData = SaveData(this)
        if (uniqueData.containsKey(key)) throw RuntimeException("Key already used, data must be unique, use a different key")
        uniqueData.put(key, saveData)
        return saveData
    }

    /**
     * @return the next save data in the list
     */
    val saveData: SaveData?
        get() = data!![currentLoadIndex++]

    /**
     * @return the unique save data in the map
     */
    fun getSaveData(key: String?): SaveData? {
        return uniqueData.get(key)
    }

    fun write(json: Json?) {
        json.writeValue("unique", uniqueData, ObjectMap::class.java)
        json.writeValue("data", data, Array::class.java, SaveData::class.java)
        json.writeValue("assets", assets.toArray(AssetData::class.java), Array<AssetData>::class.java)
        json.writeValue("resource", resource, null)
    }

    fun read(json: Json?, jsonData: JsonValue?) {
        uniqueData = json.readValue("unique", ObjectMap::class.java, jsonData)
        for (entry in uniqueData.entries()) {
            entry.value.resources = this
        }
        data = json.readValue("data", Array::class.java, SaveData::class.java, jsonData)
        for (saveData in data!!) {
            saveData!!.resources = this
        }
        assets.addAll(json.readValue("assets", Array::class.java, AssetData::class.java, jsonData))
        resource = json.readValue("resource", null, jsonData)
    }

    init {
        uniqueData = ObjectMap<String?, SaveData?>()
        data = Array(true, 3, SaveData::class.java)
        assets = Array()
        currentLoadIndex = 0
    }
}
