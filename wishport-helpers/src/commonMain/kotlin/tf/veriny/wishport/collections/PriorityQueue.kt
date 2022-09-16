/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.collections

// loosely ported/stolen from the KDS one.

/**
 * A generic priority queue based on a binary heap.
 */
public class PriorityQueue<T>(private val comparator: Comparator<T>) {
    public companion object {
        /**
         * Creates a new [PriorityQueue] for a [Comparable].
         */
        public operator fun <T : Comparable<T>> invoke(): PriorityQueue<T> {
            return PriorityQueue { a, b -> a.compareTo(b) }
        }
    }

    // helper extensions
    private fun Int.parent() = (this - 1) / 2
    private fun Int.left() = (this * 2) + 1
    private fun Int.right() = (this * 2) + 2
    private fun Comparator<T>.less(a: T, b: T) = compare(a, b) < 0
    private fun Comparator<T>.more(a: T, b: T) = compare(a, b) > 0

    // T[] isn't allowed!!!
    private var arr = Array<Any?>(32) { null }
    private var size = 0

    private fun swapify(from: Int, to: Int) {
        val tmp = arr[to]
        arr[to] = arr[from]
        arr[from] = tmp
    }

    /**
     * Adds a new item to this heap.
     */
    @Suppress("UNCHECKED_CAST")
    public fun add(item: T) {
        // powers of 2 is a good tradeoff
        if (size + 1 > arr.size) {
            arr = Array(arr.size * 2) { arr[it] }
        }

        var idx = size
        arr[idx] = item
        size += 1

        // sift up
        while (idx != 0 && comparator.less(arr[idx] as T, arr[idx.parent()] as T)) {
            swapify(idx, idx.parent())
            idx = idx.parent()
        }
    }

    /**
     * Checks the first item on the heap.
     */
    @Suppress("UNCHECKED_CAST")
    public fun poll(): T? {
        return when (size) {
            0 -> null
            else -> arr[0] as T
        }
    }

    /**
     * Removes the item at the head of this heap, or returns null if the heap is empty.
     */
    @Suppress("UNCHECKED_CAST")
    public fun remove(): T? {
        return when (size) {
            0 -> null
            1 -> {
                size -= 1
                val tmp = arr[0]
                arr[0] = null
                tmp as T
            }
            else -> {
                val head = arr[0]
                arr[0] = arr[size - 1]
                // clean up after ourselves, to allow GC to collect old objects
                // in case the queue never grows again.
                arr[size - 1] = null

                size -= 1

                // sift the lowest el back down
                heapify(0)
                head as T
            }
        }
    }

    /**
     * Bubbles items down the tree.
     */
    @Suppress("UNCHECKED_CAST")
    private fun heapify(index: Int) {
        var idx = index

        while (true) {
            val left = idx.left()
            val right = idx.right()

            var smallest = idx

            // checj uf iyur
            if (left < size && comparator.less(arr[left] as T, arr[idx] as T)) {
                smallest = left
            }

            if (right < size && comparator.less(arr[right] as T, arr[smallest] as T)) {
                smallest = right
            }

            if (smallest == idx) break

            swapify(idx, smallest)
            idx = smallest
        }
    }
}

// inefficient
/**
 * Converts an [Iterable] into a [PriorityQueue], using the specified comparator.
 */
public fun <T> Iterable<T>.heapify(comparator: Comparator<T>): PriorityQueue<T> {
    val heap = PriorityQueue(comparator)
    for (i in this) {
        heap.add(i)
    }

    return heap
}

/**
 * Converts an [Iterable] into a [PriorityQueue].
 */
public fun <T : Comparable<T>> Iterable<T>.heapify(): PriorityQueue<T> {
    return heapify { a, b -> a.compareTo(b) }
}
