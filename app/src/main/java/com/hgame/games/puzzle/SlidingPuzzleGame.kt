package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class SlidingPuzzleGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Kích thước", listOf("4 × 4 (15 ô)", "3 × 3 (8 ô)", "5 × 5 (24 ô)")))
    override val help = "Chạm vào ô nằm cùng hàng hoặc cột với chỗ trống để trượt nó. Sắp xếp các số theo thứ tự từ 1."

    private var n = 4
    private var tiles = IntArray(16)       // 0 = blank
    private var pos = FloatArray(32)       // animated x,y (in cells) per tile value
    private val fit = BoardFit()
    private var moves = 0

    override fun newGame() {
        n = intArrayOf(4, 3, 5)[opt("size")]
        tiles = IntArray(n * n) { (it + 1) % (n * n) }
        // random walk of the blank keeps it solvable
        var blank = n * n - 1; var prev = -1
        repeat(n * n * 40) {
            val opts = neighbors(blank).filter { it != prev }
            val k = opts[Random.nextInt(opts.size)]
            tiles[blank] = tiles[k]; tiles[k] = 0; prev = blank; blank = k
        }
        pos = FloatArray(n * n * 2)
        for (i in tiles.indices) { pos[tiles[i] * 2] = (i % n).toFloat(); pos[tiles[i] * 2 + 1] = (i / n).toFloat() }
        moves = 0
        info("Bước: 0")
        score(0)
    }

    private fun neighbors(i: Int) = buildList {
        val r = i / n; val c = i % n
        if (r > 0) add(i - n); if (r < n - 1) add(i + n); if (c > 0) add(i - 1); if (c < n - 1) add(i + 1)
    }

    override fun layout() {
        val size = min(w, h) - 32 * dp
        fit.fit(n, n, (w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val blank = tiles.indexOf(0)
        val br = blank / n; val bc = blank % n
        if (r != br && c != bc || (r == br && c == bc)) return
        if (moves == 0) startClock()
        // slide a whole row/column segment towards the blank
        var cur = blank
        val step = if (r == br) (if (c > bc) 1 else -1) else (if (r > br) n else -n)
        val target = r * n + c
        while (cur != target) {
            val nxt = cur + step
            tiles[cur] = tiles[nxt]; tiles[nxt] = 0; cur = nxt
        }
        moves++
        info("Bước: $moves")
        sfx(Sfx.TAP)
        animate(0.3f)
        if ((0 until n * n - 1).all { tiles[it] == it + 1 }) gameOver(true, "Xếp xong!", "$moves bước", clockSeconds)
    }

    override fun update(dt: Float) {
        val k = min(1f, dt * 18f)
        for (i in tiles.indices) {
            val v = tiles[i]
            pos[v * 2] += ((i % n) - pos[v * 2]) * k
            pos[v * 2 + 1] += ((i / n) - pos[v * 2 + 1]) * k
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        val g = cs * 0.05f
        c.rrect(fit.x0 - 8 * dp, fit.y0 - 8 * dp, fit.right + 8 * dp, fit.bottom + 8 * dp, 18 * dp, Pal.SURFACE)
        for (v in 1 until n * n) {
            val x = fit.x0 + pos[v * 2] * cs; val y = fit.y0 + pos[v * 2 + 1] * cs
            val correct = tiles[v - 1] == v
            val hue = (v - 1) * 300f / (n * n)
            val col = Pal.hsv(250f + hue * 0.4f, 0.55f, if (correct) 0.95f else 0.75f)
            c.rrect(x + g, y + g + 4 * dp, x + cs - g, y + cs - g + 4 * dp, cs * 0.14f, Pal.shade(col, 0.55f))
            c.rrect(x + g, y + g, x + cs - g, y + cs - g, cs * 0.14f, col)
            c.text(v.toString(), x + cs / 2, y + cs / 2, cs * 0.42f, Pal.WHITE)
        }
    }
}
