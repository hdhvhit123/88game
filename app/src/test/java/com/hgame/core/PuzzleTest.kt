package com.hgame.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PuzzleTest {
    @Test fun allSokobanLevelsAreSolvable() {
        val bad = ArrayList<Int>()
        Sokoban.LEVELS.forEachIndexed { i, l ->
            val s = Sokoban(l)
            assertEquals("level ${i + 1} boxes vs goals", s.goal.count { it }, s.boxes.size)
            val pushes = s.solvePushes()
            println("sokoban level ${i + 1}: $pushes pushes")
            if (pushes == -1) bad.add(i + 1)
        }
        assertEquals("unsolvable levels", emptyList<Int>(), bad)
    }

    @Test fun waterSortPours() {
        val ws = WaterSort(2, 1)
        ws.tubes.clear()
        ws.tubes.add(arrayListOf(0, 1, 1, 0)); ws.tubes.add(arrayListOf(1, 0, 0, 1)); ws.tubes.add(arrayListOf())
        assertEquals(1, ws.pourAmount(0, 2))
        assertEquals(true, ws.solvable())
    }

    @Test fun waterSortDealsAreUsuallySolvable() {
        var ok = 0
        repeat(10) { s ->
            val ws = WaterSort(5); ws.deal(Random(s))
            if (ws.solvable() != false) ok++
        }
        assertTrue(ok >= 8)
    }
}
