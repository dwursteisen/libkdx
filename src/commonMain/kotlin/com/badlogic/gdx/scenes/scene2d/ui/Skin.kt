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
package com.badlogic.gdx.scenes.scene2d.ui

import Button.ButtonStyle
import CheckBox.CheckBoxStyle
import ImageButton.ImageButtonStyle
import ImageTextButton.ImageTextButtonStyle
import Label.LabelStyle
import List.ListStyle
import Slider.SliderStyle
import Window.WindowStyle
import java.lang.RuntimeException

/**
 * A skin stores resources for UI widgets to use (texture regions, ninepatches, fonts, colors, etc). Resources are named and can
 * be looked up by name and type. Resources can be described in JSON. Skin provides useful conversions, such as allowing access to
 * regions in the atlas as ninepatches, sprites, drawables, etc. The get* methods return an instance of the object in the skin.
 * The new* methods return a copy of an instance in the skin.
 *
 *
 * See the [documentation](https://github.com/libgdx/libgdx/wiki/Skin) for more.
 *
 * @author Nathan Sweet
 */
class Skin : Disposable {

    var resources: ObjectMap<java.lang.Class, ObjectMap<String, Any>> = ObjectMap()
    var atlas: TextureAtlas? = null
    private val jsonClassTags: ObjectMap<String, java.lang.Class> = ObjectMap(defaultTagClasses.size)

    /**
     * Creates an empty skin.
     */
    constructor() {}

    /**
     * Creates a skin containing the resources in the specified skin JSON file. If a file in the same directory with a ".atlas"
     * extension exists, it is loaded as a [TextureAtlas] and the texture regions added to the skin. The atlas is
     * automatically disposed when the skin is disposed.
     */
    constructor(skinFile: FileHandle) {
        val atlasFile: FileHandle = skinFile.sibling(skinFile.nameWithoutExtension().toString() + ".atlas")
        if (atlasFile.exists()) {
            atlas = TextureAtlas(atlasFile)
            addRegions(atlas)
        }
        load(skinFile)
    }

    /**
     * Creates a skin containing the resources in the specified skin JSON file and the texture regions from the specified atlas.
     * The atlas is automatically disposed when the skin is disposed.
     */
    constructor(skinFile: FileHandle, atlas: TextureAtlas?) {
        this.atlas = atlas
        addRegions(atlas)
        load(skinFile)
    }

    /**
     * Creates a skin containing the texture regions from the specified atlas. The atlas is automatically disposed when the skin
     * is disposed.
     */
    constructor(atlas: TextureAtlas?) {
        this.atlas = atlas
        addRegions(atlas)
    }

    /**
     * Adds all resources in the specified skin JSON file.
     */
    fun load(skinFile: FileHandle) {
        try {
            getJsonLoader(skinFile).fromJson(Skin::class.java, skinFile)
        } catch (ex: SerializationException) {
            throw SerializationException("Error reading file: $skinFile", ex)
        }
    }

    /**
     * Adds all named texture regions from the atlas. The atlas will not be automatically disposed when the skin is disposed.
     */
    fun addRegions(atlas: TextureAtlas?) {
        val regions: Array<AtlasRegion> = atlas.getRegions()
        var i = 0
        val n = regions.size
        while (i < n) {
            val region: AtlasRegion = regions[i]
            var name: String = region.name
            if (region.index !== -1) {
                name += "_" + region.index
            }
            add(name, region, TextureRegion::class.java)
            i++
        }
    }

    @JvmOverloads
    fun add(name: String?, resource: Any?, type: java.lang.Class = resource.javaClass) {
        if (name == null) throw java.lang.IllegalArgumentException("name cannot be null.")
        if (resource == null) throw java.lang.IllegalArgumentException("resource cannot be null.")
        var typeResources: ObjectMap<String?, Any?>? = resources.get(type)
        if (typeResources == null) {
            typeResources = ObjectMap(if (type == TextureRegion::class.java || type == Drawable::class.java || type == Sprite::class.java) 256 else 64)
            resources.put(type, typeResources)
        }
        typeResources.put(name, resource)
    }

