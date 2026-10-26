package com.hgame.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChessTest {
    @Test fun perftFromStart() {
        val c = Chess()
        assertEquals(20L, c.perft(1))
        assertEquals(400L, c.perft(2))
        assertEquals(8902L, c.perft(3))
    }

    @Test fun perftKiwipete() {
        val c = Chess()
        c.loadFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        assertEquals(48L, c.perft(1))
        assertEquals(2039L, c.perft(2))
        assertEquals(97862L, c.perft(3))
    }

    @Test fun perftPosition3() {
        val c = Chess()
        c.loadFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")
        assertEquals(14L, c.perft(1))
        assertEquals(191L, c.perft(2))
        assertEquals(2812L, c.perft(3))
    }

    @Test fun foolsMateIsCheckmate() {
        val c = Chess()
        fun mv(f: String, t: String) {
            val sq = { s: String -> (s[1] - '1') * 8 + (s[0] - 'a') }
            val m = c.legalMoves().first { Chess.from(it) == sq(f) && Chess.dest(it) == sq(t) }
            c.play(m)
        }
        mv("f2", "f3"); mv("e7", "e5"); mv("g2", "g4"); mv("d8", "h4")
        assertEquals(2, c.result())
    }

    @Test fun engineFindsMateInOne() {
        val c = Chess()
        c.b.fill(0)
        // White: Kg1, Qh5, Bc4 ; Black: Ke8, pawns f7? -> Scholar's mate pattern Qxf7#
        c.b[6] = Chess.K; c.b[39] = Chess.Q; c.b[26] = Chess.B
        c.b[60] = -Chess.K; c.b[53] = -Chess.P; c.b[59] = -Chess.Q; c.b[61] = -Chess.B; c.b[52] = -Chess.P; c.b[51] = -Chess.P
        c.castle = 0
        val m = c.bestMove(3, 3000)
        assertEquals(53, Chess.dest(m))
        c.play(m)
        assertEquals(1, c.result())
    }

    @Test fun castlingAndEnPassantAreGenerated() {
        val c = Chess()
        c.b.fill(0)
        c.b[4] = Chess.K; c.b[7] = Chess.R; c.b[0] = Chess.R; c.b[60] = -Chess.K
        c.castle = 3
        val moves = c.legalMoves()
        assert(moves.any { Chess.from(it) == 4 && Chess.dest(it) == 6 })
        assert(moves.any { Chess.from(it) == 4 && Chess.dest(it) == 2 })
        val d = Chess()
        d.b.fill(0); d.b[4] = Chess.K; d.b[60] = -Chess.K
        d.b[36] = Chess.P; d.b[51] = -Chess.P; d.white = false; d.castle = 0
        d.play(Chess.move(51, 35))
        val ep = d.legalMoves().first { Chess.from(it) == 36 && Chess.dest(it) == 43 }
        d.play(ep)
        assertEquals(0, d.b[35])
        assertNotEquals(0, d.b[43])
    }
}
