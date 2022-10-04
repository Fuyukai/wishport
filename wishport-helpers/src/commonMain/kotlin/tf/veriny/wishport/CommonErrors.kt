package tf.veriny.wishport

public sealed interface CommonErrors : Fail

/**
 * Returned when a collection index is out of range. The [index] attribute is the index requested.
 */
public class IndexOutOfRange(public val index: UInt) : CommonErrors {
    override fun toString(): String {
        return "IndexOutOfRange[index = $index]"
    }
}

/**
 * Returned when a collection is too small for the operation requested.
 */
public class TooSmall(public val sizeRequested: UInt) : CommonErrors {
    override fun toString(): String {
        return "TooSmall[sizeRequested = $sizeRequested]"
    }
}