package com.hgame.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BoardGamesTest {
    @Test fun connectFourDetectsAndBlocksWins() {
        val g = ConnectFour()
        g.drop(0, 1); g.drop(1, 1); g.drop(2, 1)
        assertEquals(3, g.bestMove(2, 4))           // must block
        g.drop(3, 1)
        assertNotNull(g.winLine())
    }

    @Test fun connectFourTakesWin() {
        val g = ConnectFour()
        repeat(3) { g.drop(5, 2) }
        g.drop(0, 1); g.drop(1, 1)
        assertEquals(5, g.bestMove(2, 4))
    }

    @Test fun reversiOpeningHasFourMoves() {
        val r = Reversi()
        assertEquals(listOf(19, 26, 37, 44), r.moves(1))
        val f = r.play(19, 1)
        assertEquals(listOf(27), f)
        assertEquals(4, r.count(1))
    }

    @Test fun gomokuCompletesFive() {
        val g = Gomoku(15)
        for (c in 3..6) g.cells[7 * 15 + c] = 2
        val m = g.bestMove(2, 2, Random(1))
        assertTrue(m == 7 * 15 + 2 || m == 7 * 15 + 7)
        g.cells[m] = 2
        assertNotNull(g.winLine(m))
    }

    @Test fun checkersForcesCaptureAndMultiJump() {
        val c = Checkers()
        c.b.fill(0)
        c.b[7 * 8 + 0] = 1          // bottom man at row 7 col 0
        c.b[6 * 8 + 1] = -1
        c.b[4 * 8 + 3] = -1
        val ms = c.moves(1)
        assertEquals(1, ms.size)
        assertEquals(2, ms[0].captured.size)
        c.play(ms[0])
        assertEquals(1, c.b[3 * 8 + 4])
    }

    @Test fun checkersStartHasSevenMoves() {
        assertEquals(7, Checkers().moves(1).size)
    }

    @Test fun sudokuGeneratesUniquePuzzle() {
        val (p, s) = Sudoku.generate(30, Random(3))
        assertEquals(1, Sudoku.countSolutions(p.copyOf()))
        for (i in 0 until 81) if (p[i] != 0) assertEquals(s[i], p[i])
    }

    @Test fun minesFirstRevealIsSafe() {
        repeat(20) {
            val m = Mines(9, 9, 10, Random(it))
            assertTrue(m.reveal(40))
            assertEquals(10, m.mine.count { x -> x })
        }
    }

    @Test fun board2048Merges() {
        val b = Board2048(4, Random(0))
        b.place(0, 0, 2); b.place(0, 1, 2); b.place(0, 2, 4)
        b.move(3, spawnAfter = false)
        assertEquals(4, b.cells[0][0]!!.value)
        assertEquals(4, b.cells[0][1]!!.value)
        assertEquals(4, b.score)
    }

    @Test fun ticTacToeAiNeverLoses() {
        // random X vs perfect O, O must never lose
        repeat(200) { seed ->
            val rnd = Random(seed)
            val b = IntArray(9); var turn = 1
            while (TicTacToe.winner(b) == 0) {
                val m = if (turn == 1) (0 until 9).filter { b[it] == 0 }.random(rnd) else TicTacToe.bestMove(b, 2)
                b[m] = turn; turn = 3 - turn
            }
            assertTrue(TicTacToe.winner(b) != 1)
        }
    }

    @Test fun floodItSolvable() {
        val f = FloodIt(10, 6, Random(5))
        val n = f.greedyMoves()
        assertTrue(n in 5..60)
    }
}