    fun remove(name: String?, type: java.lang.Class?) {
        if (name == null) throw java.lang.IllegalArgumentException("name cannot be null.")
        val typeResources: ObjectMap<String, Any> = resources.get(type)
        typeResources.remove(name)
    }

    /**
     * Returns a resource named "default" for the specified type.
     *
     * @throws GdxRuntimeException if the resource was not found.
     */
    operator fun <T> get(type: java.lang.Class<T>?): T {
        return get("default", type)
    }

    /**
     * Returns a named resource of the specified type.
     *
     * @throws GdxRuntimeException if the resource was not found.
     */
    operator fun <T> get(name: String?, type: java.lang.Class<T>?): T? {
        if (name == null) throw java.lang.IllegalArgumentException("name cannot be null.")
        if (type == null) throw java.lang.IllegalArgumentException("type cannot be null.")
        if (type == Drawable::class.java) return getDrawable(name)
        if (type == TextureRegion::class.java) return getRegion(name)
        if (type == NinePatch::class.java) return getPatch(name)
        if (type == Sprite::class.java) return getSprite(name)
        val typeResources: ObjectMap<String, Any> = resources.get(type)
            ?: throw GdxRuntimeException("No " + type.getName() + " registered with name: " + name)
        val resource: Any = typeResources.get(name)
            ?: throw GdxRuntimeException("No " + type.getName() + " registered with name: " + name)
        return resource as T
    }

    /**
     * Returns a named resource of the specified type.
     *
     * @return null if not found.
     */
    fun <T> optional(name: String?, type: java.lang.Class<T>?): T? {
        if (name == null) throw java.lang.IllegalArgumentException("name cannot be null.")
        if (type == null) throw java.lang.IllegalArgumentException("type cannot be null.")
        val typeResources: ObjectMap<String, Any> = resources.get(type) ?: return null
        return typeResources.get(name)
    }

    fun has(name: String?, type: java.lang.Class?): Boolean {
        val typeResources: ObjectMap<String, Any> = resources.get(type) ?: return false
        return typeResources.containsKey(name)
    }

    /**
     * Returns the name to resource mapping for the specified type, or null if no resources of that type exist.
     */
    fun <T> getAll(type: java.lang.Class<T>?): ObjectMap<String, T> {
        return resources.get(type) as ObjectMap<String, T>
    }

    fun getColor(name: String?): Color {
        return get<T>(name, Color::class.java)
    }

    fun getFont(name: String?): BitmapFont {
        return get<T>(name, BitmapFont::class.java)
    }

    /**
     * Returns a registered texture region. If no region is found but a texture exists with the name, a region is created from the
     * texture and stored in the skin.
     */
    fun getRegion(name: String): TextureRegion? {
        var region: TextureRegion? = optional<T>(name, TextureRegion::class.java)
        if (region != null) return region
        val texture: Texture = optional(name, Texture::class.java)
            ?: throw GdxRuntimeException("No TextureRegion or Texture registered with name: $name")
        region = TextureRegion(texture)
        add(name, region, TextureRegion::class.java)
        return region
    }

    /**
     * @return an array with the [TextureRegion] that have an index != -1, or null if none are found.
     */
    fun getRegions(regionName: String): Array<TextureRegion?>? {
        var regions: Array<TextureRegion?>? = null
        var i = 0
        var region: TextureRegion? = optional<T>(regionName + "_" + i++, TextureRegion::class.java)
        if (region != null) {
            regions = Array<TextureRegion?>()
            while (region != null) {
                regions.add(region)
                region = optional<T>(regionName + "_" + i++, TextureRegion::class.java)
            }
        }
        return regions
    }

    /**
     * Returns a registered tiled drawable. If no tiled drawable is found but a region exists with the name, a tiled drawable is
     * created from the region and stored in the skin.
     */
    fun getTiledDrawable(name: String): TiledDrawable? {
        var tiled: TiledDrawable? = optional<T>(name, TiledDrawable::class.java)
        if (tiled != null) return tiled
        tiled = TiledDrawable(getRegion(name))
        tiled.setName(name)
        add(name, tiled, TiledDrawable::class.java)
        return tiled
    }

