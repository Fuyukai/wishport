/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.internals

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.CancelScope

// This is based off of using a simple set of active cancel scopes, rather than a list.
// This drastically simplifies deadline tracking at the cost of perf, but I've put some thought into
// this to avoid the common performance pitfalls.
//
// 1) When a deadline is added, it and any children with a non-infinite, non-smaller deadline are
//    added to the set.
// 2) After the event loop has ran its first phase (running scheduled tasks), the list of deadlined
//    tasks are gathered. We do this by a simple linear search for all tasks with deadlines in the
//    past, in order.
// 3) These deadlines are eaten from the container, and cancellations will be dispatched.
//
// Only scopes with either a non-positive infinity deadline or any parent with a non-positive
// infinity deadline are added here. If a scope fails this pre-condition, it and all its children
// are removed.

// XXX: We already have a heap, maybe consider just using the heap sort over the basic insertion
//      sort implemented here.
/**
 * Contains the list of cancel that are currently waiting for a deadline.
 */
@LowLevelApi
public class Deadlines {
    private val scopes = hashSetOf<CancelScope>()

    /**
     * Recursively walks the cancel scope tree, adding any scope that needs to be added to the
     * set of scopes.
     */
    private inline fun recursivelyAddScopes(root: CancelScope, fn: (CancelScope) -> Boolean) {
        val todo = ArrayDeque<CancelScope>()
        todo.add(root)
        while (todo.isNotEmpty()) {
            val next = todo.removeFirst()
            val should = fn(next)

            if (should) {
                todo.addAll(next.children)
                scopes.add(next)
            }
        }
    }

    /**
     * Recursively walks the cancel scope tree, remove any scope that needs to be removed from the
     * set of scopes.
     */
    private inline fun recursivelyDelScopes(root: CancelScope, fn: (CancelScope) -> Boolean) {
        val todo = ArrayDeque<CancelScope>()
        todo.add(root)
        while (todo.isNotEmpty()) {
            val next = todo.removeFirst()
            val should = fn(next)

            if (should) {
                todo.addAll(next.children)
                scopes.remove(next)
            }
        }
    }

    private fun recursivelyAddScopes(root: CancelScope) {
        return recursivelyAddScopes(root) {
            root.effectiveDeadline != CancelScope.NEVER_CANCELLED
        }
    }

    /**
     * Adds a cancel scope to the current set of tracked cancel scopes.
     */
    public fun add(scope: CancelScope) {
        assert(!scope.exited && scope.effectiveDeadline != CancelScope.NEVER_CANCELLED) {
            "why is a scope with no deadline being added?"
        }

        recursivelyAddScopes(scope)
    }

    public fun remove(scope: CancelScope) {
        assert(scope.exited || scope.effectiveDeadline == CancelScope.NEVER_CANCELLED) {
            "why is a scope with a deadline being removed?"
        }

        recursivelyDelScopes(scope) { it.effectiveDeadline != CancelScope.NEVER_CANCELLED }
    }

    /**
     * Purges any expired cancellation scopes from the set of deadlines.
     */
    public fun purge(deadline: Long): Pair<List<CancelScope>, CancelScope?> {
        if (scopes.isEmpty()) {
            return Pair(emptyList(), null)
        }

        // loop over each scope, remove any with an expired deadline, and find the scope with the
        // soonest deadline
        // we assign the properties to local vars to avoid walking the tree repeatedly.
        // small micro-opt but w/e
        var min: CancelScope? = null
        var minDl: Long = 0L
        val purged = mutableListOf<CancelScope>()
        for (item in scopes) {
            val dl = item.effectiveDeadline
            if (dl <= deadline) {
                purged.add(item)
            } else {
                if (min == null || dl < minDl) {
                    min = item
                    minDl = item.effectiveDeadline
                }
            }
        }

        // done separately to avoid iteration issues.
        purged.forEach { scopes.remove(it) }

        return Pair(purged, min)
    }

    /**
     * Checks if there are any sleeping tasks.
     */
    public fun isEmpty(): Boolean = scopes.isEmpty()
}
