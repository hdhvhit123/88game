package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.SoundBank
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.random.Random

class PianoTilesGame : Game() {
    override val realtime = true
    override val help = "Chạm vào các phím đen khi chúng trượt xuống. Không được bỏ sót phím nào và đừng chạm vào ô trắng! Tốc độ tăng dần."

    private class Row(val lane: Int, var hit: Boolean = false)
    private val rowsList = ArrayList<Row>()
    private var offset = 0f; private var speed = 0f; private var started = false
    private var points = 0; private var rowH = 0f; private var wrong = -1; private var wrongRow = -1
    private var missedFlash = 0f

    override fun newGame() {
        rowsList.clear(); offset = 0f; started = false; points = 0; wrong = -1; wrongRow = -1
        repeat(8) { rowsList.add(Row(Random.nextInt(4))) }
        score(0)
    }

    override fun layout() { rowH = h / 4f; speed = h * 0.55f }

    // row k (0 = bottom-most active row) is drawn at y = h - (k + 1) * rowH + offset
    private fun rowTop(k: Int) = h - (k + 1) * rowH + offset

    override fun pointerDown(id: Int, x: Float, y: Float) {
        if (over) return
        val lane = (x / (w / 4)).toInt().coerceIn(0, 3)
        val k = rowsList.indexOfFirst { !it.hit }
        if (k < 0) return
        val top = rowTop(k)
        if (y < top - rowH * 0.25f || y > top + rowH * 1.25f) {
            // tapped a row above or below the next one: check if it's white space
            val idx = ((h + offset - y) / rowH).toInt()
            if (idx in rowsList.indices && rowsList[idx].lane != lane) fail(lane, idx)
            return
        }
        if (rowsList[k].lane == lane) {
            rowsList[k].hit = true; points++; score(points); started = true
            SoundBank.playNote(points)
        } else fail(lane, k)
    }

    private fun fail(lane: Int, row: Int) {
        wrong = lane; wrongRow = row; sfx(Sfx.ERROR); buzz(80)
        gameOver(false, "Sai phím!", "Bạn chạm được $points phím", points)
    }

    override fun update(dt: Float) {
        if (missedFlash > 0) missedFlash -= dt
        if (!started) return
        speed = h * (0.55f + points * 0.006f).coerceAtMost(1.6f)
        offset += speed * dt
        while (offset >= rowH) {
            val bottom = rowsList.first()
            if (!bottom.hit) {
                offset = rowH * 0.6f; wrong = bottom.lane; wrongRow = 0; missedFlash = 1f; sfx(Sfx.ERROR); buzz(80)
                gameOver(false, "Bỏ lỡ một phím!", "Bạn chạm được $points phím", points); return
            }
            rowsList.removeAt(0); rowsList.add(Row(Random.nextInt(4))); offset -= rowH
        }
    }

    override fun draw(c: Canvas) {
        c.rect(0f, 0f, w, h, 0xFFF5F5F5.toInt())
        val lw = w / 4
        rowsList.forEachIndexed { k, r ->
            val top = rowTop(k)
            if (top > h || top + rowH < 0) return@forEachIndexed
            val x = r.lane * lw
            if (!r.hit) {
                c.rect(x + 1, top + 1, x + lw - 1, top + rowH - 1, 0xFF111322.toInt())
                c.rect(x + 1, top + rowH * 0.85f, x + lw - 1, top + rowH - 1, 0xFF2A2E4A.toInt())
                if (!started && k == 0) c.text("BẮT ĐẦU", x + lw / 2, top + rowH / 2, 15 * dp, Pal.WHITE)
            } else c.rect(x + 1, top + 1, x + lw - 1, top + rowH - 1, 0xFFCFD3E6.toInt())
            if (over && k == wrongRow) c.rect(wrong * lw, top, wrong * lw + lw, top + rowH, Pal.alpha(Pal.RED, 0.85f))
        }
        for (i in 1..3) c.line(i * lw, 0f, i * lw, h, 1f * dp, 0xFFD0D0D8.toInt())
        c.rrect(w / 2 - 40 * dp, 10 * dp, w / 2 + 40 * dp, 52 * dp, 14 * dp, Pal.alpha(Pal.BLACK, 0.55f))
        c.text("$points", w / 2, 31 * dp, 26 * dp, Pal.RED)
    }
}
