package tf.veriny.wishport.internals

import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.Clock

@LowLevelApi
public actual object PlatformClock : Clock {
    override fun getCurrentTime(): Long {
        return getMonotonicTime()
    }
}