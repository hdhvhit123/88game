package com.hgame.core

import kotlin.random.Random

/**
 * Futoshiki: fill an n x n Latin square respecting inequality signs between neighbours.
 * hSign[r*(n-1)+c] relates (r,c) and (r,c+1); vSign[r*n+c] relates (r,c) and (r+1,c).
 * Sign values: 0 none, 1 means first < second, 2 means first > second.
 */
class Futoshiki(val n: Int) {
    val solution = IntArray(n * n)
    val givens = IntArray(n * n)
    val hSign = IntArray(n * (n - 1))
    val vSign = IntArray((n - 1) * n)

    fun generate(rnd: Random, signDensity: Float, targetGivens: Int) {
        // random Latin square: shuffled cyclic square with shuffled rows/cols/symbols
        val rows = (0 until n).shuffled(rnd); val cols = (0 until n).shuffled(rnd); val sym = (1..n).shuffled(rnd)
        for (r in 0 until n) for (c in 0 until n) solution[r * n + c] = sym[(rows[r] + cols[c]) % n]
        // more variety: random intercalate-free swaps are skipped; cyclic squares are fine for play
        hSign.fill(0); vSign.fill(0)
        for (r in 0 until n) for (c in 0 until n - 1) if (rnd.nextFloat() < signDensity)
            hSign[r * (n - 1) + c] = if (solution[r * n + c] < solution[r * n + c + 1]) 1 else 2
        for (r in 0 until n - 1) for (c in 0 until n) if (rnd.nextFloat() < signDensity)
            vSign[r * n + c] = if (solution[r * n + c] < solution[(r + 1) * n + c]) 1 else 2
        solution.copyInto(givens)
        for (i in (0 until n * n).shuffled(rnd)) {
            if (givens.count { it != 0 } <= targetGivens) break
            val keep = givens[i]; givens[i] = 0
            if (countSolutions(givens.copyOf(), 2) != 1) givens[i] = keep
        }
        // if still ambiguous with few signs, uniqueness is already guaranteed by the loop above
    }

    fun ok(g: IntArray, i: Int, v: Int): Boolean {
        val r = i / n; val c = i % n
        for (k in 0 until n) { if (k != c && g[r * n + k] == v) return false; if (k != r && g[k * n + c] == v) return false }
        fun rel(a: Int, b: Int, s: Int): Boolean = a == 0 || b == 0 || (if (s == 1) a < b else a > b)
        if (c > 0 && hSign[r * (n - 1) + c - 1] != 0 && !rel(g[r * n + c - 1], v, hSign[r * (n - 1) + c - 1])) return false
        if (c < n - 1 && hSign[r * (n - 1) + c] != 0 && !rel(v, g[r * n + c + 1], hSign[r * (n - 1) + c])) return false
        if (r > 0 && vSign[(r - 1) * n + c] != 0 && !rel(g[(r - 1) * n + c], v, vSign[(r - 1) * n + c])) return false
        if (r < n - 1 && vSign[r * n + c] != 0 && !rel(v, g[(r + 1) * n + c], vSign[r * n + c])) return false
        return true
    }

    fun countSolutions(g: IntArray, limit: Int): Int {
        val i = g.indexOf(0)
        if (i < 0) return 1
        var total = 0
        for (v in 1..n) if (ok(g, i, v)) {
            g[i] = v
            total += countSolutions(g, limit - total)
            g[i] = 0
            if (total >= limit) break
        }
        return total
    }
}
