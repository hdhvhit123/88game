package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.MazeGen
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.Swipe
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.text
import kotlin.math.min

class MazeGame : Game() {
    override val bestMode = BestMode.HIGH
    override val help = "Đưa quả bóng từ góc trên bên trái đến lá cờ ở góc dưới bên phải. Vuốt để chạy theo hướng đó — " +
        "bóng sẽ tự chạy dọc hành lang tới ngã rẽ tiếp theo. Mỗi màn mê cung lớn hơn."

    private lateinit var m: MazeGen
    private var level = 1
    private var pos = 0
    private var dispX = 0f; private var dispY = 0f
    private val queue = ArrayDeque<Int>()
    private lateinit var swipe: Swipe
    private var cell = 0f; private var x0 = 0f; private var y0 = 0f
    private val trail = LinkedHashSet<Int>()

    override fun newGame() {
        level = host.loadInt("level", 1)
        val cols = min(6 + level, 22)
        val rows = (cols * 1.45f).toInt()
        m = MazeGen(cols, rows)
        pos = 0; dispX = 0f; dispY = 0f; queue.clear(); trail.clear(); trail.add(0)
        info("Màn $level")
        startClock()
    }

    override fun layout() {
        swipe = Swipe(18 * dp)
        cell = min((w - 20 * dp) / m.cols, (h - 20 * dp) / m.rows)
        x0 = (w - cell * m.cols) / 2; y0 = (h - cell * m.rows) / 2
    }

    override fun onClock(seconds: Int) = info("Màn $level · ⏱ ${seconds / 60}:${"%02d".format(seconds % 60)}")

    private fun run(dir: Int) {
        if (over) return
        var c = if (queue.isEmpty()) pos else queue.last()
        if (!m.open(c, dir)) { sfx(Sfx.TICK); return }
        var d = dir
        var steps = 0
        while (m.open(c, d) && steps < 200) {
            c = m.neighbor(c, d); queue.add(c); steps++
            if (m.exits(c) != 2) break
            // follow the corridor around bends
            d = (0..3).first { it != (d + 2) % 4 && m.open(c, it) }
        }
        animate(5f)
    }

    override fun down(x: Float, y: Float) = swipe.down(x, y)
    override fun move(x: Float, y: Float) { val d = swipe.move(x, y); if (d >= 0) run(d) }
    override fun up(x: Float, y: Float) { swipe.active = false }

    override fun update(dt: Float) {
        if (queue.isEmpty()) return
        animate(0.2f)
        val target = queue.first()
        val tx = (target % m.cols).toFloat(); val ty = (target / m.cols).toFloat()
        val speed = 14f * dt
        val dx = tx - dispX; val dy = ty - dispY
        val dist = kotlin.math.abs(dx) + kotlin.math.abs(dy)
        if (dist <= speed) {
            dispX = tx; dispY = ty; pos = target; queue.removeFirst(); trail.add(pos)
            if (pos == m.cols * m.rows - 1) {
                queue.clear()
                host.saveInt("level", level + 1)
                score(level)
                gameOver(true, "Thoát khỏi mê cung!", "Màn $level xong trong ${clockSeconds}s")
            }
        } else {
            dispX += dx / dist * speed; dispY += dy / dist * speed
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.rect(x0, y0, x0 + cell * m.cols, y0 + cell * m.rows, 0xFF1C2142.toInt())
        for (t in trail) c.circle(x0 + (t % m.cols + 0.5f) * cell, y0 + (t / m.cols + 0.5f) * cell, cell * 0.12f, Pal.alpha(Pal.CYAN, 0.35f))
        val ex = x0 + (m.cols - 0.5f) * cell; val ey = y0 + (m.rows - 0.5f) * cell
        c.rect(ex - cell / 2, ey - cell / 2, ex + cell / 2, ey + cell / 2, Pal.alpha(Pal.GREEN, 0.3f))
        c.text("🏁", ex, ey, cell * 0.7f, Pal.WHITE)
        val lw = maxOf(2f * dp, cell * 0.12f)
        val col = 0xFFB9A8FF.toInt()
        for (i in m.walls.indices) {
            val x = x0 + (i % m.cols) * cell; val y = y0 + (i / m.cols) * cell
            if (!m.open(i, 0)) c.line(x, y, x + cell, y, lw, col)
            if (!m.open(i, 3)) c.line(x, y, x, y + cell, lw, col)
            if (i % m.cols == m.cols - 1 && !m.open(i, 1)) c.line(x + cell, y, x + cell, y + cell, lw, col)
            if (i / m.cols == m.rows - 1 && !m.open(i, 2)) c.line(x, y + cell, x + cell, y + cell, lw, col)
        }
        c.ball(x0 + (dispX + 0.5f) * cell, y0 + (dispY + 0.5f) * cell, cell * 0.32f, Pal.PINK)
    }
}
