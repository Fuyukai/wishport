package tf.veriny.wishport.internals

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.Clock

/**
 * A clock that uses the current computer's idea of time.
 */
@LowLevelApi
public expect object PlatformClock : Clock