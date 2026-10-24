package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.RushHour
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class BlockEscapeGame : Game() {
    override val bestMode = BestMode.HIGH
    override val options = listOf(GameOption("level", "Độ khó", listOf("Dễ", "Vừa", "Khó"), 1))
    override val help = "Kéo các khối gỗ trượt theo chiều dài của chúng để mở đường cho khối đỏ thoát ra cửa bên phải. " +
        "Khối nằm ngang chỉ trượt ngang, khối đứng chỉ trượt dọc."

    private var rh: RushHour? = null
    private var optimal = 0
    private val fit = BoardFit()
    private var drag = -1
    private var dragStart = 0f; private var dragPos0 = 0
    private var dragOffset = 0f
    private var dragRange = 0..0
    private var moves = 0
    private var solvedCount = 0
    private var exitAnim = -1f

    override fun newGame() {
        rh = null; drag = -1; moves = 0; exitAnim = -1f
        solvedCount = host.loadInt("solved", 0)
        info("Đang tạo màn chơi…")
        val target = intArrayOf(5, 10, 15)[opt("level")]
        background({ RushHour.generate(target, Random.Default) }) { (puzzle, m) ->
            rh = puzzle; optimal = m
            info("Bước 0 · Tối ưu $m")
        }
    }

    override fun layout() {
        val size = min(w - 40 * dp, h - 60 * dp)
        fit.fit(6, 6, (w - size) / 2, (h - size) / 2, (w + size) / 2, (h + size) / 2)
    }

    override fun down(x: Float, y: Float) {
        val p = rh ?: return
        if (over || exitAnim >= 0f) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val i = p.occupancy()[r * 6 + c]
        if (i < 0) return
        drag = i; dragPos0 = p.blocks[i].pos
        dragStart = if (p.blocks[i].horizontal) x else y
        dragOffset = 0f
        dragRange = p.range(i)
        sfx(Sfx.CLICK)
    }

    override fun move(x: Float, y: Float) {
        val p = rh ?: return
        if (drag < 0) return
        val b = p.blocks[drag]
        val delta = ((if (b.horizontal) x else y) - dragStart) / fit.cell
        dragOffset = (dragPos0 + delta).coerceIn(dragRange.first.toFloat(), dragRange.last.toFloat()) - dragPos0
        redraw()
    }

    override fun up(x: Float, y: Float) {
        val p = rh ?: return
        if (drag < 0) return
        val b = p.blocks[drag]
        val np = (dragPos0 + dragOffset).roundToInt().coerceIn(dragRange)
        if (np != b.pos) { b.pos = np; moves++; sfx(Sfx.TAP); info("Bước $moves · Tối ưu $optimal") }
        drag = -1; dragOffset = 0f
        if (p.solved()) {
            exitAnim = 0f; animate(1f)
            sfx(Sfx.WIN)
            solvedCount++; host.saveInt("solved", solvedCount)
            score(solvedCount)
            after(0.7f) { gameOver(true, "Thoát được rồi!", "$moves bước (tối ưu $optimal)") }
        }
        redraw()
    }

    override fun update(dt: Float) { if (exitAnim >= 0f) exitAnim += dt * 3f }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        c.rrect(fit.x0 - 10 * dp, fit.y0 - 10 * dp, fit.right + 10 * dp, fit.bottom + 10 * dp, 16 * dp, 0xFF4E342E.toInt())
        c.rect(fit.x0, fit.y0, fit.right, fit.bottom, 0xFFD7B98E.toInt())
        for (k in 1 until 6) {
            c.rect(fit.x0 + k * cs - 0.5f, fit.y0, fit.x0 + k * cs + 0.5f, fit.bottom, 0x22000000)
            c.rect(fit.x0, fit.y0 + k * cs - 0.5f, fit.right, fit.y0 + k * cs + 0.5f, 0x22000000)
        }
        // exit gap
        c.rect(fit.right, fit.y0 + 2 * cs + 4 * dp, fit.right + 10 * dp, fit.y0 + 3 * cs - 4 * dp, 0xFFD7B98E.toInt())
        c.text("➜", fit.right + 22 * dp, fit.y0 + 2.5f * cs, 22 * dp, Pal.GREEN)
        val p = rh
        if (p == null) { c.text("Đang tạo màn…", w / 2, h / 2, 18 * dp, Pal.WOOD_DARK); return }
        p.blocks.forEachIndexed { i, b ->
            var pos = b.pos.toFloat()
            if (i == drag) pos = dragPos0 + dragOffset
            if (i == 0 && exitAnim >= 0f) pos += exitAnim * 3
            val l = fit.x0 + (if (b.horizontal) pos else b.fixed.toFloat()) * cs
            val t = fit.y0 + (if (b.horizontal) b.fixed.toFloat() else pos) * cs
            val r = l + (if (b.horizontal) b.len else 1) * cs
            val bt = t + (if (b.horizontal) 1 else b.len) * cs
            val col = if (i == 0) Pal.RED else Pal.hsv(25f + (i * 37) % 40, 0.55f, 0.55f + (i % 3) * 0.1f)
            val g = cs * 0.07f
            c.rrect(l + g, t + g + 3 * dp, r - g, bt - g + 3 * dp, cs * 0.18f, Pal.shade(col, 0.5f))
            c.rrect(l + g, t + g, r - g, bt - g, cs * 0.18f, if (i == drag) Pal.tint(col, 0.2f) else col)
            c.rrect(l + g * 2.5f, t + g * 2.5f, r - g * 2.5f, t + (bt - t) * 0.35f, cs * 0.12f, Pal.alpha(Pal.WHITE, 0.15f))
            if (i == 0) c.text("★", (l + r) / 2, (t + bt) / 2, cs * 0.4f, Pal.YELLOW)
        }
    }
}
