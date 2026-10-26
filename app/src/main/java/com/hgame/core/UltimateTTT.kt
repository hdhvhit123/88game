package com.hgame.core

import kotlin.random.Random

/**
 * Ultimate tic-tac-toe. cells[big*9+small]; the small index chosen sends the opponent
 * to that big board. 0 empty, 1 X, 2 O.
 */
class UltimateTTT {
    val cells = IntArray(81)
    val boards = IntArray(9)      // winner of each small board: 0 open, 1, 2, 3 draw
    var next = -1                 // forced big board, -1 = any
    var turn = 1

    fun copy() = UltimateTTT().also { cells.copyInto(it.cells); boards.copyInto(it.boards); it.next = next; it.turn = turn }

    fun legal(): List<Int> {
        val out = ArrayList<Int>()
        for (big in 0 until 9) {
            if (boards[big] != 0) continue
            if (next >= 0 && big != next) continue
            for (s in 0 until 9) if (cells[big * 9 + s] == 0) out.add(big * 9 + s)
        }
        return out
    }

    fun play(i: Int) {
        cells[i] = turn
        val big = i / 9
        val sub = IntArray(9) { cells[big * 9 + it] }
        boards[big] = TicTacToe.winner(sub)
        val small = i % 9
        next = if (boards[small] == 0) small else -1
        turn = 3 - turn
    }

    fun winner(): Int {
        val big = IntArray(9) { if (boards[it] == 3) 0 else boards[it] }
        val w = TicTacToe.winner(big)
        if (w == 1 || w == 2) return w
        return if (boards.all { it != 0 }) 3 else 0
    }

    /** Monte-Carlo move choice: random playouts for each legal move. */
    fun bestMove(playouts: Int, rnd: Random = Random.Default): Int {
        val moves = legal()
        if (moves.size == 1) return moves[0]
        val me = turn
        // immediate win check
        for (m in moves) { val c = copy(); c.play(m); if (c.winner() == me) return m }
        var best = moves[0]; var bestScore = -1.0
        val per = maxOf(8, playouts / moves.size)
        for (m in moves) {
            var score = 0.0
            repeat(per) {
                val c = copy(); c.play(m)
                var w = c.winner()
                while (w == 0) { val l = c.legal(); c.play(l[rnd.nextInt(l.size)]); w = c.winner() }
                score += when (w) { me -> 1.0; 3 -> 0.5; else -> 0.0 }
            }
            if (score > bestScore) { bestScore = score; best = m }
        }
        return best
    }
}
