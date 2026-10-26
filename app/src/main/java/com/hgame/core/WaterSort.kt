package com.hgame.core

import kotlin.random.Random

/** Water sort puzzle. Each tube holds up to [cap] units, listed bottom to top. */
class WaterSort(val colors: Int, val empty: Int = 2, val cap: Int = 4) {
    val tubes = ArrayList<ArrayList<Int>>()

    fun copy() = WaterSort(colors, empty, cap).also { c -> tubes.forEach { c.tubes.add(ArrayList(it)) } }

    fun deal(rnd: Random) {
        val units = (0 until colors).flatMap { col -> List(cap) { col } }.shuffled(rnd)
        tubes.clear()
        for (t in 0 until colors) tubes.add(ArrayList(units.subList(t * cap, (t + 1) * cap)))
        repeat(empty) { tubes.add(ArrayList()) }
    }

    /** Number of units that would move from [a] to [b] (0 if illegal). */
    fun pourAmount(a: Int, b: Int): Int {
        if (a == b) return 0
        val from = tubes[a]; val to = tubes[b]
        if (from.isEmpty() || to.size >= cap) return 0
        val top = from.last()
        if (to.isNotEmpty() && to.last() != top) return 0
        var run = 0
        for (i in from.indices.reversed()) if (from[i] == top) run++ else break
        return minOf(run, cap - to.size)
    }

    fun pour(a: Int, b: Int): Int {
        val n = pourAmount(a, b)
        repeat(n) { tubes[b].add(tubes[a].removeAt(tubes[a].size - 1)) }
        return n
    }

    fun solved() = tubes.all { it.isEmpty() || (it.size == cap && it.all { c -> c == it[0] }) }

    private fun key(): String = tubes.map { it.joinToString(",") }.sorted().joinToString("|")

    /** Depth-first search for solvability, bounded by [limit] visited states (null = gave up). */
    fun solvable(limit: Int = 60000): Boolean? {
        val seen = HashSet<String>()
        var budget = limit
        fun dfs(s: WaterSort): Boolean? {
            if (s.solved()) return true
            if (!seen.add(s.key())) return false
            if (--budget <= 0) return null
            var unknown = false
            for (a in s.tubes.indices) for (b in s.tubes.indices) {
                val n = s.pourAmount(a, b)
                if (n == 0) continue
                // skip pointless moves: moving a complete uniform tube into an empty one
                if (s.tubes[b].isEmpty() && s.tubes[a].all { it == s.tubes[a][0] }) continue
                val c = s.copy(); c.pour(a, b)
                when (dfs(c)) { true -> return true; null -> unknown = true; else -> {} }
            }
            return if (unknown) null else false
        }
        return dfs(copy())
    }
}
