package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.poly
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.sin
import kotlin.random.Random

class DinoRunGame : Game() {
    override val realtime = true
    override val help = "Khủng long chạy mãi không dừng. Chạm nửa trên màn hình để nhảy qua xương rồng, " +
        "chạm giữ nửa dưới để cúi né chim bay. Càng chạy xa càng nhanh."

    private class Obs(var x: Float, val kind: Int, val size: Int) // kind 0 cactus, 1 bird

    private val obs = ArrayList<Obs>()
    private var y = 0f; private var vy = 0f; private var ducking = false
    private var ground = 0f; private var dx = 0f; private var speed = 0f; private var dist = 0f
    private var spawnIn = 0f; private var started = false; private var t = 0f
    private var night = 0f
    private val stars = FloatArray(30) { Random.nextFloat() }

    override fun newGame() { obs.clear(); y = 0f; vy = 0f; dist = 0f; spawnIn = 1f; started = false; ducking = false; score(0) }

    override fun layout() { ground = h * 0.68f; dx = w * 0.18f }

    private val u get() = 3.2f * dp   // pixel unit for the blocky sprites

    override fun down(x: Float, y0: Float) {
        if (over) return
        if (!started) started = true
        if (y0 > h * 0.6f) ducking = true else jump()
    }

    override fun up(x: Float, y0: Float) { ducking = false }

    private fun jump() { if (y == 0f) { vy = -h * 1.05f; sfx(Sfx.JUMP) } }

    override fun update(dt: Float) {
        t += dt
        if (!started) return
        speed = w * (0.55f + dist / 6000f).coerceAtMost(1.4f)
        dist += speed * dt / dp * 0.05f
        score(dist.toInt())
        if (dist.toInt() % 100 == 0 && dist > 50 && (dist - speed * dt / dp * 0.05f).toInt() % 100 != 0) sfx(Sfx.TICK)
        night = ((dist / 700).toInt() % 2).toFloat().let { target -> night + (target - night) * dt * 1.5f }
        val g = h * (if (ducking && y < 0) 7f else 3.4f)
        vy += g * dt; y += vy * dt
        if (y > 0f) { y = 0f; vy = 0f }
        for (o in obs) o.x -= speed * dt
        obs.removeAll { it.x < -60 * dp }
        spawnIn -= dt
        if (spawnIn <= 0f) {
            val bird = dist > 300 && Random.nextFloat() < 0.3f
            obs.add(Obs(w + 30 * dp, if (bird) 1 else 0, if (bird) Random.nextInt(3) else 1 + Random.nextInt(3)))
            spawnIn = (0.7f + Random.nextFloat() * 1.0f) * (w * 0.6f / speed)
        }
        // collision with shrunken hitboxes
        val dw = 12 * u; val dh = if (ducking && y == 0f) 8 * u else 14 * u
        val l = dx + 2 * u; val r = dx + dw - 2 * u; val top = ground + y - dh + u; val bot = ground + y
        for (o in obs) {
            val (ol, ot, or, ob) = box(o)
            if (r > ol && l < or && bot > ot && top < ob) {
                sfx(Sfx.HIT); buzz(60)
                gameOver(false, "Va chạm!", "Bạn chạy được ${dist.toInt()} m", dist.toInt()); return
            }
        }
    }

    private fun box(o: Obs): FloatArray = if (o.kind == 0) {
        val ww = (4 + o.size * 3) * u; val hh = (10 + o.size * 2) * u
        floatArrayOf(o.x + u, ground - hh, o.x + ww - u, ground)
    } else {
        val by = ground - intArrayOf(6, 13, 20)[o.size] * u
        floatArrayOf(o.x + u, by - 6 * u, o.x + 13 * u, by)
    }

    private operator fun FloatArray.component4() = this[3]

