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

/** Provides information about, and access to, a single constructor for a Class.
 * @author nexsoftware
 */
class Constructor internal constructor(constructor: java.lang.reflect.Constructor) {

    private val constructor: java.lang.reflect.Constructor
    /** Returns an array of Class objects that represent the formal parameter types, in declaration order, of the constructor.  */
    val parameterTypes: Array<Any>
        get() = constructor.getParameterTypes()

    /** Returns the Class object representing the class or interface that declares the constructor.  */
    val declaringClass: java.lang.Class
        get() = constructor.getDeclaringClass()

    var isAccessible: Boolean
        get() = constructor.isAccessible()
        set(accessible) {
            constructor.setAccessible(accessible)
        }

    /** Uses the constructor to create and initialize a new instance of the constructor's declaring class, with the supplied
     * initialization parameters.  */
    @Throws(com.badlogic.gdx.utils.reflect.ReflectionException::class)
    fun newInstance(vararg args: Any?): Any {
        return try {
            constructor.newInstance(*args)
        } catch (e: IllegalArgumentException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Illegal argument(s) supplied to constructor for class: " + declaringClass.getName(),
                e)
        } catch (e: InstantiationException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Could not instantiate instance of class: " + declaringClass.getName(), e)
        } catch (e: IllegalAccessException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Could not instantiate instance of class: " + declaringClass.getName(), e)
        } catch (e: InvocationTargetException) {
            throw com.badlogic.gdx.utils.reflect.ReflectionException("Exception occurred in constructor for class: " + declaringClass.getName(), e)
        }
    }

    init {
        this.constructor = constructor
    }
}
