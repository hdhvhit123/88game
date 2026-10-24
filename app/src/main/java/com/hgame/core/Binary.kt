package com.hgame.core

import kotlin.random.Random

/** Binary puzzle (Takuzu). Cells: -1 empty, 0, 1. Rules: no three equal in a row, equal counts, unique lines. */
class Binary(val n: Int) {
    val solution = IntArray(n * n)
    val givens = IntArray(n * n) { -1 }

    fun valid(g: IntArray, i: Int): Boolean {
        val r = i / n; val c = i % n
        val v = g[i]
        if (v < 0) return true
        // three in a row
        fun at(rr: Int, cc: Int) = if (rr in 0 until n && cc in 0 until n) g[rr * n + cc] else -2
        for (k in -2..0) {
            if ((0..2).all { at(r, c + k + it) == v }) return false
            if ((0..2).all { at(r + k + it, c) == v }) return false
        }
        // counts
        var rowCount = 0; var colCount = 0
        for (k in 0 until n) { if (g[r * n + k] == v) rowCount++; if (g[k * n + c] == v) colCount++ }
        if (rowCount > n / 2 || colCount > n / 2) return false
        // unique full rows / cols
        val rowFull = (0 until n).all { g[r * n + it] >= 0 }
        if (rowFull) for (o in 0 until n) if (o != r && (0 until n).all { g[o * n + it] >= 0 && g[o * n + it] == g[r * n + it] }) return false
        val colFull = (0 until n).all { g[it * n + c] >= 0 }
        if (colFull) for (o in 0 until n) if (o != c && (0 until n).all { g[it * n + o] >= 0 && g[it * n + o] == g[it * n + c] }) return false
        return true
    }

    private fun fill(g: IntArray, rnd: Random): Boolean {
        val i = g.indexOf(-1)
        if (i < 0) return true
        for (v in listOf(0, 1).shuffled(rnd)) {
            g[i] = v
            if (valid(g, i) && fill(g, rnd)) return true
        }
        g[i] = -1
        return false
    }

    fun count(g: IntArray, limit: Int): Int {
        val i = g.indexOf(-1)
        if (i < 0) return 1
        var t = 0
        for (v in 0..1) {
            g[i] = v
            if (valid(g, i)) t += count(g, limit - t)
            if (t >= limit) break
        }
        g[i] = -1
        return t
    }

    fun generate(rnd: Random) {
        val g = IntArray(n * n) { -1 }
        fill(g, rnd)
        g.copyInto(solution); g.copyInto(givens)
        for (i in (0 until n * n).shuffled(rnd)) {
            val keep = givens[i]; givens[i] = -1
            if (count(givens.copyOf(), 2) != 1) givens[i] = keep
        }
    }
}
