package com.hgame.core

import kotlin.random.Random

/** Flood-It: change the colour of the region connected to the top-left cell. */
class FloodIt(val n: Int, val colors: Int, rnd: Random = Random.Default) {
    val cells = IntArray(n * n) { rnd.nextInt(colors) }

    /** Cells connected to (0,0) by the current corner colour. */
    fun region(): Set<Int> {
        val c0 = cells[0]
        val seen = HashSet<Int>()
        val stack = ArrayDeque<Int>().apply { add(0) }
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            if (i in seen || cells[i] != c0) continue
            seen.add(i)
            val r = i / n; val c = i % n
            if (r > 0) stack.add(i - n); if (r < n - 1) stack.add(i + n)
            if (c > 0) stack.add(i - 1); if (c < n - 1) stack.add(i + 1)
        }
        return seen
    }

    fun flood(color: Int): Boolean {
        if (color == cells[0]) return false
        for (i in region()) cells[i] = color
        return true
    }

    fun solved() = cells.all { it == cells[0] }

    /** Greedy estimate of moves needed, used to set the move limit. */
    fun greedyMoves(): Int {
        val copy = FloodIt(n, colors).also { cells.copyInto(it.cells) }
        var moves = 0
        while (!copy.solved() && moves < 200) {
            var best = -1; var bestSize = -1
            for (col in 0 until colors) {
                if (col == copy.cells[0]) continue
                val trial = FloodIt(n, colors).also { copy.cells.copyInto(it.cells) }
                trial.flood(col)
                val s = trial.region().size
                if (s > bestSize) { bestSize = s; best = col }
            }
            copy.flood(best); moves++
        }
        return moves
    }
}
