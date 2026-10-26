package com.hgame.games.casual

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.Gfx
import com.hgame.engine.rect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class WhackAMoleGame : Game() {
    override val realtime = true
    override val help = "Chuột chũi sẽ trồi lên từ các hang. Chạm thật nhanh để đập chúng trong 45 giây. " +
        "Đừng đập nhầm quả bom 💣!"

    private class Hole(var x: Float = 0f, var y: Float = 0f) {
        var up = 0f        // 0 hidden .. 1 fully up
        var state = 0      // 0 idle, 1 rising, 2 waiting, 3 hiding
        var timer = 0f
        var bomb = false
        var hit = 0f
    }

    private val holes = List(9) { Hole() }
    private var time = 45f
    private var points = 0
    private var spawnTimer = 0f
    private var hr = 0f
    private var combo = 0
    private val pops = ArrayList<FloatArray>()   // x, y, life, value

    override fun newGame() {
        holes.forEach { it.up = 0f; it.state = 0; it.hit = 0f }
        time = 45f; points = 0; spawnTimer = 0.5f; combo = 0
        pops.clear()
        score(0)
    }

    override fun layout() {
        val gw = min(w, h * 0.7f)
        hr = gw / 7.5f
        val top = h * 0.25f
        holes.forEachIndexed { i, hole ->
            hole.x = w / 2 + (i % 3 - 1) * gw / 3.1f
            hole.y = top + (i / 3) * (h * 0.7f / 3.2f) + hr
        }
    }

    override fun update(dt: Float) {
        time -= dt
        info("⏱ ${time.toInt().coerceAtLeast(0)}s")
        if (time <= 0f) { gameOver(true, "Hết giờ!", "Bạn đập được $points điểm", points); return }
        val speed = 1f + (45f - time) / 30f
        spawnTimer -= dt
        if (spawnTimer <= 0f) {
            spawnTimer = (0.9f - (45f - time) * 0.012f).coerceAtLeast(0.35f) * (0.6f + Random.nextFloat() * 0.6f)
            holes.filter { it.state == 0 }.randomOrNull()?.let {
                it.state = 1; it.bomb = Random.nextFloat() < 0.15f; it.hit = 0f
                it.timer = (1.1f - (45f - time) * 0.012f).coerceAtLeast(0.55f)
            }
        }
        for (hole in holes) {
            when (hole.state) {
                1 -> { hole.up += dt * 6f * speed; if (hole.up >= 1f) { hole.up = 1f; hole.state = 2 } }
                2 -> { hole.timer -= dt; if (hole.timer <= 0f) { hole.state = 3; if (!hole.bomb && hole.hit == 0f) combo = 0 } }
                3 -> { hole.up -= dt * 5f; if (hole.up <= 0f) { hole.up = 0f; hole.state = 0 } }
            }
            if (hole.hit > 0f) hole.hit -= dt
        }
        pops.removeAll { it[2] -= dt; it[2] <= 0f }
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        for (hole in holes) {
            if (hole.up < 0.4f || hole.hit > 0f || hole.state == 0) continue
            if (x in hole.x - hr..hole.x + hr && y in hole.y - hr * 2f..hole.y + hr * 0.4f) {
                hole.hit = 0.3f; hole.state = 3
                if (hole.bomb) {
                    points = maxOf(0, points - 5); combo = 0
                    sfx(Sfx.BOOM); buzz(80)
                    pops.add(floatArrayOf(hole.x, hole.y - hr * 1.5f, 0.8f, -5f))
                } else {
                    combo++
                    val v = 1 + combo / 5
                    points += v
                    sfx(Sfx.HIT); buzz(20)
                    pops.add(floatArrayOf(hole.x, hole.y - hr * 1.5f, 0.8f, v.toFloat()))
                }
                score(points)
                return
            }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF7BD389.toInt(), 0xFF3C9D58.toInt())
        c.text("Combo ×${1 + combo / 5}", w / 2, h * 0.08f, 18 * dp, Pal.alpha(Pal.WHITE, 0.9f))
        // time bar
        c.rect(20 * dp, h * 0.13f, w - 20 * dp, h * 0.13f + 8 * dp, Pal.alpha(Pal.BLACK, 0.2f))
        c.rect(20 * dp, h * 0.13f, 20 * dp + (w - 40 * dp) * (time / 45f).coerceIn(0f, 1f), h * 0.13f + 8 * dp, Pal.YELLOW)
        for (hole in holes) {
            Gfx.rect.set(hole.x - hr, hole.y - hr * 0.35f, hole.x + hr, hole.y + hr * 0.35f)
            c.drawOval(Gfx.rect, Gfx.fillPaint(0xFF3B2414.toInt()))
            if (hole.up > 0f) {
                c.save()
                c.clipRect(hole.x - hr * 1.2f, hole.y - hr * 3f, hole.x + hr * 1.2f, hole.y)
                val my = hole.y + hr * 1.1f - hole.up * hr * 1.9f
                if (hole.bomb) {
                    c.circle(hole.x, my, hr * 0.72f, 0xFF222433.toInt())
                    c.circle(hole.x - hr * 0.25f, my - hr * 0.25f, hr * 0.15f, Pal.alpha(Pal.WHITE, 0.5f))
                    c.rect(hole.x - hr * 0.1f, my - hr * 0.95f, hole.x + hr * 0.1f, my - hr * 0.65f, 0xFF8D6E63.toInt())
                    c.circle(hole.x, my - hr * 1.0f, hr * 0.12f, Pal.ORANGE)
                } else {
                    val body = if (hole.hit > 0f) 0xFFB07A55.toInt() else 0xFF8D5A3B.toInt()
                    Gfx.rect.set(hole.x - hr * 0.7f, my - hr * 0.8f, hole.x + hr * 0.7f, my + hr * 1.4f)
                    c.drawRoundRect(Gfx.rect, hr * 0.7f, hr * 0.7f, Gfx.fillPaint(body))
                    c.circle(hole.x, my + hr * 0.05f, hr * 0.32f, 0xFFD9A47E.toInt())
                    c.circle(hole.x, my - hr * 0.08f, hr * 0.12f, 0xFF3A2015.toInt())
                    if (hole.hit > 0f) {
                        c.text("✕", hole.x - hr * 0.3f, my - hr * 0.35f, hr * 0.3f, Pal.BLACK)
                        c.text("✕", hole.x + hr * 0.3f, my - hr * 0.35f, hr * 0.3f, Pal.BLACK)
                    } else {
                        c.circle(hole.x - hr * 0.28f, my - hr * 0.35f, hr * 0.1f, Pal.BLACK)
                        c.circle(hole.x + hr * 0.28f, my - hr * 0.35f, hr * 0.1f, Pal.BLACK)
                    }
                }
                c.restore()
            }
            Gfx.rect.set(hole.x - hr * 1.05f, hole.y - hr * 0.05f, hole.x + hr * 1.05f, hole.y + hr * 0.45f)
            c.drawArc(Gfx.rect, 0f, 180f, true, Gfx.fillPaint(0xFF2E7D32.toInt()))
        }
        for (p in pops) {
            val a = p[2] / 0.8f
            val s = if (p[3] > 0) "+${p[3].toInt()}" else "${p[3].toInt()}"
            c.text(s, p[0], p[1] - (1 - a) * 40 * dp, 24 * dp, Pal.alpha(if (p[3] > 0) Pal.YELLOW else Pal.RED, a))
        }
    }
}
