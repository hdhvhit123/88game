package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class PinballGame : Game() {
    override val realtime = true
    override val help = "Chạm nửa trái màn hình để bật cần gạt trái, nửa phải để bật cần gạt phải (có thể chạm cả hai). " +
        "Bóng chạm nấm nảy, đèn và mục tiêu sẽ ghi điểm. Bật sáng cả 3 đèn để nhân điểm. Có 3 bóng."

    private class Seg(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val bounce: Float = 0.6f)
    private class Bumper(val x: Float, val y: Float, val r: Float, var flash: Float = 0f)
    private val segs = ArrayList<Seg>(); private val bumpers = ArrayList<Bumper>()
    private val lights = BooleanArray(3); private val lightPos = ArrayList<FloatArray>()
    private var bx = 0f; private var by = 0f; private var vx = 0f; private var vy = 0f; private var br = 0f
    private var leftUp = false; private var rightUp = false; private var la = 0f; private var ra = 0f
    private var balls = 3; private var points = 0; private var mult = 1; private var launching = true
    private val fx = Particles()
    private var fl = 0f; private var flen = 0f; private var fy = 0f; private var lpX = 0f; private var rpX = 0f
    private val touches = HashMap<Int, Boolean>()   // pointer id -> left side

    override fun newGame() { balls = 3; points = 0; mult = 1; lights.fill(false); fx.clear(); score(0); resetBall(); status() }

    private fun status() = info("Bóng: $balls · Nhân ×$mult")

    override fun layout() {
        segs.clear(); bumpers.clear(); lightPos.clear()
        br = w * 0.028f
        val m = 10 * dp
        // outer walls with angled top corners
        segs.add(Seg(m, h * 0.15f, m, h * 0.72f)); segs.add(Seg(w - m, h * 0.15f, w - m, h * 0.72f))
        segs.add(Seg(m, h * 0.15f, w * 0.25f, m)); segs.add(Seg(w * 0.25f, m, w * 0.75f, m)); segs.add(Seg(w * 0.75f, m, w - m, h * 0.15f))
        // slopes into flippers
        flen = w * 0.22f; fy = h * 0.84f; lpX = w * 0.26f; rpX = w * 0.74f
        segs.add(Seg(m, h * 0.72f, lpX, fy)); segs.add(Seg(w - m, h * 0.72f, rpX, fy))
        // slingshots
        segs.add(Seg(w * 0.2f, h * 0.6f, w * 0.3f, h * 0.72f, 1.3f)); segs.add(Seg(w * 0.8f, h * 0.6f, w * 0.7f, h * 0.72f, 1.3f))
        bumpers.add(Bumper(w * 0.32f, h * 0.27f, w * 0.07f)); bumpers.add(Bumper(w * 0.68f, h * 0.27f, w * 0.07f)); bumpers.add(Bumper(w * 0.5f, h * 0.4f, w * 0.075f))
        bumpers.add(Bumper(w * 0.18f, h * 0.45f, w * 0.04f)); bumpers.add(Bumper(w * 0.82f, h * 0.45f, w * 0.04f))
        for (i in 0..2) lightPos.add(floatArrayOf(w * (0.35f + i * 0.15f), h * 0.12f))
        if (bx == 0f) resetBall()
    }

    private fun resetBall() { bx = w * 0.5f; by = h * 0.62f; vx = 0f; vy = 0f; launching = true }

    override fun pointerDown(id: Int, x: Float, y: Float) {
        if (over) return
        if (launching) { launching = false; vx = (if (x < w / 2) -1 else 1) * w * 0.3f; vy = -h * 1.6f; sfx(Sfx.JUMP); return }
        val left = x < w / 2; touches[id] = left
        if (left) { if (!leftUp) sfx(Sfx.CLICK); leftUp = true } else { if (!rightUp) sfx(Sfx.CLICK); rightUp = true }
    }

    override fun pointerUp(id: Int, x: Float, y: Float) {
        touches.remove(id)
        leftUp = touches.values.any { it }; rightUp = touches.values.any { !it }
    }

    private fun flipperEnd(left: Boolean): Pair<Float, Float> {
        val a = if (left) Math.toRadians((25.0 - la * 55)) else Math.toRadians((155.0 + ra * 55))
        val px = if (left) lpX else rpX
        return (px + cos(a).toFloat() * flen) to (fy + sin(a).toFloat() * flen)
    }

    override fun update(dt: Float) {
        fx.update(dt)
        val prevLa = la; val prevRa = ra
        la = if (leftUp) (la + dt * 14f).coerceAtMost(1f) else (la - dt * 8f).coerceAtLeast(0f)
        ra = if (rightUp) (ra + dt * 14f).coerceAtMost(1f) else (ra - dt * 8f).coerceAtLeast(0f)
        for (b in bumpers) if (b.flash > 0) b.flash -= dt * 4
        if (launching) return
        val sub = 8; val sdt = dt / sub
        repeat(sub) {
            vy += h * 1.1f * sdt
            bx += vx * sdt; by += vy * sdt
            for (s in segs) collideSeg(s.x1, s.y1, s.x2, s.y2, s.bounce, 0f)
            val (lx, ly) = flipperEnd(true); val (rx, ry) = flipperEnd(false)
            collideSeg(lpX, fy, lx, ly, 0.3f, if (la > prevLa) 1f else 0f, true)
            collideSeg(rpX, fy, rx, ry, 0.3f, if (ra > prevRa) 1f else 0f, false)
            for (b in bumpers) {
                val d = hypot(bx - b.x, by - b.y)
                if (d < b.r + br && d > 0.01f) {
                    val nx = (bx - b.x) / d; val ny = (by - b.y) / d
                    bx = b.x + nx * (b.r + br); by = b.y + ny * (b.r + br)
                    val sp = maxOf(hypot(vx, vy), h * 0.9f)
                    vx = nx * sp; vy = ny * sp
                    b.flash = 1f; add(if (b.r > w * 0.05f) 100 else 50); sfx(Sfx.POP)
                    fx.burst(b.x + nx * b.r, b.y + ny * b.r, Pal.YELLOW, 6, 120 * dp, 2.5f * dp, 0.3f)
                }
            }
            lightPos.forEachIndexed { i, p ->
                if (!lights[i] && hypot(bx - p[0], by - p[1]) < br * 2.2f) {
                    lights[i] = true; add(250); sfx(Sfx.COIN)
                    if (lights.all { it }) { mult = (mult + 1).coerceAtMost(5); lights.fill(false); sfx(Sfx.WIN); status() }
                }
            }
        }
        val sp = hypot(vx, vy); val maxV = h * 2.6f
        if (sp > maxV) { vx *= maxV / sp; vy *= maxV / sp }
        if (by > h + br * 2) {
            balls--; sfx(Sfx.LOSE); buzz(60); mult = 1; status()
            if (balls <= 0) gameOver(false, "Hết bóng!", "Bạn ghi được $points điểm", points) else resetBall()
        }
    }

    private fun add(p: Int) { points += p * mult; score(points) }

    private fun collideSeg(x1: Float, y1: Float, x2: Float, y2: Float, bounce: Float, kick: Float, left: Boolean = true) {
        val dx = x2 - x1; val dy = y2 - y1; val l2 = dx * dx + dy * dy
        val t = (((bx - x1) * dx + (by - y1) * dy) / l2).coerceIn(0f, 1f)
        val cx = x1 + t * dx; val cy = y1 + t * dy
        val d = hypot(bx - cx, by - cy)
        val thick = 4 * dp
        if (d < br + thick && d > 0.001f) {
            val nx = (bx - cx) / d; val ny = (by - cy) / d
            bx = cx + nx * (br + thick); by = cy + ny * (br + thick)
            val vn = vx * nx + vy * ny
            if (vn < 0) {
                vx -= (1 + bounce) * vn * nx; vy -= (1 + bounce) * vn * ny
                if (bounce > 1f) { add(25); sfx(Sfx.HIT) }
            }
            if (kick > 0f) {
                val k = h * 1.9f * (0.4f + t * 0.8f)
                vx += nx * k * 0.6f; vy += ny * k
                if (vy > -h * 0.6f) vy = -h * 0.9f * (0.5f + t)
                vx += (if (left) 1 else -1) * k * 0.08f
            }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1A0B3A.toInt(), 0xFF070214.toInt())
        for (s in segs) c.line(s.x1, s.y1, s.x2, s.y2, 8 * dp, if (s.bounce > 1f) Pal.PINK else 0xFF5C6BC0.toInt())
        for (b in bumpers) {
            c.circle(b.x, b.y, b.r * 1.15f, Pal.alpha(Pal.CYAN, 0.25f + b.flash * 0.5f))
            c.circle(b.x, b.y, b.r, if (b.flash > 0) Pal.YELLOW else 0xFF7C4DFF.toInt())
            c.ring(b.x, b.y, b.r * 0.7f, 3 * dp, Pal.WHITE)
            c.text(if (b.r > w * 0.05f) "100" else "50", b.x, b.y, b.r * 0.45f, Pal.WHITE)
        }
        lightPos.forEachIndexed { i, p -> c.circle(p[0], p[1], br * 1.4f, if (lights[i]) Pal.YELLOW else Pal.alpha(Pal.YELLOW, 0.25f)) }
        c.text("×$mult", w / 2, h * 0.06f + 30 * dp, 14 * dp, Pal.YELLOW)
        for (left in listOf(true, false)) {
            val (ex, ey) = flipperEnd(left); val px = if (left) lpX else rpX
            c.line(px, fy, ex, ey, 14 * dp, Pal.ORANGE); c.circle(px, fy, 9 * dp, Pal.RED)
        }
        c.ball(bx, by, br, 0xFFE0E0E0.toInt())
        fx.draw(c)
        c.rrect(w * 0.3f, h * 0.9f, w * 0.7f, h * 0.9f + 36 * dp, 10 * dp, Pal.alpha(Pal.BLACK, 0.5f))
        c.text("$points", w / 2, h * 0.9f + 18 * dp, 20 * dp, Pal.CYAN)
        if (launching) c.text("Chạm để phóng bóng", w / 2, h * 0.52f, 16 * dp, Pal.WHITE)
        for (i in 0 until balls) c.circle(20 * dp + i * 16 * dp, h - 16 * dp, 5 * dp, Pal.WHITE)
    }
}
