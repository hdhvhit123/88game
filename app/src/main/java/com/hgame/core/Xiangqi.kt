package com.hgame.core

import kotlin.math.abs

/**
 * Xiangqi (Chinese chess). Board 9 files x 10 ranks, index = rank * 9 + file.
 * Rank 0 is red's back rank. Pieces: 1 general, 2 advisor, 3 elephant, 4 horse,
 * 5 chariot, 6 cannon, 7 soldier; positive = red, negative = black.
 */
class Xiangqi {
    val b = IntArray(90)
    var red = true

    companion object {
        const val K = 1; const val A = 2; const val E = 3; const val H = 4; const val R = 5; const val C = 6; const val S = 7
        private val VALUE = intArrayOf(0, 10000, 120, 120, 270, 600, 285, 30)
        fun move(f: Int, t: Int) = f or (t shl 7)
        fun from(m: Int) = m and 127
        fun dest(m: Int) = (m shr 7) and 127
    }

    init { reset() }

    fun reset() {
        b.fill(0)
        val back = intArrayOf(R, H, E, A, K, A, E, H, R)
        for (f in 0 until 9) { b[f] = back[f]; b[81 + f] = -back[f] }
        b[2 * 9 + 1] = C; b[2 * 9 + 7] = C; b[7 * 9 + 1] = -C; b[7 * 9 + 7] = -C
        for (f in 0 until 9 step 2) { b[3 * 9 + f] = S; b[6 * 9 + f] = -S }
        red = true
    }

    fun copy(): Xiangqi { val x = Xiangqi(); b.copyInto(x.b); x.red = red; return x }

    private fun inPalace(r: Int, f: Int, isRed: Boolean) = f in 3..5 && (if (isRed) r in 0..2 else r in 7..9)
    private fun mine(p: Int, isRed: Boolean) = if (isRed) p > 0 else p < 0

