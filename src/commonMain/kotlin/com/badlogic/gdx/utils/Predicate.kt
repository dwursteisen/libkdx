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
import com.badlogic.gdx.utils.PropertiesUtils
import com.badlogic.gdx.utils.Queue.QueueIterable
import com.badlogic.gdx.utils.Queue.QueueIterator
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.ScreenUtils
import java.lang.IndexOutOfBoundsException

import kotlin.jvm.Throws

/**
 * Interface used to select items within an iterator against a predicate.
 *
 * @author Xoppa
 */
interface Predicate<T> {

    /**
     * @return true if the item matches the criteria and should be included in the iterator's items
     */
    fun evaluate(arg0: T): Boolean
    class PredicateIterator<T>(iterator: Iterator<T>?, predicate: Predicate<T>?) : Iterator<T> {
        var iterator: Iterator<T>? = null
        var predicate: Predicate<T>? = null
        var end = false
        var peeked = false
        var next: T? = null

        constructor(iterable: Iterable<T>, predicate: Predicate<T>?) : this(iterable.iterator(), predicate) {}

        operator fun set(iterable: Iterable<T>, predicate: Predicate<T>?) {
            set(iterable.iterator(), predicate)
        }

        operator fun set(iterator: Iterator<T>?, predicate: Predicate<T>?) {
            this.iterator = iterator
            this.predicate = predicate
            peeked = false
            end = peeked
            next = null
        }

        override fun hasNext(): Boolean {
            if (end) return false
            if (next != null) return true
            peeked = true
            while (iterator!!.hasNext()) {
                val n = iterator!!.next()
                if (predicate!!.evaluate(n)) {
                    next = n
                    return true
                }
            }
            end = true
            return false
        }

        override fun next(): T? {
            if (next == null && !hasNext()) return null
            val result = next
            next = null
            peeked = false
            return result
        }

        fun remove() {
            if (peeked) throw GdxRuntimeException("Cannot remove between a call to hasNext() and next().")
            iterator.remove()
        }

        init {
            set(iterator, predicate)
        }
    }

    class PredicateIterable<T>(iterable: Iterable<T>?, predicate: Predicate<T>?) : Iterable<T> {
        var iterable: Iterable<T>? = null
        var predicate: Predicate<T>? = null
        var iterator: PredicateIterator<T>? = null
        operator fun set(iterable: Iterable<T>?, predicate: Predicate<T>?) {
            this.iterable = iterable
            this.predicate = predicate
        }

        /**
         * Returns an iterator. Remove is supported.
         *
         *
         * If [Collections.allocateIterators] is false, the same iterator instance is returned each time this method is called. Use
         * the [Predicate.PredicateIterator] constructor for nested or multithreaded iteration.
         */
        override fun iterator(): Iterator<T> {
            if (Collections.allocateIterators) return PredicateIterator(iterable!!.iterator(), predicate)
            if (iterator == null) iterator = PredicateIterator(iterable!!.iterator(), predicate) else iterator.set(iterable!!.iterator(), predicate)
            return iterator
        }

        init {
            set(iterable, predicate)
        }
    }
}
