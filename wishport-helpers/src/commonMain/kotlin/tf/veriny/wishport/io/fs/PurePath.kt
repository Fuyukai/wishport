/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.Either
import tf.veriny.wishport.Fail
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.b
import tf.veriny.wishport.collections.escapedString

// Heavily, heavily inspired by Rust's std::path.
// It's a really good module and the design is ripped off essentially one-to-one, except backed by
// byte strings rather than OsStr (although, they're essentially the same in Rust current afaict).
// funnily nearly all this extra code is to support windows which has some baffling conventions
// based all the way back on MS-DOS.
// Also see: https://users.rust-lang.org/t/understanding-windows-paths/58583
// Kinda sucks w/o good pattern matching but hopefully K2 will bring that

/** The path separator character for the current platform. */
public expect val PATH_SEP: ByteString

/**
 * A Windows path prefix. Windows uses various prefixes to construct its paths, such as drive
 * letters or Uniform Naming Convention prefixes.
 */
public sealed interface PathPrefix {
    /**
     * If this prefix means that the path is verbatim. If this is the case, no normalisation will
     * be performed by [PurePath],
     */
    public val isVerbatim: Boolean

    /**
     * The verbatim prefix ``\\?\``. Named as such because these paths are passed directly to
     * the NT APIs without processing. This consists of the first component immediately after
     * the component, e.g. ``\\?\cats`` would just be ``b"cats"``.
     */
    public data class Verbatim(public val content: ByteString) : PathPrefix {
        override val isVerbatim: Boolean = true
    }

    /**
     *
     * A verbatim UNC path, with the prefix ``\\?\UNC\`` and consisting of a ``server\share`.
     */
    public data class VerbatimUNC(
        public val server: ByteString,
        public val share: ByteString,
    ) : PathPrefix {
        override val isVerbatim: Boolean = true
    }

    /**
     * The verbatim prefix, immediately followed by the drive letter, e.g. ``\\?\C:``.
     */
    public data class VerbatimDisk(public val driveLetter: ByteString) : PathPrefix {
        override val isVerbatim: Boolean = true
    }

    /**
     * The device namespace prefix ``\\.\``, immediately followed by the device, e.g.
     * ``\\.\PhysicalDrive0``.
     */
    public data class DeviceNameSpace(public val device: ByteString) : PathPrefix {
        override val isVerbatim: Boolean = false
    }

    /**
     * A prefix using Windows' UNC convention, in the format ``\\server\share``.
     */
    public data class UNC(
        public val server: ByteString,
        public val share: ByteString,
    ) : PathPrefix {
        override val isVerbatim: Boolean = false
    }

    /**
     * A prefix that consists of just a regular drive letter.
     */
    public data class DriveLetter(public val letter: ByteString) : PathPrefix {
        override val isVerbatim: Boolean = false
    }
}

/**
 * A single component of a [PurePath].
 */
public sealed interface PathComponent {
    /**
     * Wraps a [PathPrefix] in a [PathComponent]. This will only occur on Windows.
     */
    public data class Prefix(public val prefix: PathPrefix) : PathComponent

    /**
     * The root directory. This appears after the prefix and before all other components if
     * the path is absolute.
     */
    public object RootDir : PathComponent {
        public val ROOT: ByteString = b("/")

        override fun toString(): String {
            return "PathComponent[/]"
        }
    }

    /**
     * A reference to the current directory, i.e. ``.``.
     */
    public object CurrentDir : PathComponent {
        public val DOT: ByteString = b(".")

        override fun toString(): String {
            return "PathComponent[.]"
        }
    }

    /**
     * A reference to the previous directory, i.e. ``..``.
     */
    public object PreviousDir : PathComponent {
        public val DOTDOT: ByteString = b("..")

        override fun toString(): String {
            return "PathComponent[..]"
        }
    }

    /**
     * A regular component of a path, e.g. ``var`` and ``tmp`` in ``/var/tmp``.
     */
    public data class Normal(public val data: ByteString) : PathComponent {
        override fun toString(): String {
            return "PathComponent[${data.escapedString()}]"
        }
    }
}

/**
 * A PurePath is a path that can do path handling operations which don't access a filesystem.
 */
public interface PurePath<Flavour : PurePath<Flavour>> {
    /**
     * The list of [PathComponent] instances that make up this [PurePath].
     */
    public val components: List<PathComponent>

    /** Returns True if this path is an absolute path. */
    public val isAbsolute: Boolean

    /**
     * Gets the parent of this [PurePath]. This will return ``null`` if this path has no parent,
     * i.e. it is the root directory or there are no more components above it.
     */
    public val parent: Flavour?

    /**
     * Gets a list of the ancestors for this [PurePath]. This will be all paths that are direct
     * or indirect parents of this path.
     */
    public val ancestors: List<Flavour>

    /**
     * Checks if this path is a child of the other [path].
     */
    public fun isChildOf(path: Flavour): Boolean

    /**
     * Appends [path] to this path. The behaviour of this varies in PurePath implementations
     * depending on the platform semantics.
     *
     * If [path] is an absolute path, it will replace the current path.
     */
    public fun resolveChild(path: Flavour): Flavour

    /**
     * Appends the [other] string content to this path.
     */
    public fun resolveChild(other: ByteString): PathResult<Flavour>

    /**
     * Replaces the name of this path.
     */
    public fun withName(other: ByteString): PathResult<Flavour>
}

/**
 * The [PurePath] type that the current system natively uses.
 */
public expect class SystemPurePath : PurePath<SystemPurePath>

/**
 * Gets a [SystemPurePath] for the specified [s]tring.
 */
public expect fun systemPathFor(s: String): SystemPurePath

/**
 * Gets a [SystemPurePath] for the specified ByteString.
 */
public expect fun systemPathFor(bs: ByteString): PathResult<SystemPurePath>

// error types
/**
 * Returned from PurePath creation methods if an illegal character is in the path provided.
 */
public class IllegalPathCharacter(public val character: Char) : Fail

public typealias PathResult<Flavour> = Either<Flavour, IllegalPathCharacter>
