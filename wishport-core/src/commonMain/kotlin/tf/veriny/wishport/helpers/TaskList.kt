/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.helpers

import tf.veriny.wishport.annotations.LowLevelApi

// XXX: this is <T>'d for easier testing.
//      once it's reimpled itll be renamed like LinkedDequeSet or some other nonsense

// quick and dirty linked-list + set wrapper used for parking lot esque classes.
// we need a custom map structure anyway, so I need to write a proper hashmap replacement
// but this'll do for now.
@OptIn(LowLevelApi::class)
internal class TaskList<T> : Iterable<T> {
    // doubly linked
    private class Node<T>(
        var item: T
    ) {
        var prev: Node<T>? = null
        var next: Node<T>? = null
    }

    // stores the nodes themselves
    private val backing = mutableMapOf<T, Node<T>>()

    // used for getting first n last
    private var head: Node<T>? = null
    private var tail: Node<T>? = null

    val size: Int get() = backing.size

    override fun iterator(): Iterator<T> {
        return backing.keys.iterator()
    }

    /**
     * Appends a task to this list of tasks.
     */
    fun append(task: T) {
        val node = Node(task)
        node.prev = tail
        tail?.next = node
        tail = node

        if (head == null) head = node

        backing[task] = node
    }

    /**
     * Removes the first task from this list.
     */
    fun removeFirst(): T? {
        val node = head ?: return null
        head = node.next
        head?.prev = null

        backing.remove(node.item)

        if (backing.isEmpty()) {
            head = null
            tail = null
        }

        return node.item
    }

    /**
     * Removes the last task from this list.
     */
    fun removeLast(): T? {
        val node = tail ?: return null

        tail = node.prev
        tail?.next = null

        backing.remove(node.item)

        if (backing.isEmpty()) {
            head = null
            tail = null
        }

        return node.item
    }

    /**
     * Removes a task from this list, if it exists.
     */
    fun removeTask(task: T) {
        val item = backing.remove(task) ?: return
        item.prev?.next = item.next
        item.next?.prev = item.prev

        if (item == tail) {
            tail = item.prev
        }
        if (item == head) {
            head = item.next
        }
    }
}
