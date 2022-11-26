package tf.veriny.wishport.collections

/**
 * A [List] that is guaranteed to contain at least one element.
 */
public class NonEmptyList<E : Any>
private constructor(private val backing: FastArrayList<E>) : List<E> by backing {
    public companion object {
        public operator fun <E : Any> invoke(other: List<E>): NonEmptyList<E> {
            if (other.isEmpty()) throw IllegalArgumentException("provided list has no elements")

            val newList = FastArrayList<E>()
            newList.addAll(other)
            return NonEmptyList(newList)
        }
    }

    override fun isEmpty(): Boolean = false

    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("NonEmptyList[")

        val iterator = this.iterator()
        while (iterator.hasNext()) {
            val el = iterator.next()
            builder.append(el.toString())
            if (iterator.hasNext()) builder.append(", ")
        }

        return builder.append(']').toString()
    }

    override fun hashCode(): Int {
        return backing.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other == null || other !is NonEmptyList<*>) return false
        return other.backing == backing
    }
}

/**
 * Creates a new, non-empty list containing [el] as well as all the [rest].
 */
public fun <E : Any> nonEmptyListOf(el: E, vararg rest: E): NonEmptyList<E> {
    val list = FastArrayList<E>(rest.size + 1)
    list.add(el)
    list.addAll(rest)
    return NonEmptyList(list)
}