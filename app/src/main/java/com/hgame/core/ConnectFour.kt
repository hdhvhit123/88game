package com.hgame.core

/** Connect Four on a 7x6 board, cells[row*7+col], row 0 = top. 0 empty, 1/2 players. */
class ConnectFour {
    val cells = IntArray(42)

    fun copy() = ConnectFour().also { cells.copyInto(it.cells) }
    fun canDrop(col: Int) = cells[col] == 0

    /** Drops a disc; returns the row it landed in, or -1. */
    fun drop(col: Int, player: Int): Int {
        for (r in 5 downTo 0) if (cells[r * 7 + col] == 0) { cells[r * 7 + col] = player; return r }
        return -1
    }

    fun undrop(col: Int) { for (r in 0..5) if (cells[r * 7 + col] != 0) { cells[r * 7 + col] = 0; return } }

    /** Winning four cells, or null. */
    fun winLine(): IntArray? {
        val dirs = arrayOf(intArrayOf(0, 1), intArrayOf(1, 0), intArrayOf(1, 1), intArrayOf(1, -1))
        for (r in 0..5) for (c in 0..6) {
            val p = cells[r * 7 + c]
            if (p == 0) continue
            for (d in dirs) {
                val er = r + d[0] * 3; val ec = c + d[1] * 3
                if (er !in 0..5 || ec !in 0..6) continue
                if ((1..3).all { cells[(r + d[0] * it) * 7 + c + d[1] * it] == p }) {
                    return IntArray(4) { (r + d[0] * it) * 7 + c + d[1] * it }
                }
            }
        }
        return null
    }

    fun full() = (0..6).none { canDrop(it) }

    private fun scoreWindow(a: Int, b: Int, c: Int, d: Int, me: Int): Int {
        val arr = intArrayOf(cells[a], cells[b], cells[c], cells[d])
        val mine = arr.count { it == me }; val opp = arr.count { it == 3 - me }; val empty = arr.count { it == 0 }
        return when {
            mine == 4 -> 1000
            mine == 3 && empty == 1 -> 6
            mine == 2 && empty == 2 -> 2
            opp == 3 && empty == 1 -> -5
            else -> 0
        }
    }

    fun evaluate(me: Int): Int {
        var s = 0
        for (r in 0..5) if (cells[r * 7 + 3] == me) s += 3
        for (r in 0..5) for (c in 0..6) {
            if (c <= 3) s += scoreWindow(r * 7 + c, r * 7 + c + 1, r * 7 + c + 2, r * 7 + c + 3, me)
            if (r <= 2) s += scoreWindow(r * 7 + c, (r + 1) * 7 + c, (r + 2) * 7 + c, (r + 3) * 7 + c, me)
            if (r <= 2 && c <= 3) s += scoreWindow(r * 7 + c, (r + 1) * 7 + c + 1, (r + 2) * 7 + c + 2, (r + 3) * 7 + c + 3, me)
            if (r <= 2 && c >= 3) s += scoreWindow(r * 7 + c, (r + 1) * 7 + c - 1, (r + 2) * 7 + c - 2, (r + 3) * 7 + c - 3, me)
        }
        return s
    }

    private val order = intArrayOf(3, 2, 4, 1, 5, 0, 6)

    fun negamax(depth: Int, alphaIn: Int, beta: Int, me: Int): Int {
        if (winLine() != null) return -100000 - depth   // previous mover won
        if (full()) return 0
        if (depth == 0) return evaluate(me) - evaluate(3 - me)
        var alpha = alphaIn
        for (c in order) if (canDrop(c)) {
            drop(c, me)
            val s = -negamax(depth - 1, -beta, -alpha, 3 - me)
            undrop(c)
            if (s >= beta) return s
            if (s > alpha) alpha = s
        }
        return alpha
    }

    fun bestMove(me: Int, depth: Int): Int {
        var best = -1; var bestS = Int.MIN_VALUE
        for (c in order) if (canDrop(c)) {
            drop(c, me)
            val s = -negamax(depth - 1, -1_000_000, 1_000_000, 3 - me)
            undrop(c)
            if (s > bestS) { bestS = s; best = c }
        }
        return best
    }
}
