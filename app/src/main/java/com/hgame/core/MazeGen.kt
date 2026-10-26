package com.hgame.core

import kotlin.random.Random

/** Perfect maze via randomized depth-first search. walls[cell] bits: 1 N, 2 E, 4 S, 8 W. */
class MazeGen(val cols: Int, val rows: Int, rnd: Random = Random.Default) {
    val walls = IntArray(cols * rows) { 15 }

    init {
        val seen = BooleanArray(cols * rows)
        val stack = ArrayDeque<Int>()
        stack.add(0); seen[0] = true
        while (stack.isNotEmpty()) {
            val c = stack.last()
            val x = c % cols; val y = c / cols
            val opts = ArrayList<Int>(4)
            if (y > 0 && !seen[c - cols]) opts.add(0)
            if (x < cols - 1 && !seen[c + 1]) opts.add(1)
            if (y < rows - 1 && !seen[c + cols]) opts.add(2)
            if (x > 0 && !seen[c - 1]) opts.add(3)
            if (opts.isEmpty()) { stack.removeLast(); continue }
            val d = opts[rnd.nextInt(opts.size)]
            val n = neighbor(c, d)
            walls[c] = walls[c] and (1 shl d).inv()
            walls[n] = walls[n] and (1 shl ((d + 2) % 4)).inv()
            seen[n] = true
            stack.add(n)
        }
    }

    fun open(c: Int, d: Int) = walls[c] and (1 shl d) == 0
    fun neighbor(c: Int, d: Int) = when (d) { 0 -> c - cols; 1 -> c + 1; 2 -> c + cols; else -> c - 1 }
    fun exits(c: Int) = (0..3).count { open(c, it) }
}
