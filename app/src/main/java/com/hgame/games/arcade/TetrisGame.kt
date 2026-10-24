package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.rrectStroke
import com.hgame.engine.text
import com.hgame.engine.tile
import kotlin.math.abs
import kotlin.math.min

class TetrisGame : Game() {
    override val realtime = true
    override val help = "Chạm vào bàn để xoay khối, vuốt trái/phải để di chuyển, vuốt xuống để rơi nhanh, vuốt lên để thả ngay. " +
        "Hoặc dùng các nút bên dưới. Lấp đầy hàng để xoá; xoá nhiều hàng một lúc được thưởng lớn."

    // 7 tetrominoes, each as 4 rotations of 4 cells encoded in a 4x4 box
    private val shapes = arrayOf(
        intArrayOf(0x0F00, 0x2222, 0x00F0, 0x4444), // I
        intArrayOf(0x8E00, 0x6440, 0x0E20, 0x44C0), // J
        intArrayOf(0x2E00, 0x4460, 0x0E80, 0xC440), // L
        intArrayOf(0x6600, 0x6600, 0x6600, 0x6600), // O
        intArrayOf(0x6C00, 0x4620, 0x06C0, 0x8C40), // S
        intArrayOf(0x4E00, 0x4640, 0x0E40, 0x4C40), // T
        intArrayOf(0xC600, 0x2640, 0x0C60, 0x4C80), // Z
    )
    private val colors = intArrayOf(Pal.CYAN, Pal.BLUE, Pal.ORANGE, Pal.YELLOW, Pal.GREEN, Pal.PURPLE, Pal.RED)
    private val cols = 10; private val rows = 20
    private val grid = IntArray(cols * rows) { -1 }
    private var piece = 0; private var rot = 0; private var px = 0; private var py = 0
    private var next = 0; private var hold = -1; private var holdUsed = false
    private var bag = ArrayList<Int>()
    private var fallAcc = 0f; private var lockAcc = 0f
    private var points = 0; private var lines = 0; private var level = 1
    private var cell = 0f; private var bx = 0f; private var by = 0f
    private var clearing = IntArray(0); private var clearT = 0f
    private val fx = Particles()
    private var downX = 0f; private var downY = 0f; private var lastX = 0f; private var moved = false; private var soft = false
    private val btnL = Btn("◀", Pal.SURFACE2); private val btnR = Btn("▶", Pal.SURFACE2)
    private val btnRot = Btn("⟳", Pal.ACCENT); private val btnDrop = Btn("⤓", Pal.PINK); private val btnHold = Btn("Giữ", Pal.SURFACE2)
    private var heldBtn: Btn? = null; private var repeatT = 0f

    private fun cells(p: Int, r: Int): IntArray {
        val m = shapes[p][r]; val out = IntArray(8); var k = 0
        for (i in 0 until 16) if (m and (0x8000 shr i) != 0) { out[k++] = i % 4; out[k++] = i / 4 }
        return out
    }

    private fun fits(p: Int, r: Int, x: Int, y: Int): Boolean {
        val cs = cells(p, r)
        for (i in 0 until 4) {
            val cx = x + cs[i * 2]; val cy = y + cs[i * 2 + 1]
            if (cx < 0 || cx >= cols || cy >= rows) return false
            if (cy >= 0 && grid[cy * cols + cx] >= 0) return false
        }
        return true
    }

    private fun draw7(): Int { if (bag.isEmpty()) { bag = ArrayList((0..6).shuffled()) }; return bag.removeAt(0) }

    override fun newGame() {
        grid.fill(-1); bag.clear(); hold = -1; points = 0; lines = 0; level = 1
        next = draw7(); spawn(); clearing = IntArray(0); fx.clear()
        score(0); status()
    }

    private fun status() = info("Cấp $level · $lines hàng")

    private fun spawn() {
        piece = next; next = draw7(); rot = 0; px = 3; py = -1; holdUsed = false; fallAcc = 0f; lockAcc = 0f
        if (!fits(piece, rot, px, py)) { sfx(Sfx.LOSE); gameOver(false, "Hết chỗ!", "$lines hàng · cấp $level", points) }
    }

    override fun layout() {
        val ctrlH = 70 * dp
        cell = min((w - 110 * dp) / cols, (h - ctrlH - 16 * dp) / rows)
        bx = 10 * dp + (w - 110 * dp - cell * cols) / 2; by = 8 * dp
        val y = h - ctrlH / 2 - 4 * dp; val bw = (w - 6 * 8 * dp) / 5
        listOf(btnHold, btnL, btnRot, btnR, btnDrop).forEachIndexed { i, b -> b.setCenter(8 * dp + bw / 2 + i * (bw + 8 * dp), y, bw, ctrlH - 14 * dp) }
    }

