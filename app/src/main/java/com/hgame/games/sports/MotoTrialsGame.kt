package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.gradRect
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.cos
import kotlin.math.sin

class MotoTrialsGame : Game() {
    override val realtime = true
    override val bestMode = BestMode.LOW
    override val help = "Vượt địa hình nhiều dốc và ụ nhảy để về đích nhanh nhất. Giữ nửa phải để ga, nửa trái để phanh. " +
        "Khi bay, ga làm xe ngả ra sau, phanh làm xe chúi về trước — hãy tiếp đất bằng hai bánh. Mỗi vòng lộn được trừ 2 giây. 5 chặng."

    // Each level is a list of (x, y) control points; terrain is linear between them.
    private val levels = listOf(
        floatArrayOf(0f, 8f, 20f, 8f, 26f, 6f, 30f, 8f, 45f, 8f, 52f, 5f, 55f, 5f, 58f, 8f, 75f, 8f, 80f, 6.5f, 84f, 8f, 100f, 8f),
        floatArrayOf(0f, 8f, 15f, 8f, 22f, 4f, 24f, 4f, 30f, 9f, 40f, 9f, 48f, 6f, 50f, 9f, 60f, 9f, 64f, 7f, 66f, 9f, 70f, 7f, 72f, 9f, 90f, 9f, 98f, 6f, 110f, 6f),
        floatArrayOf(0f, 8f, 12f, 8f, 20f, 3f, 21f, 3f, 23f, 9f, 32f, 9f, 36f, 7f, 44f, 7f, 46f, 10f, 56f, 10f, 64f, 5f, 66f, 5f, 67f, 10f, 80f, 10f, 85f, 8f, 88f, 10f, 105f, 10f),
        floatArrayOf(0f, 8f, 10f, 8f, 18f, 5f, 26f, 5f, 28f, 8f, 34f, 8f, 37f, 4f, 38f, 4f, 40f, 9f, 50f, 9f, 52f, 7f, 54f, 9f, 56f, 7f, 58f, 9f, 60f, 7f, 62f, 9f, 74f, 9f, 84f, 3f, 86f, 3f, 88f, 9f, 110f, 9f),
        floatArrayOf(0f, 8f, 10f, 8f, 16f, 5f, 18f, 5f, 22f, 9f, 28f, 9f, 35f, 4f, 37f, 4f, 42f, 10f, 48f, 10f, 50f, 8f, 52f, 10f, 60f, 10f, 66f, 6f, 72f, 6f, 75f, 10f, 82f, 10f, 89f, 4f, 91f, 4f, 96f, 11f, 120f, 11f),
    )
    private var level = 0
    private var pts = levels[0]
    private fun terrain(x: Float): Float {
        if (x <= pts[0]) return pts[1]
        var i = 0
        while (i < pts.size - 2) {
            if (x <= pts[i + 2]) { val t = (x - pts[i]) / (pts[i + 2] - pts[i]); return pts[i + 1] + (pts[i + 3] - pts[i + 1]) * t }
            i += 2
        }
        return pts[pts.size - 1]
    }
    private val finish get() = pts[pts.size - 2] - 6f
    private val bike = SideVehicle(0.5f, 2.2f, 1.5f) { terrain(it) }
    private var gas = false; private var brake = false; private val touches = HashMap<Int, Boolean>()
    private var time = 0f; private var total = 0f; private var dead = 0f; private var flips = 0; private var rot = 0f; private var lastAng = 0f
    private var msg = ""; private var msgT = 0f; private var done = 0f

    override fun newGame() { level = 0; total = 0f; load() }

    private fun load() {
        pts = levels[level]; bike.place(3f); time = 0f; dead = 0f; flips = 0; rot = 0f; lastAng = bike.angle; done = 0f
        touches.clear(); gas = false; brake = false
        info("Chặng ${level + 1}/5")
    }

    override fun pointerDown(id: Int, x: Float, y: Float) { touches[id] = x > w / 2; gas = touches.values.any { it }; brake = touches.values.any { !it } }
    override fun pointerUp(id: Int, x: Float, y: Float) { touches.remove(id); gas = touches.values.any { it }; brake = touches.values.any { !it } }

    override fun update(dt: Float) {
        if (msgT > 0) msgT -= dt
        if (done > 0f) { done -= dt; if (done <= 0f) { if (level == levels.size - 1) finishAll() else { level++; load() } }; return }
        if (dead > 0f) { dead -= dt; if (dead <= 0f) { msg = "Thử lại chặng"; msgT = 1f; load() }; return }
        time += dt
        val throttle = if (gas) 1f else if (brake) -0.9f else 0f
        repeat(3) { bike.step(dt / 3, throttle, power = 16f, topSpeed = 15f) }
        // count flips while airborne
        var d = bike.angle - lastAng
        while (d > Math.PI) d -= (2 * Math.PI).toFloat(); while (d < -Math.PI) d += (2 * Math.PI).toFloat()
        lastAng = bike.angle
        if (!bike.rearOnGround && !bike.frontOnGround) rot += d else {
            if (kotlin.math.abs(rot) > 5.5f) { flips++; time -= 2f; msg = "Lộn vòng! −2s"; msgT = 1.2f; sfx(Sfx.COIN) }
            rot = 0f
        }
        info("Chặng ${level + 1}/5 · ⏱ ${"%.1f".format(time)}s")
        if (bike.crashed) { dead = 1.2f; sfx(Sfx.HIT); buzz(100); msg = "Ngã xe!"; msgT = 1.2f; return }
        if (bike.x > finish) {
            total += time; score(total.toInt()); sfx(Sfx.WIN)
            msg = "Về đích: ${"%.1f".format(time)}s"; msgT = 1.5f; done = 1.5f
        }
    }

