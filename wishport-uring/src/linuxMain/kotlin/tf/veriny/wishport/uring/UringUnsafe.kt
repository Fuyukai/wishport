package tf.veriny.wishport.uring

/**
 * Marker annotation for marking io_uring structs as unsafe.
 */
@RequiresOptIn(message = "C-level constructs are intrinsically unsafe and require explicit opt-in")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
public annotation class UringUnsafe
