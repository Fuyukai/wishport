import tf.veriny.wishport.AsyncClosingScope
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.core.Nursery
import tf.veriny.wishport.core.open
import tf.veriny.wishport.get
import tf.veriny.wishport.io.fs.*
import tf.veriny.wishport.runUntilCompleteNoResult
import kotlin.system.measureNanoTime

public fun main_1() {
    var farTime = 0L
    var kTime = 0L

    repeat(10_000) {
        farTime += measureNanoTime {
            val arr = FastArrayList<Unit>()
            repeat(1_000) {
                arr.add(Unit)
            }
        }
    }

    repeat(10_000) {
        kTime += measureNanoTime {
            val arr = ArrayList<Unit>(16)
            repeat(1_000) {
                arr.add(Unit)
            }
        }
    }

    println("FastArrayList avg: ${farTime / 10_000}")
    println("Kotlin ArrayList avg: ${kTime / 10_000}")
}

@OptIn(LowLevelApi::class)
public fun `main`(): Unit = runUntilCompleteNoResult {
    var result = 0L

    repeat(1000) {
        val r = measureNanoTime {
            AsyncClosingScope { scope ->
                val path = PosixPurePath.from("/dev/zero").get()!!
                Nursery.open { n ->
                    repeat(1024) {
                        n.startSoon { FilesystemHandle.openRawFile(scope, path) }
                    }
                }
            }
        }
        result += r
    }

    print("average ns: ${result / 10000L}")
}
