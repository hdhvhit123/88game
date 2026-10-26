package com.hgame.core

import kotlin.random.Random

/** Net/pipes puzzle: rotate tiles so every pipe connects into one network. Masks: 1 N, 2 E, 4 S, 8 W. */
class Pipes(val n: Int, rnd: Random = Random.Default) {
    val tiles = IntArray(n * n)
    val source = (n / 2) * n + n / 2

    init {
        val seen = BooleanArray(n * n)
        val frontier = ArrayList<Pair<Int, Int>>()
        fun addEdges(c: Int) {
            for (d in 0..3) {
                val nb = neighbor(c, d)
                if (nb >= 0 && !seen[nb]) frontier.add(c to d)
            }
        }
        seen[source] = true; addEdges(source)
        while (frontier.isNotEmpty()) {
            val (c, d) = frontier.removeAt(rnd.nextInt(frontier.size))
            val nb = neighbor(c, d)
            if (seen[nb]) continue
            // avoid four-way crosses so tiles stay readable
            if (Integer.bitCount(tiles[c]) >= 3) continue
            tiles[c] = tiles[c] or (1 shl d)
            tiles[nb] = tiles[nb] or (1 shl ((d + 2) % 4))
            seen[nb] = true
            addEdges(nb)
        }
        // any cell left unconnected (rare due to the cross rule) is attached anyway
        for (c in 0 until n * n) if (!seen[c]) {
            for (d in 0..3) { val nb = neighbor(c, d); if (nb >= 0 && seen[nb]) { tiles[c] = tiles[c] or (1 shl d); tiles[nb] = tiles[nb] or (1 shl ((d + 2) % 4)); seen[c] = true; break } }
        }
    }

    fun neighbor(c: Int, d: Int): Int {
        val x = c % n; val y = c / n
        return when (d) { 0 -> if (y > 0) c - n else -1; 1 -> if (x < n - 1) c + 1 else -1; 2 -> if (y < n - 1) c + n else -1; else -> if (x > 0) c - 1 else -1 }
    }

    fun rotate(c: Int) { val t = tiles[c]; tiles[c] = ((t shl 1) or (t shr 3)) and 15 }

    fun scramble(rnd: Random) { for (c in tiles.indices) repeat(rnd.nextInt(4)) { rotate(c) } }

    /** Cells connected to the source through matching pipe ends. */
    fun connected(): BooleanArray {
        val seen = BooleanArray(n * n)
        val st = ArrayDeque<Int>(); st.add(source); seen[source] = true
        while (st.isNotEmpty()) {
            val c = st.removeLast()
            for (d in 0..3) {
                if (tiles[c] and (1 shl d) == 0) continue
                val nb = neighbor(c, d)
                if (nb < 0 || seen[nb]) continue
                if (tiles[nb] and (1 shl ((d + 2) % 4)) == 0) continue
                seen[nb] = true; st.add(nb)
            }
        }
        return seen
    }

    fun solved(): Boolean {
        if (!connected().all { it }) return false
        for (c in tiles.indices) for (d in 0..3) if (tiles[c] and (1 shl d) != 0) {
            val nb = neighbor(c, d)
            if (nb < 0 || tiles[nb] and (1 shl ((d + 2) % 4)) == 0) return false
        }
        return true
    }
}
