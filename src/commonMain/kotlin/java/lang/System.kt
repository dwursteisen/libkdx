package java.lang


expect class System {

    companion object {

        fun arraycopy(itemA: Array<*>, startA: Int, itemsB: Array<*>, startB: Int, size: Int)
    }
}
