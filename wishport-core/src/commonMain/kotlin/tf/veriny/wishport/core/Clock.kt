package tf.veriny.wishport.core

import tf.veriny.wishport.annotations.LowLevelApi

/**
 * The clock is responsible for scheduling tasks in the future. The Wishport clock only provides
 * two guarantees:
 *
 *  1. Monotonicity (i.e. calling [getCurrentTime] will always return a greater number than last
 *                   time)
 *  2. All units are in Long nanoseconds.
 *
 * The number of nanoseconds may be irrelevant to the actual time passing within your personal
 * frame of reference for various reasons (such as time dilation, your death at the hands of a black
 * hole, or a testing clock).
 */
@LowLevelApi
public interface Clock {
    /**
     * Gets the current time, in nanoseconds, relative to the start point for this clock.
     */
    public fun getCurrentTime(): Long
}