import tf.veriny.wishport.*
import tf.veriny.wishport.annotations.LowLevelApi
import tf.veriny.wishport.annotations.ProvisionalApi
import tf.veriny.wishport.collections.FastArrayList
import tf.veriny.wishport.io.fs.FileOpenType
import tf.veriny.wishport.io.fs.openTemporaryFile
import tf.veriny.wishport.io.fs.systemPathFor
import tf.veriny.wishport.io.readUpto
import tf.veriny.wishport.io.writeAll
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

@OptIn(LowLevelApi::class, ProvisionalApi::class)
public fun main(): Unit = runUntilCompleteNoResult {
    var result = 0L

    repeat(10_000) {
        val r = measureNanoTime {
            AsyncClosingScope { scope ->
                Imperatavize.cancellable<Unit, Fail> {
                    val first = scope.openUnbufferedSystemFile(systemPathFor("/dev/zero")).q()
                    val second = openTemporaryFile(scope, FileOpenType.WRITE_ONLY).q()
                    first.readUpto(4096U).andThen { second.writeAll(it) }.q()
                }
            }
        }
        result += r
    }

    print("average us: ${result / 10000L / 1000L}")
}
