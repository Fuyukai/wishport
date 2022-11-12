import tf.veriny.wishport.AsyncClosingScope
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.core.Nursery
import tf.veriny.wishport.core.open
import tf.veriny.wishport.get
import tf.veriny.wishport.io.fs.*
import tf.veriny.wishport.runUntilCompleteNoResult
import kotlin.system.measureNanoTime

@OptIn(LowLevelApi::class)
public fun main(): Unit = runUntilCompleteNoResult {
    val result = measureNanoTime {
        AsyncClosingScope { scope ->
            val path = PosixPurePath.from("/dev/zero").get()!!
            Nursery.open { n ->
                repeat(1024) {
                    n.startSoon { FilesystemHandle.openRawFile(scope, path) }
                }
            }
        }
    }

    println("ns: $result")
}
