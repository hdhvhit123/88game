package com.hgame.games.sports

import android.graphics.Canvas
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
import kotlin.random.Random

class HillClimbGame : Game() {
    override val realtime = true
    override val help = "Giữ nửa phải màn hình để tăng ga, nửa trái để phanh/lùi. Khi bay trên không, ga làm xe ngả ra sau, phanh làm xe chúi về trước. " +
        "Nhặt bình xăng ⛽ để không hết nhiên liệu và xu 🪙 để ghi điểm. Đừng để đầu tài xế chạm đất!"

    private val seed = Random.nextInt(1000)
    private fun terrain(x: Float): Float {
        val k = (x / 400f).coerceIn(0f, 3f)   // hills get bigger the further you go
        return 6f - (sin(x * 0.11f + seed) * 1.4f * (0.4f + k * 0.4f) + sin(x * 0.045f + seed * 2) * (3.5f + k * 1.2f) +
            sin(x * 0.017f) * 6f * (0.4f + k * 0.25f))
    }
    private val car = SideVehicle(0.55f, 2.6f, 1.6f) { terrain(it) }
    private var gas = false; private var brake = false
    private var fuel = 1f; private var coins = 0; private var best = 0f
    private val items = ArrayList<FloatArray>()   // x, kind (0 coin,1 fuel), taken
    private var nextItem = 20f; private var t = 0f; private var dead = 0f
    private val touches = HashMap<Int, Boolean>()

    override fun newGame() {
        car.place(5f); fuel = 1f; coins = 0; best = 0f; items.clear(); nextItem = 20f; dead = 0f; touches.clear(); gas = false; brake = false
        score(0)
    }

    override fun pointerDown(id: Int, x: Float, y: Float) { touches[id] = x > w / 2; refresh() }
    override fun pointerUp(id: Int, x: Float, y: Float) { touches.remove(id); refresh() }
    private fun refresh() { gas = touches.values.any { it }; brake = touches.values.any { !it } }

    override fun update(dt: Float) {
        t += dt
        if (dead > 0f) { dead -= dt; if (dead <= 0f) end(); return }
        val throttle = if (fuel <= 0f) 0f else if (gas) 1f else if (brake) -0.8f else 0f
        if (throttle != 0f) fuel -= dt * 0.035f
        val steps = 3
        repeat(steps) { car.step(dt / steps, throttle) }
        while (nextItem < car.x + 60f) {
            items.add(floatArrayOf(nextItem, if ((nextItem.toInt() / 10) % 9 == 8) 1f else 0f, 0f))
            nextItem += if (items.last()[1] == 1f) 12f else 3f + Random.nextFloat() * 6f
        }
        for (it in items) if (it[2] == 0f && kotlin.math.abs(it[0] - car.x) < 1.5f && kotlin.math.abs(terrain(it[0]) - 1.2f - car.head.y) < 3f) {
            it[2] = 1f
            if (it[1] == 1f) { fuel = 1f; sfx(Sfx.COIN) } else { coins++; sfx(Sfx.POP) }
        }
        items.removeAll { it[0] < car.x - 30f }
        if (car.x > best) best = car.x
        score(best.toInt() + coins * 10)
        info("${best.toInt()} m · 🪙 $coins")
        if (car.crashed) { dead = 1.2f; sfx(Sfx.HIT); buzz(100); return }
        if (fuel <= 0f && kotlin.math.abs(car.speed) < 0.002f) { dead = 0.6f; sfx(Sfx.LOSE) }
    }

