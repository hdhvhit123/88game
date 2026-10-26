package com.hgame.core

/** 3x3 tic-tac-toe helpers. Board cells: 0 empty, 1 X, 2 O. */
object TicTacToe {
    val LINES = arrayOf(
        intArrayOf(0, 1, 2), intArrayOf(3, 4, 5), intArrayOf(6, 7, 8),
        intArrayOf(0, 3, 6), intArrayOf(1, 4, 7), intArrayOf(2, 5, 8),
        intArrayOf(0, 4, 8), intArrayOf(2, 4, 6),
    )

    /** Returns 1 or 2 for a winner, 3 for a draw, 0 if the game continues. */
    fun winner(b: IntArray): Int {
        for (l in LINES) if (b[l[0]] != 0 && b[l[0]] == b[l[1]] && b[l[1]] == b[l[2]]) return b[l[0]]
        return if (b.all { it != 0 }) 3 else 0
    }

    fun winLine(b: IntArray): IntArray? = LINES.firstOrNull { b[it[0]] != 0 && b[it[0]] == b[it[1]] && b[it[1]] == b[it[2]] }

    /** Perfect play for [me]. */
    fun bestMove(b: IntArray, me: Int): Int {
        var best = -1; var bestScore = Int.MIN_VALUE
        for (i in 0 until 9) if (b[i] == 0) {
            b[i] = me
            val s = -negamax(b, 3 - me, 1)
            b[i] = 0
            if (s > bestScore) { bestScore = s; best = i }
        }
        return best
    }

    private fun negamax(b: IntArray, turn: Int, depth: Int): Int {
        val w = winner(b)
        if (w == 3) return 0
        if (w != 0) return if (w == turn) 10 - depth else depth - 10
        var best = Int.MIN_VALUE
        for (i in 0 until 9) if (b[i] == 0) {
            b[i] = turn
            best = maxOf(best, -negamax(b, 3 - turn, depth + 1))
            b[i] = 0
        }
        return best
    }
}
