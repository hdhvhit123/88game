package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.hypot

class AirHockeyGame : Game() {
    override val realtime = true
    override val options = listOf(GameOption("level", "Độ khó", listOf("Dễ", "Vừa", "Khó"), 1))
    override val help = "Kéo cây gạt (màu xanh) ở nửa sân dưới để đánh đĩa vào khung thành đối phương ở trên. Ai ghi 7 bàn trước sẽ thắng."

    private var px = 0f; private var py = 0f; private var pvx = 0f; private var pvy = 0f
    private var mx = 0f; private var my = 0f; private var mvx = 0f; private var mvy = 0f    // my mallet
    private var ax = 0f; private var ay = 0f                                              // AI mallet
    private var tx = 0f; private var ty = 0f; private var lastMx = 0f; private var lastMy = 0f
    private var me = 0; private var ai = 0; private var pause = 0f
    private var pr = 0f; private var mr = 0f; private var goalW = 0f
    private val fx = Particles()

    override fun newGame() { me = 0; ai = 0; fx.clear(); resetPuck(true); info("0 : 0") }

    override fun layout() {
        pr = w * 0.055f; mr = w * 0.085f; goalW = w * 0.38f
        if (mx == 0f) { mx = w / 2; my = h * 0.85f; tx = mx; ty = my; ax = w / 2; ay = h * 0.15f; resetPuck(true) }
    }

    private fun resetPuck(toMe: Boolean) { px = w / 2; py = if (toMe) h * 0.6f else h * 0.4f; pvx = 0f; pvy = 0f; pause = 0.8f }

    override fun down(x: Float, y: Float) { tx = x; ty = y }
    override fun move(x: Float, y: Float) { tx = x; ty = y }

    override fun update(dt: Float) {
        fx.update(dt)
        // my mallet follows finger, limited to bottom half
        lastMx = mx; lastMy = my
        mx = tx.coerceIn(mr, w - mr); my = ty.coerceIn(h / 2 + mr, h - mr)
        mvx = (mx - lastMx) / dt.coerceAtLeast(0.001f); mvy = (my - lastMy) / dt.coerceAtLeast(0.001f)
        // AI
        val skill = floatArrayOf(0.45f, 0.75f, 1.05f)[opt("level")]
        val homeY = h * 0.12f
        val targetX: Float; val targetY: Float
        if (py < h / 2) { targetX = px; targetY = if (pvy < 0 || py < ay + mr) minOf(py - pr * 0.5f, h / 2 - mr) else py - mr * 1.3f }
        else { targetX = w / 2 + (px - w / 2) * 0.4f; targetY = homeY }
        val sp = w * 1.6f * skill
        val ldx = targetX - ax; val ldy = targetY - ay; val ld = hypot(ldx, ldy)
        val lax = ax; val lay = ay
        if (ld > 1f) { val step = minOf(ld, sp * dt); ax += ldx / ld * step; ay += ldy / ld * step }
        ax = ax.coerceIn(mr, w - mr); ay = ay.coerceIn(mr, h / 2 - mr)
        val avx = (ax - lax) / dt.coerceAtLeast(0.001f); val avy = (ay - lay) / dt.coerceAtLeast(0.001f)
        if (pause > 0f) { pause -= dt; return }
        val sub = 4; val sdt = dt / sub
        repeat(sub) {
            px += pvx * sdt; py += pvy * sdt
            if (px < pr) { px = pr; pvx = -pvx * 0.9f; sfx(Sfx.TICK) }
            if (px > w - pr) { px = w - pr; pvx = -pvx * 0.9f; sfx(Sfx.TICK) }
            val inGoalX = kotlin.math.abs(px - w / 2) < goalW / 2
            if (py < pr && !inGoalX) { py = pr; pvy = -pvy * 0.9f; sfx(Sfx.TICK) }
            if (py > h - pr && !inGoalX) { py = h - pr; pvy = -pvy * 0.9f; sfx(Sfx.TICK) }
            hit(mx, my, mvx, mvy); hit(ax, ay, avx, avy)
        }
        val f = 0.996f; pvx *= f; pvy *= f
        val maxV = h * 2.2f; val v = hypot(pvx, pvy); if (v > maxV) { pvx *= maxV / v; pvy *= maxV / v }
        if (py < -pr) goal(true) else if (py > h + pr) goal(false)
    }