    fun pseudoMoves(out: MutableList<Int>, isRed: Boolean = red) {
        for (sq in 0 until 90) {
            val p = b[sq]
            if (p == 0 || !mine(p, isRed)) continue
            val r = sq / 9; val f = sq % 9
            fun tryAdd(tr: Int, tf: Int) {
                if (tr !in 0..9 || tf !in 0..8) return
                val t = tr * 9 + tf
                if (b[t] == 0 || !mine(b[t], isRed)) out.add(move(sq, t))
            }
            when (abs(p)) {
                K -> for ((dr, df) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) if (inPalace(r + dr, f + df, isRed)) tryAdd(r + dr, f + df)
                A -> for ((dr, df) in listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1)) if (inPalace(r + dr, f + df, isRed)) tryAdd(r + dr, f + df)
                E -> for ((dr, df) in listOf(2 to 2, 2 to -2, -2 to 2, -2 to -2)) {
                    val tr = r + dr; val tf = f + df
                    if (tr !in 0..9 || tf !in 0..8) continue
                    if (isRed && tr > 4 || !isRed && tr < 5) continue
                    if (b[(r + dr / 2) * 9 + f + df / 2] != 0) continue
                    tryAdd(tr, tf)
                }
                H -> for ((dr, df) in listOf(2 to 1, 2 to -1, -2 to 1, -2 to -1, 1 to 2, -1 to 2, 1 to -2, -1 to -2)) {
                    val tr = r + dr; val tf = f + df
                    if (tr !in 0..9 || tf !in 0..8) continue
                    val lr = r + if (abs(dr) == 2) dr / 2 else 0
                    val lf = f + if (abs(df) == 2) df / 2 else 0
                    if (b[lr * 9 + lf] != 0) continue
                    tryAdd(tr, tf)
                }
                R, C -> for ((dr, df) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                    var tr = r + dr; var tf = f + df
                    var jumped = false
                    while (tr in 0..9 && tf in 0..8) {
                        val t = tr * 9 + tf
                        if (abs(p) == R) {
                            if (b[t] == 0) out.add(move(sq, t))
                            else { if (!mine(b[t], isRed)) out.add(move(sq, t)); break }
                        } else {
                            if (!jumped) {
                                if (b[t] == 0) out.add(move(sq, t)) else jumped = true
                            } else if (b[t] != 0) {
                                if (!mine(b[t], isRed)) out.add(move(sq, t)); break
                            }
                        }
                        tr += dr; tf += df
                    }
                }
                S -> {
                    val fwd = if (isRed) 1 else -1
                    tryAdd(r + fwd, f)
                    val crossed = if (isRed) r >= 5 else r <= 4
                    if (crossed) { tryAdd(r, f - 1); tryAdd(r, f + 1) }
                }
            }
        }
    }

    fun generalsFacing(): Boolean {
        val rk = b.indexOf(K); val bk = b.indexOf(-K)
        if (rk < 0 || bk < 0 || rk % 9 != bk % 9) return false
        var s = rk + 9
        while (s < bk) { if (b[s] != 0) return false; s += 9 }
        return true
    }

    fun inCheck(isRed: Boolean = red): Boolean {
        val k = b.indexOf(if (isRed) K else -K)
        if (k < 0) return true
        if (generalsFacing()) return true
        val moves = ArrayList<Int>(64)
        pseudoMoves(moves, !isRed)
        return moves.any { dest(it) == k }
    }

    fun make(m: Int): Int {
        val f = from(m); val t = dest(m)
        val cap = b[t]
        b[t] = b[f]; b[f] = 0
        red = !red
        return cap
    }

    fun legalMoves(): List<Int> {
        val ms = ArrayList<Int>(64)
        pseudoMoves(ms)
        return ms.filter { m -> val c = copy(); c.make(m); !c.inCheck(!c.red) }
    }

    /** 0 ongoing, 1 red wins, 2 black wins. In xiangqi having no legal move loses. */
    fun result(): Int {
        if (legalMoves().isEmpty()) return if (red) 2 else 1
        return 0
    }

    fun evaluate(): Int {
        var s = 0
        for (sq in 0 until 90) {
            val p = b[sq]
            if (p == 0) continue
            val r = sq / 9; val f = sq % 9
            var v = VALUE[abs(p)]
            val adv = if (p > 0) r else 9 - r     // progress towards the enemy
            when (abs(p)) {
                S -> v += if (adv >= 5) 40 + (4 - abs(f - 4)) * 8 + (adv - 5) * 10 else 0
                H -> v += 6 * (4 - abs(f - 4)) + adv * 4
                C -> v += if (f == 4) 15 else 0
                R -> v += adv * 3
            }
            s += if (p > 0) v else -v
        }
        return if (red) s else -s
    }

    fun bestMove(depth: Int, millis: Long, randomness: Int = 0): Int {
        val moves = legalMoves().toMutableList()
        if (moves.isEmpty()) return -1
        val deadline = System.currentTimeMillis() + millis
        var best = moves[0]
        for (d in 1..depth) {
            moves.remove(best); moves.add(0, best)
            var bestScore = -1_000_000; var bestThis = best; var aborted = false
            for (m in moves) {
                val c = copy(); c.make(m)
                val sc = -c.negamax(d - 1, -1_000_000, -bestScore, deadline) + if (randomness > 0) (0..randomness).random() else 0
                if (System.currentTimeMillis() > deadline && d > 1) { aborted = true; break }
                if (sc > bestScore) { bestScore = sc; bestThis = m }
            }
            if (aborted) break
            best = bestThis
            if (bestScore > 50000) break
        }
        return best
    }

    private var nodes = 0
    private fun negamax(depth: Int, alphaIn: Int, beta: Int, deadline: Long): Int {
        if (depth <= 0) return evaluate()
        var alpha = alphaIn
        val moves = ArrayList<Int>(64)
        pseudoMoves(moves)
        moves.sortByDescending { VALUE[abs(b[dest(it)])] }
        var any = false
        for (m in moves) {
            val c = copy(); c.make(m)
            if (c.inCheck(!c.red)) continue
            any = true
            val s = -c.negamax(depth - 1, -beta, -alpha, deadline)
            if (s >= beta) return s
            if (s > alpha) alpha = s
            if ((++nodes and 255) == 0 && System.currentTimeMillis() > deadline) return alpha
        }
        return if (any) alpha else -100000 - depth
    }
}
