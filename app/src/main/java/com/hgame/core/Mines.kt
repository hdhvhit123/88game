package com.hgame.core

import kotlin.random.Random

/** Minesweeper board. Mines are placed on the first reveal so the first tap is always safe. */
class Mines(val cols: Int, val rows: Int, val mineCount: Int, private val rnd: Random = Random.Default) {
    val mine = BooleanArray(cols * rows)
    val open = BooleanArray(cols * rows)
    val flag = BooleanArray(cols * rows)
    val adj = IntArray(cols * rows)
    var placed = false
        private set
    var exploded = -1
        private set

    fun neighbors(i: Int): List<Int> {
        val x = i % cols; val y = i / cols
        val out = ArrayList<Int>(8)
        for (dy in -1..1) for (dx in -1..1) {
            if (dx == 0 && dy == 0) continue
            val nx = x + dx; val ny = y + dy
            if (nx in 0 until cols && ny in 0 until rows) out.add(ny * cols + nx)
        }
        return out
    }

    fun placeMines(safe: Int) {
        val forbidden = neighbors(safe).toMutableSet().apply { add(safe) }
        val candidates = (0 until cols * rows).filter { it !in forbidden }.shuffled(rnd)
        for (k in 0 until minOf(mineCount, candidates.size)) mine[candidates[k]] = true
        for (i in mine.indices) adj[i] = neighbors(i).count { mine[it] }
        placed = true
    }

    /** Reveals [i]; returns false if a mine exploded. */
    fun reveal(i: Int): Boolean {
        if (!placed) placeMines(i)
        if (open[i] || flag[i]) return true
        if (mine[i]) { open[i] = true; exploded = i; return false }
        val stack = ArrayDeque<Int>()
        stack.add(i)
        while (stack.isNotEmpty()) {
            val k = stack.removeLast()
            if (open[k]) continue
            open[k] = true
            flag[k] = false
            if (adj[k] == 0) for (n in neighbors(k)) if (!open[n] && !mine[n]) stack.add(n)
        }
        return true
    }

    /** Chord: if the number of flags around an open cell matches, reveal the rest. */
    fun chord(i: Int): Boolean {
        if (!open[i] || adj[i] == 0) return true
        val ns = neighbors(i)
        if (ns.count { flag[it] } != adj[i]) return true
        var ok = true
        for (n in ns) if (!flag[n] && !open[n]) ok = reveal(n) && ok
        return ok
    }

    fun toggleFlag(i: Int) { if (!open[i]) flag[i] = !flag[i] }
    fun flags() = flag.count { it }
    fun won() = placed && (0 until cols * rows).all { mine[it] || open[it] }
}
