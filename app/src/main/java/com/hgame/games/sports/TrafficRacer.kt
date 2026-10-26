package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.random.Random

/**
 * Endless top-down traffic racer shared by Car Racing (car, 4 lanes) and Moto Rush (bike, weaving bonus).
 * Hold left/right half to steer; the speed increases over time.
 */
abstract class TrafficRacer : Game() {
    override val realtime = true
    protected abstract val bike: Boolean
    protected open val lanes = 4

    private class Car(var lane: Float, var y: Float, val speed: Float, val color: Int, val truck: Boolean)
    private val traffic = ArrayList<Car>()
    private val coins = ArrayList<FloatArray>()
    private var px = 0f; private var steer = 0; private var speed = 0f; private var dist = 0f; private var scroll = 0f
    private var spawnT = 0f; private var points = 0; private var nearMiss = 0f; private var msg = ""; private var msgT = 0f
    private var roadL = 0f; private var roadR = 0f; private var laneW = 0f; private var playerY = 0f
    private val fx = Particles(); private val touches = HashMap<Int, Int>()
    private var crashT = 0f

    override fun newGame() { traffic.clear(); coins.clear(); fx.clear(); speed = 0f; dist = 0f; points = 0; spawnT = 1f; crashT = 0f; px = 0f; score(0) }

    override fun layout() {
        roadL = w * 0.1f; roadR = w * 0.9f; laneW = (roadR - roadL) / lanes; playerY = h * 0.78f
        if (px == 0f) px = roadL + laneW * (lanes / 2 - 0.5f)
    }

    override fun pointerDown(id: Int, x: Float, y: Float) { touches[id] = if (x < w / 2) -1 else 1; steer = touches.values.lastOrNull() ?: 0 }
    override fun pointerUp(id: Int, x: Float, y: Float) { touches.remove(id); steer = touches.values.lastOrNull() ?: 0 }

    private fun laneX(l: Float) = roadL + laneW * (l + 0.5f)

    override fun update(dt: Float) {
        fx.update(dt); if (msgT > 0) msgT -= dt
        if (crashT > 0f) { crashT -= dt; if (crashT <= 0f) gameOver(false, "Va chạm!", "Quãng đường ${dist.toInt()} m · $points điểm", points); return }
        val top = h * (1.1f + dist / 3000f).coerceAtMost(2.4f)
        speed += (top - speed) * dt * 0.6f
        scroll += speed * dt; dist += speed * dt / dp * 0.05f
        px = (px + steer * w * (if (bike) 1.3f else 1.0f) * dt).coerceIn(roadL + laneW * 0.4f, roadR - laneW * 0.4f)
        spawnT -= dt
        if (spawnT <= 0f) {
            spawnT = (0.9f - dist / 6000f).coerceAtLeast(0.35f) * (0.6f + Random.nextFloat() * 0.6f)
            val lane = Random.nextInt(lanes)
            if (traffic.none { it.lane.toInt() == lane && it.y < 0 }) {
                traffic.add(Car(lane.toFloat(), -h * 0.15f, speed * (0.35f + Random.nextFloat() * 0.25f), Pal.VIVID[Random.nextInt(8)], Random.nextFloat() < 0.15f))
            }
            if (Random.nextFloat() < 0.35f) coins.add(floatArrayOf(laneX(Random.nextInt(lanes).toFloat()), -h * 0.1f, 0f))
        }
        val pw = laneW * (if (bike) 0.28f else 0.55f); val ph = laneW * (if (bike) 0.75f else 1.0f)
        for (c in traffic) c.y += (speed - c.speed) * dt
        traffic.removeAll { it.y > h + h * 0.2f }
        for (c in coins) c[1] += speed * dt
        coins.removeAll { it[1] > h + 20 * dp || it[2] != 0f }
        for (c in coins) if (abs(c[0] - px) < pw && abs(c[1] - playerY) < ph * 0.6f) { c[2] = 1f; points += 5; sfx(Sfx.COIN) }
        for (car in traffic) {
            val cw = laneW * 0.6f; val ch = laneW * (if (car.truck) 1.9f else 1.15f)
            val cx = laneX(car.lane)
            val dx = abs(cx - px); val dy = abs(car.y - playerY)
            if (dx < (cw + pw) / 2 * 0.9f && dy < (ch + ph) / 2 * 0.9f) {
                crashT = 1f; sfx(Sfx.BOOM); buzz(120); fx.burst(px, playerY, Pal.ORANGE, 40, 300 * dp, 5 * dp); return
            }
            // near miss bonus when passing very close
            if (dy < 4 * dp && dx < (cw + pw) / 2 + laneW * 0.25f && nearMiss <= 0f) {
                nearMiss = 0.5f; points += if (bike) 10 else 5; msg = "Suýt nữa! +${if (bike) 10 else 5}"; msgT = 0.8f; sfx(Sfx.TICK)
            }
        }
        if (nearMiss > 0) nearMiss -= dt
        score(points + dist.toInt() / 10)
        info("${dist.toInt()} m · ${(speed / h * 100).toInt()} km/h")
    }

