package tf.veriny.wishport.channel

import tf.veriny.wishport.Fail

/**
 * Returned from channels if an operation on the channel would require waiting.
 */
public object ChannelWouldBlock : Fail