package com.hgame.core

import kotlin.random.Random

/** Places words in a letter grid in any of 8 directions and fills the rest randomly. */
class WordSearch(val n: Int, words: List<String>, rnd: Random = Random.Default, allowBackwards: Boolean = true) {
    val grid = CharArray(n * n) { ' ' }
    val placed = LinkedHashMap<String, List<Int>>()

    init {
        val dirs = if (allowBackwards) listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1)
        else listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1)
        for (word in words.shuffled(rnd).sortedByDescending { it.length }) {
            if (word.length > n) continue
            var done = false
            repeat(200) {
                if (done) return@repeat
                val (dr, dc) = dirs[rnd.nextInt(dirs.size)]
                val r0 = rnd.nextInt(n); val c0 = rnd.nextInt(n)
                val cells = (word.indices).map { (r0 + dr * it) to (c0 + dc * it) }
                if (cells.any { (r, c) -> r !in 0 until n || c !in 0 until n }) return@repeat
                if (cells.withIndex().any { (i, rc) -> val g = grid[rc.first * n + rc.second]; g != ' ' && g != word[i] }) return@repeat
                cells.forEachIndexed { i, (r, c) -> grid[r * n + c] = word[i] }
                placed[word] = cells.map { (r, c) -> r * n + c }
                done = true
            }
        }
        for (i in grid.indices) if (grid[i] == ' ') grid[i] = 'A' + rnd.nextInt(26)
    }
}