    private fun vehicle(c: Canvas, x: Float, y: Float, col: Int, truck: Boolean, player: Boolean) {
        if (player && bike) {
            val l = laneW * 0.75f; val wd = laneW * 0.28f
            c.rrect(x - wd * 0.3f, y - l / 2, x + wd * 0.3f, y + l / 2, wd * 0.3f, 0xFF212121.toInt())
            c.rrect(x - wd / 2, y - l * 0.25f, x + wd / 2, y + l * 0.25f, wd * 0.4f, col)
            c.circle(x, y - l * 0.02f, wd * 0.38f, Pal.YELLOW)
            c.rect(x - wd * 0.8f, y - l * 0.28f, x + wd * 0.8f, y - l * 0.23f, 0xFF616161.toInt())
            return
        }
        val cw = laneW * 0.6f; val ch = laneW * (if (truck) 1.9f else 1.15f)
        c.rrect(x - cw / 2 + 3 * dp, y - ch / 2 + 5 * dp, x + cw / 2 + 3 * dp, y + ch / 2 + 5 * dp, cw * 0.2f, Pal.alpha(Pal.BLACK, 0.3f))
        c.rrect(x - cw / 2, y - ch / 2, x + cw / 2, y + ch / 2, cw * 0.2f, col)
        if (truck) { c.rrect(x - cw / 2, y - ch / 2, x + cw / 2, y - ch / 2 + cw * 0.9f, cw * 0.2f, Pal.shade(col, 0.7f)); c.rect(x - cw * 0.4f, y - ch / 2 + cw, x + cw * 0.4f, y + ch / 2 - cw * 0.1f, 0xFFECEFF1.toInt()) }
        else {
            val win = 0xFF263238.toInt()
            c.rrect(x - cw * 0.38f, y - ch * 0.22f, x + cw * 0.38f, y - ch * 0.02f, cw * 0.1f, win)
            c.rrect(x - cw * 0.36f, y + ch * 0.18f, x + cw * 0.36f, y + ch * 0.32f, cw * 0.1f, win)
        }
        val lightY = if (player) y - ch / 2 + 3 * dp else y + ch / 2 - 3 * dp
        c.circle(x - cw * 0.32f, lightY, cw * 0.08f, if (player) Pal.YELLOW else Pal.RED); c.circle(x + cw * 0.32f, lightY, cw * 0.08f, if (player) Pal.YELLOW else Pal.RED)
    }

    override fun draw(c: Canvas) {
        c.rect(0f, 0f, w, h, 0xFF558B2F.toInt())
        val treeStep = h * 0.18f
        var ty = (scroll % treeStep) - treeStep
        while (ty < h) { c.circle(roadL * 0.45f, ty, roadL * 0.35f, 0xFF2E7D32.toInt()); c.circle(w - roadL * 0.45f, ty + treeStep / 2, roadL * 0.35f, 0xFF2E7D32.toInt()); ty += treeStep }
        c.rect(roadL, 0f, roadR, h, 0xFF424242.toInt())
        c.rect(roadL - 4 * dp, 0f, roadL, h, Pal.WHITE); c.rect(roadR, 0f, roadR + 4 * dp, h, Pal.WHITE)
        val dash = h * 0.08f
        for (i in 1 until lanes) {
            val x = roadL + laneW * i
            var y = (scroll % (dash * 2)) - dash * 2
            while (y < h) { c.rect(x - 1.5f * dp, y, x + 1.5f * dp, y + dash, Pal.alpha(Pal.WHITE, 0.7f)); y += dash * 2 }
        }
        for (cn in coins) { c.circle(cn[0], cn[1], laneW * 0.15f, Pal.YELLOW); c.text("$", cn[0], cn[1], laneW * 0.18f, 0xFF8D6E00.toInt()) }
        for (car in traffic) vehicle(c, laneX(car.lane), car.y, car.color, car.truck, false)
        if (crashT <= 0f || (crashT * 10).toInt() % 2 == 0) vehicle(c, px, playerY, if (bike) Pal.RED else Pal.CYAN, false, true)
        fx.draw(c)
        c.rrect(w / 2 - 60 * dp, 10 * dp, w / 2 + 60 * dp, 44 * dp, 12 * dp, Pal.alpha(Pal.BLACK, 0.5f))
        c.text("${dist.toInt()} m", w / 2, 27 * dp, 18 * dp, Pal.WHITE)
        if (msgT > 0f) c.text(msg, w / 2, h * 0.45f, 18 * dp, Pal.alpha(Pal.YELLOW, msgT.coerceAtMost(1f)))
        if (dist < 30f) c.text("Giữ trái / phải để lái", w / 2, h * 0.6f, 16 * dp, Pal.WHITE)
    }
}

class CarRacingGame : TrafficRacer() {
    override val bike = false
    override val help = "Lái xe trên đường cao tốc đông đúc. Giữ nửa trái/phải màn hình để chuyển làn. Tránh va chạm, nhặt xu $ để thêm điểm. Tốc độ tăng dần."
}

class MotoRushGame : TrafficRacer() {
    override val bike = true
    override val lanes = 5
    override val help = "Phóng mô tô luồn lách giữa dòng xe. Giữ nửa trái/phải màn hình để nghiêng xe. Vượt sát xe khác được thưởng 'Suýt nữa', nhặt xu $ thêm điểm. Đừng đâm xe!"
}
