package com.hgame.core

import kotlin.random.Random

/**
 * Flow puzzle generator: a random Hamiltonian path (snake + backbite moves) is cut into
 * segments; each segment's two ends become a colour pair. The cut path is a full-board solution.
 */
class FlowGen(val n: Int, rnd: Random) {
    val solution = IntArray(n * n)          // colour index per cell
    val ends = ArrayList<Pair<Int, Int>>()  // endpoints per colour

    init {
        val path = ArrayList<Int>(n * n)
        for (r in 0 until n) for (k in 0 until n) path.add(r * n + if (r % 2 == 0) k else n - 1 - k)
        val pos = IntArray(n * n)
        fun reindex() { path.forEachIndexed { i, c -> pos[c] = i } }
        reindex()
        repeat(n * n * 30) {
            if (rnd.nextBoolean()) { path.reverse(); reindex() }
            val head = path[0]
            val nbs = neighbors(head).filter { it != path[1] }
            if (nbs.isEmpty()) return@repeat
            val x = nbs[rnd.nextInt(nbs.size)]
            val k = pos[x]
            path.subList(0, k).reverse()
            for (i in 0 until k) pos[path[i]] = i
        }
        // cut into segments of length 3..(n+2)
        var i = 0; var color = 0
        while (i < path.size) {
            val remaining = path.size - i
            var len = rnd.nextInt(3, n + 3)
            if (remaining - len < 3) len = remaining
            for (k in i until i + len) solution[path[k]] = color
            ends.add(path[i] to path[i + len - 1])
            i += len; color++
        }
    }

    fun neighbors(c: Int): List<Int> {
        val r = c / n; val col = c % n
        return buildList { if (r > 0) add(c - n); if (r < n - 1) add(c + n); if (col > 0) add(c - 1); if (col < n - 1) add(c + 1) }
    }
}
