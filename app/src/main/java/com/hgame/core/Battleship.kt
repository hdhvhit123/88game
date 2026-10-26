package com.hgame.core

import kotlin.random.Random

/** One player's 10x10 fleet grid. */
class Fleet(private val rnd: Random = Random.Default) {
    val size = 10
    val ship = IntArray(100) { -1 }       // ship id per cell
    val shot = BooleanArray(100)
    val lengths = intArrayOf(5, 4, 3, 3, 2)
    val ships = ArrayList<List<Int>>()

    fun randomize() {
        ship.fill(-1); ships.clear()
        for ((id, len) in lengths.withIndex()) {
            while (true) {
                val horiz = rnd.nextBoolean()
                val r = rnd.nextInt(if (horiz) size else size - len + 1)
                val c = rnd.nextInt(if (horiz) size - len + 1 else size)
                val cells = (0 until len).map { if (horiz) r * size + c + it else (r + it) * size + c }
                // keep ships from touching each other
                val blocked = cells.any { cell ->
                    val cr = cell / size; val cc = cell % size
                    (-1..1).any { dr -> (-1..1).any { dc ->
                        val nr = cr + dr; val nc = cc + dc
                        nr in 0 until size && nc in 0 until size && ship[nr * size + nc] >= 0
                    } }
                }
                if (blocked) continue
                cells.forEach { ship[it] = id }
                ships.add(cells)
                break
            }
        }
    }

    /** Fires at [i]: returns 0 miss, 1 hit, 2 sunk (ship id via [ship]). */
    fun fire(i: Int): Int {
        shot[i] = true
        val id = ship[i]
        if (id < 0) return 0
        return if (ships[id].all { shot[it] }) 2 else 1
    }

    fun sunk(id: Int) = ships[id].all { shot[it] }
    fun allSunk() = ships.indices.all { sunk(it) }
}

/** Hunt/target AI: fires on a checkerboard until it hits, then works around the hits. */
class BattleshipAi(private val rnd: Random = Random.Default) {
    fun choose(target: Fleet, smart: Boolean): Int {
        val open = (0 until 100).filter { !target.shot[it] }
        if (!smart) return open.random(rnd)
        // unsunk hits
        val hits = (0 until 100).filter { target.shot[it] && target.ship[it] >= 0 && !target.sunk(target.ship[it]) }
        if (hits.isNotEmpty()) {
            val cands = ArrayList<Int>()
            if (hits.size >= 2) {
                val sameRow = hits.all { it / 10 == hits[0] / 10 }
                val sameCol = hits.all { it % 10 == hits[0] % 10 }
                if (sameRow) {
                    val r = hits[0] / 10; val minC = hits.minOf { it % 10 }; val maxC = hits.maxOf { it % 10 }
                    if (minC > 0) cands.add(r * 10 + minC - 1); if (maxC < 9) cands.add(r * 10 + maxC + 1)
                } else if (sameCol) {
                    val c = hits[0] % 10; val minR = hits.minOf { it / 10 }; val maxR = hits.maxOf { it / 10 }
                    if (minR > 0) cands.add((minR - 1) * 10 + c); if (maxR < 9) cands.add((maxR + 1) * 10 + c)
                }
            }
            val filtered = cands.filter { !target.shot[it] }
            if (filtered.isNotEmpty()) return filtered.random(rnd)
            for (hcell in hits) {
                val r = hcell / 10; val c = hcell % 10
                for ((dr, dc) in listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0)) {
                    val nr = r + dr; val nc = c + dc
                    if (nr in 0..9 && nc in 0..9 && !target.shot[nr * 10 + nc]) cands.add(nr * 10 + nc)
                }
            }
            if (cands.isNotEmpty()) return cands.random(rnd)
        }
        // skip cells next to sunk ships (ships never touch)
        val nearSunk = HashSet<Int>()
        for (id in target.ships.indices) if (target.sunk(id)) for (cell in target.ships[id]) {
            val r = cell / 10; val c = cell % 10
            for (dr in -1..1) for (dc in -1..1) { val nr = r + dr; val nc = c + dc; if (nr in 0..9 && nc in 0..9) nearSunk.add(nr * 10 + nc) }
        }
        val parity = open.filter { (it / 10 + it % 10) % 2 == 0 && it !in nearSunk }
        return (parity.ifEmpty { open.filter { it !in nearSunk }.ifEmpty { open } }).random(rnd)
    }
}
