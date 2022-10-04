package tf.veriny.wishport.annotations

/**
 * Marker annotation for APIs that are provisional and subject to breaking changes and/or removal.
 */
@RequiresOptIn(
    message = "This API is subject to change and requires opt-in",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
public annotation class ProvisionalApi