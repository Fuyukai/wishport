package tf.veriny.wishport.core

/**
 * Thrown when something is deeply, deeply wrong in Wishport's core. Do not attempt to catch this.
 */
public class InternalWishportError(
    message: String, cause: Throwable? = null,
) : Throwable(message, cause)