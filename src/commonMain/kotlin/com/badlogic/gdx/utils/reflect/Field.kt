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
package com.badlogic.gdx.utils.reflect

import java.lang.ClassNotFoundException
import java.lang.IllegalAccessException
import java.lang.InstantiationException
import java.lang.NoSuchFieldException
import java.lang.NoSuchMethodException
import java.lang.SecurityException
import java.lang.reflect.GenericArrayType
import java.lang.reflect.InvocationTargetException
import kotlin.jvm.Throws

/** Provides information about, and access to, a single field of a class or interface.
 * @author nexsoftware
 */
class Field internal constructor(field: java.lang.reflect.Field) {

    private val field: java.lang.reflect.Field
    /** Returns the name of the field.  */
    val name: String
        get() = field.getName()

    /** Returns a Class object that identifies the declared type for the field.  */
    val type: java.lang.Class
        get() = field.getType()

    /** Returns the Class object representing the class or interface that declares the field.  */
    val declaringClass: java.lang.Class
        get() = field.getDeclaringClass()

    var isAccessible: Boolean
        get() = field.isAccessible()
        set(accessible) {
            field.setAccessible(accessible)
        }

    /** Return true if the field does not include any of the `private`, `protected`, or `public` modifiers.  */
    val isDefaultAccess: Boolean
        get() = !isPrivate && !isProtected && !isPublic

    /** Return true if the field includes the `final` modifier.  */
    val isFinal: Boolean
        get() = java.lang.reflect.Modifier.isFinal(field.getModifiers())

    /** Return true if the field includes the `private` modifier.  */
    val isPrivate: Boolean
        get() = java.lang.reflect.Modifier.isPrivate(field.getModifiers())

    /** Return true if the field includes the `protected` modifier.  */
    val isProtected: Boolean
        get() = java.lang.reflect.Modifier.isProtected(field.getModifiers())

    /** Return true if the field includes the `public` modifier.  */
    val isPublic: Boolean
        get() = java.lang.reflect.Modifier.isPublic(field.getModifiers())

    /** Return true if the field includes the `static` modifier.  */
    val isStatic: Boolean
        get() = java.lang.reflect.Modifier.isStatic(field.getModifiers())

    /** Return true if the field includes the `transient` modifier.  */
    val isTransient: Boolean
        get() = java.lang.reflect.Modifier.isTransient(field.getModifiers())

    /** Return true if the field includes the `volatile` modifier.  */
    val isVolatile: Boolean
        get() = java.lang.reflect.Modifier.isVolatile(field.getModifiers())

    /** Return true if the field is a synthetic field.  */
    val isSynthetic: Boolean
        get() = field.isSynthetic()

    /** If the type of the field is parameterized, returns the Class object representing the parameter type at the specified index,
     * null otherwise.  */
    fun getElementType(index: Int): java.lang.Class? {
        val genericType: java.lang.reflect.Type = field.getGenericType()
        if (genericType is java.lang.reflect.ParameterizedType) {
            val actualTypes: Array<java.lang.reflect.Type> = (genericType as java.lang.reflect.ParameterizedType).getActualTypeArguments()
            if (actualTypes.size - 1 >= index) {
                val actualType: java.lang.reflect.Type = actualTypes[index]
                if (actualType is java.lang.Class) return actualType as java.lang.Class else if (actualType is java.lang.reflect.ParameterizedType) return (actualType as java.lang.reflect.ParameterizedType).getRawType() as java.lang.Class else if (actualType is GenericArrayType) {
                    val componentType: java.lang.reflect.Type = (actualType as GenericArrayType).getGenericComponentType()
                    if (componentType is java.lang.Class) return com.badlogic.gdx.utils.reflect.ArrayReflection.newInstance(componentType as java.lang.Class, 0).javaClass
                }
            }
        }
        return null
    }

    /** Returns true if the field includes an annotation of the provided class type.  */
    fun isAnnotationPresent(annotationType: java.lang.Class<out Annotation?>?): Boolean {
        return field.isAnnotationPresent(annotationType)
    }

    /** Returns an array of [Annotation] objects reflecting all annotations declared by this field,
     * or an empty array if there are none. Does not include inherited annotations.  */
    val declaredAnnotations: Array<com.badlogic.gdx.utils.reflect.Annotation?>
        get() {
            val annotations: Array<Annotation> = field.getDeclaredAnnotations()
            val result: Array<com.badlogic.gdx.utils.reflect.Annotation?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Annotation>(annotations.size)
            for (i in annotations.indices) {
                result[i] = com.badlogic.gdx.utils.reflect.Annotation(annotations[i])
            }
            return result
        }

    /** Returns an [Annotation] object reflecting the annotation provided, or null of this field doesn't
     * have such an annotation. This is a convenience function if the caller knows already which annotation
     * type he's looking for.  */
    fun getDeclaredAnnotation(annotationType: java.lang.Class<out Annotation?>): com.badlogic.gdx.utils.reflect.Annotation? {
        val annotations: Array<Annotation> = field.getDeclaredAnnotations() ?: return null
        for (annotation in annotations) {
            if (annotation.annotationType() == annotationType) {
                return com.badlogic.gdx.utils.reflect.Annotation(annotation)
            }
        }
        return null
    }

    /** Returns the value of the field on the supplied object.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    operator fun get(obj: Any?): Any {
        return try {
            field.get(obj)
        } catch (e: java.lang.IllegalArgumentException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Object is not an instance of $declaringClass", e)
        } catch (e: IllegalAccessException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Illegal access to field: $name", e)
        }
    }

    /** Sets the value of the field on the supplied object.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    operator fun set(obj: Any?, value: Any?) {
        try {
            field.set(obj, value)
        } catch (e: java.lang.IllegalArgumentException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Argument not valid for field: $name", e)
        } catch (e: IllegalAccessException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Illegal access to field: $name", e)
        }
    }

    init {
        this.field = field
    }
}
