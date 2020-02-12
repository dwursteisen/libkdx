package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.reflect.ClassReflection

/**
 * Reads/writes Java objects to/from JSON, automatically. See the wiki for usage:
 * https://github.com/libgdx/libgdx/wiki/Reading-%26-writing-JSON
 *
 * @author Nathan Sweet
 */
class Json {

    private val debug = false

    private var writer: JsonWriter? = null
    private var typeName: String? = "class"
    private var usePrototypes = true
    private var outputType: OutputType? = null
    private var quoteLongValues = false
    private var ignoreUnknownFields = false
    private var ignoreDeprecated = false
    private var readDeprecated = false
    private var enumNames = true
    private var sortFields = false
    private var defaultSerializer: Json.Serializer? = null
    private val typeToFields: ObjectMap<java.lang.Class?, OrderedMap<String, FieldMetadata>?> = ObjectMap<Any?, Any?>()
    private val tagToClass: ObjectMap<String?, java.lang.Class?> = ObjectMap<Any?, Any?>()
    private val classToTag: ObjectMap<java.lang.Class?, String?> = ObjectMap<Any?, Any?>()
    private val classToSerializer: ObjectMap<java.lang.Class?, Json.Serializer?> = ObjectMap<Any?, Any?>()
    private val classToDefaultValues: ObjectMap<java.lang.Class?, kotlin.Array<Any?>?> = ObjectMap<Any?, Any?>()
    private val equals1 = arrayOf<Any?>(null), private
    val equals2 = arrayOf<Any?>(null)

    fun Json() {
        outputType = OutputType.minimal
    }

    fun Json(outputType: OutputType?) {
        this.outputType = outputType
    }

    /**
     * When true, fields in the JSON that are not found on the class will not throw a [SerializationException]. Default is
     * false.
     */
    fun setIgnoreUnknownFields(ignoreUnknownFields: Boolean) {
        this.ignoreUnknownFields = ignoreUnknownFields
    }

    fun getIgnoreUnknownFields(): Boolean {
        return ignoreUnknownFields
    }

    /**
     * When true, fields with the [Deprecated] annotation will not be read or written. Default is false.
     *
     * @see .setReadDeprecated
     * @see .setDeprecated
     */
    fun setIgnoreDeprecated(ignoreDeprecated: Boolean) {
        this.ignoreDeprecated = ignoreDeprecated
    }

    /**
     * When true, fields with the [Deprecated] annotation will be read (but not written) when
     * [.setIgnoreDeprecated] is true. Default is false.
     *
     * @see .setDeprecated
     */
    fun setReadDeprecated(readDeprecated: Boolean) {
        this.readDeprecated = readDeprecated
    }

    /**
     * Default is [OutputType.minimal].
     *
     * @see JsonWriter.setOutputType
     */
    fun setOutputType(outputType: OutputType?) {
        this.outputType = outputType
    }

    /**
     * Default is false.
     *
     * @see JsonWriter.setQuoteLongValues
     */
    fun setQuoteLongValues(quoteLongValues: Boolean) {
        this.quoteLongValues = quoteLongValues
    }

    /**
     * When true, [Enum.name] is used to write enum values. When false, [Enum.toString] is used which may not be
     * unique. Default is true.
     */
    fun setEnumNames(enumNames: Boolean) {
        this.enumNames = enumNames
    }

    /**
     * Sets a tag to use instead of the fully qualifier class name. This can make the JSON easier to read.
     */
    fun addClassTag(tag: String?, type: java.lang.Class?) {
        tagToClass.put(tag, type)
        classToTag.put(type, tag)
    }

    /**
     * Returns the class for the specified tag, or null.
     */
    fun getClass(tag: String?): java.lang.Class? {
        return tagToClass[tag]
    }

    /**
     * Returns the tag for the specified class, or null.
     */
    fun getTag(type: java.lang.Class?): String? {
        return classToTag[type]
    }

    /**
     * Sets the name of the JSON field to store the Java class name or class tag when required to avoid ambiguity during
     * deserialization. Set to null to never output this information, but be warned that deserialization may fail. Default is
     * "class".
     */
    fun setTypeName(typeName: String?) {
        this.typeName = typeName
    }

    /**
     * Sets the serializer to use when the type being deserialized is not known (null).
     *
     * @param defaultSerializer May be null.
     */
    fun setDefaultSerializer(defaultSerializer: Json.Serializer?) {
        this.defaultSerializer = defaultSerializer
    }

    /**
     * Registers a serializer to use for the specified type instead of the default behavior of serializing all of an objects
     * fields.
     */
    fun <T> setSerializer(type: java.lang.Class<T>?, serializer: Json.Serializer<T>?) {
        classToSerializer.put(type, serializer)
    }

    fun <T> getSerializer(type: java.lang.Class<T>?): Json.Serializer<T>? {
        return classToSerializer[type]
    }

    /**
     * When true, field values that are identical to a newly constructed instance are not written. Default is true.
     */
    fun setUsePrototypes(usePrototypes: Boolean) {
        this.usePrototypes = usePrototypes
    }

