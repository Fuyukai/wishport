package tf.veriny.wishport.internals.io

import kotlinx.cinterop.*
import platform.extra.wp_opendir
import platform.posix.*
import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.Unsafe
import tf.veriny.wishport.collections.ByteString
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.io.fs.*

@OptIn(Unsafe::class)
internal fun realPathOfFd(fd: Int): Either<ByteString, ResourceError> = memScoped {
    val path = "/proc/self/fd/${fd}"
    val out = allocArray<ByteVar>(PATH_MAX)

    val res = readlink(path, out, PATH_MAX)
    return if (res < 0) {
        posix_errno().toSysResult()
    } else {
        val data = out.readZeroTerminated(res.toInt())
        Either.ok(ByteString.uncopied(data))
    }
}

/**
 * Performs a full directory reading.
 */
@OptIn(Unsafe::class)
internal fun doReadDir(fd: Int): Either<List<DirectoryEntry>, ResourceError> {
    return realPathOfFd(fd)
        .andThen {
            // open a fresh copy of the directory as fdopendir is completely fucked up
            val pinned = it.pinnedTerminated()
            val dir = wp_opendir(pinned.addressOf(0))
            pinned.unpin()

            if (dir == null) {
                val errno = posix_errno()
                closedir(dir)
                return errno.toSysResult()
            }

            val entries = FastArrayList<DirectoryEntry>()
            while (true) {
                // do a best attempt at preserving errno
                // the gc might fuck with us but w/e. worst case we finish early
                set_posix_errno(0)
                val next = readdir(dir)

                if (next == null) {
                    val errno = posix_errno()
                    if (errno == 0) break
                    closedir(dir)
                    return errno.toSysResult()
                }

                val rawName = next.pointed.d_name.readZeroTerminated()
                val name = ByteString.uncopied(rawName)

                // skip dot and dotdot from being in the listing
                if (name == PathComponent.CurrentDir.DOT) continue
                if (name == PathComponent.PreviousDir.DOTDOT) continue

                val type = FileType.toSet(next.pointed.d_type.toUShort())
                entries.add(DirectoryEntry(type, name))
            }

            closedir(dir)
            Either.ok(entries)
        }
}
