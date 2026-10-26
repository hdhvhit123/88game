package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class MastermindGame : Game() {
    override val bestMode = BestMode.HIGH
    override val options = listOf(GameOption("dup", "Màu lặp lại", listOf("Không", "Có")))
    override val help = "Đoán mã bí mật gồm 4 màu trong 10 lượt. Sau mỗi lượt: chấm đen = đúng màu đúng vị trí, " +
        "chấm trắng = đúng màu sai vị trí. Chạm màu bên dưới để điền, chạm ô đã điền để xoá."

    private val palette = intArrayOf(Pal.RED, Pal.BLUE, Pal.GREEN, Pal.YELLOW, Pal.PURPLE, Pal.ORANGE)
    private var secret = IntArray(4)
    private val guesses = ArrayList<IntArray>()
    private val feedback = ArrayList<Pair<Int, Int>>()
    private var current = IntArray(4) { -1 }
    private val okBtn = Btn("Đoán", Pal.ACCENT)
    private var rowH = 0f; private var top = 0f; private var peg = 0f
    private val palX = FloatArray(6); private var palY = 0f

    override fun newGame() {
        val dup = opt("dup") == 1
        secret = if (dup) IntArray(4) { Random.nextInt(6) } else (0 until 6).shuffled().take(4).toIntArray()
        guesses.clear(); feedback.clear(); current = IntArray(4) { -1 }
        info("Lượt 1/10")
    }

    override fun layout() {
        top = 64 * dp
        rowH = min((h - top - 150 * dp) / 10, 52 * dp)
        peg = rowH * 0.36f
        palY = h - 92 * dp
        for (i in 0 until 6) palX[i] = w * (i + 0.5f) / 6
        okBtn.setCenter(w / 2, h - 34 * dp, 160 * dp, 44 * dp)
    }

    private fun slotX(i: Int) = w * 0.18f + i * (w * 0.14f)

    override fun up(x: Float, y: Float) {
        if (over) return
        for (i in 0 until 6) {
            val dx = x - palX[i]; val dy = y - palY
            if (dx * dx + dy * dy < (peg * 1.6f) * (peg * 1.6f)) {
                val slot = current.indexOf(-1)
                if (slot >= 0) { current[slot] = i; sfx(Sfx.TAP) }
                redraw(); return
            }
        }
        val rowY = top + guesses.size * rowH + rowH / 2
        if (kotlin.math.abs(y - rowY) < rowH / 2) for (i in 0 until 4) if (kotlin.math.abs(x - slotX(i)) < peg * 1.3f) { current[i] = -1; sfx(Sfx.CLICK); redraw(); return }
        if (okBtn.hit(x, y) && current.all { it >= 0 }) submit()
        redraw()
    }

    private fun submit() {
        val g = current.copyOf()
        var black = 0
        val sc = IntArray(6); val gc = IntArray(6)
        for (i in 0 until 4) if (g[i] == secret[i]) black++ else { sc[secret[i]]++; gc[g[i]]++ }
        val white = (0 until 6).sumOf { min(sc[it], gc[it]) }
        guesses.add(g); feedback.add(black to white)
        current = IntArray(4) { -1 }
        sfx(if (black == 4) Sfx.WIN else Sfx.POP)
        if (black == 4) { score((11 - guesses.size) * 10); gameOver(true, "Giải mã thành công!", "Sau ${guesses.size} lượt") }
        else if (guesses.size >= 10) gameOver(false, "Hết lượt!", "Mã đúng được hiện ở trên")
        else info("Lượt ${guesses.size + 1}/10")
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        // secret row
        c.rrect(w * 0.1f, 10 * dp, w * 0.9f, 52 * dp, 14 * dp, Pal.SURFACE2)
        for (i in 0 until 4) {
            if (over) c.ball(slotX(i), 31 * dp, peg * 0.9f, palette[secret[i]])
            else { c.circle(slotX(i), 31 * dp, peg * 0.9f, Pal.SURFACE); c.text("?", slotX(i), 31 * dp, peg, Pal.MUTED) }
        }
        for (r in 0 until 10) {
            val y = top + r * rowH + rowH / 2
            val isCurrent = r == guesses.size && !over
            c.rrect(w * 0.06f, y - rowH * 0.45f, w * 0.94f, y + rowH * 0.45f, rowH * 0.3f, if (isCurrent) 0xFF2E3570.toInt() else Pal.SURFACE)
            c.text("${r + 1}", w * 0.1f, y, rowH * 0.3f, Pal.MUTED, bold = false)
            val row = if (r < guesses.size) guesses[r] else if (isCurrent) current else null
            for (i in 0 until 4) {
                val v = row?.get(i) ?: -1
                if (v >= 0) c.ball(slotX(i), y, peg, palette[v]) else c.circle(slotX(i), y, peg * 0.45f, 0xFF12152B.toInt())
            }
            if (r < feedback.size) {
                val (b, wh) = feedback[r]
                for (k in 0 until 4) {
                    val px = w * 0.78f + (k % 2) * peg * 0.9f; val py = y - peg * 0.45f + (k / 2) * peg * 0.9f
                    when {
                        k < b -> c.circle(px, py, peg * 0.3f, 0xFF111111.toInt())
                        k < b + wh -> c.circle(px, py, peg * 0.3f, Pal.WHITE)
                        else -> c.ring(px, py, peg * 0.25f, 1 * dp, Pal.MUTED)
                    }
                }
            }
        }
        for (i in 0 until 6) c.ball(palX[i], palY, peg * 1.2f, palette[i])
        okBtn.enabled = current.all { it >= 0 } && !over
        okBtn.draw(c, 16 * dp)
    }
}
