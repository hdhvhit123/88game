package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Sokoban
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.Swipe
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.rrect

class SokobanGame : Game() {
    override val bestMode = BestMode.HIGH
    override val help = "Bạn là thủ kho. Đẩy tất cả các thùng vào các điểm đích (vòng tròn). Chỉ đẩy được, không kéo, " +
        "và mỗi lần chỉ đẩy một thùng. Vuốt hoặc dùng nút mũi tên để di chuyển, ↶ để đi lại."

    private var level = 0
    private lateinit var s: Sokoban
    private val history = ArrayList<Pair<Int, Set<Int>>>()
    private val fit = BoardFit()
    private lateinit var swipe: Swipe
    private val undoBtn = Btn("↶", Pal.SURFACE2)
    private val pad = listOf(Btn("▲", Pal.SURFACE2), Btn("▶", Pal.SURFACE2), Btn("▼", Pal.SURFACE2), Btn("◀", Pal.SURFACE2))
    private var moves = 0
    private var facing = 2

    override fun newGame() {
        level = host.loadInt("level", 0).coerceIn(0, Sokoban.LEVELS.size - 1)
        s = Sokoban(Sokoban.LEVELS[level])
        history.clear(); moves = 0
        status()
    }

    private fun status() = info("Màn ${level + 1}/${Sokoban.LEVELS.size} · Bước $moves")

    override fun layout() {
        swipe = Swipe(22 * dp)
        val ctrl = 170 * dp
        fit.fit(s.w, s.h, 10 * dp, 10 * dp, w - 10 * dp, h - ctrl)
        val cx = w / 2; val cy = h - ctrl / 2 + 6 * dp; val b = 54 * dp
        pad[0].setCenter(cx, cy - b, b, b * 0.9f)
        pad[2].setCenter(cx, cy, b, b * 0.9f)
        pad[1].setCenter(cx + b * 1.1f, cy - b / 2, b, b * 0.9f)
        pad[3].setCenter(cx - b * 1.1f, cy - b / 2, b, b * 0.9f)
        undoBtn.setCenter(w - 50 * dp, cy - b / 2, 56 * dp, 56 * dp)
    }

    private fun step(dir: Int) {
        if (over) return
        val (dx, dy) = when (dir) { 0 -> 0 to -1; 1 -> 1 to 0; 2 -> 0 to 1; else -> -1 to 0 }
        val before = s.player to HashSet(s.boxes)
        facing = dir
        val r = s.move(dx, dy)
        if (r == 0) return
        history.add(before)
        moves++
        sfx(if (r == 2) Sfx.TAP else Sfx.TICK)
        status()
        if (s.solved()) {
            host.saveInt("level", (level + 1) % Sokoban.LEVELS.size)
            score(level + 1)
            gameOver(true, "Hoàn thành màn ${level + 1}!", "$moves bước · Chạm Chơi lại để sang màn sau")
        }
        redraw()
    }

    override fun down(x: Float, y: Float) {
        for ((i, b) in pad.withIndex()) if (b.hit(x, y)) { b.pressed = true; step(i); return }
        swipe.down(x, y)
    }

    override fun move(x: Float, y: Float) { val d = swipe.move(x, y); if (d >= 0) step(d) }

    override fun up(x: Float, y: Float) {
        pad.forEach { it.pressed = false }
        swipe.active = false
        if (undoBtn.hit(x, y) && history.isNotEmpty()) {
            val (p, b) = history.removeAt(history.size - 1)
            s.player = p; s.boxes.clear(); s.boxes.addAll(b); moves--
            if (over) resumePlay()
            sfx(Sfx.TAP); status()
        }
        redraw()
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        for (y in 0 until s.h) for (x in 0 until s.w) {
            val i = y * s.w + x
            val l = fit.x0 + x * cs; val t = fit.y0 + y * cs
            if (s.wall[i]) {
                c.rect(l, t, l + cs + 0.5f, t + cs + 0.5f, 0xFF5D4A8A.toInt())
                c.rect(l, t + cs * 0.75f, l + cs + 0.5f, t + cs + 0.5f, 0xFF43356A.toInt())
                c.line(l, t + cs * 0.38f, l + cs, t + cs * 0.38f, 1f * dp, 0xFF4A3C75.toInt())
            } else if (insideFloor(x, y)) {
                c.rect(l, t, l + cs + 0.5f, t + cs + 0.5f, if ((x + y) % 2 == 0) 0xFF2A2F55.toInt() else 0xFF262B4F.toInt())
                if (s.goal[i]) { c.ring(l + cs / 2, t + cs / 2, cs * 0.22f, cs * 0.07f, Pal.YELLOW); c.circle(l + cs / 2, t + cs / 2, cs * 0.07f, Pal.YELLOW) }
            }
        }
        for (b in s.boxes) {
            val l = fit.x0 + (b % s.w) * cs; val t = fit.y0 + (b / s.w) * cs
            val onGoal = s.goal[b]
            val col = if (onGoal) 0xFF3DDC97.toInt() else 0xFFE3A257.toInt()
            c.rrect(l + cs * 0.08f, t + cs * 0.12f, l + cs * 0.92f, t + cs * 0.96f, cs * 0.12f, Pal.shade(col, 0.55f))
            c.rrect(l + cs * 0.08f, t + cs * 0.06f, l + cs * 0.92f, t + cs * 0.9f, cs * 0.12f, col)
            c.line(l + cs * 0.2f, t + cs * 0.2f, l + cs * 0.8f, t + cs * 0.76f, cs * 0.07f, Pal.shade(col, 0.75f))
            c.line(l + cs * 0.8f, t + cs * 0.2f, l + cs * 0.2f, t + cs * 0.76f, cs * 0.07f, Pal.shade(col, 0.75f))
        }
        val px = fit.x0 + (s.player % s.w + 0.5f) * cs; val py = fit.y0 + (s.player / s.w + 0.5f) * cs
        c.circle(px, py + cs * 0.05f, cs * 0.36f, 0xFF1E88E5.toInt())
        c.circle(px, py - cs * 0.08f, cs * 0.26f, 0xFFFFD8B0.toInt())
        val ex = when (facing) { 1 -> 0.08f; 3 -> -0.08f; else -> 0f } * cs
        val ey = when (facing) { 0 -> -0.05f; 2 -> 0.02f; else -> 0f } * cs
        c.circle(px - cs * 0.09f + ex, py - cs * 0.1f + ey, cs * 0.04f, Pal.BLACK)
        c.circle(px + cs * 0.09f + ex, py - cs * 0.1f + ey, cs * 0.04f, Pal.BLACK)
        pad.forEach { it.draw(c, 20 * dp) }
        undoBtn.enabled = history.isNotEmpty(); undoBtn.draw(c, 24 * dp)
    }

    /** Floor reachable from the player's region (so the outside of the level stays blank). */
    private var floorCache: BooleanArray? = null
    private var floorLevel = -1
    private fun insideFloor(x: Int, y: Int): Boolean {
        if (floorLevel != level || floorCache == null) {
            val seen = BooleanArray(s.w * s.h)
            val st = ArrayDeque<Int>(); st.add(s.player); seen[s.player] = true
            while (st.isNotEmpty()) {
                val cc = st.removeLast()
                for (d in intArrayOf(1, -1, s.w, -s.w)) {
                    val n = cc + d
                    if (n in seen.indices && !s.wall[n] && !seen[n]) { seen[n] = true; st.add(n) }
                }
            }
            floorCache = seen; floorLevel = level
        }
        return floorCache!![y * s.w + x]
    }
}
