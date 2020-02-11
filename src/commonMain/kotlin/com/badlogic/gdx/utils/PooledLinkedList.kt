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

import kotlin.jvm.Throws

/**
 * A simple linked list that pools its nodes.
 *
 * @author mzechner
 */
class PooledLinkedList<T>(maxPoolSize: Int) {

    internal class Item<T> {
        var payload: T? = null
        var next: Item<T>? = null
        var prev: Item<T>? = null
    }

    private var head: Item<T>? = null
    private var tail: Item<T>? = null
    private var iter: Item<T>? = null
    private var curr: Item<T>? = null
    private var size = 0
    private val pool: Pool<Item<T>>

    /**
     * Adds the specified object to the end of the list regardless of iteration status
     */
    fun add(`object`: T) {
        val item = pool.obtain()!!
        item.payload = `object`
        item.next = null
        item.prev = null
        if (head == null) {
            head = item
            tail = item
            size++
            return
        }
        item.prev = tail
        tail!!.next = item
        tail = item
        size++
    }

    /**
     * Adds the specified object to the head of the list regardless of iteration status
     */
    fun addFirst(`object`: T) {
        val item = pool.obtain()!!
        item.payload = `object`
        item.next = head
        item.prev = null
        if (head != null) {
            head.prev = item
        } else {
            tail = item
        }
        head = item
        size++
    }

    /**
     * Returns the number of items in the list
     */
    fun size(): Int {
        return size
    }

    /**
     * Starts iterating over the list's items from the head of the list
     */
    fun iter() {
        iter = head
    }

    /**
     * Starts iterating over the list's items from the tail of the list
     */
    fun iterReverse() {
        iter = tail
    }

    /**
     * Gets the next item in the list
     *
     * @return the next item in the list or null if there are no more items
     */
    operator fun next(): T? {
        if (iter == null) return null
        val payload: T = iter.payload
        curr = iter
        iter = iter.next
        return payload
    }

    /**
     * Gets the previous item in the list
     *
     * @return the previous item in the list or null if there are no more items
     */
    fun previous(): T? {
        if (iter == null) return null
        val payload: T = iter.payload
        curr = iter
        iter = iter.prev
        return payload
    }

    /**
     * Removes the current list item based on the iterator position.
     */
    fun remove() {
        if (curr == null) return
        size--
        val c: Item<T> = curr
        val n = curr.next
        val p = curr.prev
        pool.free(curr)
        curr = null
        if (size == 0) {
            head = null
            tail = null
            return
        }
        if (c == head) {
            n!!.prev = null
            head = n
            return
        }
        if (c == tail) {
            p!!.next = null
            tail = p
            return
        }
        p!!.next = n
        n!!.prev = p
    }

    /**
     * Removes the tail of the list regardless of iteration status
     */
    fun removeLast(): T? {
        if (tail == null) {
            return null
        }
        val payload: T = tail.payload
        size--
        val p = tail.prev
        pool.free(tail)
        if (size == 0) {
            head = null
            tail = null
        } else {
            tail = p
            tail!!.next = null
        }
        return payload
    }

    fun clear() {
        iter()
        var v: T? = null
        while (next().also { v = it } != null) remove()
    }

    init {
        pool = object : Pool<Item<T>?>(16, maxPoolSize) {
            override fun newObject(): Item<T>? {
                return Item()
            }
        }
    }
}
