package com.hgame.core

import kotlin.random.Random

/**
 * Dots and Boxes with [rows] x [cols] boxes. Lines are indexed: horizontal lines first
 * ((rows+1) * cols), then vertical ones (rows * (cols+1)).
 */
class DotsBoxes(val rows: Int, val cols: Int) {
    val hCount = (rows + 1) * cols
    val lines = BooleanArray(hCount + rows * (cols + 1))
    val owner = IntArray(rows * cols)

    fun copy() = DotsBoxes(rows, cols).also { lines.copyInto(it.lines); owner.copyInto(it.owner) }

    fun h(r: Int, c: Int) = r * cols + c
    fun v(r: Int, c: Int) = hCount + r * (cols + 1) + c

    fun sides(box: Int): Int {
        val r = box / cols; val c = box % cols
        return listOf(h(r, c), h(r + 1, c), v(r, c), v(r, c + 1)).count { lines[it] }
    }

    fun boxesOf(line: Int): List<Int> {
        val out = ArrayList<Int>(2)
        if (line < hCount) {
            val r = line / cols; val c = line % cols
            if (r > 0) out.add((r - 1) * cols + c)
            if (r < rows) out.add(r * cols + c)
        } else {
            val k = line - hCount
            val r = k / (cols + 1); val c = k % (cols + 1)
            if (c > 0) out.add(r * cols + c - 1)
            if (c < cols) out.add(r * cols + c)
        }
        return out
    }

    /** Draws [line] for [player]; returns the number of boxes completed (player moves again if > 0). */
    fun play(line: Int, player: Int): Int {
        lines[line] = true
        var made = 0
        for (b in boxesOf(line)) if (sides(b) == 4 && owner[b] == 0) { owner[b] = player; made++ }
        return made
    }

    fun free() = lines.indices.filter { !lines[it] }
    fun done() = lines.all { it }
    fun score(p: Int) = owner.count { it == p }

    /** Heuristic AI: complete boxes, avoid giving a third side, otherwise give away the smallest chain. */
    fun bestMove(skill: Int, rnd: Random = Random.Default): Int {
        val free = free()
        val completing = free.filter { l -> boxesOf(l).any { sides(it) == 3 } }
        if (completing.isNotEmpty() && (skill > 0 || rnd.nextFloat() < 0.7f)) return completing.random(rnd)
        val safe = free.filter { l -> boxesOf(l).none { sides(it) == 2 } }
        if (safe.isNotEmpty() && (skill > 0 || rnd.nextFloat() < 0.5f)) return safe.random(rnd)
        if (skill < 2) return free.random(rnd)
        // sacrifice the move that lets the opponent take the fewest boxes
        var best = free[0]; var bestGive = Int.MAX_VALUE
        for (l in free) {
            val c = copy(); c.play(l, 2)
            val give = c.greedyTake()
            if (give < bestGive) { bestGive = give; best = l }
        }
        return best
    }

    private fun greedyTake(): Int {
        var n = 0
        while (true) {
            val l = free().firstOrNull { line -> boxesOf(line).any { sides(it) == 3 } } ?: return n
            n += play(l, 1)
        }
    }
}
