package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Sudoku
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

class SudokuGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("level", "Độ khó", listOf("Dễ", "Trung bình", "Khó", "Chuyên gia"), 1))
    override val help = "Điền các số từ 1 đến 9 sao cho mỗi hàng, mỗi cột và mỗi khối 3×3 đều có đủ 1–9, " +
        "không lặp lại. Chạm một ô rồi chọn số. Bật Ghi chú để đánh dấu các số có thể. Sai quá 3 lần là thua."

    private var puzzle = IntArray(81)
    private var solution = IntArray(81)
    private var grid = IntArray(81)
    private val notes = IntArray(81)        // bitmask of candidates 1..9
    private var sel = -1
    private var mistakes = 0
    private var notesMode = false
    private val fit = BoardFit()
    private val numBtns = (1..9).map { Btn(it.toString(), Pal.SURFACE2) }
    private val noteBtn = Btn("✎ Ghi chú", Pal.SURFACE2)
    private val eraseBtn = Btn("⌫ Xoá", Pal.SURFACE2)
    private val hintBtn = Btn("💡 Gợi ý", Pal.SURFACE2)
    private var errorFlash = -1

    override fun newGame() {
        val clues = intArrayOf(42, 34, 28, 24)[opt("level")]
        val (p, s) = Sudoku.generate(clues)
        puzzle = p; solution = s; grid = p.copyOf()
        notes.fill(0); sel = -1; mistakes = 0; notesMode = false
        info("Lỗi: 0/3")
        startClock()
        score(0)
    }

    override fun layout() {
        val pad = 12 * dp
        val btnArea = min(h * 0.3f, 190 * dp)
        val size = min(w - pad * 2, h - btnArea - pad * 2)
        fit.fit(9, 9, (w - size) / 2, pad, (w + size) / 2, pad + size)
        val top = fit.bottom + 16 * dp
        val bw = (w - pad * 2) / 9
        val bh = min(bw * 1.1f, (h - top) * 0.4f)
        numBtns.forEachIndexed { i, b -> b.set(pad + i * bw + 3 * dp, top, pad + (i + 1) * bw - 3 * dp, top + bh) }
        val ty = top + bh + 14 * dp
        val tw = (w - pad * 2) / 3
        val th = min(46 * dp, h - ty - 8 * dp)
        noteBtn.set(pad + 4 * dp, ty, pad + tw - 4 * dp, ty + th)
        eraseBtn.set(pad + tw + 4 * dp, ty, pad + 2 * tw - 4 * dp, ty + th)
        hintBtn.set(pad + 2 * tw + 4 * dp, ty, pad + 3 * tw - 4 * dp, ty + th)
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c >= 0 && r >= 0) { sel = r * 9 + c; sfx(Sfx.CLICK); redraw(); return }
        numBtns.forEachIndexed { i, b -> if (b.hit(x, y)) { input(i + 1); return } }
        when {
            noteBtn.hit(x, y) -> { notesMode = !notesMode; sfx(Sfx.CLICK) }
            eraseBtn.hit(x, y) -> if (sel >= 0 && puzzle[sel] == 0) { grid[sel] = 0; notes[sel] = 0; sfx(Sfx.TAP) }
            hintBtn.hit(x, y) -> hint()
        }
        redraw()
    }

    private fun hint() {
        val target = if (sel >= 0 && grid[sel] == 0) sel else (0 until 81).filter { grid[it] == 0 }.randomOrNull() ?: return
        grid[target] = solution[target]; notes[target] = 0; sel = target
        sfx(Sfx.COIN)
        checkWin()
    }

    private fun input(v: Int) {
        if (sel < 0 || puzzle[sel] != 0) return
        if (notesMode) {
            if (grid[sel] == 0) notes[sel] = notes[sel] xor (1 shl v)
            sfx(Sfx.CLICK)
            return
        }
        if (v == solution[sel]) {
            grid[sel] = v; notes[sel] = 0
            clearNotes(sel, v)
            sfx(Sfx.POP)
            checkWin()
        } else {
            mistakes++
            errorFlash = sel
            info("Lỗi: $mistakes/3")
            sfx(Sfx.ERROR); buzz(60)
            after(0.6f) { errorFlash = -1 }
            if (mistakes >= 3) {
                grid = solution.copyOf()
                gameOver(false, "Thua rồi", "Bạn đã sai 3 lần")
            }
        }
    }

    private fun clearNotes(idx: Int, v: Int) {
        val r = idx / 9; val c = idx % 9
        for (i in 0 until 81) {
            val rr = i / 9; val cc = i % 9
            if (rr == r || cc == c || (rr / 3 == r / 3 && cc / 3 == c / 3)) notes[i] = notes[i] and (1 shl v).inv()
        }
    }

    private fun checkWin() {
        if (grid.contentEquals(solution)) gameOver(true, "Hoàn thành!", "Độ khó: " + options[0].choices[opt("level")], clockSeconds)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 4 * dp, fit.y0 - 4 * dp, fit.right + 4 * dp, fit.bottom + 4 * dp, 12 * dp, Pal.LINE)
        c.rect(fit.x0, fit.y0, fit.right, fit.bottom, 0xFFF7F6FF.toInt())
        val selV = if (sel >= 0) grid[sel] else 0
        for (i in 0 until 81) {
            val r = i / 9; val col = i % 9
            val x = fit.x0 + col * cs; val y = fit.y0 + r * cs
            var bg = 0
            if (sel >= 0) {
                val sr = sel / 9; val sc = sel % 9
                if (r == sr || col == sc || (r / 3 == sr / 3 && col / 3 == sc / 3)) bg = 0xFFE6E2FF.toInt()
                if (selV != 0 && grid[i] == selV) bg = 0xFFC9BEFF.toInt()
                if (i == sel) bg = 0xFFAE9BFF.toInt()
            }
            if (i == errorFlash) bg = 0xFFFFB3BD.toInt()
            if (bg != 0) c.rect(x, y, x + cs, y + cs, bg)
            val v = grid[i]
            if (v != 0) {
                c.text(v.toString(), x + cs / 2, y + cs / 2, cs * 0.6f,
                    if (puzzle[i] != 0) 0xFF252845.toInt() else Pal.ACCENT, bold = puzzle[i] != 0)
            } else if (notes[i] != 0) {
                for (n in 1..9) if (notes[i] and (1 shl n) != 0) {
                    val nx = x + ((n - 1) % 3 + 0.5f) * cs / 3; val ny = y + ((n - 1) / 3 + 0.5f) * cs / 3
                    c.text(n.toString(), nx, ny, cs * 0.24f, 0xFF7A7F9E.toInt(), bold = false)
                }
            }
        }
        for (k in 0..9) {
            val wdt = if (k % 3 == 0) 2.5f * dp else 1f * dp
            val col = if (k % 3 == 0) 0xFF252845.toInt() else 0xFFB9BCD6.toInt()
            c.line(fit.x0 + k * cs, fit.y0, fit.x0 + k * cs, fit.bottom, wdt, col)
            c.line(fit.x0, fit.y0 + k * cs, fit.right, fit.y0 + k * cs, wdt, col)
        }
        numBtns.forEachIndexed { i, b ->
            val n = i + 1
            val done = grid.count { it == n } >= 9
            b.enabled = !done
            b.color = if (notesMode) 0xFF3A3070.toInt() else Pal.SURFACE2
            b.draw(c, (b.b - b.t) * 0.5f)
        }
        noteBtn.color = if (notesMode) Pal.ACCENT else Pal.SURFACE2
        noteBtn.draw(c, 14 * dp); eraseBtn.draw(c, 14 * dp); hintBtn.draw(c, 14 * dp)
    }
}
