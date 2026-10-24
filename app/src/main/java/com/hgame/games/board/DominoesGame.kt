package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.Dominoes
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

class DominoesGame : Game() {
    override val bestMode = BestMode.HIGH
    override val help = "Domino đôi-6: nối quân có số chấm trùng với một trong hai đầu dãy. Không có quân nối được thì bấm Bốc " +
        "để bốc từ chồng bài; hết bài thì Bỏ lượt. Ai đánh hết quân trước thắng và được cộng tổng số chấm trên tay đối phương."

    private var d = Dominoes()
    private var selected: Dominoes.Tile? = null
    private val leftBtn = Btn("◀ Đầu trái", Pal.ACCENT)
    private val rightBtn = Btn("Đầu phải ▶", Pal.ACCENT)
    private val drawBtn = Btn("Bốc quân", Pal.ORANGE)
    private var thinking = false
    private var total = 0
    private val handRects = ArrayList<Pair<Dominoes.Tile, FloatArray>>()

    override fun newGame() {
        d = Dominoes(); selected = null; thinking = false
        total = host.loadInt("total", 0)
        status()
        if (d.turn == 1) aiTurn()
    }

    override fun layout() {
        val by = h - 40 * dp
        leftBtn.setCenter(w * 0.27f, by, w * 0.42f, 44 * dp)
        rightBtn.setCenter(w * 0.73f, by, w * 0.42f, 44 * dp)
        drawBtn.setCenter(w / 2, by, w * 0.5f, 44 * dp)
    }

    private fun status() = info("Máy: ${d.hands[1].size} quân · Chồng bài: ${d.boneyard.size} · Tổng điểm: $total")

    override fun up(x: Float, y: Float) {
        if (over || thinking || d.turn != 0) return
        val sel = selected
        if (sel != null && d.chain.isNotEmpty() && d.canPlayLeft(sel) && d.canPlayRight(sel)) {
            if (leftBtn.hit(x, y)) { humanPlay(sel, true); return }
            if (rightBtn.hit(x, y)) { humanPlay(sel, false); return }
        }
        if (d.playable(0).isEmpty() && drawBtn.hit(x, y)) {
            if (d.draw(0)) sfx(Sfx.CARD) else { d.pass(0); info("Bạn bỏ lượt"); afterHuman() }
            status(); redraw(); return
        }
        for ((t, r) in handRects) if (x in r[0]..r[2] && y in r[1]..r[3]) {
            val l = d.canPlayLeft(t); val rr = d.canPlayRight(t)
            when {
                !l && !rr -> { sfx(Sfx.ERROR); selected = null }
                d.chain.isEmpty() || l != rr -> humanPlay(t, l)
                else -> { selected = t; sfx(Sfx.CLICK) }
            }
            redraw(); return
        }
        selected = null; redraw()
    }

    private fun humanPlay(t: Dominoes.Tile, left: Boolean) {
        d.play(0, t, left); selected = null
        sfx(Sfx.TAP)
        afterHuman()
    }

    private fun afterHuman() {
        status()
        if (!checkEnd()) aiTurn()
    }

    private fun aiTurn() {
        thinking = true
        after(0.8f) {
            while (d.aiChoice(1) == null && d.draw(1)) { /* keep drawing until playable */ }
            val ch = d.aiChoice(1)
            if (ch == null) { d.pass(1); info("Máy bỏ lượt") } else { d.play(1, ch.first, ch.second); sfx(Sfx.TAP) }
            thinking = false
            if (!checkEnd()) status()
        }
    }

    private fun checkEnd(): Boolean {
        val wnr = d.winner()
        if (wnr < 0) return false
        val mine = d.hands[0].sumOf { it.pips }; val theirs = d.hands[1].sumOf { it.pips }
        when (wnr) {
            0 -> { total += theirs; host.saveInt("total", total); gameOver(true, "Bạn thắng!", "+$theirs điểm (tổng $total)", total) }
            1 -> gameOver(false, "Máy thắng!", "Trên tay bạn còn $mine chấm")
            else -> gameOver(false, "Hoà – bị chặn", "Hai bên bằng chấm")
        }
        return true
    }

    private fun drawTile(c: Canvas, t: Dominoes.Tile, x: Float, y: Float, len: Float, vertical: Boolean, highlight: Boolean, faceUp: Boolean = true) {
        val wdt = len / 2
        val rw = if (vertical) wdt else len
        val rh = if (vertical) len else wdt
        c.rrect(x, y + 3 * dp, x + rw, y + rh + 3 * dp, wdt * 0.18f, 0xFF9E9AB5.toInt())
        c.rrect(x, y, x + rw, y + rh, wdt * 0.18f, if (highlight) 0xFFFFF4C2.toInt() else if (faceUp) 0xFFFDFCF7.toInt() else 0xFF5B3FD9.toInt())
        if (!faceUp) return
        if (vertical) {
            c.line(x + rw * 0.15f, y + rh / 2, x + rw * 0.85f, y + rh / 2, 1.5f * dp, 0xFF888888.toInt())
            pips(c, t.a, x, y, wdt); pips(c, t.b, x, y + wdt, wdt)
        } else {
            c.line(x + rw / 2, y + rh * 0.15f, x + rw / 2, y + rh * 0.85f, 1.5f * dp, 0xFF888888.toInt())
            pips(c, t.a, x, y, wdt); pips(c, t.b, x + wdt, y, wdt)
        }
    }

