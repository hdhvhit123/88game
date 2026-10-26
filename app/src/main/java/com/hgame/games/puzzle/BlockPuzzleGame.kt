package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class BlockPuzzleGame : Game() {
    override val help = "Kéo các khối ở dưới thả vào bảng 8×8. Lấp đầy một hàng hoặc một cột để xoá và ghi điểm. " +
        "Xoá nhiều hàng cùng lúc được thưởng combo. Trò chơi kết thúc khi không khối nào đặt vừa."

    private class Piece(val cells: List<Pair<Int, Int>>, val color: Int) {
        val w = cells.maxOf { it.second } + 1
        val h = cells.maxOf { it.first } + 1
    }

    private val shapes: List<List<Pair<Int, Int>>> = listOf(
        listOf(0 to 0), listOf(0 to 0, 0 to 1), listOf(0 to 0, 1 to 0), listOf(0 to 0, 0 to 1, 0 to 2), listOf(0 to 0, 1 to 0, 2 to 0),
        listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3), listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0), listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2),
        listOf(0 to 0, 1 to 0, 1 to 1), listOf(0 to 1, 1 to 0, 1 to 1), listOf(0 to 0, 0 to 1, 1 to 0), listOf(0 to 0, 0 to 1, 1 to 1),
        listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1), listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0), listOf(0 to 1, 1 to 1, 2 to 1, 2 to 0),
        listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1), listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2), listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2),
        listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4), listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0),
    )
    private val colors = intArrayOf(Pal.RED, Pal.ORANGE, Pal.YELLOW, Pal.GREEN, Pal.CYAN, Pal.BLUE, Pal.PURPLE, Pal.PINK)
    private val n = 8
    private val board = IntArray(64)       // 0 empty, else colour + 1 index
    private val hand = arrayOfNulls<Piece>(3)
    private var points = 0
    private var cell = 0f; private var x0 = 0f; private var y0 = 0f
    private var handY = 0f
    private var drag = -1
    private var dx = 0f; private var dy = 0f
    private val flash = FloatArray(64)

    override fun newGame() {
        board.fill(0); points = 0; drag = -1; flash.fill(0f)
        refill()
        score(0)
    }

    private fun refill() {
        for (i in 0..2) { val s = shapes[Random.nextInt(shapes.size)]; hand[i] = Piece(s, colors[Random.nextInt(colors.size)]) }
    }

    override fun layout() {
        cell = min((w - 24 * dp) / n, (h * 0.62f) / n)
        x0 = (w - cell * n) / 2; y0 = 16 * dp
        handY = y0 + cell * n + (h - (y0 + cell * n)) / 2
    }

    private fun handCenter(i: Int) = w * (i + 0.5f) / 3

    private fun fits(p: Piece, r: Int, c: Int) = p.cells.all { (dr, dc) ->
        val rr = r + dr; val cc = c + dc
        rr in 0 until n && cc in 0 until n && board[rr * n + cc] == 0
    }

    private fun anyFit(p: Piece) = (0 until n).any { r -> (0 until n).any { c -> fits(p, r, c) } }

    /** Board cell the dragged piece's top-left would snap to. */
    private fun dropCell(p: Piece): Pair<Int, Int> {
        val left = dx - p.w * cell / 2; val top = dy - p.h * cell - 50 * dp
        return ((top - y0) / cell).roundToInt() to ((left - x0) / cell).roundToInt()
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        if (y < handY - 70 * dp) return
        val i = (x / (w / 3)).toInt().coerceIn(0, 2)
        if (hand[i] == null) return
        drag = i; dx = x; dy = y
        sfx(Sfx.CLICK)
    }

    override fun move(x: Float, y: Float) { if (drag >= 0) { dx = x; dy = y; redraw() } }

    override fun up(x: Float, y: Float) {
        if (drag < 0) return
        val p = hand[drag]!!
        val (r, c) = dropCell(p)
        if (fits(p, r, c)) {
            val ci = colors.indexOf(p.color) + 1
            p.cells.forEach { (dr, dc) -> board[(r + dr) * n + c + dc] = ci }
            points += p.cells.size
            hand[drag] = null
            sfx(Sfx.TAP)
            clearLines()
            if (hand.all { it == null }) refill()
            score(points)
            if (hand.filterNotNull().none { anyFit(it) }) gameOver(false, "Hết chỗ!", "Không còn khối nào đặt vừa", points)
        } else sfx(Sfx.ERROR)
        drag = -1
        redraw()
    }

    private fun clearLines() {
        val rows = (0 until n).filter { r -> (0 until n).all { board[r * n + it] != 0 } }
        val cols = (0 until n).filter { c -> (0 until n).all { board[it * n + c] != 0 } }
        val lines = rows.size + cols.size
        if (lines == 0) return
        for (r in rows) for (c in 0 until n) { flash[r * n + c] = 1f }
        for (c in cols) for (r in 0 until n) { flash[r * n + c] = 1f }
        for (r in rows) for (c in 0 until n) board[r * n + c] = 0
        for (c in cols) for (r in 0 until n) board[r * n + c] = 0
        points += lines * 10 * lines
        sfx(if (lines > 1) Sfx.WIN else Sfx.MERGE)
        if (lines > 1) info("Combo ×$lines!")
        animate(0.6f)
    }

    override fun update(dt: Float) { for (i in flash.indices) if (flash[i] > 0f) flash[i] = maxOf(0f, flash[i] - dt * 2.5f) }

    private fun block(c: Canvas, x: Float, y: Float, s: Float, color: Int) {
        c.rrect(x + 1, y + 1 + s * 0.06f, x + s - 1, y + s - 1 + s * 0.06f, s * 0.18f, Pal.shade(color, 0.55f))
        c.rrect(x + 1, y + 1, x + s - 1, y + s - 1, s * 0.18f, color)
        c.rrect(x + s * 0.18f, y + s * 0.14f, x + s * 0.82f, y + s * 0.4f, s * 0.1f, Pal.alpha(Pal.WHITE, 0.22f))
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.rrect(x0 - 6 * dp, y0 - 6 * dp, x0 + cell * n + 6 * dp, y0 + cell * n + 6 * dp, 14 * dp, Pal.SURFACE)
        val p = if (drag >= 0) hand[drag] else null
        val preview = p?.let { dropCell(it) }?.takeIf { fits(p, it.first, it.second) }
        for (i in 0 until 64) {
            val x = x0 + (i % n) * cell; val y = y0 + (i / n) * cell
            c.rrect(x + 2, y + 2, x + cell - 2, y + cell - 2, cell * 0.16f, 0xFF262B4F.toInt())
            if (board[i] != 0) block(c, x, y, cell, colors[board[i] - 1])
            if (flash[i] > 0f) c.rrect(x, y, x + cell, y + cell, cell * 0.16f, Pal.alpha(Pal.WHITE, flash[i] * 0.8f))
        }
        if (p != null && preview != null) p.cells.forEach { (dr, dc) ->
            val x = x0 + (preview.second + dc) * cell; val y = y0 + (preview.first + dr) * cell
            c.rrect(x + 2, y + 2, x + cell - 2, y + cell - 2, cell * 0.16f, Pal.alpha(p.color, 0.35f))
        }
        for (i in 0..2) {
            val piece = hand[i] ?: continue
            if (i == drag) continue
            val s = cell * 0.5f
            val canFit = anyFit(piece)
            val ox = handCenter(i) - piece.w * s / 2; val oy = handY - piece.h * s / 2
            piece.cells.forEach { (dr, dc) -> block(c, ox + dc * s, oy + dr * s, s, if (canFit) piece.color else 0xFF4A4F70.toInt()) }
        }
        if (p != null) {
            val left = dx - p.w * cell / 2; val top = dy - p.h * cell - 50 * dp
            p.cells.forEach { (dr, dc) -> block(c, left + dc * cell, top + dr * cell, cell, p.color) }
        }
        if (points == 0 && drag < 0) c.text("Kéo khối lên bảng", w / 2, handY + 60 * dp, 13 * dp, Pal.MUTED, bold = false)
    }
}
