package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Mines
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.poly
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs

class MinesweeperGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("level", "Độ khó", listOf("Dễ (9×9, 10 mìn)", "Vừa (10×14, 25 mìn)", "Khó (12×18, 45 mìn)")))
    override val help = "Mở tất cả các ô không có mìn. Số trên ô cho biết có bao nhiêu mìn ở 8 ô xung quanh. " +
        "Chạm để mở ô, nhấn giữ (hoặc bật chế độ 🚩) để cắm cờ. Chạm vào số đã đủ cờ để mở nhanh các ô còn lại."

    private lateinit var m: Mines
    private val fit = BoardFit()
    private val modeBtn = Btn("⛏ Mở ô", Pal.SURFACE2)
    private var flagMode = false
    private var downCell = -1
    private var downX = 0f; private var downY = 0f
    private var longPressed = false
    private var pressId = 0

    override fun newGame() {
        val (c, r, n) = when (opt("level")) { 0 -> Triple(9, 9, 10); 1 -> Triple(10, 14, 25); else -> Triple(12, 18, 45) }
        m = Mines(c, r, n)
        flagMode = false
        info("💣 $n")
        score(0)
    }

    override fun layout() {
        val pad = 10 * dp
        fit.fit(m.cols, m.rows, pad, pad, w - pad, h - 80 * dp)
        modeBtn.setCenter(w / 2, h - 40 * dp, 170 * dp, 48 * dp)
    }

    private fun cellAt(x: Float, y: Float): Int {
        val c = fit.colAt(x); val r = fit.rowAt(y)
        return if (c < 0 || r < 0) -1 else r * m.cols + c
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        downCell = cellAt(x, y); downX = x; downY = y; longPressed = false
        val id = ++pressId
        if (downCell >= 0) after(0.38f) {
            if (id == pressId && downCell >= 0 && !longPressed) {
                longPressed = true
                if (!m.open[downCell]) { m.toggleFlag(downCell); buzz(30); sfx(Sfx.TAP); updateInfo() }
            }
        }
    }

    override fun move(x: Float, y: Float) {
        if (abs(x - downX) > fit.cell * 0.4f || abs(y - downY) > fit.cell * 0.4f) downCell = -1
    }

    override fun up(x: Float, y: Float) {
        pressId++
        if (over) return
        if (modeBtn.hit(x, y)) { flagMode = !flagMode; sfx(Sfx.CLICK); redraw(); return }
        val cell = downCell
        downCell = -1
        if (cell < 0 || longPressed || cellAt(x, y) != cell) return
        if (!m.placed) startClock()
        if (m.open[cell]) {
            if (!m.chord(cell)) lose() else afterReveal()
        } else if (flagMode) {
            m.toggleFlag(cell); sfx(Sfx.TAP); updateInfo()
        } else if (!m.flag[cell]) {
            if (!m.reveal(cell)) lose() else afterReveal()
        }
        redraw()
    }

    private fun afterReveal() {
        sfx(Sfx.CLICK)
        if (m.won()) {
            for (i in m.mine.indices) if (m.mine[i]) m.flag[i] = true
            gameOver(true, "Gỡ mìn thành công!", "", clockSeconds)
        }
    }

    private fun lose() {
        for (i in m.mine.indices) if (m.mine[i] && !m.flag[i]) m.open[i] = true
        sfx(Sfx.BOOM); buzz(120)
        gameOver(false, "Bùm! Trúng mìn", "Chúc may mắn lần sau")
    }

    private fun updateInfo() = info("💣 ${m.mineCount - m.flags()}")

    private val numColors = intArrayOf(0, 0xFF4D7CFF.toInt(), 0xFF2EAD5B.toInt(), 0xFFFF4D5E.toInt(),
        0xFF6A3FD9.toInt(), 0xFFB5481B.toInt(), 0xFF0F9BAA.toInt(), 0xFF222222.toInt(), 0xFF777777.toInt())

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        val g = cs * 0.06f
        val rad = cs * 0.16f
        for (i in 0 until m.cols * m.rows) {
            val x = fit.x0 + (i % m.cols) * cs; val y = fit.y0 + (i / m.cols) * cs
            if (m.open[i]) {
                c.rrect(x + g, y + g, x + cs - g, y + cs - g, rad, if (i == m.exploded) Pal.RED else 0xFFE9E7F7.toInt())
                if (m.mine[i]) {
                    c.circle(x + cs / 2, y + cs / 2, cs * 0.24f, 0xFF1C1E33.toInt())
                    c.circle(x + cs * 0.43f, y + cs * 0.43f, cs * 0.06f, Pal.WHITE)
                } else if (m.adj[i] > 0) {
                    c.text(m.adj[i].toString(), x + cs / 2, y + cs / 2, cs * 0.58f, numColors[m.adj[i]])
                }
            } else {
                val pressed = i == downCell && !longPressed
                val col = if ((i / m.cols + i % m.cols) % 2 == 0) 0xFF5B6AD0.toInt() else 0xFF5363C4.toInt()
                c.rrect(x + g, y + g + cs * 0.05f, x + cs - g, y + cs - g + cs * 0.05f, rad, 0xFF2F3778.toInt())
                c.rrect(x + g, y + g, x + cs - g, y + cs - g, rad, if (pressed) Pal.shade(col, 0.8f) else col)
                if (m.flag[i]) {
                    val bad = over && !m.mine[i]
                    c.line(x + cs * 0.4f, y + cs * 0.22f, x + cs * 0.4f, y + cs * 0.78f, cs * 0.07f, Pal.WHITE)
                    c.poly(if (bad) Pal.ORANGE else Pal.RED, x + cs * 0.42f, y + cs * 0.22f, x + cs * 0.75f, y + cs * 0.36f, x + cs * 0.42f, y + cs * 0.5f)
                }
            }
        }
        modeBtn.label = if (flagMode) "🚩 Cắm cờ" else "⛏ Mở ô"
        modeBtn.color = if (flagMode) Pal.RED else Pal.SURFACE2
        modeBtn.draw(c, 16 * dp)
    }
}
