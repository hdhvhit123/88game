package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Onet
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.line
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class TileConnectGame : Game() {
    override val realtime = true
    override val help = "Chọn hai ô có hình giống nhau để xoá chúng nếu có thể nối bằng đường gấp khúc không quá 2 lần rẽ " +
        "và không đi qua ô khác. Xoá hết các ô trước khi hết giờ. Có nút Gợi ý và Xáo trộn."

    private val icons = listOf("🍎", "🐶", "🌸", "⭐", "🚗", "🎈", "🍀", "🐱", "🍩", "⚽", "🎁", "🐟", "🍉", "🦋", "🌙", "🔔", "🍄", "🐸")
    private lateinit var o: Onet
    private var sel = -1
    private var cell = 0f; private var x0 = 0f; private var y0 = 0f
    private var line: List<Int>? = null
    private var lineT = 0f
    private var time = 0f
    private var total = 0f
    private var points = 0
    private var level = 1
    private val hintBtn = Btn("💡 Gợi ý", Pal.SURFACE2)
    private val shuffleBtn = Btn("🔀 Xáo trộn", Pal.SURFACE2)
    private var hint: Pair<Int, Int>? = null

    override fun newGame() {
        level = 1; points = 0
        startLevel()
        score(0)
    }

    private fun startLevel() {
        o = Onet(6, 8, min(8 + level * 2, icons.size))
        while (o.anyMove() == null) o.shuffle(Random.Default)
        sel = -1; line = null; hint = null
        total = 200f - level * 15f; time = total
        if (w > 0) layout()
    }

    override fun layout() {
        cell = min((w - 16 * dp) / o.W, (h - 110 * dp) / o.H)
        x0 = (w - cell * o.W) / 2; y0 = 20 * dp + (h - 100 * dp - cell * o.H) / 2
        hintBtn.setCenter(w * 0.3f, h - 34 * dp, w * 0.38f, 46 * dp)
        shuffleBtn.setCenter(w * 0.7f, h - 34 * dp, w * 0.38f, 46 * dp)
    }

    private fun cx(i: Int) = x0 + (i % o.W + 0.5f) * cell
    private fun cy(i: Int) = y0 + (i / o.W + 0.5f) * cell

    override fun up(x: Float, y: Float) {
        if (over) return
        if (hintBtn.hit(x, y)) { hint = o.anyMove(); time -= 5f; sfx(Sfx.CLICK); return }
        if (shuffleBtn.hit(x, y)) { do { o.shuffle(Random.Default) } while (o.anyMove() == null); time -= 10f; sel = -1; sfx(Sfx.CARD); return }
        val c = ((x - x0) / cell).toInt(); val r = ((y - y0) / cell).toInt()
        if (x < x0 || y < y0 || c !in 0 until o.W || r !in 0 until o.H) return
        val i = r * o.W + c
        if (o.cells[i] < 0) return
        if (sel < 0 || sel == i) { sel = if (sel == i) -1 else i; sfx(Sfx.CLICK); return }
        val p = o.path(sel, i)
        if (p != null) {
            line = p; lineT = 0.3f
            o.cells[sel] = -1; o.cells[i] = -1
            sel = -1; hint = null
            points += 10 * level
            score(points)
            sfx(Sfx.COIN)
            if (o.remaining() == 0) {
                points += time.toInt() * 2
                score(points)
                level++
                info("Qua màn! Thưởng thời gian")
                after(0.6f) { startLevel() }
            } else if (o.anyMove() == null) {
                do { o.shuffle(Random.Default) } while (o.anyMove() == null)
            }
        } else { sel = i; sfx(Sfx.ERROR) }
    }

    override fun update(dt: Float) {
        time -= dt
        lineT = maxOf(0f, lineT - dt)
        info("Màn $level · ⏱ ${time.toInt().coerceAtLeast(0)}s")
        if (time <= 0f) gameOver(false, "Hết giờ!", "Bạn đạt màn $level", points)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        // time bar
        c.rrect(16 * dp, 6 * dp, w - 16 * dp, 12 * dp, 3 * dp, Pal.SURFACE)
        c.rrect(16 * dp, 6 * dp, 16 * dp + (w - 32 * dp) * (time / total).coerceIn(0f, 1f), 12 * dp, 3 * dp, if (time < 20) Pal.RED else Pal.GREEN)
        for (i in o.cells.indices) {
            val v = o.cells[i]
            if (v < 0) continue
            val x = cx(i); val y = cy(i)
            val selected = i == sel || hint?.let { it.first == i || it.second == i } == true
            c.rrect(x - cell * 0.46f, y - cell * 0.42f, x + cell * 0.46f, y + cell * 0.5f, cell * 0.14f, 0xFFB8B0E0.toInt())
            c.rrect(x - cell * 0.46f, y - cell * 0.46f, x + cell * 0.46f, y + cell * 0.44f, cell * 0.14f, if (selected) 0xFFFFE08A.toInt() else 0xFFF6F4FF.toInt())
            c.text(icons[v], x, y, cell * 0.58f, Pal.BLACK)
        }
        val l = line
        if (l != null && lineT > 0f) for (k in 0 until l.size - 1) c.line(cx(l[k]), cy(l[k]), cx(l[k + 1]), cy(l[k + 1]), 4 * dp, Pal.alpha(Pal.YELLOW, lineT / 0.3f))
        hintBtn.draw(c, 15 * dp); shuffleBtn.draw(c, 15 * dp)
    }
}