    /**
     * Sets the type of elements in a collection. When the element type is known, the class for each element in the collection
     * does not need to be written unless different from the element type.
     */
    fun setElementType(type: java.lang.Class, fieldName: String, elementType: java.lang.Class?) {
        val metadata: FieldMetadata = getFields(type).get(fieldName)
            ?: throw SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")")
        metadata.elementType = elementType
    }

    /**
     * The specified field will be treated as if it has or does not have the [Deprecated] annotation.
     *
     * @see .setIgnoreDeprecated
     * @see .setReadDeprecated
     */
    fun setDeprecated(type: java.lang.Class, fieldName: String, deprecated: Boolean) {
        val metadata: FieldMetadata = getFields(type).get(fieldName)
            ?: throw SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")")
        metadata.deprecated = deprecated
    }

    /**
     * When true, fields are sorted alphabetically when written, otherwise the source code order is used. Default is false.
     */
    fun setSortFields(sortFields: Boolean) {
        this.sortFields = sortFields
    }

    private fun getFields(type: java.lang.Class): OrderedMap<String, FieldMetadata> {
        val fields: OrderedMap<String, FieldMetadata>? = typeToFields[type]
        if (fields != null) return fields
        val classHierarchy: Array<java.lang.Class?> = Array<Any?>()
        var nextClass: java.lang.Class = type
        while (nextClass != Any::class.java) {
            classHierarchy.add(nextClass)
            nextClass = nextClass.getSuperclass()
        }
        val allFields: ArrayList<Field> = ArrayList()
        for (i in classHierarchy.size - 1 downTo 0) Collections.addAll(allFields, ClassReflection.getDeclaredFields(classHierarchy[i]))
        val nameToField: OrderedMap<String, FieldMetadata> = OrderedMap<Any?, Any?>(allFields.size())
        var i = 0
        val n: Int = allFields.size()
        while (i < n) {
            val field: Field = allFields[i]
            if (field.isTransient()) {
                i++
                continue
            }
            if (field.isStatic()) {
                i++
                continue
            }
            if (field.isSynthetic()) {
                i++
                continue
            }
            if (!field.isAccessible()) {
                try {
                    field.setAccessible(true)
                } catch (ex: AccessControlException) {
                    i++
                    continue
                }
            }
            nameToField.put(field.getName(), FieldMetadata(field))
            i++
        }
        if (sortFields) nameToField.keys.sort()
        typeToFields.put(type, nameToField)
        return nameToField
    }

    fun toJson(`object`: Any?): String? {
        return toJson(`object`, `object`?.javaClass, null as java.lang.Class?)
    }

    fun toJson(`object`: Any?, knownType: java.lang.Class?): String? {
        return toJson(`object`, knownType, null as java.lang.Class?)
    }

    /**
     * @param knownType   May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun toJson(`object`: Any?, knownType: java.lang.Class?, elementType: java.lang.Class?): String? {
        val buffer = StringWriter()
        toJson(`object`, knownType, elementType, buffer)
        return buffer.toString()
    }

    fun toJson(`object`: Any?, file: FileHandle?) {
        toJson(`object`, `object`?.javaClass, null, file)
    }

    /**
     * @param knownType May be null if the type is unknown.
     */
    fun toJson(`object`: Any?, knownType: java.lang.Class?, file: FileHandle?) {
        toJson(`object`, knownType, null, file)
    }

    /**
     * @param knownType   May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun toJson(`object`: Any?, knownType: java.lang.Class?, elementType: java.lang.Class?, file: FileHandle) {
        var writer: Writer? = null
        try {
            writer = file.writer(false, "UTF-8")
            toJson(`object`, knownType, elementType, writer)
        } catch (ex: java.lang.Exception) {
            throw SerializationException("Error writing file: $file", ex)
        } finally {
            StreamUtils.closeQuietly(writer)
        }
    }

    fun toJson(`object`: Any?, writer: Writer?) {
        toJson(`object`, `object`?.javaClass, null, writer)
    }

    /**
     * @param knownType May be null if the type is unknown.
     */
    fun toJson(`object`: Any?, knownType: java.lang.Class?, writer: Writer?) {
        toJson(`object`, knownType, null, writer)
    }

    /**
     * @param knownType   May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun toJson(`object`: Any?, knownType: java.lang.Class?, elementType: java.lang.Class?, writer: Writer?) {
        setWriter(writer)
        try {
            writeValue(`object`, knownType, elementType)
        } finally {
            StreamUtils.closeQuietly(this.writer)
            this.writer = null
        }
    }

    /**
     * Sets the writer where JSON output will be written. This is only necessary when not using the toJson methods.
     */
    fun setWriter(writer: Writer?) {
        var writer: Writer? = writer
        if (writer !is JsonWriter) writer = JsonWriter(writer)
        this.writer = writer
        this.writer!!.setOutputType(outputType)
        this.writer!!.setQuoteLongValues(quoteLongValues)
    }

    fun getWriter(): JsonWriter? {
        return writer
    }

    /**
     * Writes all fields of the specified object to the current JSON object.
     */
    fun writeFields(`object`: Any) {
        val type: java.lang.Class = `object`.javaClass
        val defaultValues = getDefaultValues(type)
        val fields: OrderedMap<String, FieldMetadata> = getFields(type)
        var defaultIndex = 0
        val fieldNames = fields.orderedKeys()
        var i = 0
        val n = fieldNames.size
        while (i < n) {
            val metadata: FieldMetadata = fields.get(fieldNames[i])
            if (ignoreDeprecated && metadata.deprecated) {
                i++
                continue
            }
            val field: Field = metadata.field
            try {
                val value: Any = field.get(`object`)
                if (defaultValues != null) {
                    val defaultValue = defaultValues[defaultIndex++]
                    if (value == null && defaultValue == null) {
                        i++
                        continue
                    }
                    if (value != null && defaultValue != null) {
                        if (value == defaultValue) {
                            i++
                            continue
                        }
                        if (value.javaClass.isArray() && defaultValue.javaClass.isArray()) {
                            equals1[0] = value
                            equals2[0] = defaultValue
                            if (Arrays.deepEquals(equals1, equals2)) {
                                i++
                                continue
                            }
                        }
                    }
                }
                if (debug) java.lang.System.out.println("Writing field: " + field.getName().toString() + " (" + type.getName().toString() + ")")
                writer!!.name(field.getName())
                writeValue(value, field.getType(), metadata.elementType)
            } catch (ex: ReflectionException) {
                throw SerializationException("Error accessing field: " + field.getName().toString() + " (" + type.getName().toString() + ")", ex)
            } catch (ex: SerializationException) {
                ex.addTrace(field.toString() + " (" + type.getName() + ")")
                throw ex
            } catch (runtimeEx: java.lang.Exception) {
                val ex = SerializationException(runtimeEx)
                ex.addTrace(field.toString() + " (" + type.getName() + ")")
                throw ex
            }
            i++
        }
    }

    private fun getDefaultValues(type: java.lang.Class): kotlin.Array<Any?>? {
        if (!usePrototypes) return null
        if (classToDefaultValues.containsKey(type)) return classToDefaultValues[type]
        val `object`: Any
        `object` = try {
            newInstance(type)
        } catch (ex: java.lang.Exception) {
            classToDefaultValues.put(type, null)
            return null
        }
        val fields: OrderedMap<String, FieldMetadata> = getFields(type)
        val values = arrayOfNulls<Any>(fields.size)
        classToDefaultValues.put(type, values)
        var defaultIndex = 0
        val fieldNames = fields.orderedKeys()
        var i = 0
        val n = fieldNames.size
        while (i < n) {
            val metadata: FieldMetadata = fields.get(fieldNames[i])
            if (ignoreDeprecated && metadata.deprecated) {
                i++
                continue
            }
            val field: Field = metadata.field
            try {
                values[defaultIndex++] = field.get(`object`)
            } catch (ex: ReflectionException) {
                throw SerializationException("Error accessing field: " + field.getName().toString() + " (" + type.getName().toString() + ")", ex)
            } catch (ex: SerializationException) {
                ex.addTrace(field.toString() + " (" + type.getName() + ")")
                throw ex
            } catch (runtimeEx: RuntimeException) {
                val ex = SerializationException(runtimeEx)
                ex.addTrace(field.toString() + " (" + type.getName() + ")")
                throw ex
            }
            i++
        }
        return values
    }

    /**
     * @see .writeField
     */
    fun writeField(`object`: Any, name: String) {
        writeField(`object`, name, name, null)
    }

    /**
     * @param elementType May be null if the type is unknown.
     * @see .writeField
     */
    fun writeField(`object`: Any, name: String, elementType: java.lang.Class?) {
        writeField(`object`, name, name, elementType)
    }

    /**
     * @see .writeField
     */
    fun writeField(`object`: Any, fieldName: String, jsonName: String?) {
        writeField(`object`, fieldName, jsonName, null)
    }

    /**
     * Writes the specified field to the current JSON object.
     *
     * @param elementType May be null if the type is unknown.
     */
    fun writeField(`object`: Any, fieldName: String, jsonName: String?, elementType: java.lang.Class?) {
        var elementType: java.lang.Class? = elementType
        val type: java.lang.Class = `object`.javaClass
        val metadata: FieldMetadata = getFields(type).get(fieldName)
            ?: throw SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")")
        val field: Field = metadata.field
        if (elementType == null) elementType = metadata.elementType
        try {
            if (debug) java.lang.System.out.println("Writing field: " + field.getName().toString() + " (" + type.getName().toString() + ")")
            writer!!.name(jsonName)
            writeValue(field.get(`object`), field.getType(), elementType)
        } catch (ex: ReflectionException) {
            throw SerializationException("Error accessing field: " + field.getName().toString() + " (" + type.getName().toString() + ")", ex)
        } catch (ex: SerializationException) {
            ex.addTrace(field.toString() + " (" + type.getName() + ")")
            throw ex
        } catch (runtimeEx: java.lang.Exception) {
            val ex = SerializationException(runtimeEx)
            ex.addTrace(field.toString() + " (" + type.getName() + ")")
            throw ex
        }
    }

    /**
     * Writes the value as a field on the current JSON object, without writing the actual class.
     *
     * @param value May be null.
     * @see .writeValue
     */
    fun writeValue(name: String?, value: Any?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
        if (value == null) writeValue(value, null, null) else writeValue(value, value.javaClass, null)
    }

    /**
     * Writes the value as a field on the current JSON object, writing the class of the object if it differs from the specified
     * known type.
     *
     * @param value     May be null.
     * @param knownType May be null if the type is unknown.
     * @see .writeValue
     */
    fun writeValue(name: String?, value: Any?, knownType: java.lang.Class?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
        writeValue(value, knownType, null)
    }

    /**
     * Writes the value as a field on the current JSON object, writing the class of the object if it differs from the specified
     * known type. The specified element type is used as the default type for collections.
     *
     * @param value       May be null.
     * @param knownType   May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun writeValue(name: String?, value: Any?, knownType: java.lang.Class?, elementType: java.lang.Class?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
        writeValue(value, knownType, elementType)
    }

    /**
     * Writes the value, without writing the class of the object.
     *
     * @param value May be null.
     */
    fun writeValue(value: Any?) {
        if (value == null) writeValue(value, null, null) else writeValue(value, value.javaClass, null)
    }

    /**
     * Writes the value, writing the class of the object if it differs from the specified known type.
     *
     * @param value     May be null.
     * @param knownType May be null if the type is unknown.
     */
    fun writeValue(value: Any?, knownType: java.lang.Class?) {
        writeValue(value, knownType, null)
    }

    /**
     * Writes the value, writing the class of the object if it differs from the specified known type. The specified element type
     * is used as the default type for collections.
     *
     * @param value       May be null.
     * @param knownType   May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     */
    fun writeValue(value: Any?, knownType: java.lang.Class?, elementType: java.lang.Class?) {
        var knownType: java.lang.Class? = knownType
        var elementType: java.lang.Class? = elementType
        try {
            if (value == null) {
                writer!!.value(null)
                return
            }
            if (knownType != null && knownType.isPrimitive() || knownType == String::class.java || knownType == Int::class.java || knownType == Boolean::class.java || knownType == Float::class.java || knownType == Long::class.java || knownType == Double::class.java || knownType == Short::class.java || knownType == Byte::class.java || knownType == Char::class.java) {
                writer!!.value(value)
                return
            }
            var actualType: java.lang.Class = value.javaClass
            if (actualType.isPrimitive() || actualType == String::class.java || actualType == Int::class.java || actualType == Boolean::class.java || actualType == Float::class.java || actualType == Long::class.java || actualType == Double::class.java || actualType == Short::class.java || actualType == Byte::class.java || actualType == Char::class.java) {
                writeObjectStart(actualType, null)
                writeValue("value", value)
                writeObjectEnd()
                return
            }
            if (value is Json.Serializable) {
                writeObjectStart(actualType, knownType)
                (value as Json.Serializable).write(this)
                writeObjectEnd()
                return
            }
            val serializer: Json.Serializer? = classToSerializer[actualType]
            if (serializer != null) {
                serializer.write(this, value, knownType)
                return
            }

            // JSON array special cases.
            if (value is Array<*>) {
                if (knownType != null && actualType != knownType && actualType != Array::class.java) throw SerializationException("""
    Serialization of an Array other than the known type is not supported.
    Known type: $knownType
    Actual type: $actualType
    """.trimIndent())
                writeArrayStart()
                val array = value
                var i = 0
                val n = array.size
                while (i < n) {
                    writeValue(array[i], elementType, null)
                    i++
                }
                writeArrayEnd()
                return
            }
            if (value is Queue<*>) {
                if (knownType != null && actualType != knownType && actualType != Queue::class.java) throw SerializationException("""
    Serialization of a Queue other than the known type is not supported.
    Known type: $knownType
    Actual type: $actualType
    """.trimIndent())
                writeArrayStart()
                val queue = value
                var i = 0
                val n = queue.size
                while (i < n) {
                    writeValue(queue[i], elementType, null)
                    i++
                }
                writeArrayEnd()
                return
            }
            if (value is Collection) {
                if (typeName != null && actualType != ArrayList::class.java && (knownType == null || knownType != actualType)) {
                    writeObjectStart(actualType, knownType)
                    writeArrayStart("items")
                    for (item in value) writeValue(item, elementType, null)
                    writeArrayEnd()
                    writeObjectEnd()
                } else {
                    writeArrayStart()
                    for (item in value) writeValue(item, elementType, null)
                    writeArrayEnd()
                }
                return
            }
            if (actualType.isArray()) {
                if (elementType == null) elementType = actualType.getComponentType()
                val length: Int = ArrayReflection.getLength(value)
                writeArrayStart()
                for (i in 0 until length) writeValue(ArrayReflection.get(value, i), elementType, null)
                writeArrayEnd()
                return
            }

            // JSON object special cases.
            if (value is ObjectMap<*, *>) {
                if (knownType == null) knownType = ObjectMap::class.java
                writeObjectStart(actualType, knownType)
                for (entry in value.entries()!!) {
                    writer!!.name(convertToString(entry.key))
                    writeValue(entry.value, elementType, null)
                }
                writeObjectEnd()
                return
            }
            if (value is ObjectSet<*>) {
                if (knownType == null) knownType = ObjectSet::class.java
                writeObjectStart(actualType, knownType)
                writer!!.name("values")
                writeArrayStart()
                for (entry in value) writeValue(entry, elementType, null)
                writeArrayEnd()
                writeObjectEnd()
                return
            }
            if (value is IntSet) {
                if (knownType == null) knownType = IntSet::class.java
                writeObjectStart(actualType, knownType)
                writer!!.name("values")
                writeArrayStart()
                val iter: IntSetIterator? = value.iterator()
                while (iter.hasNext) {
                    writeValue(java.lang.Integer.valueOf(iter.next()), Int::class.java, null)
                }
                writeArrayEnd()
                writeObjectEnd()
                return
            }
            if (value is ArrayMap<*, *>) {
                if (knownType == null) knownType = ArrayMap::class.java
                writeObjectStart(actualType, knownType)
                val map = value
                var i = 0
                val n = map.size
                while (i < n) {
                    writer!!.name(convertToString(map.keys[i]))
                    writeValue(map.values[i], elementType, null)
                    i++
                }
                writeObjectEnd()
                return
            }
            if (value is Map) {
                if (knownType == null) knownType = HashMap::class.java
                writeObjectStart(actualType, knownType)
                for (entry in value.entrySet()) {
                    writer!!.name(convertToString(entry.getKey()))
                    writeValue(entry.getValue(), elementType, null)
                }
                writeObjectEnd()
                return
            }

            // Enum special case.
            if (ClassReflection.isAssignableFrom(Enum::class.java, actualType)) {
                if (typeName != null && (knownType == null || knownType != actualType)) {
                    // Ensures that enums with specific implementations (abstract logic) serialize correctly.
                    if (actualType.getEnumConstants() == null) actualType = actualType.getSuperclass()
                    writeObjectStart(actualType, null)
                    writer!!.name("value")
                    writer!!.value(convertToString(value as Enum<*>))
                    writeObjectEnd()
                } else {
                    writer!!.value(convertToString(value as Enum<*>))
                }
                return
            }
            writeObjectStart(actualType, knownType)
            writeFields(value)
            writeObjectEnd()
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
    }

    fun writeObjectStart(name: String?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
        writeObjectStart()
    }

    /**
     * @param knownType May be null if the type is unknown.
     */
    fun writeObjectStart(name: String?, actualType: java.lang.Class, knownType: java.lang.Class?) {
        try {
            writer!!.name(name)
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
        writeObjectStart(actualType, knownType)
    }

    fun writeObjectStart() {
        try {
            writer!!.`object`()
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
    }

    /**
     * Starts writing an object, writing the actualType to a field if needed.
     *
     * @param knownType May be null if the type is unknown.
     */
    fun writeObjectStart(actualType: java.lang.Class, knownType: java.lang.Class?) {
        try {
            writer!!.`object`()
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
        if (knownType == null || knownType != actualType) writeType(actualType)
    }

    fun writeObjectEnd() {
        try {
            writer!!.pop()
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
    }

    fun writeArrayStart(name: String?) {
        try {
            writer!!.name(name)
            writer!!.array()
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
    }

    fun writeArrayStart() {
        try {
            writer!!.array()
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
    }

    fun writeArrayEnd() {
        try {
            writer!!.pop()
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
    }

    fun writeType(type: java.lang.Class) {
        if (typeName == null) return
        var className = getTag(type)
        if (className == null) className = type.getName()
        try {
            writer!![typeName] = className
        } catch (ex: IOException) {
            throw SerializationException(ex)
        }
        if (debug) println("Writing type: " + type.getName())
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, reader: Reader?): T {
        return readValue(type, null, JsonReader().parse(reader)) as T
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, elementType: java.lang.Class?, reader: Reader?): T {
        return readValue(type, elementType, JsonReader().parse(reader)) as T
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, input: InputStream?): T {
        return readValue(type, null, JsonReader().parse(input)) as T
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, elementType: java.lang.Class?, input: InputStream?): T {
        return readValue(type, elementType, JsonReader().parse(input)) as T
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, file: FileHandle): T {
        return try {
            readValue(type, null, JsonReader().parse(file)) as T
        } catch (ex: java.lang.Exception) {
            throw SerializationException("Error reading file: $file", ex)
        }
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, elementType: java.lang.Class?, file: FileHandle): T {
        return try {
            readValue(type, elementType, JsonReader().parse(file)) as T
        } catch (ex: java.lang.Exception) {
            throw SerializationException("Error reading file: $file", ex)
        }
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, data: CharArray?, offset: Int, length: Int): T {
        return readValue(type, null, JsonReader().parse(data, offset, length)) as T
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, elementType: java.lang.Class?, data: CharArray?, offset: Int, length: Int): T {
        return readValue(type, elementType, JsonReader().parse(data, offset, length)) as T
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, json: String?): T {
        return readValue(type, null, JsonReader().parse(json)) as T
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> fromJson(type: java.lang.Class<T>?, elementType: java.lang.Class?, json: String?): T {
        return readValue(type, elementType, JsonReader().parse(json)) as T
    }

    fun readField(`object`: Any?, name: String?, jsonData: JsonValue?) {
        readField(`object`, name, name, null, jsonData)
    }

    fun readField(`object`: Any?, name: String?, elementType: java.lang.Class?, jsonData: JsonValue?) {
        readField(`object`, name, name, elementType, jsonData)
    }

    fun readField(`object`: Any?, fieldName: String?, jsonName: String?, jsonData: JsonValue?) {
        readField(`object`, fieldName, jsonName, null, jsonData)
    }

    /**
     * @param elementType May be null if the type is unknown.
     */
    fun readField(`object`: Any, fieldName: String, jsonName: String?, elementType: java.lang.Class?, jsonMap: JsonValue?) {
        var elementType: java.lang.Class? = elementType
        val type: java.lang.Class = `object`.javaClass
        val metadata: FieldMetadata = getFields(type).get(fieldName)
            ?: throw SerializationException("Field not found: " + fieldName + " (" + type.getName() + ")")
        val field: Field = metadata.field
        if (elementType == null) elementType = metadata.elementType
        readField(`object`, field, jsonName, elementType, jsonMap)
    }

    /**
     * @param object      May be null if the field is static.
     * @param elementType May be null if the type is unknown.
     */
    fun readField(`object`: Any?, field: Field, jsonName: String?, elementType: java.lang.Class?, jsonMap: JsonValue) {
        val jsonValue = jsonMap[jsonName] ?: return
        try {
            field.set(`object`, readValue(field.getType(), elementType, jsonValue))
        } catch (ex: ReflectionException) {
            throw SerializationException("Error accessing field: " + field.getName().toString() + " (" + field.getDeclaringClass().getName().toString() + ")", ex)
        } catch (ex: SerializationException) {
            ex.addTrace(field.getName().toString() + " (" + field.getDeclaringClass().getName() + ")")
            throw ex
        } catch (runtimeEx: RuntimeException) {
            val ex = SerializationException(runtimeEx)
            ex.addTrace(jsonValue.trace())
            ex.addTrace(field.getName().toString() + " (" + field.getDeclaringClass().getName() + ")")
            throw ex
        }
    }

    fun readFields(`object`: Any, jsonMap: JsonValue) {
        val type: java.lang.Class = `object`.javaClass
        val fields: OrderedMap<String, FieldMetadata> = getFields(type)
        var child = jsonMap.child
        while (child != null) {
            val metadata: FieldMetadata = fields.get(child.name()!!.replace(" ", "_"))
            if (metadata == null) {
                if (child.name == typeName) {
                    child = child.next
                    continue
                }
                if (ignoreUnknownFields || ignoreUnknownField(type, child.name)) {
                    if (debug) println("Ignoring unknown field: " + child.name + " (" + type.getName() + ")")
                    child = child.next
                    continue
                } else {
                    val ex = SerializationException(
                        "Field not found: " + child.name + " (" + type.getName() + ")")
                    ex.addTrace(child.trace())
                    throw ex
                }
            } else {
                if (ignoreDeprecated && !readDeprecated && metadata.deprecated) {
                    child = child.next
                    continue
                }
            }
            val field: Field = metadata.field
            try {
                field.set(`object`, readValue(field.getType(), metadata.elementType, child))
            } catch (ex: ReflectionException) {
                throw SerializationException("Error accessing field: " + field.getName().toString() + " (" + type.getName().toString() + ")", ex)
            } catch (ex: SerializationException) {
                ex.addTrace(field.getName().toString() + " (" + type.getName() + ")")
                throw ex
            } catch (runtimeEx: RuntimeException) {
                val ex = SerializationException(runtimeEx)
                ex.addTrace(child.trace())
                ex.addTrace(field.getName().toString() + " (" + type.getName() + ")")
                throw ex
            }
            child = child.next
        }
    }

    /**
     * Called for each unknown field name encountered by [.readFields] when [.ignoreUnknownFields]
     * is false to determine whether the unknown field name should be ignored.
     *
     * @param type      The object type being read.
     * @param fieldName A field name encountered in the JSON for which there is no matching class field.
     * @return true if the field name should be ignored and an exception won't be thrown by
     * [.readFields].
     */
    protected fun ignoreUnknownField(type: java.lang.Class?, fieldName: String?): Boolean {
        return false
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> readValue(name: String?, type: java.lang.Class<T>?, jsonMap: JsonValue): T {
        return readValue(type, null, jsonMap[name]) as T
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> readValue(name: String?, type: java.lang.Class<T>?, defaultValue: T, jsonMap: JsonValue): T {
        val jsonValue = jsonMap[name] ?: return defaultValue
        return readValue(type, null, jsonValue) as T
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> readValue(name: String?, type: java.lang.Class<T>?, elementType: java.lang.Class?, jsonMap: JsonValue): T {
        return readValue(type, elementType, jsonMap[name]) as T
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> readValue(name: String?, type: java.lang.Class<T>?, elementType: java.lang.Class?, defaultValue: T, jsonMap: JsonValue): T {
        val jsonValue = jsonMap[name]
        return readValue(type, elementType, defaultValue, jsonValue)
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> readValue(type: java.lang.Class<T>?, elementType: java.lang.Class?, defaultValue: T, jsonData: JsonValue?): T {
        return if (jsonData == null) defaultValue else readValue(type, elementType, jsonData) as T
    }

    /**
     * @param type May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> readValue(type: java.lang.Class<T>?, jsonData: JsonValue?): T {
        return readValue(type, null, jsonData) as T
    }

    /**
     * @param type        May be null if the type is unknown.
     * @param elementType May be null if the type is unknown.
     * @return May be null.
     */
    fun <T> readValue(type: java.lang.Class<T>?, elementType: java.lang.Class?, jsonData: JsonValue?): T? {
        var type: java.lang.Class<T>? = type
        var elementType: java.lang.Class? = elementType
        var jsonData: JsonValue? = jsonData ?: return null
        if (jsonData.isObject()) {
            val className = if (typeName == null) null else jsonData.getString(typeName, null)
            if (className != null) {
                type = getClass(className)
                if (type == null) {
                    type = try {
                        ClassReflection.forName(className) as java.lang.Class<T>
                    } catch (ex: ReflectionException) {
                        throw SerializationException(ex)
                    }
                }
            }
            if (type == null) {
                return if (defaultSerializer != null) defaultSerializer.read(this, jsonData, type) else jsonData as T
            }
            if (typeName != null && ClassReflection.isAssignableFrom(Collection::class.java, type)) {
                // JSON object wrapper to specify type.
                jsonData = jsonData.get("items")
                if (jsonData == null) throw SerializationException(
                    "Unable to convert object to collection: " + jsonData + " (" + type.getName() + ")")
            } else {
                val serializer: Json.Serializer? = classToSerializer[type]
                if (serializer != null) return serializer.read(this, jsonData, type)
                if (type == String::class.java || type == Int::class.java || type == Boolean::class.java || type == Float::class.java || type == Long::class.java || type == Double::class.java || type == Short::class.java || type == Byte::class.java || type == Char::class.java || ClassReflection.isAssignableFrom(Enum::class.java, type)) {
                    return readValue("value", type, jsonData)
                }
                val `object` = newInstance(type)
                if (`object` is Json.Serializable) {
                    (`object` as Json.Serializable).read(this, jsonData)
                    return `object` as T
                }

                // JSON object special cases.
                if (`object` is ObjectMap<*, *>) {
                    val result = `object`
                    var child = jsonData.child
                    while (child != null) {
                        result.put(child.name, readValue<Any>(elementType, null, child))
                        child = child.next
                    }
                    return result as T
                }
                if (`object` is ObjectSet<*>) {
                    val result = `object`
                    var child = jsonData.getChild("values")
                    while (child != null) {
                        result.add(readValue<Any>(elementType, null, child))
                        child = child.next
                    }
                    return result as T
                }
                if (`object` is IntSet) {
                    val result = `object`
                    var child = jsonData.getChild("values")
                    while (child != null) {
                        result.add(child.asInt())
                        child = child.next
                    }
                    return result as T
                }
                if (`object` is ArrayMap<*, *>) {
                    val result = `object`
                    var child = jsonData.child
                    while (child != null) {
                        result.put(child.name, readValue<Any>(elementType, null, child))
                        child = child.next
                    }
                    return result as T
                }
                if (`object` is Map) {
                    val result: Map = `object`
                    var child = jsonData.child
                    while (child != null) {
                        if (child.name == typeName) {
                            child = child.next
                            continue
                        }
                        result.put(child.name, readValue<T>(elementType, null, child))
                        child = child.next
                    }
                    return result
                }
                readFields(`object`, jsonData)
                return `object` as T
            }
        }
        if (type != null) {
            val serializer: Json.Serializer? = classToSerializer[type]
            if (serializer != null) return serializer.read(this, jsonData, type)
            if (ClassReflection.isAssignableFrom(Json.Serializable::class.java, type)) {
                // A Serializable may be read as an array, string, etc, even though it will be written as an object.
                val `object` = newInstance(type)
                (`object` as Json.Serializable).read(this, jsonData)
                return `object` as T
            }
        }
        if (jsonData.isArray()) {
            // JSON array special cases.
            if (type == null || type == Any::class.java) type = Array::class.java as java.lang.Class<T>?
            if (ClassReflection.isAssignableFrom(Array::class.java, type)) {
                val result = (if (type == Array::class.java) Array() else newInstance(type) as Array<*>)
                var child = jsonData.child
                while (child != null) {
                    result.add(readValue<Any>(elementType, null, child))
                    child = child.next
                }
                return result as T
            }
            if (ClassReflection.isAssignableFrom(Queue::class.java, type)) {
                val result = (if (type == Queue::class.java) Queue() else newInstance(type) as Queue<*>)
                var child = jsonData.child
                while (child != null) {
                    result.addLast(readValue<Any>(elementType, null, child))
                    child = child.next
                }
                return result as T
            }
            if (ClassReflection.isAssignableFrom(Collection::class.java, type)) {
                val result: Collection = if (type.isInterface()) ArrayList() else newInstance(type) as Collection
                var child = jsonData.child
                while (child != null) {
                    result.add(readValue<T>(elementType, null, child))
                    child = child.next
                }
                return result
            }
            if (type.isArray()) {
                val componentType: java.lang.Class = type.getComponentType()
                if (elementType == null) elementType = componentType
                val result: Any = ArrayReflection.newInstance(componentType, jsonData.size)
                var i = 0
                var child = jsonData.child
                while (child != null) {
                    ArrayReflection.set(result, i++, readValue<T>(elementType, null, child))
                    child = child.next
                }
                return result as T
            }
            throw SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")")
        }
        if (jsonData.isNumber()) {
            try {
                if (type == null || type == Float::class.javaPrimitiveType || type == Float::class.java) return jsonData.asFloat() as T
                if (type == Int::class.javaPrimitiveType || type == Int::class.java) return jsonData.asInt() as T
                if (type == Long::class.javaPrimitiveType || type == Long::class.java) return jsonData.asLong() as T
                if (type == Double::class.javaPrimitiveType || type == Double::class.java) return jsonData.asDouble() as T
                if (type == String::class.java) return jsonData.asString() as T?
                if (type == Short::class.javaPrimitiveType || type == Short::class.java) return jsonData.asShort() as T
                if (type == Byte::class.javaPrimitiveType || type == Byte::class.java) return jsonData.asByte() as T
            } catch (ignored: NumberFormatException) {
            }
            jsonData = JsonValue(jsonData.asString())
        }
        if (jsonData.isBoolean()) {
            try {
                if (type == null || type == Boolean::class.javaPrimitiveType || type == Boolean::class.java) return jsonData.asBoolean() as T
            } catch (ignored: NumberFormatException) {
            }
            jsonData = JsonValue(jsonData.asString())
        }
        if (jsonData.isString()) {
            val string = jsonData.asString()
            if (type == null || type == String::class.java) return string as T?
            try {
                if (type == Int::class.javaPrimitiveType || type == Int::class.java) return java.lang.Integer.valueOf(string)
                if (type == Float::class.javaPrimitiveType || type == Float::class.java) return java.lang.Float.valueOf(string)
                if (type == Long::class.javaPrimitiveType || type == Long::class.java) return java.lang.Long.valueOf(string)
                if (type == Double::class.javaPrimitiveType || type == Double::class.java) return java.lang.Double.valueOf(string)
                if (type == Short::class.javaPrimitiveType || type == Short::class.java) return string!!.toShort() as T
                if (type == Byte::class.javaPrimitiveType || type == Byte::class.java) return java.lang.Byte.valueOf(string)
            } catch (ignored: NumberFormatException) {
            }
            if (type == Boolean::class.javaPrimitiveType || type == Boolean::class.java) return java.lang.Boolean.valueOf(string)
            if (type == Char::class.javaPrimitiveType || type == Char::class.java) return string!![0] as T
            if (ClassReflection.isAssignableFrom(Enum::class.java, type)) {
                val constants = type.getEnumConstants() as kotlin.Array<Enum<*>>
                var i = 0
                val n = constants.size
                while (i < n) {
                    val e = constants[i]
                    if (string == convertToString(e)) return e as T
                    i++
                }
            }
            if (type == CharSequence::class.java) return string as T?
            throw SerializationException("Unable to convert value to required type: " + jsonData + " (" + type.getName() + ")")
        }
        return null
    }

    /**
     * Each field on the `to` object is set to the value for the field with the same name on the `from`
     * object. The `to` object must have at least all the fields of the `from` object with the same name and
     * type.
     */
    fun copyFields(from: Any, to: Any) {
        val toFields: OrderedMap<String, FieldMetadata> = getFields(to.javaClass)
        for (entry in getFields(from.javaClass)) {
            val toField: FieldMetadata = toFields.get(entry.key)
            val fromField: Field = entry.value.field
            if (toField == null) throw SerializationException("To object is missing field: " + entry.key)
            try {
                toField.field.set(to, fromField.get(from))
            } catch (ex: ReflectionException) {
                throw SerializationException("Error copying field: " + fromField.getName(), ex)
            }
        }
    }

    private fun convertToString(e: Enum<*>): String {
        return if (enumNames) e.name else e.toString()
    }

    private fun convertToString(`object`: Any?): String? {
        if (`object` is Enum<*>) return convertToString(`object`)
        return if (`object` is java.lang.Class) (`object` as java.lang.Class?).getName() else `object`.toString()
    }

    protected fun newInstance(type: java.lang.Class?): Any {
        var type: java.lang.Class? = type
        return try {
            ClassReflection.newInstance(type)
        } catch (ex: java.lang.Exception) {
            try {
                // Try a private constructor.
                val constructor: Constructor = ClassReflection.getDeclaredConstructor(type)
                constructor.setAccessible(true)
                return constructor.newInstance()
            } catch (ignored: SecurityException) {
            } catch (ignored: ReflectionException) {
                if (ClassReflection.isAssignableFrom(Enum::class.java, type)) {
                    if (type.getEnumConstants() == null) type = type.getSuperclass()
                    return type.getEnumConstants().get(0)
                }
                if (type.isArray()) throw SerializationException("Encountered JSON object when expected array of type: " + type.getName(), ex) else if (ClassReflection.isMemberClass(type) && !ClassReflection.isStaticClass(type)) throw SerializationException("Class cannot be created (non-static member class): " + type.getName(), ex) else throw SerializationException("Class cannot be created (missing no-arg constructor): " + type.getName(), ex)
            } catch (privateConstructorException: java.lang.Exception) {
                ex = privateConstructorException
            }
            throw SerializationException("Error constructing instance of class: " + type.getName(), ex)
        }
    }

    fun prettyPrint(`object`: Any?): String? {
        return prettyPrint(`object`, 0)
    }

    fun prettyPrint(json: String?): String? {
        return prettyPrint(json, 0)
    }

    fun prettyPrint(`object`: Any?, singleLineColumns: Int): String? {
        return prettyPrint(toJson(`object`), singleLineColumns)
    }

    fun prettyPrint(json: String?, singleLineColumns: Int): String? {
        return JsonReader().parse(json).prettyPrint(outputType, singleLineColumns)
    }

    fun prettyPrint(`object`: Any?, settings: PrettyPrintSettings?): String? {
        return prettyPrint(toJson(`object`), settings)
    }

    fun prettyPrint(json: String?, settings: PrettyPrintSettings?): String? {
        return JsonReader().parse(json).prettyPrint(settings)
    }

    private class FieldMetadata(field: Field) {
        val field: Field
        var elementType: java.lang.Class
        var deprecated: Boolean

        init {
            this.field = field
            val index = if (ClassReflection.isAssignableFrom(ObjectMap::class.java, field.getType())
                || ClassReflection.isAssignableFrom(Map::class.java, field.getType())) 1 else 0
            elementType = field.getElementType(index)
            deprecated = field.isAnnotationPresent(java.lang.Deprecated::class.java)
        }
    }

    interface Serializer<T> {
        fun write(json: Json?, `object`: T, knownType: java.lang.Class?)
        fun read(json: Json?, jsonData: JsonValue?, type: java.lang.Class?): T
    }

    abstract class ReadOnlySerializer<T> : Serializer<T> {
        override fun write(json: Json?, `object`: T, knownType: java.lang.Class?) {}
        abstract override fun read(json: Json?, jsonData: JsonValue?, type: java.lang.Class?): T
    }

    interface Serializable {
        fun write(json: Json?)
        fun read(json: Json?, jsonData: JsonValue?)
    }
}
