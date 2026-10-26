package com.hgame.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XiangqiTest {
    @Test fun startingPositionHas44Moves() {
        // well-known perft(1) of the initial xiangqi position
        assertEquals(44, Xiangqi().legalMoves().size)
    }

    @Test fun perft2() {
        val x = Xiangqi()
        var n = 0
        for (m in x.legalMoves()) { val c = x.copy(); c.make(m); n += c.legalMoves().size }
        assertEquals(1920, n)
    }

    @Test fun flyingGeneralIsIllegal() {
        val x = Xiangqi()
        x.b.fill(0)
        x.b[4] = Xiangqi.K; x.b[9 * 9 + 3] = -Xiangqi.K
        // red general may not step onto the open file facing black's general
        assertTrue(x.legalMoves().none { Xiangqi.dest(it) == 3 })
    }

    @Test fun engineCapturesHangingChariot() {
        val x = Xiangqi()
        x.b.fill(0)
        x.b[4] = Xiangqi.K; x.b[9 * 9 + 3] = -Xiangqi.K
        x.b[5 * 9 + 0] = Xiangqi.R; x.b[5 * 9 + 8] = -Xiangqi.R
        val m = x.bestMove(2, 2000)
        assertEquals(5 * 9 + 8, Xiangqi.dest(m))
    }
}
