package com.badlogic.gdx.utils

object Collections {
    /**
     * When true, [Iterable.iterator] for [Array], [ObjectMap], and other collections will allocate a new
     * iterator for each invocation. When false, the iterator is reused and nested use will throw an exception. Default is
     * false.
     */
    var allocateIterators = false
}
