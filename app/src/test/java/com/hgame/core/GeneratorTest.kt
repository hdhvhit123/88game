package com.hgame.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GeneratorTest {
    private fun timed(name: String, block: () -> Unit) {
        val t = System.currentTimeMillis(); block(); println("$name: ${System.currentTimeMillis() - t} ms")
    }

    @Test fun futoshikiUnique() {
        for (n in 4..6) timed("futoshiki $n") {
            val f = Futoshiki(n); f.generate(Random(n), 0.35f, n / 2)
            assertEquals(1, f.countSolutions(f.givens.copyOf(), 2))
        }
    }

    @Test fun binaryUnique() {
        for (n in intArrayOf(6, 8, 10)) timed("binary $n") {
            val b = Binary(n); b.generate(Random(n))
            assertEquals(1, b.count(b.givens.copyOf(), 2))
            assertTrue(b.solution.indices.all { b.valid(b.solution, it) })
        }
    }

    @Test fun pipesTreeIsSolvedBeforeScramble() {
        val p = Pipes(7, Random(1))
        assertTrue(p.solved())
        p.scramble(Random(2))
        repeat(0) {}
    }

    @Test fun mazeIsPerfect() {
        val m = MazeGen(12, 18, Random(4))
        // a perfect maze on N cells has N-1 passages
        var passages = 0
        for (c in m.walls.indices) for (d in 1..2) if (m.open(c, d)) passages++
        assertEquals(12 * 18 - 1, passages)
    }
}

class OnetTest {
    @Test fun matchesAdjacentAndAroundBorder() {
        val o = Onet(4, 2, 4, Random(1))
        o.cells.fill(-1)
        val W = o.W
        o.cells[1 * W + 1] = 0; o.cells[1 * W + 2] = 0
        org.junit.Assert.assertNotNull(o.path(1 * W + 1, 1 * W + 2))
        // two tiles in the same column separated by a blocker: route around via border
        o.cells.fill(-1)
        o.cells[1 * W + 2] = 1; o.cells[2 * W + 2] = 5; o.cells[2 * W + 3] = 5
        o.cells[1 * W + 3] = 1
        val p = o.path(1 * W + 2, 1 * W + 3)
        org.junit.Assert.assertNotNull(p)
        // blocked case: three turns needed
        o.cells.fill(9)
        for (c in 0 until W) { o.cells[c] = -1; o.cells[(o.H - 1) * W + c] = -1 }
        for (r in 0 until o.H) { o.cells[r * W] = -1; o.cells[r * W + W - 1] = -1 }
        o.cells[1 * W + 1] = 2; o.cells[2 * W + 4] = 2
        org.junit.Assert.assertNull(o.path(1 * W + 1, 2 * W + 4))
    }
}

class RushHourTest {
    @Test fun generatesSolvablePuzzle() {
        val t = System.currentTimeMillis()
        val (p, m) = RushHour.generate(12, Random(3))
        println("rush hour: $m moves in ${System.currentTimeMillis() - t} ms")
        org.junit.Assert.assertTrue(m > 0)
        org.junit.Assert.assertEquals(m, p.solveMoves())
    }
}