    private fun finishAll() = gameOver(true, "Hoàn thành 5 chặng!", "Tổng thời gian ${"%.1f".format(total)}s", total.toInt())

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h, 0f, 0xFFFFB74D.toInt(), 0xFFFFE0B2.toInt())
        val scale = w / 18f
        val camX = bike.x - 6f; val camY = (bike.rear.y + bike.front.y) / 2 - (h * 0.55f) / scale
        fun X(x: Float) = (x - camX) * scale
        fun Y(y: Float) = (y - camY) * scale
        for (i in 0..5) { val mx = ((i * 14f - camX * 0.25f) % 84f + 84f) % 84f - 14f; c.circle(mx * scale, h * 0.7f, w * 0.4f, 0xFFD7A86E.toInt()) }
        val p = Gfx.path; p.reset()
        var x = camX - 1f; p.moveTo(X(x), h)
        while (x < camX + 20f) { p.lineTo(X(x), Y(terrain(x))); x += 0.25f }
        p.lineTo(X(x), h); p.close()
        c.drawPath(p, Gfx.fillPaint(0xFF6D4C41.toInt()))
        x = camX - 1f
        while (x < camX + 20f) { c.line(X(x), Y(terrain(x)), X(x + 0.25f), Y(terrain(x + 0.25f)), 5 * dp, 0xFF4E342E.toInt()); x += 0.25f }
        // finish flag
        val fx = X(finish); val fy = Y(terrain(finish))
        c.line(fx, fy, fx, fy - scale * 3, 3 * dp, Pal.WHITE)
        for (r in 0..3) for (k in 0..3) c.rect(fx + k * scale * 0.3f, fy - scale * 3 + r * scale * 0.3f, fx + (k + 1) * scale * 0.3f, fy - scale * 3 + (r + 1) * scale * 0.3f, if ((r + k) % 2 == 0) Pal.BLACK else Pal.WHITE)
        // bike
        val rx = X(bike.rear.x); val ry = Y(bike.rear.y); val frx = X(bike.front.x); val fry = Y(bike.front.y)
        val hx = X(bike.head.x); val hy = Y(bike.head.y)
        c.line(rx, ry, (rx + frx) / 2, (ry + fry) / 2 - scale * 0.5f, 4 * dp, Pal.RED)
        c.line((rx + frx) / 2, (ry + fry) / 2 - scale * 0.5f, frx, fry, 4 * dp, Pal.RED)
        c.line((rx + frx) / 2, (ry + fry) / 2 - scale * 0.5f, hx, hy + scale * 0.4f, 4 * dp, 0xFF1A237E.toInt())
        c.circle(hx, hy, scale * 0.32f, Pal.YELLOW)
        for ((wx, wy, spin) in listOf(Triple(rx, ry, bike.rearSpin), Triple(frx, fry, bike.frontSpin))) {
            c.circle(wx, wy, bike.wheelR * scale, 0xFF212121.toInt()); c.circle(wx, wy, bike.wheelR * scale * 0.55f, 0xFFBDBDBD.toInt())
            c.line(wx - cos(spin) * bike.wheelR * scale * 0.55f, wy - sin(spin) * bike.wheelR * scale * 0.55f, wx + cos(spin) * bike.wheelR * scale * 0.55f, wy + sin(spin) * bike.wheelR * scale * 0.55f, 2 * dp, 0xFF424242.toInt())
        }
        // progress bar
        val prog = (bike.x / finish).coerceIn(0f, 1f)
        c.rrect(16 * dp, 12 * dp, w - 16 * dp, 20 * dp, 4 * dp, Pal.alpha(Pal.BLACK, 0.25f))
        c.rrect(16 * dp, 12 * dp, 16 * dp + (w - 32 * dp) * prog, 20 * dp, 4 * dp, Pal.GREEN)
        c.text("⏱ ${"%.1f".format(time)}s", w / 2, 38 * dp, 18 * dp, 0xFF3E2723.toInt())
        if (msgT > 0f) c.text(msg, w / 2, h * 0.28f, 22 * dp, Pal.alpha(0xFFB71C1C.toInt(), msgT.coerceAtMost(1f)))
        c.rrect(18 * dp, h - 90 * dp, 88 * dp, h - 18 * dp, 14 * dp, Pal.alpha(Pal.RED, if (brake) 0.9f else 0.5f)); c.text("PHANH", 53 * dp, h - 54 * dp, 13 * dp, Pal.WHITE)
        c.rrect(w - 88 * dp, h - 90 * dp, w - 18 * dp, h - 18 * dp, 14 * dp, Pal.alpha(Pal.GREEN, if (gas) 0.9f else 0.5f)); c.text("GA", w - 53 * dp, h - 54 * dp, 14 * dp, Pal.WHITE)
    }
}
