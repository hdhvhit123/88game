package com.hgame.core

import kotlin.random.Random

/** Match-3 board logic. cells[r*n+c] = gem type 0 until kinds. */
class Match3(val n: Int = 8, val kinds: Int = 6, private val rnd: Random = Random.Default) {
    val cells = IntArray(n * n)

    init {
        do {
            for (i in cells.indices) {
                var t: Int
                do { t = rnd.nextInt(kinds); cells[i] = t } while (matchesAt(i))
            }
        } while (!hasMove())
    }

    private fun matchesAt(i: Int): Boolean {
        val r = i / n; val c = i % n; val t = cells[i]
        if (c >= 2 && cells[i - 1] == t && cells[i - 2] == t) return true
        if (r >= 2 && cells[i - n] == t && cells[i - 2 * n] == t) return true
        return false
    }

    /** All cells that are part of a horizontal or vertical run of 3+. */
    fun findMatches(): Set<Int> {
        val out = HashSet<Int>()
        for (r in 0 until n) {
            var run = 1
            for (c in 1..n) {
                if (c < n && cells[r * n + c] == cells[r * n + c - 1] && cells[r * n + c] >= 0) run++
                else { if (run >= 3) for (k in c - run until c) out.add(r * n + k); run = 1 }
            }
        }
        for (c in 0 until n) {
            var run = 1
            for (r in 1..n) {
                if (r < n && cells[r * n + c] == cells[(r - 1) * n + c] && cells[r * n + c] >= 0) run++
                else { if (run >= 3) for (k in r - run until r) out.add(k * n + c); run = 1 }
            }
        }
        return out
    }

    fun swap(a: Int, b: Int) { val t = cells[a]; cells[a] = cells[b]; cells[b] = t }

    fun adjacent(a: Int, b: Int) = (a / n == b / n && kotlin.math.abs(a - b) == 1) || kotlin.math.abs(a - b) == n

    fun hasMove(): Boolean = findMove() != null

    fun findMove(): Pair<Int, Int>? {
        for (i in cells.indices) for (j in intArrayOf(i + 1, i + n)) {
            if (j >= n * n || (j == i + 1 && i % n == n - 1)) continue
            swap(i, j)
            val ok = findMatches().isNotEmpty()
            swap(i, j)
            if (ok) return i to j
        }
        return null
    }

    /**
     * Removes [matched], drops gems down and refills. Returns for each cell how many rows its
     * gem fell (new gems count from above the board) so the view can animate.
     */
    fun collapse(matched: Set<Int>): IntArray {
        val fall = IntArray(n * n)
        for (i in matched) cells[i] = -1
        for (c in 0 until n) {
            var write = n - 1
            for (r in n - 1 downTo 0) {
                val i = r * n + c
                if (cells[i] >= 0) {
                    val t = cells[i]; cells[i] = -1
                    cells[write * n + c] = t; fall[write * n + c] = write - r
                    write--
                }
            }
            val missing = write + 1
            for (r in write downTo 0) { cells[r * n + c] = rnd.nextInt(kinds); fall[r * n + c] = missing }
        }
        return fall
    }

    fun shuffle() {
        do { val l = cells.toList().shuffled(rnd); l.forEachIndexed { i, v -> cells[i] = v } } while (findMatches().isNotEmpty() || !hasMove())
    }
}
