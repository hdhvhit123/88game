package com.hgame.games.word

import android.graphics.Canvas
import com.hgame.core.Words
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Keyboard
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.line
import com.hgame.engine.ring
import com.hgame.engine.text
import kotlin.math.min

class HangmanGame : Game() {
    override val help = "Đoán từng chữ cái của từ tiếng Anh bí mật theo chủ đề gợi ý. Mỗi lần đoán sai, hình người treo cổ " +
        "được vẽ thêm một phần. Sai 6 lần là thua. Đoán đúng nhiều từ liên tiếp để tăng điểm."

    private var word = ""
    private var category = ""
    private val guessed = HashSet<Char>()
    private var wrong = 0
    private var points = 0
    private val kb = Keyboard().apply { showEnter = false }

    override fun newGame() {
        points = 0
        score(0)
        nextWord()
    }

    private fun nextWord() {
        val (cat, list) = Words.HANGMAN.entries.random().toPair()
        category = cat; word = list.random().uppercase()
        guessed.clear(); wrong = 0
        kb.colorOf = { k ->
            val ch = k.firstOrNull()
            when {
                k.length != 1 || ch == null || ch !in guessed -> null
                ch in word -> 0xFF3DAA5C.toInt()
                else -> 0xFF5A2A3A.toInt()
            }
        }
        info("Chủ đề: $category")
    }

    override fun layout() {
        val kbH = min(h * 0.28f, 190 * dp)
        kb.layout(6 * dp, h - kbH - 8 * dp, w - 6 * dp, h - 8 * dp)
    }

    override fun down(x: Float, y: Float) { kb.down(x, y); redraw() }

    override fun up(x: Float, y: Float) {
        val k = kb.up(x, y)
        redraw()
        if (k == null || k.length != 1 || over) return
        val ch = k[0]
        if (!guessed.add(ch)) return
        if (ch in word) {
            sfx(Sfx.COIN)
            if (word.all { it in guessed }) {
                points += 10 + (6 - wrong) * 5
                score(points)
                info("Đúng rồi: $word! Từ tiếp theo…")
                sfx(Sfx.WIN)
                after(1.2f) { nextWord() }
            }
        } else {
            wrong++
            sfx(Sfx.ERROR); buzz(40)
            if (wrong >= 6) gameOver(false, "Bị treo cổ!", "Từ đúng là: $word", points)
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val gx = w / 2; val top = 24 * dp
        val gh = min(h * 0.36f, 260 * dp)
        val lc = Pal.WOOD; val lw = 6 * dp
        c.line(gx - gh * 0.5f, top + gh, gx + gh * 0.2f, top + gh, lw, lc)
        c.line(gx - gh * 0.3f, top + gh, gx - gh * 0.3f, top, lw, lc)
        c.line(gx - gh * 0.3f, top, gx + gh * 0.15f, top, lw, lc)
        c.line(gx + gh * 0.15f, top, gx + gh * 0.15f, top + gh * 0.15f, 3 * dp, Pal.MUTED)
        val bx = gx + gh * 0.15f; val pc = Pal.TEXT; val pw = 4 * dp
        if (wrong > 0) c.ring(bx, top + gh * 0.24f, gh * 0.09f, pw, pc)
        if (wrong > 1) c.line(bx, top + gh * 0.33f, bx, top + gh * 0.6f, pw, pc)
        if (wrong > 2) c.line(bx, top + gh * 0.4f, bx - gh * 0.12f, top + gh * 0.5f, pw, pc)
        if (wrong > 3) c.line(bx, top + gh * 0.4f, bx + gh * 0.12f, top + gh * 0.5f, pw, pc)
        if (wrong > 4) c.line(bx, top + gh * 0.6f, bx - gh * 0.1f, top + gh * 0.78f, pw, pc)
        if (wrong > 5) c.line(bx, top + gh * 0.6f, bx + gh * 0.1f, top + gh * 0.78f, pw, pc)
        c.text("Sai: $wrong/6", w - 50 * dp, top + 10 * dp, 14 * dp, if (wrong >= 4) Pal.RED else Pal.MUTED)
        // word slots
        val n = word.length
        val slot = min(40 * dp, (w - 30 * dp) / n)
        val wy = top + gh + 50 * dp
        val sx = (w - slot * n) / 2
        for (i in 0 until n) {
            val x = sx + i * slot + slot / 2
            c.line(x - slot * 0.38f, wy + slot * 0.4f, x + slot * 0.38f, wy + slot * 0.4f, 3 * dp, Pal.LINE)
            val show = word[i] in guessed || over
            if (show) c.text(word[i].toString(), x, wy, slot * 0.7f, if (word[i] in guessed) Pal.TEXT else Pal.RED)
        }
        c.text("Chủ đề: $category", w / 2, wy + slot + 14 * dp, 15 * dp, Pal.CYAN, bold = false)
        kb.draw(c)
        Gfx.rect.setEmpty()
    }
}