    override fun draw(c: Canvas) {
        val sky = Pal.mix(0xFFF7F7F7.toInt(), 0xFF101426.toInt(), night)
        val ink = Pal.mix(0xFF535353.toInt(), 0xFFE0E0E0.toInt(), night)
        c.rect(0f, 0f, w, h, sky)
        if (night > 0.05f) for (i in 0 until 15) c.circle(stars[i] * w, stars[i + 15] * ground * 0.8f, 1.5f * dp, Pal.alpha(Pal.WHITE, night))
        if (night > 0.05f) { c.circle(w * 0.8f, h * 0.15f, 18 * dp, Pal.alpha(0xFFFFF59D.toInt(), night)); c.circle(w * 0.8f + 8 * dp, h * 0.15f - 5 * dp, 16 * dp, Pal.alpha(sky, night)) }
        // clouds and ground texture
        val off = dist * dp * 20
        for (i in 0 until 4) {
            val cx = ((i * w * 0.4f - off * 0.1f) % (w * 1.6f) + w * 1.6f) % (w * 1.6f) - w * 0.2f
            c.rrect(cx, h * (0.18f + i * 0.07f), cx + 46 * dp, h * (0.18f + i * 0.07f) + 12 * dp, 6 * dp, Pal.alpha(ink, 0.15f))
        }
        c.line(0f, ground, w, ground, 1.5f * dp, ink)
        for (i in 0 until 40) {
            val gx = ((stars[i % 30] * w * 2 + i * 37 * dp - off) % w + w) % w
            c.rect(gx, ground + 5 * dp + (i % 4) * 5 * dp, gx + (2 + i % 3) * dp, ground + 6.5f * dp + (i % 4) * 5 * dp, ink)
        }
        for (o in obs) {
            if (o.kind == 0) {
                for (k in 0 until o.size) {
                    val cx = o.x + k * 3 * u; val hh = (10 + o.size * 2) * u - (k % 2) * 3 * u
                    c.rrect(cx + 1.5f * u, ground - hh, cx + 4 * u, ground, u, 0xFF43A047.toInt())
                    c.rrect(cx, ground - hh * 0.65f, cx + u * 1.5f, ground - hh * 0.35f, u * 0.6f, 0xFF388E3C.toInt())
                    c.rrect(cx + 4 * u, ground - hh * 0.75f, cx + 5.5f * u, ground - hh * 0.45f, u * 0.6f, 0xFF388E3C.toInt())
                }
            } else {
                val b = box(o)
                val flap = sin(t * 12) > 0
                c.rrect(b[0] + 2 * u, b[1] + 2 * u, b[0] + 12 * u, b[1] + 5 * u, 2 * u, ink)
                c.poly(ink, b[0], b[1] + 3 * u, b[0] + 3 * u, b[1] + 2 * u, b[0] + 3 * u, b[1] + 4.5f * u)
                if (flap) c.poly(ink, b[0] + 5 * u, b[1] + 2 * u, b[0] + 9 * u, b[1] + 2 * u, b[0] + 6 * u, b[1] - 4 * u)
                else c.poly(ink, b[0] + 5 * u, b[1] + 4 * u, b[0] + 9 * u, b[1] + 4 * u, b[0] + 6 * u, b[1] + 9 * u)
            }
        }
        // dino
        val gy = ground + y
        val leg = if (started && y == 0f && !over) ((t * 12).toInt() % 2) else -1
        if (ducking && y == 0f) {
            c.rrect(dx, gy - 8 * u, dx + 16 * u, gy - 2 * u, 2 * u, ink)
            c.rrect(dx + 12 * u, gy - 9 * u, dx + 20 * u, gy - 4 * u, 1.5f * u, ink)
            c.rect(dx + 16 * u, gy - 8 * u, dx + 17 * u, gy - 7 * u, sky)
        } else {
            c.rrect(dx + 6 * u, gy - 16 * u, dx + 14 * u, gy - 10 * u, 1.5f * u, ink)        // head
            c.rect(dx + 8 * u, gy - 15 * u, dx + 9 * u, gy - 14 * u, sky)                        // eye
            c.rect(dx + 10 * u, gy - 11 * u, dx + 14 * u, gy - 10 * u, ink)
            c.rrect(dx + 1 * u, gy - 11 * u, dx + 10 * u, gy - 3 * u, 2 * u, ink)               // body
            c.rect(dx - 2 * u, gy - 10 * u, dx + 2 * u, gy - 7 * u, ink)                          // tail
            c.rect(dx + 9 * u, gy - 8 * u, dx + 11 * u, gy - 7 * u, ink)                          // arm
        }
        if (!(ducking && y == 0f)) {
            c.rect(dx + 3 * u, gy - 3 * u, dx + 5 * u, gy - if (leg == 0) 1 * u else 0f, ink)
            c.rect(dx + 7 * u, gy - 3 * u, dx + 9 * u, gy - if (leg == 1) 1 * u else 0f, ink)
        }
        c.text("${dist.toInt()} m", w - 16 * dp, 24 * dp, 16 * dp, ink, android.graphics.Paint.Align.RIGHT)
        if (!started && !over) c.text("Chạm để bắt đầu chạy", w / 2, h * 0.35f, 20 * dp, ink)
    }
}