    private fun tryMove(dx: Int, dy: Int): Boolean {
        if (fits(piece, rot, px + dx, py + dy)) { px += dx; py += dy; if (dx != 0) lockAcc = 0f; return true }
        return false
    }

    private fun rotate() {
        val nr = (rot + 1) % 4
        for (k in intArrayOf(0, -1, 1, -2, 2)) if (fits(piece, nr, px + k, py)) { px += k; rot = nr; lockAcc = 0f; sfx(Sfx.TICK); return }
        if (fits(piece, nr, px, py - 1)) { py--; rot = nr; sfx(Sfx.TICK) }
    }

    private fun hardDrop() {
        var d = 0
        while (tryMove(0, 1)) d++
        points += d * 2; lock()
    }

    private fun holdPiece() {
        if (holdUsed) return
        val cur = piece
        if (hold < 0) { hold = cur; spawn() } else { piece = hold; hold = cur; rot = 0; px = 3; py = -1 }
        holdUsed = true; sfx(Sfx.CLICK)
    }

    private fun lock() {
        val cs = cells(piece, rot)
        for (i in 0 until 4) {
            val cy = py + cs[i * 2 + 1]
            if (cy < 0) { sfx(Sfx.LOSE); gameOver(false, "Hết chỗ!", "$lines hàng · cấp $level", points); return }
            grid[cy * cols + px + cs[i * 2]] = piece
        }
        sfx(Sfx.TAP)
        val full = (0 until rows).filter { r -> (0 until cols).all { grid[r * cols + it] >= 0 } }
        if (full.isNotEmpty()) {
            clearing = full.toIntArray(); clearT = 0.25f
            points += intArrayOf(0, 100, 300, 500, 800)[full.size] * level
            lines += full.size; level = 1 + lines / 10
            sfx(if (full.size >= 4) Sfx.WIN else Sfx.MERGE); buzz(20)
            for (r in full) for (cc in 0 until cols) fx.burst(bx + (cc + 0.5f) * cell, by + (r + 0.5f) * cell, colors[grid[r * cols + cc]], 3, 140 * dp, 3 * dp)
            score(points); status()
        } else spawn()
    }

    private fun finishClear() {
        val rowsLeft = (0 until rows).filter { it !in clearing }
        val copy = grid.copyOf(); grid.fill(-1)
        var dst = rows - 1
        for (r in rowsLeft.reversed()) { for (c in 0 until cols) grid[dst * cols + c] = copy[r * cols + c]; dst-- }
        clearing = IntArray(0); spawn()
    }

    override fun update(dt: Float) {
        fx.update(dt)
        if (clearing.isNotEmpty()) { clearT -= dt; if (clearT <= 0) finishClear(); return }
        heldBtn?.let { b ->
            repeatT -= dt
            if (repeatT <= 0f) { repeatT = 0.07f; if (b === btnL) tryMove(-1, 0) else if (b === btnR) tryMove(1, 0) }
        }
        val interval = if (soft) 0.03f else (0.8f * Math.pow(0.85, (level - 1).toDouble()).toFloat()).coerceAtLeast(0.06f)
        fallAcc += dt
        if (fallAcc >= interval) {
            fallAcc = 0f
            if (tryMove(0, 1)) { if (soft) points++ } else lockAcc += interval
        }
        if (!fits(piece, rot, px, py + 1)) { lockAcc += dt; if (lockAcc > 0.5f) lock() }
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        for (b in listOf(btnHold, btnL, btnRot, btnR, btnDrop)) if (b.hit(x, y)) {
            b.pressed = true; heldBtn = b; repeatT = 0.2f
            when (b) { btnL -> tryMove(-1, 0); btnR -> tryMove(1, 0); btnRot -> rotate(); btnDrop -> hardDrop(); btnHold -> holdPiece() }
            return
        }
        downX = x; downY = y; lastX = x; moved = false; soft = false
    }

    override fun move(x: Float, y: Float) {
        if (heldBtn != null || over || clearing.isNotEmpty()) return
        while (x - lastX > cell) { tryMove(1, 0); lastX += cell; moved = true }
        while (lastX - x > cell) { tryMove(-1, 0); lastX -= cell; moved = true }
        if (y - downY > cell * 2 && abs(y - downY) > abs(x - downX)) { soft = true; moved = true }
    }

    override fun up(x: Float, y: Float) {
        val b = heldBtn
        if (b != null) { b.pressed = false; heldBtn = null; return }
        if (over || clearing.isNotEmpty()) { soft = false; return }
        val dy = y - downY
        if (!moved && abs(x - downX) < 12 * dp && abs(dy) < 12 * dp) rotate()
        else if (dy < -cell * 2.5f && abs(dy) > abs(x - downX)) hardDrop()
        soft = false
    }

