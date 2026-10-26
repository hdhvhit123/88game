package com.hgame.core

/** Reversi / Othello on 8x8. cells: 0 empty, 1 black, 2 white. Black moves first. */
class Reversi {
    val cells = IntArray(64)

    companion object {
        private val DIRS = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)
        private val WEIGHTS = intArrayOf(
            100, -20, 10, 5, 5, 10, -20, 100, -20, -50, -2, -2, -2, -2, -50, -20,
            10, -2, -1, -1, -1, -1, -2, 10, 5, -2, -1, -1, -1, -1, -2, 5,
            5, -2, -1, -1, -1, -1, -2, 5, 10, -2, -1, -1, -1, -1, -2, 10,
            -20, -50, -2, -2, -2, -2, -50, -20, 100, -20, 10, 5, 5, 10, -20, 100)
    }

    init { reset() }

    fun reset() { cells.fill(0); cells[27] = 2; cells[36] = 2; cells[28] = 1; cells[35] = 1 }
    fun copy() = Reversi().also { cells.copyInto(it.cells) }

    /** Discs that would be flipped by [p] playing at [sq]. */
    fun flips(sq: Int, p: Int): List<Int> {
        if (cells[sq] != 0) return emptyList()
        val out = ArrayList<Int>()
        for (d in DIRS) {
            val line = ArrayList<Int>()
            var s = sq; var prevF = sq % 8
            while (true) {
                s += d
                if (s !in 0..63 || kotlin.math.abs(s % 8 - prevF) > 1) { line.clear(); break }
                prevF = s % 8
                when (cells[s]) {
                    3 - p -> line.add(s)
                    p -> break
                    else -> { line.clear(); break }
                }
            }
            out.addAll(line)
        }
        return out
    }

    fun moves(p: Int) = (0 until 64).filter { flips(it, p).isNotEmpty() }

    fun play(sq: Int, p: Int): List<Int> {
        val f = flips(sq, p)
        cells[sq] = p
        for (s in f) cells[s] = p
        return f
    }

    fun count(p: Int) = cells.count { it == p }

    fun evaluate(p: Int): Int {
        var s = 0
        for (i in 0 until 64) if (cells[i] == p) s += WEIGHTS[i] else if (cells[i] == 3 - p) s -= WEIGHTS[i]
        s += (moves(p).size - moves(3 - p).size) * 5
        return s
    }

    fun negamax(p: Int, depth: Int, alphaIn: Int, beta: Int, passed: Boolean = false): Int {
        val ms = moves(p)
        if (depth == 0) return evaluate(p)
        if (ms.isEmpty()) {
            if (passed) { val d = count(p) - count(3 - p); return d * 1000 }
            return -negamax(3 - p, depth - 1, -beta, -alphaIn, true)
        }
        var alpha = alphaIn
        for (m in ms) {
            val c = copy(); c.play(m, p)
            val s = -c.negamax(3 - p, depth - 1, -beta, -alpha)
            if (s >= beta) return s
            if (s > alpha) alpha = s
        }
        return alpha
    }

    fun bestMove(p: Int, depth: Int): Int {
        var best = -1; var bestS = Int.MIN_VALUE
        for (m in moves(p).shuffled()) {
            val c = copy(); c.play(m, p)
            val s = -c.negamax(3 - p, depth - 1, -1_000_000, 1_000_000)
            if (s > bestS) { bestS = s; best = m }
        }
        return best
    }
}
