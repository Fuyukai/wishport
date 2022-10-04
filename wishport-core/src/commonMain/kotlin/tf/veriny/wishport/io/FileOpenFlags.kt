package tf.veriny.wishport.io

/**
 * Enumeration of possible flags for opening a file. These may not be available on every platform;
 * unused flags will be ignored.
 */
public enum class FileOpenFlags {
    // linux: O_APPEND, windows: FILE_APPEND_DATA
    /**
     * When this flag is provided, data will be written to the end of the file, rather than the
     * beginning. On some platforms, the file position will be set to the end and the data will be
     * written in one atomic step, but this behaviour is not guaranteed.
     */
    APPEND,

    // linux: O_CREAT, windows: OPEN_ALWAYS
    /**
     * When this flag is provided, the file will be created if it does not exist. If the file
     * exists, then this flag will do nothing. See [MUST_CREATE].
     */
    CREATE_IF_NOT_EXISTS,

    // linux: O_CREAT + O_EXCL, windows: CREATE_NEW
    /**
     * When this flag is provided, the file will be created, and it must not exist beforehand.
     */
    MUST_CREATE,

    // linux: O_DIRECT, windows: FILE_FLAG_NO_BUFFERING + FILE_FLAG_WRITE_THROUGH
    /**
     * When this flag is provided, then the use of kernel-space cache buffers will be avoided.
     * This flag requires special co-operation from the I/O backend.
     */
    DIRECT,

    // linux: O_NOATIME, windows: n/a
    /**
     * When this flag is provided, a best effort is attempted at not editing the access time for
     * the file.
     */
    NO_ACCESS_TIME,

    // linux: O_NOFOLLOW, windows: seemingly n/a
    /**
     * When this flag is provided, and the trailing part of a file path is a symbolic link, then
     * the symbolic link will not be followed
     */
    NO_FOLLOW,

    // linux: O_TMPFILE, windows: FILE_FLAG_DELETE_ON_CLOSE + FILE_ATTRIBUTE_TEMPORARY
    /**
     * When this flag is provided, the file will be automatically deleted once all references
     * to it are closed.
     */
    TEMPORARY_FILE,

    // linux: O_TRUNC, windows: TRUNCATE_EXISTING
    /**
     * When this flag is provided, the file will be truncated to zero bytes before being opened.
     */
    TRUNCATE,
    ;
}