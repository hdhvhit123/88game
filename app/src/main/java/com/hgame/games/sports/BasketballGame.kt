package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.gradRect
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class BasketballGame : Game() {
    override val realtime = true
    override val help = "Vuốt quả bóng lên phía rổ để ném. Hướng và tốc độ vuốt quyết định đường bay. Có 60 giây, ném vào liên tiếp để nhân điểm. " +
        "Ném 'sạch lưới' (không chạm vành) được thêm điểm. Sau mỗi quả vào, vị trí ném sẽ thay đổi."

    private var bx = 0f; private var by = 0f; private var vx = 0f; private var vy = 0f; private var flying = false
    private var scale = 1f; private var time = 60f; private var points = 0; private var streak = 0
    private var hoopX = 0f; private var hoopY = 0f; private var hoopW = 0f; private var hoopMove = 0f
    private var startX = 0f; private var startY = 0f
    private var sx = 0f; private var sy = 0f; private var st = 0L
    private var touchedRim = false; private var scored = false; private var passedTop = false
    private var msg = ""; private var msgT = 0f; private var t = 0f; private var net = 0f
    private val fx = Particles()

    override fun newGame() { time = 60f; points = 0; streak = 0; fx.clear(); score(0); placeBall() }

    override fun layout() { hoopW = w * 0.26f; hoopY = h * 0.28f; if (hoopX == 0f) hoopX = w / 2; placeBall() }

    private fun placeBall() {
        startX = w * (0.25f + Random.nextFloat() * 0.5f); startY = h * 0.82f
        bx = startX; by = startY; vx = 0f; vy = 0f; flying = false; scale = 1f; touchedRim = false; scored = false; passedTop = false
    }

    override fun down(x: Float, y: Float) { sx = x; sy = y; st = System.nanoTime() }

    override fun up(x: Float, y: Float) {
        if (flying || over) return
        if (hypot(sx - bx, sy - by) > w * 0.25f) return
        val dy = sy - y; if (dy < 40 * dp) return
        val dt = ((System.nanoTime() - st) / 1e9f).coerceIn(0.05f, 0.5f)
        val speed = (dy / dt).coerceIn(800 * dp, 3200 * dp)
        vy = -h * 1.55f * (speed / (2000 * dp)).coerceIn(0.7f, 1.35f)
        vx = (x - sx) / dt * 0.6f
        flying = true; sfx(Sfx.JUMP)
    }

    override fun update(dt: Float) {
        t += dt; fx.update(dt); if (msgT > 0) msgT -= dt; if (net > 0) net -= dt * 3
        time -= dt
        info("⏱ ${time.toInt().coerceAtLeast(0)}s · Chuỗi $streak")
        if (time <= 0f && !flying) { sfx(Sfx.WIN); gameOver(true, "Hết giờ!", "Bạn ghi được $points điểm", points); return }
        if (points >= 20) hoopMove += dt
        hoopX = w / 2 + if (points >= 20) kotlin.math.sin(hoopMove * 1.2f) * w * 0.22f else 0f
        if (!flying) return
        val g = h * 2.6f
        val sub = 4; val sdt = dt / sub
        repeat(sub) {
            val prevY = by
            vy += g * sdt; bx += vx * sdt; by += vy * sdt
            scale = (0.62f + 0.38f * ((by - hoopY) / (startY - hoopY)).coerceIn(0f, 1f)).coerceAtMost(1f)
            val r = br()
            if (by < hoopY - r) passedTop = true
            // rim collisions only on the way down
            if (vy > 0 && passedTop) {
                for (rx in floatArrayOf(hoopX - hoopW / 2, hoopX + hoopW / 2)) {
                    val d = hypot(bx - rx, by - hoopY)
                    if (d < r + 3 * dp && d > 0.01f) {
                        val nx = (bx - rx) / d; val ny = (by - hoopY) / d
                        bx = rx + nx * (r + 3 * dp); by = hoopY + ny * (r + 3 * dp)
                        val vn = vx * nx + vy * ny
                        if (vn < 0) { vx -= 1.6f * vn * nx; vy -= 1.6f * vn * ny }
                        touchedRim = true; sfx(Sfx.TICK)
                    }
                }
                if (!scored && prevY < hoopY && by >= hoopY && abs(bx - hoopX) < hoopW / 2 - r * 0.6f) scoreBasket()
            }
            // backboard
            if (by < hoopY && by > hoopY - hoopW * 0.9f && abs(bx - hoopX) < hoopW * 0.7f && vy < 0 && scale < 0.66f) { vy = -vy * 0.3f }
        }
        if (by > h + br() * 2 || bx < -br() * 3 || bx > w + br() * 3) {
            if (!scored) { streak = 0; msg = "Trượt!"; msgT = 0.8f; sfx(Sfx.ERROR) }
            placeBall()
        }
    }

    private fun br() = w * 0.085f * scale

    private fun scoreBasket() {
        scored = true; streak++
        val swish = !touchedRim
        val pts = (if (swish) 3 else 2) * (1 + streak / 3)
        points += pts; score(points); net = 1f
        msg = if (swish) "SẠCH LƯỚI! +$pts" else "+$pts"; msgT = 1f
        fx.burst(hoopX, hoopY + 20 * dp, Pal.ORANGE, 14, 200 * dp, 3 * dp)
        sfx(if (swish) Sfx.COIN else Sfx.POP)
        if (streak % 3 == 0) time += 3f
    }

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h, 0f, 0xFF263238.toInt(), 0xFF37474F.toInt())
        c.rect(0f, h * 0.68f, w, h, 0xFFC68642.toInt())
        for (i in 0 until 10) c.rect(0f, h * 0.68f + i * h * 0.032f, w, h * 0.68f + i * h * 0.032f + 1, Pal.alpha(0xFF8D5524.toInt(), 0.4f))
        c.line(0f, h * 0.68f, w, h * 0.68f, 3 * dp, Pal.WHITE)
        // backboard
        val bbw = hoopW * 1.5f; val bbh = hoopW * 0.95f
        c.rect(hoopX - 3 * dp, hoopY - bbh * 0.2f, hoopX + 3 * dp, h * 0.68f, 0xFF90A4AE.toInt())
        c.rrect(hoopX - bbw / 2, hoopY - bbh, hoopX + bbw / 2, hoopY + bbh * 0.12f, 6 * dp, Pal.alpha(Pal.WHITE, 0.92f))
        c.line(hoopX - hoopW * 0.35f, hoopY - bbh * 0.55f, hoopX + hoopW * 0.35f, hoopY - bbh * 0.55f, 2.5f * dp, Pal.RED)
        c.line(hoopX - hoopW * 0.35f, hoopY - bbh * 0.55f, hoopX - hoopW * 0.35f, hoopY - bbh * 0.05f, 2.5f * dp, Pal.RED)
        c.line(hoopX + hoopW * 0.35f, hoopY - bbh * 0.55f, hoopX + hoopW * 0.35f, hoopY - bbh * 0.05f, 2.5f * dp, Pal.RED)
        val ballBehind = flying && vy > 0 && by < hoopY + br()
        if (!ballBehind) drawNet(c)
        // ball
        if (!flying || scale > 0f) {
            c.circle(bx, h * 0.68f + (h - h * 0.68f) * 0.2f, br() * 0.8f * (if (flying) 0.5f else 1f), Pal.alpha(Pal.BLACK, 0.2f))
            c.ball(bx, by, br(), 0xFFF57C00.toInt())
            c.line(bx - br(), by, bx + br(), by, 1.5f * dp, 0xFF5D2E00.toInt()); c.line(bx, by - br(), bx, by + br(), 1.5f * dp, 0xFF5D2E00.toInt())
        }
        if (ballBehind) drawNet(c)
        fx.draw(c)
        c.text("$points", w / 2, 36 * dp, 32 * dp, Pal.WHITE)
        if (msgT > 0f) c.text(msg, w / 2, h * 0.5f, 22 * dp, Pal.alpha(Pal.YELLOW, msgT.coerceAtMost(1f)))
        if (points == 0 && !flying && msgT <= 0f) c.text("Vuốt bóng lên để ném", w / 2, h * 0.94f, 15 * dp, Pal.WHITE)
    }

    private fun drawNet(c: Canvas) {
        val l = hoopX - hoopW / 2; val r = hoopX + hoopW / 2; val nb = hoopY + hoopW * (0.55f + net * 0.15f)
        for (i in 0..6) {
            val x1 = l + (r - l) * i / 6f; val x2 = hoopX - hoopW * 0.3f + hoopW * 0.6f * i / 6f
            c.line(x1, hoopY, x2, nb, 1.5f * dp, Pal.alpha(Pal.WHITE, 0.85f))
        }
        for (k in 1..3) { val yy = hoopY + (nb - hoopY) * k / 4f; val ww = hoopW / 2 - (hoopW * 0.2f) * k / 4f; c.line(hoopX - ww, yy, hoopX + ww, yy, 1.2f * dp, Pal.alpha(Pal.WHITE, 0.7f)) }
        c.line(l, hoopY, r, hoopY, 4 * dp, 0xFFE64A19.toInt())
    }
}
