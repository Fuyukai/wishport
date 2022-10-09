package tf.veriny.wishport.core

import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.internals.EventLoop
import tf.veriny.wishport.internals.nanosleep
import tf.veriny.wishport.sync.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(LowLevelApi::class)
class `Test worker threads` {
    @Test
    public fun `Test that the worker thread actually does something`() = runUntilCompleteNoResult {
        val result = runSynchronouslyOffThread({ "test" }) {
            // just make sure the I/O gets woken up
            nanosleep(1L * NS_PER_SEC)
            Either.ok(it)
        }
        assertTrue(result.isSuccess)
        assertEquals("test", result.get())
    }

    @Test
    public fun `Test running in a separate worker thread`() = runUntilCompleteNoResult {
        val p = Promise<Long>()
        val clock = EventLoop.get().clock

        // tricky, and also kinda gross, way of testing it
        Nursery.open {
            it.startSoonNoResult {
                runSynchronouslyOffThread({ clock }) { c ->
                    val time = c.getCurrentTime()
                    nanosleep(2L * NS_PER_SEC)
                    val after = c.getCurrentTime()
                    p.set(after - time)

                    Either.ok(Unit)
                }
            }

            // sleep for one second and make sure that the promise is not set
            sleep(1L * NS_PER_SEC)
            assertFalse(p.flag, "flag was set when it shouldn't've been")
            // sleep for 1.1 seconds and make sure that the promise *is* set
            sleep(1L * NS_PER_SEC + NS_PER_SEC / 10)
            assertTrue(p.flag, "flag wasn't set when it should've been")
            assertTrue(p.wait().get()!! >= 2 * NS_PER_SEC, "didn't sleep for 2 seconds?")
        }
    }

    @Test
    fun `Test being woken up by a worker thread multiple times`() = runUntilCompleteNoResult {
        var accum = 0
        for (i in 0 until 3) {
            accum += runSynchronouslyOffThread({ i }) { Either.ok(i) }.get()!!
        }
        assertEquals(3, accum)
    }
}