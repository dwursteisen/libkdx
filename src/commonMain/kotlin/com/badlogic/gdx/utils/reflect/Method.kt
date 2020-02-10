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

/** Provides information about, and access to, a single method on a class or interface.
 * @author nexsoftware
 */
class Method internal constructor(method: java.lang.reflect.Method) {

    private val method: java.lang.reflect.Method
    /** Returns the name of the method.  */
    val name: String
        get() = method.getName()

    /** Returns a Class object that represents the formal return type of the method.  */
    val returnType: java.lang.Class
        get() = method.getReturnType()

    /** Returns an array of Class objects that represent the formal parameter types, in declaration order, of the method.  */
    val parameterTypes: Array<Any>
        get() = method.getParameterTypes()

    /** Returns the Class object representing the class or interface that declares the method.  */
    val declaringClass: java.lang.Class
        get() = method.getDeclaringClass()

    var isAccessible: Boolean
        get() = method.isAccessible()
        set(accessible) {
            method.setAccessible(accessible)
        }

    /** Return true if the method includes the `abstract` modifier.  */
    val isAbstract: Boolean
        get() = java.lang.reflect.Modifier.isAbstract(method.getModifiers())

    /** Return true if the method does not include any of the `private`, `protected`, or `public` modifiers.  */
    val isDefaultAccess: Boolean
        get() = !isPrivate && !isProtected && !isPublic

    /** Return true if the method includes the `final` modifier.  */
    val isFinal: Boolean
        get() = java.lang.reflect.Modifier.isFinal(method.getModifiers())

    /** Return true if the method includes the `private` modifier.  */
    val isPrivate: Boolean
        get() = java.lang.reflect.Modifier.isPrivate(method.getModifiers())

    /** Return true if the method includes the `protected` modifier.  */
    val isProtected: Boolean
        get() = java.lang.reflect.Modifier.isProtected(method.getModifiers())

    /** Return true if the method includes the `public` modifier.  */
    val isPublic: Boolean
        get() = java.lang.reflect.Modifier.isPublic(method.getModifiers())

    /** Return true if the method includes the `native` modifier.  */
    val isNative: Boolean
        get() = java.lang.reflect.Modifier.isNative(method.getModifiers())

    /** Return true if the method includes the `static` modifier.  */
    val isStatic: Boolean
        get() = java.lang.reflect.Modifier.isStatic(method.getModifiers())

    /** Return true if the method takes a variable number of arguments.  */
    val isVarArgs: Boolean
        get() = method.isVarArgs()

    /** Invokes the underlying method on the supplied object with the supplied parameters.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    operator fun invoke(obj: Any?, vararg args: Any?): Any {
        return try {
            method.invoke(obj, *args)
        } catch (e: IllegalArgumentException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Illegal argument(s) supplied to method: $name", e)
        } catch (e: IllegalAccessException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Illegal access to method: $name", e)
        } catch (e: InvocationTargetException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Exception occurred in method: $name", e)
        }
    }

    /** Returns true if the method includes an annotation of the provided class type.  */
    fun isAnnotationPresent(annotationType: java.lang.Class<out Annotation?>?): Boolean {
        return method.isAnnotationPresent(annotationType)
    }

    /** Returns an array of [Annotation] objects reflecting all annotations declared by this method,
     * or an empty array if there are none. Does not include inherited annotations.
     * Does not include parameter annotations.  */
    val declaredAnnotations: Array<com.badlogic.gdx.utils.reflect.Annotation?>
        get() {
            val annotations: Array<Annotation> = method.getDeclaredAnnotations()
            val result: Array<com.badlogic.gdx.utils.reflect.Annotation?> = arrayOfNulls<com.badlogic.gdx.utils.reflect.Annotation>(annotations.size)
            for (i in annotations.indices) {
                result[i] = com.badlogic.gdx.utils.reflect.Annotation(annotations[i])
            }
            return result
        }

    /** Returns an [Annotation] object reflecting the annotation provided, or null of this method doesn't
     * have such an annotation. This is a convenience function if the caller knows already which annotation
     * type he's looking for.  */
    fun getDeclaredAnnotation(annotationType: java.lang.Class<out Annotation?>): com.badlogic.gdx.utils.reflect.Annotation? {
        val annotations: Array<Annotation> = method.getDeclaredAnnotations() ?: return null
        for (annotation in annotations) {
            if (annotation.annotationType() == annotationType) {
                return com.badlogic.gdx.utils.reflect.Annotation(annotation)
            }
        }
        return null
    }

    init {
        this.method = method
    }
}
