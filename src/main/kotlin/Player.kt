
import com.github.kittinunf.fuel.httpPost
import com.google.gson.Gson

/**
 * @author Ondrej Musil
 */
class Player {

    data class StartData(val nickname: String, val email: String, val slots: Int)
    data class StartResponse(val gameId: String)
    data class GuessData(val gameId: String, val guess: IntArray)
    data class GuessResponse(val evaluation: GuessRating, val guessCount: Int)
    data class GuessRating(val black: Int, val white: Int)

    private val nickname = "oneionn"
    private val email = "musil.ondra@gmail.com"

    private val startUrl = "https://ikariera.etnetera.cz/veletrhy/start"
    private val guessUrl = "https://ikariera.etnetera.cz/veletrhy/guess"

    private var gameSize = 0
    private var gameId = ""

    private var guessCount = 0

    private var gameSolution: IntArray? = null

    fun play(mock: Boolean) {
        gameSize = if (mock) 5 else 100
        startGame()
//        playBruteForce()
        playDivideAndConquer()
    }

    private fun startGame() {
        val startData = StartData(nickname, email, gameSize)
        val requestBody = Gson().toJson(startData)
        println(requestBody.toString())
        val (ignoredRequest, ignoredResponse, result) =
            startUrl.httpPost()
                    .header("Content-Type" to "application/json")
                    .body(requestBody.toString())
                    .responseString()
        result.fold({
            val resp = Gson().fromJson(it, StartResponse::class.java)
            gameId = resp.gameId
            println("Game started!")
        }, {
            println("Start game failed.")
            System.exit(333)
        })
    }

    private fun sendGuess(guess: IntArray): GuessRating {
        guessCount++
        val requestBody = Gson().toJson(GuessData(gameId, guess))
        val (ignoredRequest, ignoredResponse, result) =
                guessUrl.httpPost()
                        .header("Content-Type" to "application/json")
                        .body(requestBody.toString())
                        .responseString()
        result.fold({
            val resp = Gson().fromJson(it, GuessResponse::class.java)
            return resp.evaluation
        }, {
            println("Guess failed.")
            return GuessRating(0, 0)
        })
    }

    private fun playBruteForce() {

        val freqs = IntArray(gameSize + 1)
        val solution = IntArray(gameSize + 1)

        // get frequencies for heuristic
        for (x in 1..gameSize) {
            val guess = IntArray(gameSize, {_ -> x})
            val result = sendGuess(guess)
            freqs[x] = result.black
        }
        println("Frequencies: ${freqs.joinToString()}")

        // test single numbers vs positions
        for (num in 1..gameSize) {
            for (pos in 1..gameSize) {
                if (freqs[num] < 1) {
                    break
                }
                if (solution[pos] == 0) { // position is free
                    val guess = getNumberOnPosition(num, pos)
                    val result = sendGuess(guess)
                    if (result.black == 1) { // placement is correct
                        solution[pos] = num
                        freqs[num]--
                    }
                }
            }
        }

        // submit solution
        val guess = solution.sliceArray(1..gameSize)
        println("Submitting solution [${guess.joinToString()}]")
        val result = sendGuess(guess)
        if (result.black == gameSize) {
            println("Eureka! Guess count: $guessCount")
        }
    }

    private fun getNumberOnPosition(number: Int, position: Int): IntArray {
        val arr = IntArray(gameSize)
        arr[position - 1] = number
        return arr
    }

    private fun playDivideAndConquer() {
        val cards = IntArray(gameSize + 1)  // cards = cardinalities
        gameSolution = IntArray(gameSize)

        var firstCard = gameSize // we don't have ask for first card, instead we can count it by subtracting others

        // get frequencies for heuristic
        for (x in 2..gameSize) {
            val guess = IntArray(gameSize, {_ -> x})
            val result = sendGuess(guess)
            cards[x] = result.black
            firstCard -= cards[x]
            if (firstCard < 1) {
                break
            }
        }
        cards[1] = firstCard
//        println("Card sum: ${cards.sum()}")

        val leftCards = findLeftCards(0, gameSize / 2, cards)
        val rightCards = computeRightCards(gameSize / 2, gameSize, cards, leftCards)
        submitSolution()
    }

    private fun findLeftCards(left: Int, right: Int, parentCards: IntArray): IntArray {
        val sectorSize = right - left
        val cards = IntArray(gameSize + 1)
        val nonZeroCards = mutableListOf<Int>()

        var firstCard = sectorSize
        var firstColor = 0
        var firstColorSkipped = false
        for (x in 1..gameSize) {
            if (parentCards[x] > 0) {
                if (!firstColorSkipped) {
                    firstColor = x
                    firstColorSkipped = true
                    continue
                }
                val guess = IntArray(gameSize, {
                    if (it in left until right) x else 0
                })
                val result = sendGuess(guess)
                cards[x] = result.black
                firstCard -= cards[x]
                if (cards[x] > 0) {
                    nonZeroCards.add(x)
                }
                if (firstCard < 1) {
                    break
                }
            }
        }
        cards[firstColor] = firstCard
        nonZeroCards.add(firstColor)
//        println("Card sum: ${cards.sum()}")

        if (nonZeroCards.size == 1) {
            writeSolution(left, right, nonZeroCards[0])
            return cards
        }
        val leftCards = findLeftCards(left,  left + sectorSize / 2, cards)
        val rightCards = computeRightCards(left + sectorSize / 2, right, cards, leftCards)
        return cards
    }

    private fun computeRightCards(left: Int, right: Int, parentCards: IntArray,
                                  compCards: IntArray): IntArray {
        val sectorSize = right - left
        val cards = IntArray(gameSize + 1)
        val nonZeroCards = mutableListOf<Int>()

        for (x in 1..gameSize) {
            cards[x] = parentCards[x] - compCards[x]
            if (cards[x] > 0) {
                nonZeroCards.add(x)
            }
        }
        if (nonZeroCards.size == 1) {
            writeSolution(left, right, nonZeroCards[0])
            return cards
        }
//        println("Card sum: ${cards.sum()}")
        val leftCards = findLeftCards(left,  left + sectorSize / 2, cards)
        val rightCards = computeRightCards(left + sectorSize / 2, right, cards, leftCards)
        return cards
    }

    private fun writeSolution(left: Int, right: Int, color: Int) {
        for (p in left until right) {
            gameSolution?.set(p, color)
        }
    }

    private fun submitSolution() {
        gameSolution?.let {
            val result = sendGuess(it)
            if (result.black == gameSize) {
                println("Eureka! Guess count: $guessCount")
            }
        }
    }
}
