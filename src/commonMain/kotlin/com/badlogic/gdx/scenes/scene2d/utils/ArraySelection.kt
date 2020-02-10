package com.badlogic.gdx.scenes.scene2d.utils

import com.badlogic.gdx.utils.Array

/**
 * A selection that supports range selection by knowing about the array of items being selected.
 *
 * @author Nathan Sweet
 */
class ArraySelection<T>(private val array: Array<T>) : Selection<T>() {

    var rangeSelect = true
    private var rangeStart: T? = null
    fun choose(item: T?) {
        if (item == null) throw java.lang.IllegalArgumentException("item cannot be null.")
        if (isDisabled) return
        if (!rangeSelect || !multiple) {
            super.choose(item)
            return
        }
        if (selected.size > 0 && UIUtils.shift()) {
            val rangeStartIndex = if (rangeStart == null) -1 else array.indexOf(rangeStart, false)
            if (rangeStartIndex != -1) {
                val oldRangeStart = rangeStart
                snapshot()
                // Select new range.
                var start = rangeStartIndex
                var end = array.indexOf(item, false)
                if (start > end) {
                    val temp = end
                    end = start
                    start = temp
                }
                if (!UIUtils.ctrl()) selected.clear(8)
                for (i in start..end) selected.add(array[i])
                if (fireChangeEvent()) revert() else changed()
                rangeStart = oldRangeStart
                cleanup()
                return
            }
        }
        super.choose(item)
        rangeStart = item
    }

    /**
     * Called after the selection changes, clears the range start item.
     */
    protected fun changed() {
        rangeStart = null
    }

    /**
     * Removes objects from the selection that are no longer in the items array. If [.getRequired] is true and there is no
     * selected item, the first item is selected.
     */
    fun validate() {
        val array = array
        if (array.size === 0) {
            clear()
            return
        }
        val iter: MutableIterator<T> = items().iterator()
        while (iter.hasNext()) {
            val selected = iter.next()
            if (!array.contains(selected, false)) iter.remove()
        }
        if (required && selected.size === 0) set(array.first())
    }
}
