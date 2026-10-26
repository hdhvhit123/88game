package com.hgame.core

import kotlin.random.Random

/** Sliding-block "escape" puzzle on 6x6. Block 0 is the red key block in row 2 that exits to the right. */
class RushHour(val blocks: List<Block>) {
    data class Block(val horizontal: Boolean, val len: Int, val fixed: Int, var pos: Int) {
        fun cells(): List<Int> = (0 until len).map { if (horizontal) fixed * 6 + pos + it else (pos + it) * 6 + fixed }
    }

    fun copy() = RushHour(blocks.map { it.copy() })

    fun occupancy(): IntArray {
        val g = IntArray(36) { -1 }
        blocks.forEachIndexed { i, b -> b.cells().forEach { g[it] = i } }
        return g
    }

    /** Range of positions block [i] can slide to. */
    fun range(i: Int): IntRange {
        val g = occupancy(); val b = blocks[i]
        var lo = b.pos; var hi = b.pos
        while (lo > 0 && g[if (b.horizontal) b.fixed * 6 + lo - 1 else (lo - 1) * 6 + b.fixed] == -1) lo--
        while (hi + b.len < 6 && g[if (b.horizontal) b.fixed * 6 + hi + b.len else (hi + b.len) * 6 + b.fixed] == -1) hi++
        return lo..hi
    }

    fun solved() = blocks[0].pos == 4

    private fun key(): Long { var k = 0L; for (b in blocks) k = k * 8 + b.pos; return k }

    /** Minimum number of block moves to solve (-1 if unsolvable). */
    fun solveMoves(limit: Int = 200_000): Int {
        val seen = HashSet<Long>()
        val q = ArrayDeque<Pair<RushHour, Int>>()
        q.add(copy() to 0); seen.add(key())
        while (q.isNotEmpty()) {
            val (s, d) = q.removeFirst()
            if (s.solved()) return d
            if (seen.size > limit) return -1
            for (i in s.blocks.indices) {
                val r = s.range(i)
                for (p in r) {
                    if (p == s.blocks[i].pos) continue
                    val n = s.copy(); n.blocks[i].pos = p
                    if (seen.add(n.key())) q.add(n to d + 1)
                }
            }
        }
        return -1
    }

    companion object {
        /** Random puzzle needing at least [minMoves] moves (best effort within [tries]). */
        fun generate(minMoves: Int, rnd: Random, tries: Int = 400): Pair<RushHour, Int> {
            var best: Pair<RushHour, Int>? = null
            repeat(tries) {
                val blocks = arrayListOf(Block(true, 2, 2, rnd.nextInt(0, 3)))
                val g = IntArray(36) { -1 }
                blocks[0].cells().forEach { g[it] = 0 }
                val count = rnd.nextInt(8, 13)
                var attempts = 0
                while (blocks.size < count && attempts++ < 200) {
                    val h = rnd.nextBoolean()
                    val len = if (rnd.nextInt(4) == 0) 3 else 2
                    val fixed = rnd.nextInt(6)
                    if (h && fixed == 2) continue   // keep the exit row free of other horizontal blocks
                    val pos = rnd.nextInt(0, 7 - len)
                    val b = Block(h, len, fixed, pos)
                    if (b.cells().any { g[it] != -1 }) continue
                    b.cells().forEach { g[it] = blocks.size }
                    blocks.add(b)
                }
                val rh = RushHour(blocks)
                if (rh.solved()) return@repeat
                val m = rh.solveMoves(60_000)
                if (m > 0 && (best == null || m > best!!.second)) best = rh to m
                if (m >= minMoves) return rh to m
            }
            return best ?: (RushHour(listOf(Block(true, 2, 2, 0), Block(false, 2, 3, 1))) to 2)
        }
    }
}
