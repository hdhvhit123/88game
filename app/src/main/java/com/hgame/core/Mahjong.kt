package com.hgame.core

import kotlin.random.Random

/** Mahjong solitaire on a stepped pyramid. Deals are built by simulated removal, so they are always solvable. */
class Mahjong(rnd: Random = Random.Default) {
    class Tile(val layer: Int, val row: Int, val col: Int) { var kind = 0; var alive = true }

    val tiles = ArrayList<Tile>()

    init {
        fun rect(layer: Int, r0: Int, c0: Int, rows: Int, cols: Int) {
            for (r in r0 until r0 + rows) for (c in c0 until c0 + cols) tiles.add(Tile(layer, r, c))
        }
        rect(0, 0, 0, 6, 8); rect(1, 1, 1, 4, 6); rect(2, 2, 2, 2, 4); rect(3, 2, 3, 1, 2)
        var ok = false
        while (!ok) {
            tiles.forEach { it.alive = true }
            ok = true
            var kind = 0
            val kinds = 21
            while (tiles.any { it.alive }) {
                val free = tiles.filter { it.alive && isFree(it) }
                if (free.size < 2) { ok = false; break }
                val pick = free.shuffled(rnd).take(2)
                pick.forEach { it.kind = kind % kinds; it.alive = false }
                kind++
            }
        }
        tiles.forEach { it.alive = true }
    }

    private fun at(layer: Int, row: Int, col: Int) = tiles.firstOrNull { it.alive && it.layer == layer && it.row == row && it.col == col }

    fun isFree(t: Tile): Boolean {
        if (!t.alive) return false
        if (at(t.layer + 1, t.row, t.col) != null) return false
        return at(t.layer, t.row, t.col - 1) == null || at(t.layer, t.row, t.col + 1) == null
    }

    fun remaining() = tiles.count { it.alive }

    fun hint(): Pair<Tile, Tile>? {
        val free = tiles.filter { isFree(it) }
        for (i in free.indices) for (j in i + 1 until free.size) if (free[i].kind == free[j].kind) return free[i] to free[j]
        return null
    }

    fun shuffleRemaining(rnd: Random) {
        val alive = tiles.filter { it.alive }
        val kinds = alive.map { it.kind }.shuffled(rnd)
        alive.forEachIndexed { i, t -> t.kind = kinds[i] }
    }
}
