package com.hgame.core

import kotlin.random.Random

/** Sudoku generator and solver on a flat IntArray(81); 0 = empty. */
object Sudoku {
    fun canPlace(g: IntArray, idx: Int, v: Int): Boolean {
        val r = idx / 9; val c = idx % 9
        for (i in 0 until 9) {
            if (g[r * 9 + i] == v || g[i * 9 + c] == v) return false
        }
        val br = r / 3 * 3; val bc = c / 3 * 3
        for (rr in br until br + 3) for (cc in bc until bc + 3) if (g[rr * 9 + cc] == v) return false
        return true
    }

    /** Fills [g] in place with a random complete solution. */
    fun fill(g: IntArray, rnd: Random): Boolean {
        val idx = g.indexOf(0)
        if (idx < 0) return true
        for (v in (1..9).shuffled(rnd)) {
            if (canPlace(g, idx, v)) {
                g[idx] = v
                if (fill(g, rnd)) return true
                g[idx] = 0
            }
        }
        return false
    }

    /** Counts solutions, stopping at [limit]. */
    fun countSolutions(g: IntArray, limit: Int = 2): Int {
        // choose the empty cell with fewest candidates
        var best = -1; var bestCount = 10
        for (i in 0 until 81) if (g[i] == 0) {
            var n = 0
            for (v in 1..9) if (canPlace(g, i, v)) n++
            if (n < bestCount) { bestCount = n; best = i; if (n <= 1) break }
        }
        if (best < 0) return 1
        if (bestCount == 0) return 0
        var total = 0
        for (v in 1..9) if (canPlace(g, best, v)) {
            g[best] = v
            total += countSolutions(g, limit - total)
            g[best] = 0
            if (total >= limit) break
        }
        return total
    }

    /** Returns (puzzle, solution). [clues] is the target number of givens (puzzle stays unique). */
    fun generate(clues: Int, rnd: Random = Random.Default): Pair<IntArray, IntArray> {
        val sol = IntArray(81)
        fill(sol, rnd)
        val puzzle = sol.copyOf()
        var filled = 81
        for (i in (0 until 81).shuffled(rnd)) {
            if (filled <= clues) break
            val keep = puzzle[i]
            puzzle[i] = 0
            if (countSolutions(puzzle.copyOf()) != 1) puzzle[i] = keep else filled--
        }
        return puzzle to sol
    }
}
