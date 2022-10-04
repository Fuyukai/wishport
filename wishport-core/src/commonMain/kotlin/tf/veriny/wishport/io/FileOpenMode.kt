package tf.veriny.wishport.io

/**
 * Enumeration of possible ways to open a file.
 */
public enum class FileOpenMode {
    /**
     * The file will be opened in read-only mode.
     */
    READ_ONLY,

    /**
     * The file will be opened in write-only mode.
     */
    WRITE_ONLY,

    /**
     * The file will be opened for both reading and writing.
     */
    READ_WRITE,

    ;
}