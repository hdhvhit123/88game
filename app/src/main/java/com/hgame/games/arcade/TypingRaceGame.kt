package com.hgame.games.arcade

import android.graphics.Canvas
import android.graphics.Paint
import com.hgame.core.Words
import com.hgame.engine.Game
import com.hgame.engine.Keyboard
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import com.hgame.engine.textWidth
import kotlin.random.Random

class TypingRaceGame : Game() {
    override val realtime = true
    override val help = "Gõ chính xác câu hiển thị bằng bàn phím trên màn hình để xe của bạn chạy. Đua với 3 xe máy, " +
        "về đích trước để thắng. Dấu cách và dấu câu được tự động bỏ qua; gõ sai phải gõ lại đúng chữ đó. Điểm là tốc độ gõ (từ/phút)."

    private val kb = Keyboard().apply { showEnter = false }
    private var text = ""; private var pos = 0; private var errorT = 0f
    private val bots = FloatArray(3); private val botSpeed = FloatArray(3)
    private var time = 0f; private var started = false; private var countdown = 3f; private var mistakes = 0

    override fun newGame() {
        text = Words.TYPING[Random.nextInt(Words.TYPING.size)].uppercase()
        pos = 0; time = 0f; started = false; countdown = 3f; mistakes = 0
        skipNonLetters()
        for (i in 0..2) { bots[i] = 0f; botSpeed[i] = (25f + i * 8f + Random.nextFloat() * 10f) } // words per minute
        score(0); info("Chuẩn bị…")
    }

    override fun layout() { kb.layout(4 * dp, h * 0.62f, w - 4 * dp, h - 6 * dp) }

    override fun down(x: Float, y: Float) { kb.down(x, y); redraw() }

    override fun up(x: Float, y: Float) {
        val k = kb.up(x, y) ?: return
        if (!started || over) return
        if (k == "DEL") return
        if (k[0] == text[pos]) { pos++; sfx(Sfx.CLICK) }
        else { errorT = 0.3f; mistakes++; sfx(Sfx.ERROR); buzz(20) }
        skipNonLetters()
        if (pos >= text.length) finish()
        redraw()
    }

    /** Spaces and punctuation are skipped automatically; only letters are typed. */
    private fun skipNonLetters() { while (pos < text.length && !text[pos].isLetter()) pos++ }

    private fun wpm() = if (time <= 0f) 0 else ((pos / 5f) / (time / 60f)).toInt()

    private fun finish() {
        val place = 1 + bots.count { it >= 1f }
        val s = wpm(); score(s)
        sfx(if (place == 1) Sfx.WIN else Sfx.LOSE)
        gameOver(place == 1, if (place == 1) "Về nhất!" else "Về thứ $place", "Tốc độ $s từ/phút · $mistakes lỗi", s)
    }

    override fun update(dt: Float) {
        if (errorT > 0) errorT -= dt
        if (!started) {
            countdown -= dt
            if (countdown <= 0f) { started = true; info("Gõ đi!"); sfx(Sfx.TAP) }
            return
        }
        time += dt
        for (i in 0..2) bots[i] = (bots[i] + botSpeed[i] * 5f / 60f * dt / text.length * (0.8f + Random.nextFloat() * 0.4f)).coerceAtMost(1f)
        info("${wpm()} từ/phút")
        if (bots.all { it >= 1f } ) { score(wpm()); gameOver(false, "Về chót!", "Hãy luyện thêm nhé · ${wpm()} từ/phút", wpm()) }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        // race track
        val top = 12 * dp; val laneH = (h * 0.3f - top) / 4
        val colors = intArrayOf(Pal.CYAN, Pal.ORANGE, Pal.PINK, Pal.GREEN)
        val progress = floatArrayOf(pos / text.length.toFloat(), bots[0], bots[1], bots[2])
        for (i in 0..3) {
            val y = top + i * laneH
            c.rrect(8 * dp, y + 2 * dp, w - 8 * dp, y + laneH - 2 * dp, 8 * dp, if (i == 0) 0xFF2A3160.toInt() else Pal.SURFACE)
            c.rect(w - 40 * dp, y + 2 * dp, w - 34 * dp, y + laneH - 2 * dp, Pal.alpha(Pal.WHITE, 0.6f))
            val x = 16 * dp + progress[i] * (w - 80 * dp)
            c.rrect(x, y + laneH * 0.25f, x + 34 * dp, y + laneH * 0.75f, 6 * dp, colors[i])
            c.rrect(x + 18 * dp, y + laneH * 0.32f, x + 28 * dp, y + laneH * 0.55f, 3 * dp, Pal.alpha(0xFFB3E5FC.toInt(), 0.9f))
            c.text(if (i == 0) "BẠN" else "MÁY $i", w - 22 * dp, y + laneH / 2, 9 * dp, Pal.MUTED)
        }
        // text box with wrapping
        val boxT = h * 0.32f; val boxB = h * 0.6f
        c.rrect(10 * dp, boxT, w - 10 * dp, boxB, 14 * dp, Pal.SURFACE)
        val size = 18 * dp; val lineH = size * 1.45f
        var x = 22 * dp; var y = boxT + 26 * dp
        val words = text.split(" ")
        var idx = 0
        for (wd in words) {
            val ww = textWidth("$wd ", size)
            if (x + ww > w - 22 * dp) { x = 22 * dp; y += lineH }
            for (ch in "$wd ") {
                val cw = textWidth(ch.toString(), size)
                val col = when {
                    idx < pos -> Pal.GREEN
                    idx == pos -> if (errorT > 0) Pal.RED else Pal.YELLOW
                    else -> Pal.MUTED
                }
                if (idx == pos) c.rect(x, y + size * 0.6f, x + maxOf(cw, size * 0.4f), y + size * 0.7f, col)
                c.text(ch.toString(), x, y, size, col, Paint.Align.LEFT)
                x += cw; idx++
                if (idx > text.length) break
            }
        }
        kb.draw(c)
        if (!started) {
            c.rrect(w / 2 - 50 * dp, h * 0.18f - 40 * dp, w / 2 + 50 * dp, h * 0.18f + 40 * dp, 20 * dp, Pal.alpha(Pal.BLACK, 0.6f))
            c.text("${countdown.toInt() + 1}", w / 2, h * 0.18f, 48 * dp, Pal.YELLOW)
        }
    }
}
