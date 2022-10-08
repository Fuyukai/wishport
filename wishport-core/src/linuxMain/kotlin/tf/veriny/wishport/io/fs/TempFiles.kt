/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package tf.veriny.wishport.io.fs

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.core.InternalWishportError
import tf.veriny.wishport.io.FileOpenFlags
import tf.veriny.wishport.io.FileOpenMode

// inspired heavily by python tempfile.

private object TempFileNamer : Iterator<String>, Iterable<String> {
    override fun iterator(): Iterator<String> {
        return this
    }

    override fun hasNext(): Boolean {
        return true
    }

    override fun next(): String {
        return SecureRandom.randomString(26)
    }
}

/**
 * Gets a list of candidate directories for temporary directories.
 */
private fun getCandidateDirectories(): List<SystemPurePath> {
    val dirs = mutableListOf<SystemPurePath>()

    for (envvar in listOf("TMPDIR", "TEMP", "TMP")) {
        val name = getEnvironmentVariable(envvar) ?: continue
        if (name.isEmpty()) continue
        val result = SystemPurePath.from(name)
        if (result.isSuccess) dirs.add(result.get()!!)
    }

    dirs.add(systemPathFor("/tmp").get()!!)
    dirs.add(systemPathFor("/var/tmp").get()!!)

    // use current dir as a fallback
    getWorkingDirectory().get()?.also { dirs.add(it) }

    return dirs
}

private lateinit var tempDir: SystemFilesystemHandle

@OptIn(Unsafe::class)
private suspend fun getTempDir(): CancellableSuccess<SystemFilesystemHandle> {
    if (::tempDir.isInitialized) {
        return Cancellable.ok(tempDir)
    }

    val writeBuffer = "kras mazov".encodeToByteArray()
    val candidates = getCandidateDirectories()

    for (candidate in candidates) {
        val result =
            SystemFilesystem.getFileHandle(
                candidate, FileOpenMode.READ_ONLY,
                setOf(FileOpenFlags.PATH, FileOpenFlags.DIRECTORY)
            )

        val path = if (result.isSuccess) result.get()!! else continue

        // hack: openat with CURRENT_DIR
        val opened = path.openRelative(
            PosixPurePath.CURRENT_DIR, FileOpenMode.READ_WRITE,
            setOf(
                FileOpenFlags.TEMPORARY_FILE, FileOpenFlags.MUST_CREATE
            )
        )
            .andAlso { it.writeFrom(writeBuffer) }
            .andThen { it.close() }

        if (opened.isSuccess) {
            // ok, it let us, return that
            tempDir = path
            return Cancellable.ok(path)
        } else {
            path.close()
            continue
        }
    }

    throw InternalWishportError("Failed to find any temporary directory!")
}

/**
 * Opens an unnamed temporary file in the temporary directory.
 */
@ProvisionalApi
public actual suspend fun openTemporaryFile(
    scope: AsyncClosingScope,
    mode: FileOpenMode,
    flags: Set<FileOpenFlags>
): CancellableResult<FilesystemHandle<SystemPurePath>, Fail> {
    val realFlags = flags + setOf(
        FileOpenFlags.MUST_CREATE, FileOpenFlags.TEMPORARY_FILE
    )

    return getTempDir().andThen {
        it.openRelative(scope, PosixPurePath.CURRENT_DIR, FileOpenMode.READ_WRITE, realFlags)
    }
}

/**
 * Creates a temporary directory and returns a [FilesystemHandle] for it.
 */
@OptIn(Unsafe::class)
@ProvisionalApi
public actual suspend fun <S, F : Fail> createTemporaryDirectory(
    block: suspend (FilesystemHandle<SystemPurePath>) -> CancellableResult<S, F>
): CancellableResult<S, Fail> {
    val fileName = TempFileNamer.next()
    return getTempDir().andThen { tmp ->
        val flags = setOf(FileOpenFlags.DIRECTORY, FileOpenFlags.PATH)

        for (seq in 0 until 100) {
            val result = systemPathFor(fileName)
                .andAlso {
                    SystemFilesystem.mkdirRelative(tmp, it)
                }
                .andThen {
                    SystemFilesystem.getRelativeFileHandle(tmp, it, FileOpenMode.READ_WRITE, flags)
                }

            return@andThen if (result.isFailure) {
                val failure = result.getFailure()!!
                // try again if it exists, or return the error otherwise
                // safe cast, <Success> doesn't exist
                @Suppress("UNCHECKED_CAST")
                if (failure == FileExists) continue
                else result as CancellableResult<S, Fail>
            } else {
                // run and then close
                // don't care about cleaning it up cos we might be cancelled.
                // not an ideal situation, but oh well...
                result.andThen {
                    val res = block(it)
                    it.close()
                    res
                }
            }
        }

        Cancellable.failed(FileExists)
    }
}
