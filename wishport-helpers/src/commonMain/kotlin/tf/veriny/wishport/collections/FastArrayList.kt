/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

@file:Suppress("UseWithIndex")

package tf.veriny.wishport.collections

private fun roundUp(number: Int): Int {
    return (1).shl(32 - number.countLeadingZeroBits())
}

// impl details:
// basic benchmarks say this is about 2x as fast on add(), the most important op
// we get this by unmerging various paths that ArrayList merges together, as well as being more
// efficient overall with allocations.
// this is untested properly!!! use with caution until im not too lazy to write the tests.

/**
 * An implementation of [MutableList] that is meant to be faster than the stock [ArrayList].
 */
public class FastArrayList<E : Any>
private constructor(private var backing: Array<Any?>) : MutableList<E> {
    private inner class FALIterator : MutableListIterator<E> {
        var cursor = 0
        private var lastCursor = -1

        override fun hasNext(): Boolean = cursor < size
        override fun hasPrevious(): Boolean = cursor > 0

        override fun nextIndex(): Int = cursor
        override fun previousIndex(): Int = cursor - 1

        @Suppress("UNCHECKED_CAST")
        override fun next(): E {
            if (cursor > size) throw NoSuchElementException()
            lastCursor = cursor++

            return backing[lastCursor] as E
        }

        @Suppress("UNCHECKED_CAST")
        override fun previous(): E {
            if (cursor <= 0) throw NoSuchElementException()
            lastCursor = --cursor

            return backing[lastCursor] as E
        }

        // modification methods
        override fun add(element: E) {
            add(cursor++, element)
            lastCursor = -1
        }

        override fun remove() {
            if (lastCursor == -1) {
                throw IllegalStateException("nothing to remove idiot!!")
            }

            removeAt(lastCursor)
            cursor = lastCursor
            lastCursor = -1
        }

        override fun set(element: E) {
            if (lastCursor == -1) {
                throw IllegalStateException("nothjing to set idiot!!!")
            }

            set(lastCursor, element)
        }
    }

    public constructor(initialSize: Int = 16) : this(arrayOfNulls(initialSize))

    // == impl details == //
    override var size: Int = 0
        private set

    override fun isEmpty(): Boolean = size == 0
    override fun iterator(): MutableIterator<E> = FALIterator()
    override fun listIterator(): MutableListIterator<E> = FALIterator()
    override fun listIterator(index: Int): MutableListIterator<E> {
        return FALIterator().also { it.cursor = index }
    }

    // == modification methods == //

    override fun add(element: E): Boolean {
        if (size + 1 > backing.size) {
            val new = Array<Any?>(roundUp(backing.size * 2)) { null }
            backing.copyInto(new)
            backing = new
        }

        backing[size++] = element as Any?
        return true
    }

    override fun add(index: Int, element: E) {
        // make new array, copy first half and then second half
        val dest = if (size + 1 > backing.size) {
            Array<Any?>(roundUp(backing.size * 2)) { null }
        } else backing

        // avoid pointless copies to the first half of the list
        if (dest !== backing) {
            backing.copyInto(dest, destinationOffset = 0, startIndex = 0, endIndex = index)
        }

        backing[index] = element
        backing.copyInto(dest, destinationOffset = index + 1, startIndex = index, endIndex = backing.size)
        backing = dest
    }

    override fun addAll(elements: Collection<E>): Boolean {
        val requiredSize = size + elements.size
        if (requiredSize > backing.size) {
            val new = Array<Any?>(roundUp(requiredSize)) { null }
            backing.copyInto(new)
            backing = new
        }

        for (i in elements) {
            backing[size++] = i
        }

        return true
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        val requiredSize = size + elements.size
        val dest = if (requiredSize > backing.size) {
            val new = Array<Any?>(roundUp(requiredSize)) { null }
            backing.copyInto(new)
            new
        } else backing

        if (backing !== dest) {
            backing.copyInto(dest, destinationOffset = 0, startIndex = 0, endIndex = index)
        }

        var count = 0
        for (i in elements) {
            dest[index + count++] = i
        }

        backing.copyInto(dest, destinationOffset = index + count, startIndex = index, endIndex = backing.size)
        backing = dest

        return true
    }

    @Suppress("UNCHECKED_CAST")
    override fun removeAt(index: Int): E {
        if (index < 0) throw IndexOutOfBoundsException("too small: $index")
        if (index >= size) throw IndexOutOfBoundsException("too big: $index")

        val item = backing[index] as E

        // copy all items past index back one
        if (index != size - 1) {
            backing.copyInto(backing, destinationOffset = index, startIndex = index + 1, endIndex = backing.size)
        }
        backing[size - 1] = null
        size--
        return item
    }

    override fun remove(element: E): Boolean {
        val index = indexOf(element)
        return if (index != -1) {
            removeAt(index)
            true
        } else false
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        // oh man, this one really sucks
        if (elements.isEmpty()) return false

        val newArr = arrayOfNulls<Any?>(roundUp(backing.size))
        // just do a quadratic fucking search.

        var removedAny = false
        var originalCount = 0
        var newIndex = 0
        while (true) {
            if (originalCount > backing.size) break
            val i = backing[originalCount++] ?: break

            // this is where for/else really comes in handy
            var found = false

            for (el in elements) {
                if (i == el) {
                    found = true
                    removedAny = true
                    break
                }
            }

            if (!found) {
                newArr[newIndex++] = i
            }
        }

        backing = newArr
        return removedAny
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        if (elements.isEmpty()) {
            clear()
            return false
        }

        val newArr = arrayOfNulls<Any?>(roundUp(elements.size))

        var origCount = 0
        var newCount = 0

        while (true) {
            if (origCount > size) break
            val el = backing[origCount++] ?: break

            for (i in elements) {
                if (i == el) {
                    newArr[newCount++] = el
                    break
                }
            }
        }

        backing = newArr
        return newCount > 0
    }

    override fun set(index: Int, element: E): E {
        if (index < 0) throw IndexOutOfBoundsException("too small: $index")
        if (index >= size) throw IndexOutOfBoundsException("too big: $index")

        val prev = backing[index]
        backing.set(index, element)

        @Suppress("UNCHECKED_CAST")
        return prev as E
    }
    // == query methods == //

    @Suppress("UNCHECKED_CAST")
    override fun get(index: Int): E {
        if (index < 0) throw IndexOutOfBoundsException("too small: $index")
        if (index >= size) throw IndexOutOfBoundsException("too big: $index")
        return backing[index]!! as E
    }

    override fun contains(element: E): Boolean {
        for (i in backing) {
            if (i == null) break
            if (i == element) return true
        }

        return false
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        if (elements.size > size) return false

        // quadratic search
        var found = 0
        for (i in backing) {
            if (i == null) break

            for (el in elements) {
                if (i == el) found++
            }

            if (found == elements.size) return true
        }

        return false
    }

    override fun indexOf(element: E): Int {
        var count = 0
        while (true) {
            if (count >= size) return -1

            val el = backing[count] ?: return -1
            if (el == element) return count
            count++
        }
    }

    override fun lastIndexOf(element: E): Int {
        var count = size - 1

        while (true) {
            if (count < 0) return -1
            if (backing[count] == element) return count
            count--
        }
    }

    override fun clear() {
        backing = Array(16) { null }
        size = 0
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        // TODO: care about implementing this
        throw UnsupportedOperationException("not supported!")
    }

    // == non-contract methods == //
    /** The size of the underlying array. */
    public val memorySize: Int
        get() = backing.size

    /**
     * Trims this list down to size.
     */
    public fun trim() {
        val newSize = roundUp(size)
        if (newSize >= memorySize) return

        // written weirdly so that it keeps the left-to-rightness
        Array(newSize) { backing[it] }.also { backing = it }
    }

    /**
     * Clears this list and reallocates with the specified size.
     */
    public fun clearTo(size: Int) {
        backing = Array(size) { null }
        this.size = 0
    }

    override fun equals(other: Any?): Boolean {
        // blatantly violate the equals contract
        if (other == null) return false
        if (other === this) return true
        if (other !is List<*>) return false

        if (other.size != size) return false
        for ((i1, i2) in other.zip(this)) {
            if (i1 != i2) return false
        }

        return true
    }

    override fun hashCode(): Int {
        return backing.contentDeepHashCode()
    }

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append('<')

        val iterator = this.iterator()
        while (iterator.hasNext()) {
            val el = iterator.next()
            builder.append(el.toString())
            if (iterator.hasNext()) builder.append(", ")
        }

        return builder.append('>').toString()
    }
}
