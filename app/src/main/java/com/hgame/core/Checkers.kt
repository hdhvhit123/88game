package com.hgame.core

import kotlin.math.abs

/**
 * English draughts on 8x8 (index = row*8+col, row 0 at the top).
 * +1 man / +2 king for the bottom player (moves up), -1 / -2 for the top player.
 * Captures are mandatory and multi-jumps must be completed.
 */
class Checkers {
    val b = IntArray(64)

    /** A move is a path of squares; [captured] lists the jumped squares. */
    class Move(val path: List<Int>, val captured: List<Int>) {
        val from get() = path.first()
        val to get() = path.last()
    }

    init { reset() }

    fun reset() {
        b.fill(0)
        for (sq in 0 until 64) {
            val r = sq / 8; val c = sq % 8
            if ((r + c) % 2 == 1) { if (r < 3) b[sq] = -1 else if (r > 4) b[sq] = 1 }
        }
    }

    fun copy() = Checkers().also { b.copyInto(it.b) }

    private fun dirs(p: Int): List<Pair<Int, Int>> = when (p) {
        1 -> listOf(-1 to -1, -1 to 1)
        -1 -> listOf(1 to -1, 1 to 1)
        else -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
    }

    fun moves(side: Int): List<Move> {
        val caps = ArrayList<Move>()
        for (sq in 0 until 64) if (b[sq] * side > 0) jumps(sq, b[sq], listOf(sq), emptyList(), caps)
        if (caps.isNotEmpty()) return caps
        val out = ArrayList<Move>()
        for (sq in 0 until 64) {
            if (b[sq] * side <= 0) continue
            val r = sq / 8; val c = sq % 8
            for ((dr, dc) in dirs(b[sq])) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0..7 && nc in 0..7 && b[nr * 8 + nc] == 0) out.add(Move(listOf(sq, nr * 8 + nc), emptyList()))
            }
        }
        return out
    }

    private fun jumps(sq: Int, piece: Int, path: List<Int>, caps: List<Int>, out: MutableList<Move>) {
        val r = sq / 8; val c = sq % 8
        var extended = false
        for ((dr, dc) in dirs(piece)) {
            val mr = r + dr; val mc = c + dc; val tr = r + 2 * dr; val tc = c + 2 * dc
            if (tr !in 0..7 || tc !in 0..7) continue
            val mid = mr * 8 + mc; val to = tr * 8 + tc
            if (mid in caps) continue
            val origin = path.first()
            if (b[mid] * piece < 0 && (b[to] == 0 || to == origin)) {
                extended = true
                // a man that reaches the far row stops (becomes king)
                val crowned = (piece == 1 && tr == 0) || (piece == -1 && tr == 7)
                if (crowned) out.add(Move(path + to, caps + mid))
                else jumps(to, piece, path + to, caps + mid, out)
            }
        }
        if (!extended && caps.isNotEmpty()) out.add(Move(path, caps))
    }

    fun play(m: Move) {
        val p = b[m.from]
        b[m.from] = 0
        for (cpt in m.captured) b[cpt] = 0
        var np = p
        if (p == 1 && m.to / 8 == 0) np = 2
        if (p == -1 && m.to / 8 == 7) np = -2
        b[m.to] = np
    }

    fun evaluate(side: Int): Int {
        var s = 0
        for (sq in 0 until 64) {
            val p = b[sq]
            if (p == 0) continue
            val r = sq / 8
            var v = if (abs(p) == 2) 175 else 100
            if (abs(p) == 1) v += if (p > 0) (7 - r) * 4 else r * 4
            val c = sq % 8
            if (c in 2..5 && r in 2..5) v += 6
            s += if (p > 0) v else -v
        }
        return s * side
    }

    fun negamax(side: Int, depth: Int, alphaIn: Int, beta: Int): Int {
        val ms = moves(side)
        if (ms.isEmpty()) return -100000 - depth
        if (depth == 0) return evaluate(side)
        var alpha = alphaIn
        for (m in ms) {
            val c = copy(); c.play(m)
            val s = -c.negamax(-side, depth - 1, -beta, -alpha)
            if (s >= beta) return s
            if (s > alpha) alpha = s
        }
        return alpha
    }

    fun bestMove(side: Int, depth: Int): Move? {
        val ms = moves(side).shuffled()
        var best: Move? = null; var bestS = Int.MIN_VALUE
        for (m in ms) {
            val c = copy(); c.play(m)
            val s = -c.negamax(-side, depth - 1, -1_000_000, 1_000_000)
            if (s > bestS) { bestS = s; best = m }
        }
        return best
    }
}
