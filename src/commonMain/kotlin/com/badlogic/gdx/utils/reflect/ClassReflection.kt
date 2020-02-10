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
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

/** Utilities for Class reflection.
 * @author nexsoftware
 */
object ClassReflection {

    /** Returns the Class object associated with the class or interface with the supplied string name.  */
    @JvmStatic
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun forName(name: String): java.lang.Class {
        return try {
            java.lang.Class.forName(name)
        } catch (e: ClassNotFoundException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Class not found: $name", e)
        }
    }

    /** Returns the simple name of the underlying class as supplied in the source code.  */
    @JvmStatic
    fun getSimpleName(c: java.lang.Class): String {
        return c.getSimpleName()
    }

    /** Determines if the supplied Object is assignment-compatible with the object represented by supplied Class.  */
    @JvmStatic
    fun isInstance(c: java.lang.Class, obj: Any?): Boolean {
        return c.isInstance(obj)
    }

    /** Determines if the class or interface represented by first Class parameter is either the same as, or is a superclass or
     * superinterface of, the class or interface represented by the second Class parameter.  */
    @JvmStatic
    fun isAssignableFrom(c1: java.lang.Class, c2: java.lang.Class?): Boolean {
        return c1.isAssignableFrom(c2)
    }

    /** Returns true if the class or interface represented by the supplied Class is a member class.  */
    @JvmStatic
    fun isMemberClass(c: java.lang.Class): Boolean {
        return c.isMemberClass()
    }

    /** Returns true if the class or interface represented by the supplied Class is a static class.  */
    @JvmStatic
    fun isStaticClass(c: java.lang.Class): Boolean {
        return java.lang.reflect.Modifier.isStatic(c.getModifiers())
    }

    /** Determines if the supplied Class object represents an array class.  */
    fun isArray(c: java.lang.Class): Boolean {
        return c.isArray()
    }

    /** Determines if the supplied Class object represents a primitive type.  */
    fun isPrimitive(c: java.lang.Class): Boolean {
        return c.isPrimitive()
    }

    /** Determines if the supplied Class object represents an enum type.  */
    fun isEnum(c: java.lang.Class): Boolean {
        return c.isEnum()
    }

    /** Determines if the supplied Class object represents an annotation type.  */
    fun isAnnotation(c: java.lang.Class): Boolean {
        return c.isAnnotation()
    }

    /** Determines if the supplied Class object represents an interface type.  */
    fun isInterface(c: java.lang.Class): Boolean {
        return c.isInterface()
    }

    /** Determines if the supplied Class object represents an abstract type.  */
    fun isAbstract(c: java.lang.Class): Boolean {
        return java.lang.reflect.Modifier.isAbstract(c.getModifiers())
    }

    /** Creates a new instance of the class represented by the supplied Class.  */
    @JvmStatic
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun <T> newInstance(c: java.lang.Class<T>): T {
        return try {
            c.newInstance()
        } catch (e: InstantiationException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Could not instantiate instance of class: " + c.getName(), e)
        } catch (e: IllegalAccessException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Could not instantiate instance of class: " + c.getName(), e)
        }
    }

    /** Returns the Class representing the component type of an array. If this class does not represent an array class this method returns null.	  */
    fun getComponentType(c: java.lang.Class): java.lang.Class {
        return c.getComponentType()
    }

    /** Returns an array of [Constructor] containing the public constructors of the class represented by the supplied Class.  */
    fun getConstructors(c: java.lang.Class): Array<com.badlogic.gdx.utils.reflect.Constructor?> {
        val constructors: Array<java.lang.reflect.Constructor> = c.getConstructors()
        val result: Array<com.badlogic.gdx.utils.reflect.Constructor?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Constructor>(constructors.size)
        var i = 0
        val j = constructors.size
        while (i < j) {
            result[i] = com.badlogic.gdx.utils.reflect.Constructor(constructors[i])
            i++
        }
        return result
    }

