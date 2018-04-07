import kotlin.math.sign

/**
 * @author Ondrej Musil
 */

fun main(args: Array<String>) {
    val foo = intArrayOf(3, 4, 3, 4, 5, 1, 2, 1, 4, 2, 2, 2, 2)
    var bar = intArrayOf(4, 2, 4, 3, 2, 2, 3, 3, 3, 3)

    var inter = foo.intersect(bar.asIterable())

    println(inter.size)
    println(countPegs(foo, bar))
    println(5 !in foo)
    println(5 !in bar)
}

fun countPegs(foo: IntArray, bar: IntArray): Int {
    var pegs = 0

    val fooCopy = foo.copyOf().sortedArray()
    val barCopy = bar.copyOf().sortedArray()

    var fooIndex = 0
    var barIndex = 0

    while (fooIndex < foo.size && barIndex < bar.size) {
        val f = fooCopy[fooIndex]
        val b = barCopy[barIndex]
        when ((f - b).sign) {
            0 -> {
                pegs++
                fooIndex++
                barIndex++
            }
            -1 -> fooIndex++
            1 -> barIndex++
        }
    }
    return pegs
}
