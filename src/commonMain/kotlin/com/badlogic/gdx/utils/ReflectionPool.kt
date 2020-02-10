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
package com.badlogic.gdx.utils

import com.badlogic.gdx.utils.Pools
import com.badlogic.gdx.utils.Predicate.PredicateIterator
import com.badlogic.gdx.utils.PropertiesUtils
import com.badlogic.gdx.utils.Queue.QueueIterable
import com.badlogic.gdx.utils.Queue.QueueIterator
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.ScreenUtils
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.RuntimeException
import kotlin.jvm.Throws

/**
 * Pool that creates new instances of a type using reflection. The type must have a zero argument constructor.
 * [Constructor.setAccessible] will be used if the class and/or constructor is not visible.
 *
 * @author Nathan Sweet
 */
class ReflectionPool<T> @JvmOverloads constructor(type: java.lang.Class<T>, initialCapacity: Int = 16, max: Int = Int.MAX_VALUE) : Pool<T>(initialCapacity, max) {

    private val constructor: Constructor?
    private fun findConstructor(type: java.lang.Class<T>): Constructor? {
        return try {
            ClassReflection.getConstructor(type, null as Array<java.lang.Class?>?)
        } catch (ex1: java.lang.Exception) {
            try {
                val constructor: Constructor = ClassReflection.getDeclaredConstructor(type, null as Array<java.lang.Class?>?)
                constructor.setAccessible(true)
                constructor
            } catch (ex2: ReflectionException) {
                null
            }
        }
    }

    override fun newObject(): T? {
        return try {
            constructor.newInstance(null as Array<Any?>?)
        } catch (ex: java.lang.Exception) {
            throw GdxRuntimeException("Unable to create new instance: " + constructor.getDeclaringClass().getName(), ex)
        }
    }

    init {
        constructor = findConstructor(type)
        if (constructor == null) throw RuntimeException("Class cannot be created (missing no-arg constructor): " + type.getName())
    }
}
