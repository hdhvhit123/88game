package com.hgame.core

/** Gomoku (five in a row) on an n x n board with a pattern-scoring AI. */
class Gomoku(val n: Int = 15) {
    val cells = IntArray(n * n)

    fun inside(r: Int, c: Int) = r in 0 until n && c in 0 until n
    private val dirs = arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, -1))

    /** The five (or more) stones through [idx] if that move won. */
    fun winLine(idx: Int): List<Int>? {
        val p = cells[idx]
        if (p == 0) return null
        val r = idx / n; val c = idx % n
        for (d in dirs) {
            val line = arrayListOf(idx)
            for (sgn in intArrayOf(1, -1)) {
                var rr = r + d[0] * sgn; var cc = c + d[1] * sgn
                while (inside(rr, cc) && cells[rr * n + cc] == p) { line.add(rr * n + cc); rr += d[0] * sgn; cc += d[1] * sgn }
            }
            if (line.size >= 5) return line
        }
        return null
    }

    /** Score of the lines [p] would form by playing at (r,c). */
    private fun lineScore(r: Int, c: Int, p: Int): Int {
        var total = 0
        for (d in dirs) {
            var count = 1; var open = 0
            for (sgn in intArrayOf(1, -1)) {
                var rr = r + d[0] * sgn; var cc = c + d[1] * sgn
                while (inside(rr, cc) && cells[rr * n + cc] == p) { count++; rr += d[0] * sgn; cc += d[1] * sgn }
                if (inside(rr, cc) && cells[rr * n + cc] == 0) open++
            }
            total += when {
                count >= 5 -> 1_000_000
                count == 4 && open == 2 -> 100_000
                count == 4 && open == 1 -> 12_000
                count == 3 && open == 2 -> 8_000
                count == 3 && open == 1 -> 600
                count == 2 && open == 2 -> 400
                count == 2 && open == 1 -> 60
                count == 1 && open == 2 -> 10
                else -> 0
            }
        }
        return total
    }

    /** Picks a move for [p]. [skill] 0..2 changes how defensive/precise it is. */
    fun bestMove(p: Int, skill: Int, rnd: kotlin.random.Random = kotlin.random.Random.Default): Int {
        if (cells.all { it == 0 }) return (n / 2) * n + n / 2
        var best = -1; var bestS = -1.0
        val attackW = doubleArrayOf(1.0, 1.0, 1.1)[skill]
        val defendW = doubleArrayOf(0.6, 0.9, 1.0)[skill]
        for (i in cells.indices) {
            if (cells[i] != 0) continue
            val r = i / n; val c = i % n
            // only consider cells near existing stones
            var near = false
            loop@ for (dr in -2..2) for (dc in -2..2) {
                if (inside(r + dr, c + dc) && cells[(r + dr) * n + c + dc] != 0) { near = true; break@loop }
            }
            if (!near) continue
            var s = lineScore(r, c, p) * attackW + lineScore(r, c, 3 - p) * defendW
            s += rnd.nextDouble() * (if (skill == 0) 300.0 else 5.0)
            if (s > bestS) { bestS = s; best = i }
        }
        return best
    }
}