    private val layouts = arrayOf(
        floatArrayOf(), floatArrayOf(.5f, .5f), floatArrayOf(.25f, .25f, .75f, .75f),
        floatArrayOf(.25f, .25f, .5f, .5f, .75f, .75f), floatArrayOf(.25f, .25f, .75f, .25f, .25f, .75f, .75f, .75f),
        floatArrayOf(.25f, .25f, .75f, .25f, .5f, .5f, .25f, .75f, .75f, .75f),
        floatArrayOf(.25f, .22f, .75f, .22f, .25f, .5f, .75f, .5f, .25f, .78f, .75f, .78f),
    )

    private fun pips(c: Canvas, n: Int, x: Float, y: Float, s: Float) {
        val l = layouts[n]
        var i = 0
        while (i < l.size) { c.circle(x + l[i] * s, y + l[i + 1] * s, s * 0.09f, 0xFF22243A.toInt()); i += 2 }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF14523C.toInt(), 0xFF0B3325.toInt())
        // AI hand (face down until the round ends)
        val aiN = d.hands[1].size
        val small = min(36 * dp, (w - 40 * dp) / maxOf(1, aiN) * 1.6f)
        for (k in 0 until aiN) drawTile(c, d.hands[1][k], 20 * dp + k * small * 0.62f, 10 * dp, small, true, false, faceUp = over)
        // chain, wrapped into rows
        val chainTop = 10 * dp + small + 20 * dp
        val chainBottom = h * 0.6f
        var len = 56 * dp
        val n = d.chain.size
        while (len > 22 * dp) {
            val perRow = ((w - 24 * dp) / len).toInt().coerceAtLeast(1)
            val rows = (n + perRow - 1) / perRow
            if (rows * (len / 2 + 8 * dp) <= chainBottom - chainTop) break
            len -= 4 * dp
        }
        val perRow = ((w - 24 * dp) / len).toInt().coerceAtLeast(1)
        val rowsUsed = (n + perRow - 1) / perRow
        val blockTop = chainTop + ((chainBottom - chainTop) - rowsUsed * (len / 2 + 8 * dp)) / 2
        d.chain.forEachIndexed { i, t ->
            val row = i / perRow; val col = i % perRow
            val inRow = if (row == rowsUsed - 1) n - row * perRow else perRow
            val left = (w - inRow * len) / 2
            drawTile(c, t, left + col * len, blockTop + row * (len / 2 + 8 * dp), len - 2 * dp, false, false)
        }
        if (d.chain.isEmpty()) c.text("Đặt quân đầu tiên", w / 2, (chainTop + chainBottom) / 2, 16 * dp, Pal.alpha(Pal.WHITE, 0.7f), bold = false)
        else c.text("Đầu trái: ${d.leftEnd}   ·   Đầu phải: ${d.rightEnd}", w / 2, chainBottom + 8 * dp, 14 * dp, Pal.alpha(Pal.WHITE, 0.85f))
        // human hand
        handRects.clear()
        val hand = d.hands[0]
        val tileLen = min(min(80 * dp, (w - 24 * dp) / maxOf(4, hand.size) * 1.8f), h - chainBottom - 130 * dp)
        val tw = tileLen / 2
        val gap = min(tw * 1.15f, (w - 24 * dp - tw) / maxOf(1, hand.size - 1))
        val startX = (w - (gap * (hand.size - 1) + tw)) / 2
        val hy = h - 76 * dp - tileLen
        val canPlay = d.playable(0).toSet()
        hand.forEachIndexed { i, t ->
            val x = startX + i * gap
            val lift = if (t == selected) 14 * dp else if (t in canPlay && d.turn == 0 && !over) 6 * dp else 0f
            drawTile(c, t, x, hy - lift, tileLen, true, t == selected)
            handRects.add(t to floatArrayOf(x, hy - lift, x + tw, hy - lift + tileLen))
        }
        val sel = selected
        if (!over && d.turn == 0 && !thinking) {
            if (sel != null && d.chain.isNotEmpty() && d.canPlayLeft(sel) && d.canPlayRight(sel)) {
                leftBtn.draw(c, 14 * dp); rightBtn.draw(c, 14 * dp)
            } else if (canPlay.isEmpty()) {
                drawBtn.label = if (d.boneyard.isEmpty()) "Bỏ lượt" else "Bốc quân (${d.boneyard.size})"
                drawBtn.draw(c, 15 * dp)
            } else c.text("Chạm quân nổi lên để đánh", w / 2, h - 40 * dp, 14 * dp, Pal.alpha(Pal.WHITE, 0.8f), bold = false)
        } else if (thinking) c.text("Máy đang đánh…", w / 2, h - 40 * dp, 15 * dp, Pal.YELLOW)
    }
}
