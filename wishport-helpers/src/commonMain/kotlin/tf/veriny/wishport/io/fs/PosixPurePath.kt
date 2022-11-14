/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.Either
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.collections.b
import tf.veriny.wishport.collections.startsWith
import tf.veriny.wishport.get

/**
 * A [PurePath] that implements POSIX semantics.
 */
public open class PosixPurePath(
    override val components: List<PathComponent>,
) : PurePath<PosixPurePath> {
    public companion object {
        private enum class ParserState {
            INITIAL,
            FIRST_DOT,
            SECOND_DOT,
            REGULAR_COMPONENT,
            ;
        }

        private const val SEP = '/'.code.toByte()
        private const val NULL: Byte = 0
        private const val DOT: Byte = '.'.code.toByte()

        /** The path representing the current directory. */
        public val CURRENT_DIR: PosixPurePath = from(".").get()!!

        public fun from(s: String): PathResult<PosixPurePath> {
            return from(b(s))
        }

        /**
         * Gets a new [PosixPurePath] from a [ByteString].
         */
        @OptIn(Unsafe::class)
        public fun from(bs: ByteString): PathResult<PosixPurePath> {
            val components = FastArrayList<PathComponent>()

            // check for absolute paths
            var cursor = if (bs.startsWith(SEP)) {
                components.add(PathComponent.RootDir)
                1
            } else {
                0
            }

            // state machine variables
            val buffer = ByteArray(bs.size)
            var bufferSize = 0
            var state = ParserState.INITIAL

            while (cursor < bs.size) {
                val next = bs.getUnsafe(cursor)
                // null chars are illegal
                when {
                    next == NULL -> {
                        return Either.err(IllegalPathCharacter((0).toChar()))
                    }
                    next == SEP && state == ParserState.FIRST_DOT -> {
                        components.add(PathComponent.CurrentDir)
                        state = ParserState.INITIAL
                    }
                    next == SEP && state == ParserState.SECOND_DOT -> {
                        components.add(PathComponent.PreviousDir)
                        state = ParserState.INITIAL
                    }
                    next == SEP && state == ParserState.REGULAR_COMPONENT -> {
                        val data = buffer.copyOfRange(0, bufferSize)
                        val component = PathComponent.Normal(ByteString.uncopied(data))
                        components.add(component)
                        bufferSize = 0
                        state = ParserState.INITIAL
                    }
                    // ignore
                    next == SEP && state == ParserState.INITIAL -> {}
                    next == DOT && state == ParserState.INITIAL -> {
                        state = ParserState.FIRST_DOT
                    }
                    next == DOT && state == ParserState.FIRST_DOT -> {
                        state = ParserState.SECOND_DOT
                    }
                    state == ParserState.INITIAL || state == ParserState.REGULAR_COMPONENT -> {
                        state = ParserState.REGULAR_COMPONENT
                        buffer[bufferSize] = next
                        bufferSize++
                    }
                }

                cursor++
            }

            if (state == ParserState.FIRST_DOT) components.add(PathComponent.CurrentDir)
            else if (state == ParserState.SECOND_DOT) components.add(PathComponent.PreviousDir)
            else if (bufferSize > 0) {
                val data = buffer.copyOfRange(0, bufferSize)
                val component = PathComponent.Normal(ByteString.uncopied(data))
                components.add(component)
            }

            return Either.ok(PosixPurePath(components))
        }
    }

    override val isAbsolute: Boolean get() {
        return components.first() == PathComponent.RootDir
    }

    override val parent: PosixPurePath? by lazy {
        // obviously no parent
        if (components.size == 1) null
        else {
            PosixPurePath(components.dropLast(1))
        }
    }

    override val ancestors: List<PosixPurePath> by lazy {
        when (components.size) {
            1 -> emptyList()
            2 -> listOf(parent!!)
            else -> {
                val working = ArrayList<PosixPurePath>(components.size - 1)
                var last = parent
                while (true) {
                    if (last == null) break
                    working.add(last)
                    last = last.parent
                }
                working
            }
        }
    }

    override fun isChildOf(path: PosixPurePath): Boolean {
        // un-absolute pure paths cannot be compared child-wise
        // as there exists no reference point to compare
        // but absolute paths can be compared with other absolutes
        // and relatives can be compared with other relatives
        if (isAbsolute != path.isAbsolute) {
            return false
        }

        // obviously false if the other one is bigger than us
        // also, if the path has the same amount of components it cannot be a child as at best it
        // will be the same directory
        if (path.components.size >= components.size) {
            return false
        }

        for ((c1, c2) in path.components.zip(components)) {
            if (c1 != c2) return false
        }

        return true
    }

    override fun resolveChild(path: PosixPurePath): PosixPurePath {
        return if (path.isAbsolute) path
        else PosixPurePath(components + path.components)
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is PosixPurePath) return false

        return components == other.components
    }

    override fun hashCode(): Int {
        return components.hashCode()
    }

    override fun toString(): String {
        return "PosixPurePath[${toByteString()}]"
    }
}
