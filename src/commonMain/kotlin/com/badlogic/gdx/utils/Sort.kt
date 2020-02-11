package com.badlogic.gdx.utils

/**
 * Provides methods to sort arrays of objects. Sorting requires working memory and this class allows that memory to be reused to
 * avoid allocation. The sorting is otherwise identical to the Arrays.sort methods (uses timsort).<br></br>
 * <br></br>
 * Note that sorting primitive arrays with the Arrays.sort methods does not allocate memory (unless sorting large arrays of char,
 * short, or byte).
 *
 * @author Nathan Sweet
 */
class Sort {

    private var timSort: TimSort<*>? = null
    private var comparableTimSort: ComparableTimSort? = null
    fun <T : Comparable<*>?> sort(a: Array<T?>?) {
        if (comparableTimSort == null) comparableTimSort = ComparableTimSort()
        comparableTimSort!!.doSort(a!!.items as kotlin.Array<Any?>, 0, a.size)
    }

    /**
     * The specified objects must implement [Comparable].
     */
    fun sort(a: kotlin.Array<Any?>?) {
        if (comparableTimSort == null) comparableTimSort = ComparableTimSort()
        comparableTimSort!!.doSort(a, 0, a!!.size)
    }

    /**
     * The specified objects must implement [Comparable].
     */
    fun sort(a: kotlin.Array<Any?>?, fromIndex: Int, toIndex: Int) {
        if (comparableTimSort == null) comparableTimSort = ComparableTimSort()
        comparableTimSort!!.doSort(a, fromIndex, toIndex)
    }

    fun <T> sort(a: Array<T?>?, c: Comparator<in T?>?) {
        if (timSort == null) timSort = TimSort<Any?>()
        timSort.doSort(a!!.items as kotlin.Array<Any?>, c, 0, a.size)
    }

    fun <T> sort(a: kotlin.Array<T?>?, c: Comparator<in T?>?) {
        if (timSort == null) timSort = TimSort<Any?>()
        timSort.doSort(a, c, 0, a!!.size)
    }

    fun <T> sort(a: kotlin.Array<T?>?, c: Comparator<in T?>?, fromIndex: Int, toIndex: Int) {
        if (timSort == null) timSort = TimSort<Any?>()
        timSort.doSort(a, c, fromIndex, toIndex)
    }

    companion object {
        private var instance: Sort? = null

        /**
         * Returns a Sort instance for convenience. Multiple threads must not use this instance at the same time.
         */
        fun instance(): Sort? {
            if (instance == null) instance = Sort()
            return instance
        }
    }
}
