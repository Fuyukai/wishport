package tf.veriny.wishport.io

import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * Combines [PartialSendStream] and [Stream] into one object.
 */
@ProvisionalApi
public interface PartialStream : PartialSendStream, Stream