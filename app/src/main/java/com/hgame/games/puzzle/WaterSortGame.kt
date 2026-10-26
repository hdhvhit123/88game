package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.WaterSort
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rect
import com.hgame.engine.rrectStroke
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class WaterSortGame : Game() {
    override val bestMode = BestMode.HIGH
    override val help = "Chạm một ống để chọn, rồi chạm ống khác để rót. Chỉ rót được lên lớp nước cùng màu hoặc vào ống trống. " +
        "Sắp xếp để mỗi ống chỉ chứa một màu. Có thể đi lại hoặc thêm ống phụ (1 lần mỗi màn)."

    private val palette = intArrayOf(0xFFFF4D5E.toInt(), 0xFF4D7CFF.toInt(), 0xFF3DDC97.toInt(), 0xFFFFC93C.toInt(),
        0xFFB15CFF.toInt(), 0xFFFF9F43.toInt(), 0xFF00C2FF.toInt(), 0xFFFF5CC8.toInt(), 0xFF8BC34A.toInt(),
        0xFF8D6E63.toInt(), 0xFFE0E0E0.toInt(), 0xFF26A69A.toInt())
    private var ws = WaterSort(4)
    private var level = 1
    private var sel = -1
    private val history = ArrayList<WaterSort>()
    private val undoBtn = Btn("↶ Đi lại", Pal.SURFACE2)
    private val addBtn = Btn("+ Ống phụ", Pal.SURFACE2)
    private var extraUsed = false
    private val tx = ArrayList<Float>(); private val ty = ArrayList<Float>()
    private var tw = 0f; private var th = 0f
    private var pourAnim = 0f; private var pourFrom = -1; private var pourTo = -1

    override fun newGame() {
        level = host.loadInt("level", 1)
        val colors = min(3 + (level + 1) / 2, 12)
        val rnd = Random(level * 7919L + 13)
        do { ws = WaterSort(colors); ws.deal(rnd) } while (ws.solved() || ws.solvable(20000) == false)
        sel = -1; history.clear(); extraUsed = false; pourAnim = 0f
        info("Màn $level")
    }

    override fun layout() {
        val n = ws.tubes.size
        val perRow = if (n <= 7) n else (n + 1) / 2
        val rows = (n + perRow - 1) / perRow
        tw = min((w - 30 * dp) / perRow * 0.62f, 56 * dp)
        th = min(tw * 4.2f, (h - 160 * dp) / rows - 40 * dp)
        tx.clear(); ty.clear()
        for (i in 0 until n) {
            val r = i / perRow; val inRow = if (r == rows - 1) n - perRow * (rows - 1) else perRow
            val k = i % perRow
            tx.add(w / 2 + (k - (inRow - 1) / 2f) * (w - 30 * dp) / perRow)
            ty.add(70 * dp + r * (th + 50 * dp))
        }
        undoBtn.setCenter(w * 0.3f, h - 36 * dp, w * 0.36f, 46 * dp)
        addBtn.setCenter(w * 0.7f, h - 36 * dp, w * 0.36f, 46 * dp)
    }

    private fun tubeAt(x: Float, y: Float): Int {
        for (i in tx.indices) if (x in tx[i] - tw * 0.8f..tx[i] + tw * 0.8f && y in ty[i] - 30 * dp..ty[i] + th + 20 * dp) return i
        return -1
    }

    override fun up(x: Float, y: Float) {
        if (undoBtn.hit(x, y)) {
            if (history.isNotEmpty()) { ws = history.removeAt(history.size - 1); sel = -1; layout(); sfx(Sfx.TAP) }
            redraw(); return
        }
        if (addBtn.hit(x, y) && !extraUsed && !over) {
            extraUsed = true; history.add(ws.copy()); ws.tubes.add(ArrayList()); layout(); sfx(Sfx.POP); redraw(); return
        }
        if (over) return
        val t = tubeAt(x, y)
        if (t < 0) { sel = -1; redraw(); return }
        if (sel < 0) {
            if (ws.tubes[t].isNotEmpty()) { sel = t; sfx(Sfx.CLICK) }
        } else if (sel == t) sel = -1
        else {
            if (ws.pourAmount(sel, t) > 0) {
                history.add(ws.copy())
                ws.pour(sel, t)
                pourFrom = sel; pourTo = t; pourAnim = 1f; animate(0.45f)
                sfx(Sfx.POP)
                sel = -1
                if (ws.solved()) {
                    host.saveInt("level", level + 1)
                    score(level)
                    gameOver(true, "Hoàn thành màn $level!", "Chạm Chơi lại để sang màn tiếp theo")
                }
            } else { sfx(Sfx.ERROR); sel = if (ws.tubes[t].isNotEmpty()) t else -1 }
        }
        redraw()
    }

    override fun update(dt: Float) { pourAnim = maxOf(0f, pourAnim - dt * 2.5f) }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val unit = (th - 10 * dp) / ws.cap
        for (i in ws.tubes.indices) {
            val lift = if (i == sel) 22 * dp else 0f
            val x = tx[i]; val top = ty[i] - lift
            val l = x - tw / 2; val r = x + tw / 2; val b = top + th
            // liquid, clipped to the rounded tube
            c.save()
            Gfx.path.reset()
            Gfx.rect.set(l, top, r, b)
            Gfx.path.addRoundRect(Gfx.rect, floatArrayOf(4 * dp, 4 * dp, 4 * dp, 4 * dp, tw / 2, tw / 2, tw / 2, tw / 2), android.graphics.Path.Direction.CW)
            c.clipPath(Gfx.path)
            c.rect(l, top, r, b, Pal.alpha(Pal.WHITE, 0.06f))
            ws.tubes[i].forEachIndexed { k, col ->
                val yb = b - k * unit; val yt = yb - unit
                c.rect(l, yt, r, yb + 0.5f, palette[col])
                c.rect(l + tw * 0.15f, yt, l + tw * 0.28f, yb, Pal.alpha(Pal.WHITE, 0.18f))
            }
            if (i == pourTo && pourAnim > 0f) c.rect(l, top, r, b, Pal.alpha(Pal.WHITE, 0.25f * pourAnim))
            c.restore()
            Gfx.rect.set(l, top, r, b)
            c.drawPath(Gfx.path, Gfx.stroke.apply { strokeWidth = 2.5f * dp; color = if (i == sel) Pal.YELLOW else 0xCCBFC6F0.toInt(); shader = null })
            c.rrectStroke(l - 4 * dp, top - 4 * dp, r + 4 * dp, top + 2 * dp, 3 * dp, 2.5f * dp, 0xCCBFC6F0.toInt())
            if (ws.tubes[i].size == ws.cap && ws.tubes[i].all { it == ws.tubes[i][0] }) c.text("✓", x, top - 16 * dp, 16 * dp, Pal.GREEN)
        }
        undoBtn.enabled = history.isNotEmpty(); undoBtn.draw(c, 15 * dp)
        addBtn.enabled = !extraUsed && !over; addBtn.draw(c, 15 * dp)
    }
}