    private fun end() = gameOver(false, if (car.crashed) "Tai nạn!" else "Hết xăng!", "Quãng đường ${best.toInt()} m · $coins xu", best.toInt() + coins * 10)

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h, 0f, 0xFF4FC3F7.toInt(), 0xFFE1F5FE.toInt())
        val scale = w / 22f
        val camX = car.x - 7f; val camY = (car.rear.y + car.front.y) / 2 - (h * 0.55f) / scale
        fun X(x: Float) = (x - camX) * scale
        fun Y(y: Float) = (y - camY) * scale
        // parallax hills
        for (i in 0..6) { val hx = ((i * 9f - camX * 0.3f) % 63f + 63f) % 63f - 9f; c.circle(hx * scale, h * 0.62f, w * 0.35f, 0xFFA5D6A7.toInt()) }
        val p = Gfx.path; p.reset()
        var x = camX - 1f; p.moveTo(X(x), h)
        while (x < camX + 24f) { p.lineTo(X(x), Y(terrain(x))); x += 0.4f }
        p.lineTo(X(x), h); p.close()
        c.drawPath(p, Gfx.fillPaint(0xFF8D6E63.toInt()))
        // grass edge
        x = camX - 1f
        while (x < camX + 24f) { c.line(X(x), Y(terrain(x)), X(x + 0.4f), Y(terrain(x + 0.4f)), 6 * dp, 0xFF7CB342.toInt()); x += 0.4f }
        for (it in items) {
            if (it[2] != 0f) continue
            val ix = X(it[0]); val iy = Y(terrain(it[0]) - 1.2f)
            c.text(if (it[1] == 1f) "⛽" else "🪙", ix, iy, scale * 0.9f, Pal.WHITE)
        }
        // vehicle
        val rx = X(car.rear.x); val ry = Y(car.rear.y); val fx = X(car.front.x); val fy = Y(car.front.y)
        val hx = X(car.head.x); val hy = Y(car.head.y)
        val ang = Math.toDegrees(car.angle.toDouble()).toFloat()
        c.save(); c.rotate(ang, rx, ry)
        val len = (car.base) * scale
        c.rrect(rx - scale * 0.5f, ry - scale * 1.05f, rx + len + scale * 0.6f, ry - scale * 0.2f, scale * 0.25f, Pal.RED)
        c.rrect(rx + len * 0.15f, ry - scale * 1.7f, rx + len * 0.6f, ry - scale * 1.0f, scale * 0.15f, Pal.alpha(0xFFB3E5FC.toInt(), 0.9f))
        c.restore()
        c.line(hx, hy, (rx + fx) / 2, (ry + fy) / 2 - scale * 0.8f, 3 * dp, 0xFF37474F.toInt())
        c.circle(hx, hy, scale * 0.38f, 0xFFFFCC80.toInt())
        c.rrect(hx - scale * 0.4f, hy - scale * 0.55f, hx + scale * 0.4f, hy - scale * 0.2f, scale * 0.2f, Pal.YELLOW)
        for ((wx, wy, spin) in listOf(Triple(rx, ry, car.rearSpin), Triple(fx, fy, car.frontSpin))) {
            c.circle(wx, wy, car.wheelR * scale, 0xFF212121.toInt()); c.circle(wx, wy, car.wheelR * scale * 0.5f, 0xFF9E9E9E.toInt())
            c.line(wx, wy, wx + cos(spin) * car.wheelR * scale * 0.5f, wy + sin(spin) * car.wheelR * scale * 0.5f, 2 * dp, 0xFF424242.toInt())
        }
        // HUD: fuel gauge and pedals
        c.rrect(16 * dp, 14 * dp, 16 * dp + w * 0.4f, 28 * dp, 7 * dp, Pal.alpha(Pal.BLACK, 0.3f))
        c.rrect(16 * dp, 14 * dp, 16 * dp + w * 0.4f * fuel.coerceIn(0f, 1f), 28 * dp, 7 * dp, if (fuel > 0.25f) Pal.GREEN else Pal.RED)
        c.text("⛽", 16 * dp + w * 0.4f + 14 * dp, 21 * dp, 14 * dp, Pal.WHITE)
        c.text("${best.toInt()} m", w - 16 * dp, 22 * dp, 18 * dp, 0xFF01579B.toInt(), android.graphics.Paint.Align.RIGHT)
        pedal(c, 18 * dp, h - 90 * dp, brake, "PHANH", Pal.RED); pedal(c, w - 18 * dp - 70 * dp, h - 90 * dp, gas, "GA", Pal.GREEN)
        if (fuel <= 0f) c.text("Hết xăng!", w / 2, h * 0.3f, 22 * dp, Pal.RED)
    }

    private fun pedal(c: Canvas, x: Float, y: Float, on: Boolean, label: String, col: Int) {
        c.rrect(x, y + (if (on) 4 * dp else 0f), x + 70 * dp, y + 72 * dp, 14 * dp, Pal.alpha(col, if (on) 0.9f else 0.55f))
        c.text(label, x + 35 * dp, y + 38 * dp, 14 * dp, Pal.WHITE)
    }
}