    private fun hit(x: Float, y: Float, vx: Float, vy: Float) {
        val dx = px - x; val dy = py - y; val d = hypot(dx, dy)
        if (d < pr + mr && d > 0.01f) {
            val nx = dx / d; val ny = dy / d
            px = x + nx * (pr + mr); py = y + ny * (pr + mr)
            val rel = (pvx - vx) * nx + (pvy - vy) * ny
            if (rel < 0) { pvx -= 1.9f * rel * nx; pvy -= 1.9f * rel * ny; sfx(Sfx.HIT) }
        }
    }

    private fun goal(mine: Boolean) {
        if (mine) me++ else ai++
        info("$me : $ai")
        fx.burst(w / 2, if (mine) 0f else h, if (mine) Pal.CYAN else Pal.RED, 30, 300 * dp, 4 * dp)
        sfx(if (mine) Sfx.WIN else Sfx.LOSE); buzz(50)
        score(me)
        if (me >= 7 || ai >= 7) { gameOver(me >= 7, if (me >= 7) "Bạn thắng!" else "Máy thắng!", "Tỉ số $me : $ai", me); return }
        resetPuck(!mine)
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF0D1B3E.toInt(), 0xFF0A1430.toInt())
        c.rrect(2 * dp, 2 * dp, w - 2 * dp, h - 2 * dp, 24 * dp, 0xFFE3F2FD.toInt())
        c.line(0f, h / 2, w, h / 2, 3 * dp, Pal.alpha(Pal.RED, 0.6f))
        c.ring(w / 2, h / 2, w * 0.18f, 3 * dp, Pal.alpha(Pal.RED, 0.6f))
        c.ring(w / 2, 0f, goalW * 0.7f, 3 * dp, Pal.alpha(Pal.BLUE, 0.4f)); c.ring(w / 2, h, goalW * 0.7f, 3 * dp, Pal.alpha(Pal.BLUE, 0.4f))
        c.rect(w / 2 - goalW / 2, 0f, w / 2 + goalW / 2, 6 * dp, 0xFF212121.toInt())
        c.rect(w / 2 - goalW / 2, h - 6 * dp, w / 2 + goalW / 2, h, 0xFF212121.toInt())
        c.text("$ai", w - 30 * dp, h / 2 - 30 * dp, 30 * dp, Pal.alpha(Pal.RED, 0.6f))
        c.text("$me", w - 30 * dp, h / 2 + 30 * dp, 30 * dp, Pal.alpha(Pal.BLUE, 0.6f))
        c.circle(px + 2 * dp, py + 3 * dp, pr, Pal.alpha(Pal.BLACK, 0.2f))
        c.circle(px, py, pr, 0xFF212121.toInt()); c.ring(px, py, pr * 0.65f, 2 * dp, 0xFF424242.toInt())
        for ((x, y, col) in listOf(Triple(ax, ay, Pal.RED), Triple(mx, my, Pal.BLUE))) {
            c.circle(x + 2 * dp, y + 4 * dp, mr, Pal.alpha(Pal.BLACK, 0.25f))
            c.circle(x, y, mr, col); c.circle(x, y, mr * 0.7f, Pal.shade(col, 0.75f)); c.circle(x, y, mr * 0.4f, Pal.tint(col, 0.3f))
        }
        fx.draw(c)
        if (pause > 0f && me + ai == 0) c.text("Kéo cây gạt xanh để chơi", w / 2, h * 0.7f, 16 * dp, 0xFF1A237E.toInt())
    }
}
