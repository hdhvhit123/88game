package com.hgame.core

import kotlin.random.Random

/** 2048 rules. Tiles keep ids and previous positions so the view can animate slides. */
class Board2048(val n: Int = 4, private val rnd: Random = Random.Default) {
    class Tile(val id: Int, var value: Int, var r: Int, var c: Int) {
        var pr = r; var pc = c
        var isNew = true
        var merged = false
    }

    val cells = Array(n) { arrayOfNulls<Tile>(n) }
    /** Tiles absorbed by the last move; drawn sliding into their target, then dropped. */
    val absorbed = ArrayList<Tile>()
    var score = 0
        private set
    private var nextId = 1

    fun tiles(): List<Tile> = cells.flatMap { row -> row.filterNotNull() }

    fun reset() {
        for (row in cells) row.fill(null)
        absorbed.clear(); score = 0
        spawn(); spawn()
    }

    fun place(r: Int, c: Int, v: Int) { cells[r][c] = Tile(nextId++, v, r, c) }

    fun spawn(): Boolean {
        val empty = ArrayList<Int>()
        for (r in 0 until n) for (c in 0 until n) if (cells[r][c] == null) empty.add(r * n + c)
        if (empty.isEmpty()) return false
        val k = empty[rnd.nextInt(empty.size)]
        place(k / n, k % n, if (rnd.nextInt(10) == 0) 4 else 2)
        return true
    }

    /** dir: 0=up 1=right 2=down 3=left. Returns true if anything moved; spawns a new tile then. */
    fun move(dir: Int, spawnAfter: Boolean = true): Boolean {
        absorbed.clear()
        for (t in tiles()) { t.pr = t.r; t.pc = t.c; t.isNew = false; t.merged = false }
        var moved = false
        for (line in 0 until n) {
            // collect positions along the line in the direction of travel (first = destination side)
            val pos = (0 until n).map { i ->
                when (dir) {
                    0 -> i to line
                    2 -> (n - 1 - i) to line
                    3 -> line to i
                    else -> line to (n - 1 - i)
                }
            }
            var target = 0
            var last: Tile? = null
            for ((r, c) in pos) {
                val t = cells[r][c] ?: continue
                cells[r][c] = null
                if (last != null && last.value == t.value && !last.merged) {
                    last.value *= 2
                    last.merged = true
                    score += last.value
                    t.r = last.r; t.c = last.c
                    absorbed.add(t)
                    moved = true
                } else {
                    val (tr, tc) = pos[target++]
                    if (tr != r || tc != c) moved = true
                    t.r = tr; t.c = tc
                    cells[tr][tc] = t
                    last = t
                }
            }
        }
        if (moved && spawnAfter) spawn()
        return moved
    }

    fun canMove(): Boolean {
        for (r in 0 until n) for (c in 0 until n) {
            val v = cells[r][c]?.value ?: return true
            if (c + 1 < n && cells[r][c + 1]?.value == v) return true
            if (r + 1 < n && cells[r + 1][c]?.value == v) return true
        }
        return false
    }

    fun maxTile() = tiles().maxOfOrNull { it.value } ?: 0
}
