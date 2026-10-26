package com.hgame.games.word

import android.graphics.Canvas
import com.hgame.core.Words
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.Keyboard
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rrect
import com.hgame.engine.rrectStroke
import com.hgame.engine.text
import kotlin.math.min
import kotlin.math.sin

class WordGuessGame : Game() {
    override val bestMode = BestMode.HIGH
    override val help = "Đoán từ tiếng Anh gồm 5 chữ cái trong 6 lượt. Sau mỗi lượt: xanh lá = đúng chữ đúng vị trí, " +
        "vàng = có chữ đó nhưng sai vị trí, xám = không có trong từ."

    private val green = 0xFF3DAA5C.toInt(); private val yellow = 0xFFD4A72C.toInt(); private val gray = 0xFF3A3F5C.toInt()
    private var answer = ""
    private val rows = ArrayList<String>()
    private var current = ""
    private val kb = Keyboard()
    private val keyState = HashMap<String, Int>()
    private var streak = 0
    private var shake = 0f
    private var reveal = 1f
    private var cell = 0f; private var x0 = 0f; private var y0 = 0f

    override fun newGame() {
        answer = Words.FIVE.random().uppercase()
        rows.clear(); current = ""; keyState.clear(); reveal = 1f
        streak = host.loadInt("streak", 0)
        info("Chuỗi thắng: $streak")
        kb.colorOf = { k -> keyState[k] }
    }

    override fun layout() {
        val kbH = min(h * 0.3f, 200 * dp)
        kb.layout(6 * dp, h - kbH - 8 * dp, w - 6 * dp, h - 8 * dp)
        cell = min((w - 60 * dp) / 5, (h - kbH - 40 * dp) / 6)
        x0 = (w - cell * 5) / 2; y0 = 16 * dp + (h - kbH - 30 * dp - cell * 6) / 2
    }

    override fun down(x: Float, y: Float) { kb.down(x, y); redraw() }

    override fun up(x: Float, y: Float) {
        val k = kb.up(x, y)
        redraw()
        if (k == null || over) return
        when (k) {
            "DEL" -> { if (current.isNotEmpty()) current = current.dropLast(1); sfx(Sfx.CLICK) }
            "ENTER" -> submit()
            else -> if (current.length < 5) { current += k; sfx(Sfx.TAP) }
        }
    }

    private fun submit() {
        if (current.length < 5) { shake = 0.4f; animate(0.4f); sfx(Sfx.ERROR); return }
        val g = current
        rows.add(g); current = ""
        val marks = marks(g)
        for (i in 0 until 5) {
            val key = g[i].toString()
            val col = when (marks[i]) { 2 -> green; 1 -> yellow; else -> gray }
            val prev = keyState[key]
            if (prev == null || rank(col) > rank(prev)) keyState[key] = col
        }
        reveal = 0f; animate(1.2f)
        sfx(Sfx.POP)
        if (g == answer) {
            streak++; host.saveInt("streak", streak)
            score(streak)
            after(1.1f) { gameOver(true, listOf("Thiên tài!", "Xuất sắc!", "Tuyệt vời!", "Giỏi lắm!", "Tốt!", "Phù!")[rows.size - 1], "Từ: $answer") }
        } else if (rows.size >= 6) {
            streak = 0; host.saveInt("streak", 0)
            after(1.1f) { gameOver(false, "Tiếc quá!", "Từ đúng là: $answer") }
        }
    }

    private fun rank(c: Int) = when (c) { green -> 3; yellow -> 2; else -> 1 }

    /** 2 = right place, 1 = present elsewhere, 0 = absent (handles repeated letters). */
    private fun marks(g: String): IntArray {
        val m = IntArray(5)
        val left = IntArray(26)
        for (i in 0 until 5) if (g[i] == answer[i]) m[i] = 2 else left[answer[i] - 'A']++
        for (i in 0 until 5) if (m[i] == 0 && left[g[i] - 'A'] > 0) { m[i] = 1; left[g[i] - 'A']-- }
        return m
    }

    override fun update(dt: Float) {
        if (shake > 0f) shake -= dt
        if (reveal < 1f) reveal = min(1f, reveal + dt / 1.0f)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val g = cell * 0.08f
        for (r in 0 until 6) {
            val word = when { r < rows.size -> rows[r]; r == rows.size -> current; else -> "" }
            val done = r < rows.size
            val marks = if (done) marks(word) else null
            val dx = if (r == rows.size && shake > 0f) sin(shake * 60f) * 8 * dp else 0f
            for (i in 0 until 5) {
                val l = x0 + i * cell + g + dx; val t = y0 + r * cell + g
                val ch = word.getOrNull(i)?.toString() ?: ""
                val isLastRow = r == rows.size - 1
                val revealed = !isLastRow || reveal * 5 > i + 0.5f
                if (done && revealed) {
                    val col = when (marks!![i]) { 2 -> green; 1 -> yellow; else -> gray }
                    c.rrect(l, t, l + cell - 2 * g, t + cell - 2 * g, cell * 0.12f, col)
                } else {
                    c.rrect(l, t, l + cell - 2 * g, t + cell - 2 * g, cell * 0.12f, Pal.SURFACE)
                    c.rrectStroke(l, t, l + cell - 2 * g, t + cell - 2 * g, cell * 0.12f, 2 * dp, if (ch.isNotEmpty()) 0xFF6B7299.toInt() else Pal.LINE)
                }
                if (ch.isNotEmpty()) c.text(ch, l + cell / 2 - g, t + cell / 2 - g, cell * 0.5f, Pal.WHITE)
            }
        }
        kb.draw(c)
    }
}
