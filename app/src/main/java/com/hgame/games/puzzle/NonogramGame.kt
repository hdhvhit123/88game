package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class NonogramGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Kích thước", listOf("5 × 5", "10 × 10", "15 × 15"), 1))
    override val help = "Các số ở đầu mỗi hàng/cột cho biết độ dài các đoạn ô tô kín liên tiếp theo thứ tự. " +
        "Tô ô bằng chế độ ■, đánh dấu ô chắc chắn trống bằng chế độ ✕. Kéo để tô nhiều ô. Thoả mãn mọi gợi ý để thắng."

    private var n = 10
    private var picture = BooleanArray(100)
    private var state = IntArray(100)          // 0 empty, 1 filled, 2 crossed
    private var rowClues = listOf<List<Int>>(); private var colClues = listOf<List<Int>>()
    private var cell = 0f; private var x0 = 0f; private var y0 = 0f
    private var clueW = 0f; private var clueH = 0f
    private val modeBtn = Btn("■ Tô", Pal.ACCENT)
    private var crossMode = false
    private var paintValue = -1

    override fun newGame() {
        n = intArrayOf(5, 10, 15)[opt("size")]
        val rnd = Random.Default
        // blobby random picture: noise + smoothing so clues look like real pictures
        val noise = FloatArray(n * n) { rnd.nextFloat() }
        picture = BooleanArray(n * n) { i ->
            val r = i / n; val c = i % n
            var s = 0f; var k = 0
            for (dr in -1..1) for (dc in -1..1) { val rr = r + dr; val cc = c + dc; if (rr in 0 until n && cc in 0 until n) { s += noise[rr * n + cc]; k++ } }
            s / k > 0.47f
        }
        if (picture.none { it }) picture[0] = true
        rowClues = (0 until n).map { r -> clue((0 until n).map { picture[r * n + it] }) }
        colClues = (0 until n).map { c -> clue((0 until n).map { picture[it * n + c] }) }
        state = IntArray(n * n); crossMode = false
        startClock(); score(0)
    }

    private fun clue(line: List<Boolean>): List<Int> {
        val out = ArrayList<Int>(); var run = 0
        for (b in line) { if (b) run++ else if (run > 0) { out.add(run); run = 0 } }
        if (run > 0) out.add(run)
        return out.ifEmpty { listOf(0) }
    }

    override fun layout() {
        val maxRow = rowClues.maxOf { it.size }; val maxCol = colClues.maxOf { it.size }

        cell = min((w - 16 * dp) / (n + maxRow * 0.55f), (h - 90 * dp) / (n + maxCol * 0.6f))
        clueW = maxRow * cell * 0.55f; clueH = maxCol * cell * 0.6f
        x0 = (w - (clueW + cell * n)) / 2 + clueW
        y0 = 8 * dp + clueH
        modeBtn.setCenter(w / 2, h - 36 * dp, 160 * dp, 46 * dp)
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        if (modeBtn.hit(x, y)) return
        val i = at(x, y)
        if (i < 0) return
        val target = if (crossMode) 2 else 1
        paintValue = if (state[i] == target) 0 else target
        apply(i)
    }

    override fun move(x: Float, y: Float) { if (paintValue >= 0) { val i = at(x, y); if (i >= 0) apply(i) } }

    override fun up(x: Float, y: Float) {
        if (paintValue < 0 && modeBtn.hit(x, y)) { crossMode = !crossMode; sfx(Sfx.CLICK) }
        paintValue = -1
        check()
        redraw()
    }

    private fun apply(i: Int) {
        if (state[i] == paintValue) return
        state[i] = paintValue
        sfx(Sfx.TICK)
        redraw()
    }

    private fun at(x: Float, y: Float): Int {
        if (x < x0 || y < y0) return -1
        val c = ((x - x0) / cell).toInt(); val r = ((y - y0) / cell).toInt()
        return if (c in 0 until n && r in 0 until n) r * n + c else -1
    }

    private fun rowOk(r: Int) = clue((0 until n).map { state[r * n + it] == 1 }) == rowClues[r]
    private fun colOk(c: Int) = clue((0 until n).map { state[it * n + c] == 1 }) == colClues[c]

    private fun check() {
        if ((0 until n).all { rowOk(it) && colOk(it) }) gameOver(true, "Hoàn thành bức tranh!", "", clockSeconds)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.rect(x0, y0, x0 + n * cell, y0 + n * cell, 0xFFF3F1FF.toInt())
        val ts = cell * 0.42f
        for (r in 0 until n) {
            val ok = rowOk(r)
            rowClues[r].reversed().forEachIndexed { k, v -> c.text(v.toString(), x0 - (k + 0.5f) * cell * 0.55f, y0 + (r + 0.5f) * cell, ts, if (ok) Pal.GREEN else Pal.TEXT) }
        }
        for (col in 0 until n) {
            val ok = colOk(col)
            colClues[col].reversed().forEachIndexed { k, v -> c.text(v.toString(), x0 + (col + 0.5f) * cell, y0 - (k + 0.5f) * cell * 0.6f, ts, if (ok) Pal.GREEN else Pal.TEXT) }
        }
        for (i in 0 until n * n) {
            val x = x0 + (i % n) * cell; val y = y0 + (i / n) * cell
            when (state[i]) {
                1 -> c.rect(x + 1, y + 1, x + cell - 1, y + cell - 1, if (over) Pal.ACCENT else 0xFF2B2E4A.toInt())
                2 -> { c.line(x + cell * 0.3f, y + cell * 0.3f, x + cell * 0.7f, y + cell * 0.7f, 1.5f * dp, Pal.RED); c.line(x + cell * 0.7f, y + cell * 0.3f, x + cell * 0.3f, y + cell * 0.7f, 1.5f * dp, Pal.RED) }
            }
        }
        for (k in 0..n) {
            val wdt = if (k % 5 == 0) 2f * dp else 0.8f * dp
            c.line(x0 + k * cell, y0, x0 + k * cell, y0 + n * cell, wdt, 0xFF7A7FA8.toInt())
            c.line(x0, y0 + k * cell, x0 + n * cell, y0 + k * cell, wdt, 0xFF7A7FA8.toInt())
        }
        modeBtn.label = if (crossMode) "✕ Đánh dấu" else "■ Tô"
        modeBtn.color = if (crossMode) Pal.RED else Pal.ACCENT
        modeBtn.draw(c, 16 * dp)
    }
}
