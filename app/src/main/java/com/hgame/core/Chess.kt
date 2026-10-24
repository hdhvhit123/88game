package com.hgame.core

/**
 * Chess rules and a small alpha-beta engine.
 * Board: 64 squares, index = rank * 8 + file, rank 0 = white's back rank.
 * Pieces: +1..+6 white (P N B R Q K), negative for black, 0 empty.
 */
class Chess {
    val b = IntArray(64)
    var white = true            // side to move
    var castle = 15             // bits: 1 WK, 2 WQ, 4 BK, 8 BQ
    var ep = -1                 // en-passant target square
    var halfmove = 0
    val history = ArrayList<Long>()  // position hashes for repetition

    companion object {
        const val P = 1; const val N = 2; const val B = 3; const val R = 4; const val Q = 5; const val K = 6
        private val KN = intArrayOf(-17, -15, -10, -6, 6, 10, 15, 17)
        private val KD = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)
        private val VALUE = intArrayOf(0, 100, 320, 330, 500, 900, 20000)

        fun from(m: Int) = m and 63
        fun dest(m: Int) = (m shr 6) and 63
        fun promo(m: Int) = (m shr 12) and 7
        fun move(f: Int, t: Int, p: Int = 0) = f or (t shl 6) or (p shl 12)

        // piece-square tables from white's point of view (rank 0 = white back rank)
        private val PST_P = intArrayOf(
            0, 0, 0, 0, 0, 0, 0, 0, 5, 10, 10, -20, -20, 10, 10, 5, 5, -5, -10, 0, 0, -10, -5, 5,
            0, 0, 0, 20, 20, 0, 0, 0, 5, 5, 10, 25, 25, 10, 5, 5, 10, 10, 20, 30, 30, 20, 10, 10,
            50, 50, 50, 50, 50, 50, 50, 50, 0, 0, 0, 0, 0, 0, 0, 0)
        private val PST_N = intArrayOf(
            -50, -40, -30, -30, -30, -30, -40, -50, -40, -20, 0, 5, 5, 0, -20, -40, -30, 5, 10, 15, 15, 10, 5, -30,
            -30, 0, 15, 20, 20, 15, 0, -30, -30, 5, 15, 20, 20, 15, 5, -30, -30, 0, 10, 15, 15, 10, 0, -30,
            -40, -20, 0, 0, 0, 0, -20, -40, -50, -40, -30, -30, -30, -30, -40, -50)
        private val PST_B = intArrayOf(
            -20, -10, -10, -10, -10, -10, -10, -20, -10, 5, 0, 0, 0, 0, 5, -10, -10, 10, 10, 10, 10, 10, 10, -10,
            -10, 0, 10, 10, 10, 10, 0, -10, -10, 5, 5, 10, 10, 5, 5, -10, -10, 0, 5, 10, 10, 5, 0, -10,
            -10, 0, 0, 0, 0, 0, 0, -10, -20, -10, -10, -10, -10, -10, -10, -20)
        private val PST_K = intArrayOf(
            20, 30, 10, 0, 0, 10, 30, 20, 20, 20, 0, 0, 0, 0, 20, 20, -10, -20, -20, -20, -20, -20, -20, -10,
            -20, -30, -30, -40, -40, -30, -30, -20, -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30,
            -30, -40, -40, -50, -50, -40, -40, -30, -30, -40, -40, -50, -50, -40, -40, -30)
    }

    init { reset() }

    fun reset() {
        val back = intArrayOf(R, N, B, Q, K, B, N, R)
        b.fill(0)
        for (f in 0 until 8) {
            b[f] = back[f]; b[8 + f] = P; b[48 + f] = -P; b[56 + f] = -back[f]
        }
        white = true; castle = 15; ep = -1; halfmove = 0
        history.clear(); history.add(hash())
    }

    fun loadFen(fen: String) {
        val parts = fen.trim().split(" ")
        b.fill(0)
        var r = 7; var f = 0
        for (ch in parts[0]) {
            when {
                ch == '/' -> { r--; f = 0 }
                ch.isDigit() -> f += ch - '0'
                else -> {
                    val t = "pnbrqk".indexOf(ch.lowercaseChar()) + 1
                    b[r * 8 + f] = if (ch.isUpperCase()) t else -t
                    f++
                }
            }
        }
        white = parts.getOrElse(1) { "w" } == "w"
        val c = parts.getOrElse(2) { "-" }
        castle = (if ('K' in c) 1 else 0) or (if ('Q' in c) 2 else 0) or (if ('k' in c) 4 else 0) or (if ('q' in c) 8 else 0)
        val e = parts.getOrElse(3) { "-" }
        ep = if (e == "-") -1 else (e[1] - '1') * 8 + (e[0] - 'a')
        halfmove = parts.getOrElse(4) { "0" }.toIntOrNull() ?: 0
        history.clear(); history.add(hash())
    }

    fun copy(): Chess {
        val c = Chess()
        b.copyInto(c.b); c.white = white; c.castle = castle; c.ep = ep; c.halfmove = halfmove
        c.history.clear(); c.history.addAll(history)
        return c
    }

    fun hash(): Long {
        var h = 1469598103934665603L
        for (i in 0 until 64) h = (h xor (b[i] + 7).toLong()) * 1099511628211L
        return h xor (if (white) 1L else 2L) xor (castle.toLong() shl 8) xor ((ep + 1).toLong() shl 16)
    }

    private fun own(p: Int) = if (white) p > 0 else p < 0
    private fun enemy(p: Int, w: Boolean) = if (w) p < 0 else p > 0

    fun attacked(sq: Int, byWhite: Boolean): Boolean {
        val r = sq / 8; val f = sq % 8
        val s = if (byWhite) 1 else -1
        // pawns
        val pr = r - s
        if (pr in 0..7) {
            if (f > 0 && b[pr * 8 + f - 1] == s * P) return true
            if (f < 7 && b[pr * 8 + f + 1] == s * P) return true
        }
        for (d in KN) {
            val t = sq + d
            if (t !in 0..63 || kotlin.math.abs(t % 8 - f) > 2) continue
            if (b[t] == s * N) return true
        }
        for (d in KD) {
            val t = sq + d
            if (t !in 0..63 || kotlin.math.abs(t % 8 - f) > 1) continue
            if (b[t] == s * K) return true
        }
        val dirs = intArrayOf(1, -1, 8, -8, 9, -9, 7, -7)
        for ((k, d) in dirs.withIndex()) {
            var t = sq; var pf = f
            while (true) {
                t += d
                if (t !in 0..63) break
                val tf = t % 8
                if (kotlin.math.abs(tf - pf) > 1) break
                pf = tf
                val p = b[t]
                if (p != 0) {
                    val a = p * s
                    if (a == Q || (k < 4 && a == R) || (k >= 4 && a == B)) return true
                    break
                }
            }
        }
        return false
    }

    fun kingSquare(w: Boolean) = b.indexOf(if (w) K else -K)
    fun inCheck(w: Boolean = white) = kingSquare(w).let { it >= 0 && attacked(it, !w) }

    fun pseudoMoves(out: MutableList<Int>, capturesOnly: Boolean = false) {
        val s = if (white) 1 else -1
        for (sq in 0 until 64) {
            val p = b[sq]
            if (p == 0 || !own(p)) continue
            val r = sq / 8; val f = sq % 8
            when (p * s) {
                P -> {
                    val fwd = sq + 8 * s
                    val lastRank = if (white) 7 else 0
                    fun add(t: Int) {
                        if (t / 8 == lastRank) { for (pp in intArrayOf(Q, N, R, B)) out.add(move(sq, t, pp)) } else out.add(move(sq, t))
                    }
                    if (fwd in 0..63 && b[fwd] == 0 && (!capturesOnly || fwd / 8 == lastRank)) {
                        add(fwd)
                        val start = if (white) 1 else 6
                        val two = sq + 16 * s
                        if (r == start && b[two] == 0 && !capturesOnly) out.add(move(sq, two))
                    }
                    for (df in intArrayOf(-1, 1)) {
                        if (f + df !in 0..7) continue
                        val t = fwd + df
                        if (t !in 0..63) continue
                        if ((b[t] != 0 && enemy(b[t], white)) || t == ep) add(t)
                    }
                }
                N -> for (d in KN) {
                    val t = sq + d
                    if (t !in 0..63 || kotlin.math.abs(t % 8 - f) > 2) continue
                    if (b[t] == 0 && !capturesOnly || enemy(b[t], white)) out.add(move(sq, t))
                }
                K -> {
                    for (d in KD) {
                        val t = sq + d
                        if (t !in 0..63 || kotlin.math.abs(t % 8 - f) > 1) continue
                        if (b[t] == 0 && !capturesOnly || enemy(b[t], white)) out.add(move(sq, t))
                    }
                    if (!capturesOnly) {
                        val home = if (white) 4 else 60
                        if (sq == home && !attacked(sq, !white)) {
                            val kb = if (white) 1 else 4; val qb = if (white) 2 else 8
                            if (castle and kb != 0 && b[sq + 1] == 0 && b[sq + 2] == 0 && b[sq + 3] == s * R &&
                                !attacked(sq + 1, !white) && !attacked(sq + 2, !white)) out.add(move(sq, sq + 2))
                            if (castle and qb != 0 && b[sq - 1] == 0 && b[sq - 2] == 0 && b[sq - 3] == 0 && b[sq - 4] == s * R &&
                                !attacked(sq - 1, !white) && !attacked(sq - 2, !white)) out.add(move(sq, sq - 2))
                        }
                    }
                }
                else -> {
                    val dirs = when (p * s) { B -> intArrayOf(9, -9, 7, -7); R -> intArrayOf(1, -1, 8, -8); else -> intArrayOf(1, -1, 8, -8, 9, -9, 7, -7) }
                    for (d in dirs) {
                        var t = sq; var pf = f
                        while (true) {
                            t += d
                            if (t !in 0..63) break
                            val tf = t % 8
                            if (kotlin.math.abs(tf - pf) > 1) break
                            pf = tf
                            if (b[t] == 0) { if (!capturesOnly) out.add(move(sq, t)) }
                            else { if (enemy(b[t], white)) out.add(move(sq, t)); break }
                        }
                    }
                }
            }
        }
    }

    /** Plays [m] (assumed pseudo-legal). Returns the captured piece. */
    fun make(m: Int): Int {
        val f = from(m); val t = dest(m); val p = b[f]
        var captured = b[t]
        val s = if (p > 0) 1 else -1
        b[t] = p; b[f] = 0
        if (p * s == P) {
            if (t == ep) { val cap = t - 8 * s; captured = b[cap]; b[cap] = 0 }
            if (promo(m) != 0) b[t] = s * promo(m)
        }
        if (p * s == K && kotlin.math.abs(t - f) == 2) {
            if (t > f) { b[t - 1] = b[t + 1]; b[t + 1] = 0 } else { b[t + 1] = b[t - 2]; b[t - 2] = 0 }
        }
        ep = if (p * s == P && kotlin.math.abs(t - f) == 16) (f + t) / 2 else -1
        // castling rights
        for (sq in intArrayOf(f, t)) when (sq) {
            4 -> castle = castle and 3.inv(); 60 -> castle = castle and 12.inv()
            0 -> castle = castle and 2.inv(); 7 -> castle = castle and 1.inv()
            56 -> castle = castle and 8.inv(); 63 -> castle = castle and 4.inv()
        }
        halfmove = if (captured != 0 || p * s == P) 0 else halfmove + 1
        white = !white
        return captured
    }

    fun legalMoves(): List<Int> {
        val pseudo = ArrayList<Int>(48)
        pseudoMoves(pseudo)
        return pseudo.filter { m ->
            val c = copy(); c.make(m); !c.inCheck(!c.white)
        }
    }

    fun play(m: Int): Int { val cap = make(m); history.add(hash()); return cap }

    /** 0 ongoing, 1 white wins, 2 black wins, 3 draw. */
    fun result(): Int {
        if (legalMoves().isEmpty()) return if (inCheck()) (if (white) 2 else 1) else 3
        if (halfmove >= 100) return 3
        val h = history.last()
        if (history.count { it == h } >= 3) return 3
        val pieces = b.filter { it != 0 && kotlin.math.abs(it) != K }
        if (pieces.isEmpty() || (pieces.size == 1 && kotlin.math.abs(pieces[0]) in intArrayOf(N, B))) return 3
        return 0
    }

    fun evaluate(): Int {   // from side-to-move's perspective
        var score = 0
        for (sq in 0 until 64) {
            val p = b[sq]
            if (p == 0) continue
            val a = kotlin.math.abs(p)
            val idx = if (p > 0) sq else (7 - sq / 8) * 8 + sq % 8
            val pst = when (a) { P -> PST_P[idx]; N -> PST_N[idx]; B -> PST_B[idx]; K -> PST_K[idx]; else -> 0 }
            val v = VALUE[a] + pst
            score += if (p > 0) v else -v
        }
        return if (white) score else -score
    }

    // ---------------- search ----------------
    private var nodes = 0
    private var deadline = 0L

    /** Best move for the side to move, iterative deepening up to [maxDepth] within [millis]. */
    fun bestMove(maxDepth: Int, millis: Long, randomness: Int = 0): Int {
        val moves = legalMoves().toMutableList()
        if (moves.isEmpty()) return 0
        deadline = System.currentTimeMillis() + millis
        var best = moves[0]
        for (depth in 1..maxDepth) {
            var bestScore = -1_000_000
            var bestThis = best
            // search previous best first
            moves.remove(best); moves.add(0, best)
            var aborted = false
            for (m in moves) {
                val c = copy(); c.make(m)
                val score = -c.negamax(depth - 1, -1_000_000, -bestScore, deadline) + if (randomness > 0) (0..randomness).random() else 0
                if (System.currentTimeMillis() > deadline && depth > 1) { aborted = true; break }
                if (score > bestScore) { bestScore = score; bestThis = m }
            }
            if (!aborted) best = bestThis else break
            if (bestScore > 90000) break
        }
        return best
    }

    private fun negamax(depth: Int, alphaIn: Int, beta: Int, deadline: Long): Int {
        if (depth <= 0) return quiesce(alphaIn, beta, 4)
        var alpha = alphaIn
        val moves = ArrayList<Int>(48)
        pseudoMoves(moves)
        orderMoves(moves)
        var any = false
        for (m in moves) {
            val c = copy(); c.make(m)
            if (c.inCheck(!c.white)) continue
            any = true
            val s = -c.negamax(depth - 1, -beta, -alpha, deadline)
            if (s >= beta) return s
            if (s > alpha) alpha = s
            if ((++nodes and 1023) == 0 && System.currentTimeMillis() > deadline) return alpha
        }
        if (!any) return if (inCheck()) -100000 - depth else 0
        return alpha
    }

    private fun quiesce(alphaIn: Int, beta: Int, depth: Int): Int {
        val stand = evaluate()
        if (stand >= beta || depth == 0) return stand
        var alpha = maxOf(alphaIn, stand)
        val moves = ArrayList<Int>(16)
        pseudoMoves(moves, capturesOnly = true)
        orderMoves(moves)
        for (m in moves) {
            val c = copy(); c.make(m)
            if (c.inCheck(!c.white)) continue
            val s = -c.quiesce(-beta, -alpha, depth - 1)
            if (s >= beta) return s
            if (s > alpha) alpha = s
        }
        return alpha
    }

    private fun orderMoves(moves: MutableList<Int>) {
        moves.sortByDescending { m ->
            val victim = kotlin.math.abs(b[dest(m)])
            if (victim != 0) VALUE[victim] * 10 - VALUE[kotlin.math.abs(b[from(m)])] / 10 else if (promo(m) != 0) 5000 else 0
        }
    }

    fun perft(depth: Int): Long {
        if (depth == 0) return 1
        var n = 0L
        for (m in legalMoves()) { val c = copy(); c.make(m); n += c.perft(depth - 1) }
        return n
    }
}