    /**
     * Returns a registered ninepatch. If no ninepatch is found but a region exists with the name, a ninepatch is created from the
     * region and stored in the skin. If the region is an [AtlasRegion] then the [AtlasRegion.splits] are used,
     * otherwise the ninepatch will have the region as the center patch.
     */
    fun getPatch(name: String): NinePatch? {
        var patch: NinePatch? = optional<T>(name, NinePatch::class.java)
        return if (patch != null) patch else try {
            val region: TextureRegion? = getRegion(name)
            if (region is AtlasRegion) {
                val splits: IntArray = (region as AtlasRegion?).splits
                if (splits != null) {
                    patch = NinePatch(region, splits[0], splits[1], splits[2], splits[3])
                    val pads: IntArray = (region as AtlasRegion?).pads
                    if (pads != null) patch.setPadding(pads[0], pads[1], pads[2], pads[3])
                }
            }
            if (patch == null) patch = NinePatch(region)
            add(name, patch, NinePatch::class.java)
            patch
        } catch (ex: GdxRuntimeException) {
            throw GdxRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: $name")
        }
    }

    /**
     * Returns a registered sprite. If no sprite is found but a region exists with the name, a sprite is created from the region
     * and stored in the skin. If the region is an [AtlasRegion] then an [AtlasSprite] is used if the region has been
     * whitespace stripped or packed rotated 90 degrees.
     */
    fun getSprite(name: String): Sprite? {
        var sprite: Sprite? = optional<T>(name, Sprite::class.java)
        return if (sprite != null) sprite else try {
            val textureRegion: TextureRegion? = getRegion(name)
            if (textureRegion is AtlasRegion) {
                val region: AtlasRegion? = textureRegion as AtlasRegion?
                if (region.rotate || region.packedWidth !== region.originalWidth || region.packedHeight !== region.originalHeight) sprite = AtlasSprite(region)
            }
            if (sprite == null) sprite = Sprite(textureRegion)
            add(name, sprite, Sprite::class.java)
            sprite
        } catch (ex: GdxRuntimeException) {
            throw GdxRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: $name")
        }
    }

    /**
     * Returns a registered drawable. If no drawable is found but a region, ninepatch, or sprite exists with the name, then the
     * appropriate drawable is created and stored in the skin.
     */
    fun getDrawable(name: String): Drawable? {
        var drawable: Drawable? = optional<T>(name, Drawable::class.java)
        if (drawable != null) return drawable

        // Use texture or texture region. If it has splits, use ninepatch. If it has rotation or whitespace stripping, use sprite.
        try {
            val textureRegion: TextureRegion? = getRegion(name)
            if (textureRegion is AtlasRegion) {
                val region: AtlasRegion? = textureRegion as AtlasRegion?
                if (region.splits != null) drawable = NinePatchDrawable(getPatch(name)) else if (region.rotate || region.packedWidth !== region.originalWidth || region.packedHeight !== region.originalHeight) drawable = SpriteDrawable(getSprite(name))
            }
            if (drawable == null) drawable = TextureRegionDrawable(textureRegion)
        } catch (ignored: GdxRuntimeException) {
        }

        // Check for explicit registration of ninepatch, sprite, or tiled drawable.
        if (drawable == null) {
            val patch: NinePatch = optional<T>(name, NinePatch::class.java)
            if (patch != null) drawable = NinePatchDrawable(patch) else {
                val sprite: Sprite = optional<T>(name, Sprite::class.java)
                if (sprite != null) drawable = SpriteDrawable(sprite) else throw GdxRuntimeException(
                    "No Drawable, NinePatch, TextureRegion, Texture, or Sprite registered with name: $name")
            }
        }
        if (drawable is BaseDrawable) (drawable as BaseDrawable?).setName(name)
        add(name, drawable, Drawable::class.java)
        return drawable
    }

    /**
     * Returns the name of the specified style object, or null if it is not in the skin. This compares potentially every style
     * object in the skin of the same type as the specified style, which may be a somewhat expensive operation.
     */
    fun find(resource: Any?): String? {
        if (resource == null) throw java.lang.IllegalArgumentException("style cannot be null.")
        val typeResources: ObjectMap<String, Any> = resources.get(resource.javaClass) ?: return null
        return typeResources.findKey(resource, true)
    }

