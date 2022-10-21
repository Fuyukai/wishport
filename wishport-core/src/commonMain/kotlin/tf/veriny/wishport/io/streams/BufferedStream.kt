package tf.veriny.wishport.io.streams

import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * A combination of the [Stream] and [BufferedReadStream] API.
 */
@ProvisionalApi
public interface BufferedStream : Stream, BufferedReadStream