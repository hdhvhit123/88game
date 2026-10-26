package com.hgame.core

/**
 * Kalah (6 pits, 4 seeds). pits[0..5] = player 0 pits (left to right), pits[6] = player 0 store,
 * pits[7..12] = player 1 pits, pits[13] = player 1 store. Sowing goes counter-clockwise.
 */
class Mancala(seeds: Int = 4) {
    val pits = IntArray(14) { if (it == 6 || it == 13) 0 else seeds }
    var turn = 0

    fun copy() = Mancala().also { pits.copyInto(it.pits); it.turn = turn }
    fun store(p: Int) = if (p == 0) 6 else 13
    fun pitRange(p: Int) = if (p == 0) 0..5 else 7..12
    fun legal() = pitRange(turn).filter { pits[it] > 0 }

    /** Sows from [pit]; returns the indices visited (for animation). */
    fun play(pit: Int): List<Int> {
        val path = ArrayList<Int>()
        var seeds = pits[pit]
        pits[pit] = 0
        var i = pit
        val oppStore = store(1 - turn)
        while (seeds > 0) {
            i = (i + 1) % 14
            if (i == oppStore) continue
            pits[i]++; seeds--; path.add(i)
        }
        // capture
        if (i in pitRange(turn) && pits[i] == 1) {
            val opposite = 12 - i
            if (pits[opposite] > 0) {
                pits[store(turn)] += pits[opposite] + 1
                pits[opposite] = 0; pits[i] = 0
            }
        }
        val extra = i == store(turn)
        if (gameEnded()) collect()
        if (!extra) turn = 1 - turn
        return path
    }

    fun gameEnded() = pitRange(0).all { pits[it] == 0 } || pitRange(1).all { pits[it] == 0 }

    private fun collect() {
        for (p in 0..1) for (k in pitRange(p)) { pits[store(p)] += pits[k]; pits[k] = 0 }
    }

    fun evaluate(p: Int) = pits[store(p)] - pits[store(1 - p)]

    fun negamax(depth: Int, alphaIn: Int, beta: Int): Int {
        if (gameEnded() || depth == 0) return evaluate(turn)
        var alpha = alphaIn
        for (m in legal()) {
            val c = copy(); c.play(m)
            val s = if (c.turn == turn) c.negamax(depth - 1, alpha, beta) else -c.negamax(depth - 1, -beta, -alpha)
            if (s >= beta) return s
            if (s > alpha) alpha = s
        }
        return alpha
    }

    fun bestMove(depth: Int): Int {
        var best = -1; var bestS = Int.MIN_VALUE
        for (m in legal().shuffled()) {
            val c = copy(); c.play(m)
            val s = if (c.turn == turn) c.negamax(depth - 1, -10000, 10000) else -c.negamax(depth - 1, -10000, 10000)
            if (s > bestS) { bestS = s; best = m }
        }
        return best
    }
}