    /**
     * Returns a copy of a drawable found in the skin via [.getDrawable].
     */
    fun newDrawable(name: String): Drawable {
        return newDrawable(getDrawable(name))
    }

    /**
     * Returns a tinted copy of a drawable found in the skin via [.getDrawable].
     */
    fun newDrawable(name: String, r: Float, g: Float, b: Float, a: Float): Drawable {
        return newDrawable(getDrawable(name), Color(r, g, b, a))
    }

    /**
     * Returns a tinted copy of a drawable found in the skin via [.getDrawable].
     */
    fun newDrawable(name: String, tint: Color?): Drawable {
        return newDrawable(getDrawable(name), tint)
    }

    /**
     * Returns a copy of the specified drawable.
     */
    fun newDrawable(drawable: Drawable): Drawable {
        if (drawable is TiledDrawable) return TiledDrawable(drawable as TiledDrawable)
        if (drawable is TextureRegionDrawable) return TextureRegionDrawable(drawable as TextureRegionDrawable)
        if (drawable is NinePatchDrawable) return NinePatchDrawable(drawable as NinePatchDrawable)
        if (drawable is SpriteDrawable) return SpriteDrawable(drawable as SpriteDrawable)
        throw GdxRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass())
    }

    /**
     * Returns a tinted copy of a drawable found in the skin via [.getDrawable].
     */
    fun newDrawable(drawable: Drawable?, r: Float, g: Float, b: Float, a: Float): Drawable {
        return newDrawable(drawable, Color(r, g, b, a))
    }

    /**
     * Returns a tinted copy of a drawable found in the skin via [.getDrawable].
     */
    fun newDrawable(drawable: Drawable, tint: Color): Drawable {
        val newDrawable: Drawable
        newDrawable = if (drawable is TextureRegionDrawable) (drawable as TextureRegionDrawable).tint(tint) else if (drawable is NinePatchDrawable) (drawable as NinePatchDrawable).tint(tint) else if (drawable is SpriteDrawable) (drawable as SpriteDrawable).tint(tint) else throw GdxRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass())
        if (newDrawable is BaseDrawable) {
            val named: BaseDrawable = newDrawable as BaseDrawable
            if (drawable is BaseDrawable) named.setName((drawable as BaseDrawable).getName().toString() + " (" + tint + ")") else named.setName(" ($tint)")
        }
        return newDrawable
    }

    /**
     * Sets the style on the actor to disabled or enabled. This is done by appending "-disabled" to the style name when enabled is
     * false, and removing "-disabled" from the style name when enabled is true. A method named "getStyle" is called the actor via
     * reflection and the name of that style is found in the skin. If the actor doesn't have a "getStyle" method or the style was
     * not found in the skin, no exception is thrown and the actor is left unchanged.
     */
    fun setEnabled(actor: Actor, enabled: Boolean) {
        // Get current style.
        var method: Method? = findMethod(actor.getClass(), "getStyle") ?: return
        var style: Any
        style = try {
            method.invoke(actor)
        } catch (ignored: java.lang.Exception) {
            return
        }
        // Determine new style.
        var name = find(style) ?: return
        name = name.replace("-disabled", "") + if (enabled) "" else "-disabled"
        style = get(name, style.javaClass)!!
        // Set new style.
        method = findMethod(actor.getClass(), "setStyle")
        if (method == null) return
        try {
            method.invoke(actor, style)
        } catch (ignored: java.lang.Exception) {
        }
    }

    /**
     * Returns the [TextureAtlas] passed to this skin constructor, or null.
     */
    fun getAtlas(): TextureAtlas? {
        return atlas
    }

    /**
     * Disposes the [TextureAtlas] and all [Disposable] resources in the skin.
     */
    fun dispose() {
        if (atlas != null) atlas.dispose()
        for (entry in resources.values()) {
            for (resource in entry.values()) if (resource is Disposable) (resource as Disposable).dispose()
        }
    }

    protected fun getJsonLoader(skinFile: FileHandle): Json {
        val skin = this
        val json: Json = object : Json() {
            private static
            val parentFieldName = "parent"
            fun <T> readValue(type: java.lang.Class<T>?, elementType: java.lang.Class?, jsonData: JsonValue?): T {
                // If the JSON is a string but the type is not, look up the actual value by name.
                return if (jsonData != null && jsonData.isString() && !ClassReflection.isAssignableFrom(CharSequence::class.java, type)) get<T>(jsonData.asString(), type) else super.readValue(type, elementType, jsonData)
            }

            protected fun ignoreUnknownField(type: java.lang.Class?, fieldName: String): Boolean {
                return fieldName == parentFieldName
            }

            fun readFields(`object`: Any, jsonMap: JsonValue) {
                if (jsonMap.has(parentFieldName)) {
                    val parentName: String = readValue(parentFieldName, String::class.java, jsonMap)
                    var parentType: java.lang.Class = `object`.javaClass
                    while (true) {
                        try {
                            copyFields(get<T>(parentName, parentType), `object`)
                            break
                        } catch (ex: GdxRuntimeException) { // Parent resource doesn't exist.
                            parentType = parentType.getSuperclass() // Try resource for super class.
                            if (parentType == Any::class.java) {
                                val se = SerializationException(
                                    "Unable to find parent resource with name: $parentName")
                                se.addTrace(jsonMap.child.trace())
                                throw se
                            }
                        }
                    }
                }
                super.readFields(`object`, jsonMap)
            }
        }
        json.setTypeName(null)
        json.setUsePrototypes(false)
        json.setSerializer(Skin::class.java, object : ReadOnlySerializer<Skin?>() {
            fun read(json: Json, typeToValueMap: JsonValue, ignored: java.lang.Class?): Skin {
                var valueMap: JsonValue = typeToValueMap.child
                while (valueMap != null) {
                    try {
                        var type: java.lang.Class = json.getClass(valueMap.name())
                        if (type == null) type = ClassReflection.forName(valueMap.name())
                        readNamedObjects(json, type, valueMap)
                    } catch (ex: ReflectionException) {
                        throw SerializationException(ex)
                    }
                    valueMap = valueMap.next
                }
                return skin
            }

            private fun readNamedObjects(json: Json, type: java.lang.Class?, valueMap: JsonValue?) {
                val addType: java.lang.Class = if (type == TintedDrawable::class.java) Drawable::class.java else type
                var valueEntry: JsonValue = valueMap.child
                while (valueEntry != null) {
                    val `object`: Any = json.readValue(type, valueEntry)
                    if (`object` == null) {
                        valueEntry = valueEntry.next
                        continue
                    }
                    try {
                        add(valueEntry.name, `object`, addType)
                        if (addType != Drawable::class.java && ClassReflection.isAssignableFrom(Drawable::class.java, addType)) add(valueEntry.name, `object`, Drawable::class.java)
                    } catch (ex: java.lang.Exception) {
                        throw SerializationException(
                            "Error reading " + ClassReflection.getSimpleName(type).toString() + ": " + valueEntry.name, ex)
                    }
                    valueEntry = valueEntry.next
                }
            }
        })
        json.setSerializer(BitmapFont::class.java, object : ReadOnlySerializer<BitmapFont?>() {
            fun read(json: Json, jsonData: JsonValue?, type: java.lang.Class?): BitmapFont {
                val path: String = json.readValue("file", String::class.java, jsonData)
                val scaledSize: Int = json.readValue("scaledSize", Int::class.javaPrimitiveType, -1, jsonData)
                val flip: Boolean = json.readValue("flip", Boolean::class.java, false, jsonData)
                val markupEnabled: Boolean = json.readValue("markupEnabled", Boolean::class.java, false, jsonData)
                var fontFile: FileHandle = skinFile.parent().child(path)
                if (!fontFile.exists()) fontFile = Gdx.files.internal(path)
                if (!fontFile.exists()) throw SerializationException("Font file not found: $fontFile")

                // Use a region with the same name as the font, else use a PNG file in the same directory as the FNT file.
                val regionName: String = fontFile.nameWithoutExtension()
                return try {
                    val font: BitmapFont
                    val regions: Array<TextureRegion?>? = skin.getRegions(regionName)
                    if (regions != null) font = BitmapFont(BitmapFontData(fontFile, flip), regions, true) else {
                        val region: TextureRegion = skin.optional<T>(regionName, TextureRegion::class.java)
                        if (region != null) font = BitmapFont(fontFile, region, flip) else {
                            val imageFile: FileHandle = fontFile.parent().child("$regionName.png")
                            if (imageFile.exists()) font = BitmapFont(fontFile, imageFile, flip) else font = BitmapFont(fontFile, flip)
                        }
                    }
                    font.getData().markupEnabled = markupEnabled
                    // Scaled size is the desired cap height to scale the font to.
                    if (scaledSize != -1) font.getData().setScale(scaledSize / font.getCapHeight())
                    font
                } catch (ex: RuntimeException) {
                    throw SerializationException("Error loading bitmap font: $fontFile", ex)
                }
            }
        })
        json.setSerializer(Color::class.java, object : ReadOnlySerializer<Color?>() {
            fun read(json: Json, jsonData: JsonValue, type: java.lang.Class?): Color {
                if (jsonData.isString()) return get<T>(jsonData.asString(), Color::class.java)
                val hex: String = json.readValue("hex", String::class.java, null as String?, jsonData)
                if (hex != null) return Color.valueOf(hex)
                val r: Float = json.readValue("r", Float::class.javaPrimitiveType, 0f, jsonData)
                val g: Float = json.readValue("g", Float::class.javaPrimitiveType, 0f, jsonData)
                val b: Float = json.readValue("b", Float::class.javaPrimitiveType, 0f, jsonData)
                val a: Float = json.readValue("a", Float::class.javaPrimitiveType, 1f, jsonData)
                return Color(r, g, b, a)
            }
        })
        json.setSerializer(TintedDrawable::class.java, object : ReadOnlySerializer() {
            fun read(json: Json, jsonData: JsonValue, type: java.lang.Class?): Any {
                val name: String = json.readValue("name", String::class.java, jsonData)
                val color: Color = json.readValue("color", Color::class.java, jsonData)
                    ?: throw SerializationException("TintedDrawable missing color: $jsonData")
                val drawable: Drawable = newDrawable(name, color)
                if (drawable is BaseDrawable) {
                    val named: BaseDrawable = drawable as BaseDrawable
                    named.setName(jsonData.name.toString() + " (" + name + ", " + color + ")")
                }
                return drawable
            }
        })
        for (entry in jsonClassTags) json.addClassTag(entry.key, entry.value)
        return json
    }

    /**
     * Returns a map of [class tags][Json.addClassTag] that will be used when loading skin JSON. The map can
     * be modified before calling [.load]. By default the map is populated with the simple class names of libGDX
     * classes commonly used in skins.
     */
    fun getJsonClassTags(): ObjectMap<String, java.lang.Class> {
        return jsonClassTags
    }

    /**
     * @author Nathan Sweet
     */
    class TintedDrawable {

        var name: String? = null
        var color: Color? = null
    }

    companion object {
        private val defaultTagClasses: Array<java.lang.Class> = arrayOf<java.lang.Class>(BitmapFont::class.java, Color::class.java, TintedDrawable::class.java, NinePatchDrawable::class.java,
            SpriteDrawable::class.java, TextureRegionDrawable::class.java, TiledDrawable::class.java, ButtonStyle::class.java,
            CheckBoxStyle::class.java, ImageButtonStyle::class.java, ImageTextButtonStyle::class.java,
            LabelStyle::class.java, ListStyle::class.java, ProgressBar.ProgressBarStyle::class.java, ScrollPane.ScrollPaneStyle::class.java,
            SelectBox.SelectBoxStyle::class.java, SliderStyle::class.java, SplitPane.SplitPaneStyle::class.java, TextButton.TextButtonStyle::class.java,
            TextField.TextFieldStyle::class.java, TextTooltip.TextTooltipStyle::class.java, Touchpad.TouchpadStyle::class.java, Tree.TreeStyle::class.java,
            WindowStyle::class.java)

        private fun findMethod(type: java.lang.Class, name: String): Method? {
            val methods: Array<Method> = ClassReflection.getMethods(type)
            var i = 0
            val n = methods.size
            while (i < n) {
                val method: Method = methods[i]
                if (method.getName().equals(name)) return method
                i++
            }
            return null
        }
    }

    init {
        for (c in defaultTagClasses) jsonClassTags.put(c.getSimpleName(), c)
    }
}
