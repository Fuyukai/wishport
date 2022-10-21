package tf.veriny.wishport.io.streams

import tf.veriny.wishport.annotations.ProvisionalApi

/**
 * A combination of [BufferedStream] and [PartialStream].
 */
@ProvisionalApi
public interface BufferedPartialStream : PartialStream, BufferedStream