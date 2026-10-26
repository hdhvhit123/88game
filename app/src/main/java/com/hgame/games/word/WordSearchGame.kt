package com.hgame.games.word

import android.graphics.Canvas
import com.hgame.core.WordSearch
import com.hgame.core.Words
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.line
import com.hgame.engine.rrect
import com.hgame.engine.text
import com.hgame.engine.textWidth
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class WordSearchGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Kích thước", listOf("10 × 10", "8 × 8", "12 × 12")))
    override val help = "Tìm các từ tiếng Anh trong bảng chữ. Từ có thể nằm ngang, dọc, chéo, xuôi hoặc ngược. " +
        "Kéo ngón tay từ chữ đầu đến chữ cuối của từ để khoanh."

    private lateinit var ws: WordSearch
    private var theme = ""
    private val found = LinkedHashMap<String, Int>()
    private val fit = BoardFit()
    private var a = -1; private var b = -1
    private val lineColors = intArrayOf(Pal.PINK, Pal.CYAN, Pal.GREEN, Pal.YELLOW, Pal.ORANGE, Pal.PURPLE, Pal.BLUE, Pal.RED)

    override fun newGame() {
        val n = intArrayOf(10, 8, 12)[opt("size")]
        val (t, list) = Words.SEARCH_THEMES.entries.random().toPair()
        theme = t
        ws = WordSearch(n, list.shuffled().take(if (n == 8) 6 else if (n == 10) 8 else 10))
        found.clear(); a = -1; b = -1
        startClock(); score(0)
        info("Chủ đề: $theme · 0/${ws.placed.size}")
    }

    override fun layout() {
        val size = min(w - 16 * dp, h * 0.68f)
        fit.fit(ws.n, ws.n, (w - size) / 2, 10 * dp, (w + size) / 2, 10 * dp + size)
    }

    private fun cellAt(x: Float, y: Float): Int {
        val c = fit.colAt(x); val r = fit.rowAt(y)
        return if (c < 0 || r < 0) -1 else r * ws.n + c
    }

    /** Cells on the straight line a→b, or empty if not a valid direction. */
    private fun lineCells(a: Int, b: Int): List<Int> {
        val n = ws.n
        val dr = b / n - a / n; val dc = b % n - a % n
        if (!(dr == 0 || dc == 0 || abs(dr) == abs(dc))) return emptyList()
        val len = max(abs(dr), abs(dc))
        val sr = Integer.signum(dr); val sc = Integer.signum(dc)
        return (0..len).map { (a / n + sr * it) * n + a % n + sc * it }
    }

    override fun down(x: Float, y: Float) { if (!over) { a = cellAt(x, y); b = a; redraw() } }
    override fun move(x: Float, y: Float) { if (a >= 0) { val c = cellAt(x, y); if (c >= 0) b = c; redraw() } }

    override fun up(x: Float, y: Float) {
        if (a < 0) return
        val cells = lineCells(a, b)
        val text = cells.map { ws.grid[it] }.joinToString("")
        val hit = ws.placed.entries.firstOrNull { (wd, pos) -> wd !in found && (pos == cells || pos == cells.reversed()) }
        if (hit != null) {
            found[hit.key] = found.size
            sfx(Sfx.COIN)
            info("Chủ đề: $theme · ${found.size}/${ws.placed.size}")
            if (found.size == ws.placed.size) gameOver(true, "Tìm đủ từ!", "Chủ đề: $theme", clockSeconds)
        } else if (cells.size > 1) sfx(Sfx.ERROR)
        a = -1; b = -1
        if (text.isEmpty()) return
        redraw()
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 6 * dp, fit.y0 - 6 * dp, fit.right + 6 * dp, fit.bottom + 6 * dp, 12 * dp, 0xFFF4F2FF.toInt())
        for ((wd, k) in found) {
            val pos = ws.placed[wd]!!
            c.line(fit.cx(pos.first() % ws.n), fit.cy(pos.first() / ws.n), fit.cx(pos.last() % ws.n), fit.cy(pos.last() / ws.n),
                cs * 0.72f, Pal.alpha(lineColors[k % lineColors.size], 0.45f))
        }
        if (a >= 0) {
            val cells = lineCells(a, b)
            if (cells.isNotEmpty()) c.line(fit.cx(cells.first() % ws.n), fit.cy(cells.first() / ws.n), fit.cx(cells.last() % ws.n), fit.cy(cells.last() / ws.n),
                cs * 0.72f, Pal.alpha(Pal.ACCENT, 0.4f))
        }
        for (i in ws.grid.indices) c.text(ws.grid[i].toString(), fit.cx(i % ws.n), fit.cy(i / ws.n), cs * 0.55f, 0xFF262945.toInt())
        // word list
        var x = 16 * dp; var y = fit.bottom + 30 * dp
        val ts = 15 * dp
        for (wd in ws.placed.keys) {
            val tw = textWidth(wd, ts) + 18 * dp
            if (x + tw > w - 12 * dp) { x = 16 * dp; y += 30 * dp }
            val got = wd in found
            c.rrect(x, y - 12 * dp, x + tw - 6 * dp, y + 12 * dp, 10 * dp, if (got) Pal.alpha(lineColors[found[wd]!! % lineColors.size], 0.5f) else Pal.SURFACE)
            c.text(wd, x + (tw - 6 * dp) / 2, y, ts, if (got) Pal.MUTED else Pal.TEXT)
            if (got) c.line(x + 8 * dp, y, x + tw - 14 * dp, y, 2 * dp, Pal.TEXT)
            x += tw + 2 * dp
        }
    }
}
