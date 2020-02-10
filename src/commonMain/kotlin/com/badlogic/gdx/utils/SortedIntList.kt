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

/**
 * A sorted double linked list which uses ints for indexing
 *
 * @param <E>
</E> */
class SortedIntList<E>
/**
 * Creates an ascending list
 */
    : Iterable<SortedIntList.Node<E>?> {

    private val nodePool = NodePool<E?>() // avoid allocating nodes
    private var iterator: Iterator? = null
    var size = 0
    var first: Node<E?>? = null

    /**
     * Inserts an element into the list at the given index
     *
     * @param index Index of the element
     * @param value Element to insert
     * @return Element replaced by newly inserted element, null if nothing was replaced
     */
    fun insert(index: Int, value: E): E? {
        if (first != null) {
            var c = first
            // iterate to the right until we can't move any further because the next number is bigger than index
            while (c!!.n != null && c.n.index <= index) {
                c = c.n
            }
            // add one to the right
            if (index > c.index) {
                c.n = nodePool.obtain(c, c.n, value, index)
                if (c.n!!.n != null) {
                    c.n!!.n.p = c.n
                }
                size++
            } else if (index < c.index) {
                val newFirst = nodePool.obtain(null, first, value, index)
                first.p = newFirst
                first = newFirst
                size++
            } else {
                c.value = value
            }
        } else {
            first = nodePool.obtain(null, null, value, index)
            size++
        }
        return null
    }

    /**
     * Retrieves an element at a given index
     *
     * @param index Index of the element to retrieve
     * @return Matching element, null otherwise
     */
    operator fun get(index: Int): E? {
        var match: E? = null
        if (first != null) {
            var c = first
            while (c!!.n != null && c.index < index) {
                c = c.n
            }
            if (c.index == index) {
                match = c.value
            }
        }
        return match
    }

    /**
     * Clears list
     */
    fun clear() {
        while (first != null) {
            nodePool.free(first)
            first = first.n
        }
        size = 0
    }

    /**
     * @return size of list equal to elements contained in it
     */
    fun size(): Int {
        return size
    }

    /**
     * Returns true if the list has one or more items.
     */
    fun notEmpty(): Boolean {
        return size > 0
    }

    /**
     * Returns true if the list is empty.
     */
    val isEmpty: Boolean
        get() = size == 0

    /**
     * Returns an iterator to traverse the list.<br></br>
     * Only one iterator can be active per list at any given time.
     *
     * @return Iterator to traverse list
     */
    override fun iterator(): MutableIterator<Node<E>> {
        if (iterator == null) {
            iterator = Iterator()
        }
        return iterator.reset()
    }

    internal inner class Iterator : MutableIterator<Node<E>?> {
        private var position: Node<E?>? = null
        private var previousPosition: Node<E?>? = null
        override fun hasNext(): Boolean {
            return position != null
        }

        override fun next(): Node<E>? {
            previousPosition = position
            position = position!!.n
            return previousPosition!!
        }

        override fun remove() {
            // the contract specifies to remove the last returned element, if nothing was returned yet assumably do nothing
            if (previousPosition != null) {
                // if we are at the second element set it as the first element
                if (previousPosition === first) {
                    first = position
                } else {
                    previousPosition.p!!.n = position
                    if (position != null) {
                        position.p = previousPosition.p
                    }
                }
                size--
            }
        }

        fun reset(): Iterator {
            position = first
            previousPosition = null
            return this
        }
    }

    class Node<E> {
        /**
         * Node previous to this
         */
        var p: Node<E>? = null

        /**
         * Node next to this
         */
        var n: Node<E>? = null

        /**
         * Value held
         */
        var value: E? = null

        /**
         * Index value in list
         */
        var index = 0
    }

    internal class NodePool<E> : Pool<Node<E>?>() {
        override fun newObject(): Node<E>? {
            return Node()
        }

        fun obtain(p: Node<E>?, n: Node<E>?, value: E, index: Int): Node<E>? {
            val newNode = super.obtain()
            newNode!!.p = p
            newNode.n = n
            newNode.value = value
            newNode.index = index
            return newNode
        }
    }
}
