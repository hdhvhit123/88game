package com.hgame.core

import kotlin.random.Random

/**
 * Onet / tile connect. Inner board cols x rows with a one-cell empty border.
 * Two equal tiles match when a path with at most two turns joins them through empty cells.
 */
class Onet(val cols: Int, val rows: Int, kinds: Int, rnd: Random = Random.Default) {
    val W = cols + 2; val H = rows + 2
    val cells = IntArray(W * H) { -1 }

    init {
        val total = cols * rows
        val list = ArrayList<Int>(total)
        var k = 0
        while (list.size < total) { list.add(k % kinds); list.add(k % kinds); k++ }
        list.shuffle(rnd)
        var i = 0
        for (r in 1..rows) for (c in 1..cols) cells[r * W + c] = list[i++]
    }

    fun remaining() = cells.count { it >= 0 }

    /** Path corners from a to b (inclusive) with <= 2 turns, or null. */
    fun path(a: Int, b: Int): List<Int>? {
        if (a == b || cells[a] < 0 || cells[a] != cells[b]) return null
        val dx = intArrayOf(0, 1, 0, -1); val dy = intArrayOf(-1, 0, 1, 0)
        // BFS over (cell, dir) with turn counts
        val best = Array(W * H) { IntArray(4) { 9 } }
        val prev = Array(W * H) { IntArray(4) { -1 } }
        val q = ArrayDeque<IntArray>()
        for (d in 0..3) { best[a][d] = 0; q.add(intArrayOf(a, d)) }
        while (q.isNotEmpty()) {
            val (cell, d) = q.removeFirst()
            val turns = best[cell][d]
            var x = cell % W + dx[d]; var y = cell / W + dy[d]
            var last = cell
            while (x in 0 until W && y in 0 until H) {
                val nc = y * W + x
                if (nc != b && cells[nc] >= 0) break
                if (best[nc][d] > turns) {
                    best[nc][d] = turns; prev[nc][d] = last * 4 + d
                    if (nc == b) return rebuild(a, b, d, prev)
                    for (nd in 0..3) if (nd != d && turns + 1 <= 2 && best[nc][nd] > turns + 1) {
                        best[nc][nd] = turns + 1; prev[nc][nd] = nc * 4 + d; q.add(intArrayOf(nc, nd))
                    }
                }
                last = nc
                x += dx[d]; y += dy[d]
            }
        }
        return null
    }

    private fun rebuild(a: Int, b: Int, dEnd: Int, prev: Array<IntArray>): List<Int> {
        val walk = arrayListOf(b)
        var cell = b; var d = dEnd; var guard = 0
        while (cell != a && guard++ < 5000) {
            val p = prev[cell][d]
            if (p < 0) break
            val pc = p / 4; val pd = p % 4
            if (pc == cell) { d = pd; continue }   // a turn happened here
            walk.add(pc); cell = pc; d = pd
        }
        val path = walk.reversed()
        val out = arrayListOf(path.first())
        for (k in 1 until path.size - 1) {
            val d1 = path[k] - path[k - 1]; val d2 = path[k + 1] - path[k]
            if (d1 != d2) out.add(path[k])
        }
        if (path.size > 1) out.add(path.last())
        return out
    }

    fun anyMove(): Pair<Int, Int>? {
        val idx = cells.indices.filter { cells[it] >= 0 }
        for (i in idx.indices) for (j in i + 1 until idx.size) {
            if (cells[idx[i]] == cells[idx[j]] && path(idx[i], idx[j]) != null) return idx[i] to idx[j]
        }
        return null
    }

    fun shuffle(rnd: Random) {
        val idx = cells.indices.filter { cells[it] >= 0 }
        val vals = idx.map { cells[it] }.shuffled(rnd)
        idx.forEachIndexed { k, i -> cells[i] = vals[k] }
    }
}