    /** Returns a [Constructor] that represents the public constructor for the supplied class which takes the supplied
     * parameter types.  */
    @JvmStatic
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun getConstructor(c: java.lang.Class, vararg parameterTypes: java.lang.Class?): com.badlogic.gdx.utils.reflect.Constructor {
        return try {
            com.badlogic.gdx.utils.reflect.Constructor(c.getConstructor(*parameterTypes))
        } catch (e: SecurityException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Security violation occurred while getting constructor for class: '" + c.getName() + "'.",
                e)
        } catch (e: NoSuchMethodException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Constructor not found for class: " + c.getName(), e)
        }
    }

    /** Returns a [Constructor] that represents the constructor for the supplied class which takes the supplied parameter
     * types.  */
    @JvmStatic
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun getDeclaredConstructor(c: java.lang.Class, vararg parameterTypes: java.lang.Class?): com.badlogic.gdx.utils.reflect.Constructor {
        return try {
            com.badlogic.gdx.utils.reflect.Constructor(c.getDeclaredConstructor(*parameterTypes))
        } catch (e: SecurityException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Security violation while getting constructor for class: " + c.getName(), e)
        } catch (e: NoSuchMethodException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Constructor not found for class: " + c.getName(), e)
        }
    }

    /** Returns the elements of this enum class or null if this Class object does not represent an enum type.  */
    fun getEnumConstants(c: java.lang.Class): Array<Any> {
        return c.getEnumConstants()
    }

    /** Returns an array of [Method] containing the public member methods of the class represented by the supplied Class.  */
    @JvmStatic
    fun getMethods(c: java.lang.Class): Array<com.badlogic.gdx.utils.reflect.Method?> {
        val methods: Array<java.lang.reflect.Method> = c.getMethods()
        val result: Array<com.badlogic.gdx.utils.reflect.Method?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Method>(methods.size)
        var i = 0
        val j = methods.size
        while (i < j) {
            result[i] = com.badlogic.gdx.utils.reflect.Method(methods[i])
            i++
        }
        return result
    }

    /** Returns a [Method] that represents the public member method for the supplied class which takes the supplied parameter
     * types.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun getMethod(c: java.lang.Class, name: String, vararg parameterTypes: java.lang.Class?): com.badlogic.gdx.utils.reflect.Method {
        return try {
            com.badlogic.gdx.utils.reflect.Method(c.getMethod(name, *parameterTypes))
        } catch (e: SecurityException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Security violation while getting method: " + name + ", for class: " + c.getName(), e)
        } catch (e: NoSuchMethodException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Method not found: " + name + ", for class: " + c.getName(), e)
        }
    }

    /** Returns an array of [Method] containing the methods declared by the class represented by the supplied Class.  */
    fun getDeclaredMethods(c: java.lang.Class): Array<com.badlogic.gdx.utils.reflect.Method?> {
        val methods: Array<java.lang.reflect.Method> = c.getDeclaredMethods()
        val result: Array<com.badlogic.gdx.utils.reflect.Method?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Method>(methods.size)
        var i = 0
        val j = methods.size
        while (i < j) {
            result[i] = com.badlogic.gdx.utils.reflect.Method(methods[i])
            i++
        }
        return result
    }

    /** Returns a [Method] that represents the method declared by the supplied class which takes the supplied parameter types.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun getDeclaredMethod(c: java.lang.Class, name: String, vararg parameterTypes: java.lang.Class?): com.badlogic.gdx.utils.reflect.Method {
        return try {
            com.badlogic.gdx.utils.reflect.Method(c.getDeclaredMethod(name, *parameterTypes))
        } catch (e: SecurityException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Security violation while getting method: " + name + ", for class: " + c.getName(), e)
        } catch (e: NoSuchMethodException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Method not found: " + name + ", for class: " + c.getName(), e)
        }
    }

    /** Returns an array of [Field] containing the public fields of the class represented by the supplied Class.  */
    fun getFields(c: java.lang.Class): Array<com.badlogic.gdx.utils.reflect.Field?> {
        val fields: Array<java.lang.reflect.Field> = c.getFields()
        val result: Array<com.badlogic.gdx.utils.reflect.Field?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Field>(fields.size)
        var i = 0
        val j = fields.size
        while (i < j) {
            result[i] = com.badlogic.gdx.utils.reflect.Field(fields[i])
            i++
        }
        return result
    }

    /** Returns a [Field] that represents the specified public member field for the supplied class.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun getField(c: java.lang.Class, name: String): com.badlogic.gdx.utils.reflect.Field {
        return try {
            com.badlogic.gdx.utils.reflect.Field(c.getField(name))
        } catch (e: SecurityException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Security violation while getting field: " + name + ", for class: " + c.getName(), e)
        } catch (e: NoSuchFieldException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Field not found: " + name + ", for class: " + c.getName(), e)
        }
    }

    /** Returns an array of [Field] objects reflecting all the fields declared by the supplied class.  */
    @JvmStatic
    fun getDeclaredFields(c: java.lang.Class): Array<com.badlogic.gdx.utils.reflect.Field?> {
        val fields: Array<java.lang.reflect.Field> = c.getDeclaredFields()
        val result: Array<com.badlogic.gdx.utils.reflect.Field?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Field>(fields.size)
        var i = 0
        val j = fields.size
        while (i < j) {
            result[i] = com.badlogic.gdx.utils.reflect.Field(fields[i])
            i++
        }
        return result
    }

    /** Returns a [Field] that represents the specified declared field for the supplied class.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun getDeclaredField(c: java.lang.Class, name: String): com.badlogic.gdx.utils.reflect.Field {
        return try {
            com.badlogic.gdx.utils.reflect.Field(c.getDeclaredField(name))
        } catch (e: SecurityException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Security violation while getting field: " + name + ", for class: " + c.getName(), e)
        } catch (e: NoSuchFieldException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Field not found: " + name + ", for class: " + c.getName(), e)
        }
    }

    /** Returns true if the supplied class includes an annotation of the given type.  */
    fun isAnnotationPresent(c: java.lang.Class, annotationType: java.lang.Class<out Annotation?>?): Boolean {
        return c.isAnnotationPresent(annotationType)
    }

    /** Returns an array of [Annotation] objects reflecting all annotations declared by the supplied class, and inherited
     * from its superclass. Returns an empty array if there are none.  */
    fun getAnnotations(c: java.lang.Class): Array<com.badlogic.gdx.utils.reflect.Annotation?> {
        val annotations: Array<Annotation> = c.getAnnotations()
        val result: Array<com.badlogic.gdx.utils.reflect.Annotation?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Annotation>(annotations.size)
        for (i in annotations.indices) {
            result[i] = com.badlogic.gdx.utils.reflect.Annotation(annotations[i])
        }
        return result
    }

    /** Returns an [Annotation] object reflecting the annotation provided, or null if this class doesn't have such an
     * annotation. This is a convenience function if the caller knows already which annotation type he's looking for.  */
    fun getAnnotation(c: java.lang.Class, annotationType: java.lang.Class<out Annotation?>?): com.badlogic.gdx.utils.reflect.Annotation? {
        val annotation: Annotation = c.getAnnotation(annotationType)
        return if (annotation != null) com.badlogic.gdx.utils.reflect.Annotation(annotation) else null
    }

    /** Returns an array of [Annotation] objects reflecting all annotations declared by the supplied class, or an empty
     * array if there are none. Does not include inherited annotations.  */
    fun getDeclaredAnnotations(c: java.lang.Class): Array<com.badlogic.gdx.utils.reflect.Annotation?> {
        val annotations: Array<Annotation> = c.getDeclaredAnnotations()
        val result: Array<com.badlogic.gdx.utils.reflect.Annotation?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Annotation>(annotations.size)
        for (i in annotations.indices) {
            result[i] = com.badlogic.gdx.utils.reflect.Annotation(annotations[i])
        }
        return result
    }

    /** Returns an [Annotation] object reflecting the annotation provided, or null if this class doesn't have such an
     * annotation. This is a convenience function if the caller knows already which annotation type he's looking for.  */
    fun getDeclaredAnnotation(c: java.lang.Class, annotationType: java.lang.Class<out Annotation?>): com.badlogic.gdx.utils.reflect.Annotation? {
        val annotations: Array<Annotation> = c.getDeclaredAnnotations()
        for (annotation in annotations) {
            if (annotation.annotationType() == annotationType) return com.badlogic.gdx.utils.reflect.Annotation(annotation)
        }
        return null
    }

    fun getInterfaces(c: java.lang.Class): Array<java.lang.Class> {
        return c.getInterfaces()
    }
}