    private fun block(c: Canvas, x: Float, y: Float, s: Float, color: Int, alpha: Float = 1f) {
        val col = Pal.alpha(color, alpha)
        c.tile(x + 1, y + 1, x + s - 1, y + s - 1, s * 0.18f, col, s * 0.1f)
        c.rect(x + s * 0.2f, y + s * 0.18f, x + s * 0.55f, y + s * 0.28f, Pal.alpha(Pal.WHITE, 0.35f * alpha))
    }

    private fun mini(c: Canvas, p: Int, cx: Float, cy: Float, s: Float) {
        val cs = cells(p, 0)
        var minX = 4; var maxX = 0; var minY = 4; var maxY = 0
        for (i in 0 until 4) { minX = min(minX, cs[i * 2]); maxX = maxOf(maxX, cs[i * 2]); minY = min(minY, cs[i * 2 + 1]); maxY = maxOf(maxY, cs[i * 2 + 1]) }
        val ox = cx - (maxX - minX + 1) * s / 2 - minX * s; val oy = cy - (maxY - minY + 1) * s / 2 - minY * s
        for (i in 0 until 4) block(c, ox + cs[i * 2] * s, oy + cs[i * 2 + 1] * s, s, colors[p])
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val bw = cell * cols; val bh = cell * rows
        c.rrect(bx - 5 * dp, by - 5 * dp, bx + bw + 5 * dp, by + bh + 5 * dp, 10 * dp, Pal.LINE)
        c.rect(bx, by, bx + bw, by + bh, 0xFF0D1124.toInt())
        for (r in 0 until rows) for (cc in 0 until cols) {
            val x = bx + cc * cell; val y = by + r * cell
            val v = grid[r * cols + cc]
            if (v >= 0) {
                if (r in clearing) c.rect(x, y, x + cell, y + cell, Pal.alpha(Pal.WHITE, 0.5f + clearT * 2))
                else block(c, x, y, cell, colors[v])
            } else c.rect(x + cell * 0.47f, y + cell * 0.47f, x + cell * 0.53f, y + cell * 0.53f, 0xFF262C4F.toInt())
        }
        if (!over && clearing.isEmpty()) {
            var gy = py; while (fits(piece, rot, px, gy + 1)) gy++
            val cs = cells(piece, rot)
            for (i in 0 until 4) {
                val gx = bx + (px + cs[i * 2]) * cell; val gyy = by + (gy + cs[i * 2 + 1]) * cell
                if (gy + cs[i * 2 + 1] >= 0) c.rrectStroke(gx + 2, gyy + 2, gx + cell - 2, gyy + cell - 2, cell * 0.18f, 1.5f * dp, Pal.alpha(colors[piece], 0.6f))
            }
            for (i in 0 until 4) {
                val yy = py + cs[i * 2 + 1]
                if (yy >= 0) block(c, bx + (px + cs[i * 2]) * cell, by + yy * cell, cell, colors[piece])
            }
        }
        // side panel
        val sx = bx + bw + 14 * dp; val pw = w - sx - 8 * dp
        c.text("TIẾP", sx + pw / 2, by + 12 * dp, 12 * dp, Pal.MUTED)
        c.rrect(sx, by + 22 * dp, sx + pw, by + 22 * dp + pw * 0.8f, 10 * dp, Pal.SURFACE)
        mini(c, next, sx + pw / 2, by + 22 * dp + pw * 0.4f, pw / 5.5f)
        val hy = by + 40 * dp + pw * 0.8f
        c.text("GIỮ", sx + pw / 2, hy, 12 * dp, Pal.MUTED)
        c.rrect(sx, hy + 10 * dp, sx + pw, hy + 10 * dp + pw * 0.8f, 10 * dp, Pal.SURFACE)
        if (hold >= 0) mini(c, hold, sx + pw / 2, hy + 10 * dp + pw * 0.4f, pw / 5.5f)
        val iy = hy + 30 * dp + pw * 0.8f
        c.text("CẤP", sx + pw / 2, iy, 12 * dp, Pal.MUTED); c.text("$level", sx + pw / 2, iy + 20 * dp, 20 * dp, Pal.YELLOW)
        c.text("HÀNG", sx + pw / 2, iy + 50 * dp, 12 * dp, Pal.MUTED); c.text("$lines", sx + pw / 2, iy + 70 * dp, 20 * dp, Pal.CYAN)
        fx.draw(c)
        btnHold.enabled = !holdUsed
        for (b in listOf(btnHold, btnL, btnRot, btnR, btnDrop)) b.draw(c, if (b === btnHold) 15 * dp else 24 * dp)
    }
}
