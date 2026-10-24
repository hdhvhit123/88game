package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.poly
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class SpaceShooterGame : Game() {
    override val realtime = true
    override val help = "Kéo ngón tay ở bất kỳ đâu để điều khiển phi thuyền, súng tự động bắn. Tiêu diệt phi thuyền địch, né đạn của chúng. " +
        "Nhặt ⚡ để nâng cấp súng, 🛡 để có khiên. Cứ 10 đợt sẽ có trùm."

    private class Enemy(var x: Float, var y: Float, val kind: Int, var hp: Int, val phase: Float, var fire: Float)
    private class Shot(var x: Float, var y: Float, val vx: Float, val vy: Float)
    private class Item(var x: Float, var y: Float, val kind: Int)

    private val enemies = ArrayList<Enemy>()
    private val shots = ArrayList<Shot>()
    private val enemyShots = ArrayList<Shot>()
    private val items = ArrayList<Item>()
    private val fx = Particles()
    private val stars = Array(60) { floatArrayOf(Random.nextFloat(), Random.nextFloat(), 0.3f + Random.nextFloat()) }
    private var sx = 0f; private var sy = 0f; private var lastX = 0f; private var lastY = 0f
    private var fireT = 0f; private var power = 1; private var shield = 0f; private var lives = 3; private var hurt = 0f
    private var wave = 0; private var waveT = 0f; private var points = 0; private var t = 0f

    override fun newGame() {
        enemies.clear(); shots.clear(); enemyShots.clear(); items.clear(); fx.clear()
        power = 1; shield = 0f; lives = 3; wave = 0; waveT = 1f; points = 0; hurt = 0f
        sx = w / 2; sy = h * 0.85f; score(0); status()
    }

    override fun layout() { if (sx == 0f) { sx = w / 2; sy = h * 0.85f } }

    private fun status() = info("Đợt $wave · ❤ $lives · Súng cấp $power")

    override fun down(x: Float, y: Float) { lastX = x; lastY = y }
    override fun move(x: Float, y: Float) {
        sx = (sx + (x - lastX) * 1.2f).coerceIn(20 * dp, w - 20 * dp)
        sy = (sy + (y - lastY) * 1.2f).coerceIn(h * 0.3f, h - 30 * dp)
        lastX = x; lastY = y
    }

    private fun nextWave() {
        wave++; status()
        if (wave % 10 == 0) { enemies.add(Enemy(w / 2, -80 * dp, 3, 60 + wave * 6, 0f, 1.5f)); info("⚠ TRÙM xuất hiện!"); return }
        val n = 5 + wave
        for (i in 0 until n) {
            val kind = when { wave > 4 && i % 4 == 0 -> 2; wave > 1 && i % 2 == 0 -> 1; else -> 0 }
            enemies.add(Enemy(w * (0.1f + 0.8f * Random.nextFloat()), -30 * dp - i * 40 * dp, kind, kind + 1, Random.nextFloat() * 6, 1f + Random.nextFloat() * 2))
        }
    }

    override fun update(dt: Float) {
        t += dt; fx.update(dt)
        for (s in stars) { s[1] += s[2] * dt * 0.25f; if (s[1] > 1f) { s[1] = 0f; s[0] = Random.nextFloat() } }
        if (shield > 0) shield -= dt
        if (hurt > 0) hurt -= dt
        fireT -= dt
        if (fireT <= 0f) {
            fireT = 0.16f
            val v = -h * 1.2f
            when (power) {
                1 -> shots.add(Shot(sx, sy - 20 * dp, 0f, v))
                2 -> { shots.add(Shot(sx - 8 * dp, sy - 18 * dp, 0f, v)); shots.add(Shot(sx + 8 * dp, sy - 18 * dp, 0f, v)) }
                else -> { shots.add(Shot(sx, sy - 20 * dp, 0f, v)); shots.add(Shot(sx - 10 * dp, sy - 16 * dp, -h * 0.15f, v)); shots.add(Shot(sx + 10 * dp, sy - 16 * dp, h * 0.15f, v)) }
            }
            sfx(Sfx.SHOOT)
        }
        for (s in shots) { s.x += s.vx * dt; s.y += s.vy * dt }
        shots.removeAll { it.y < -20 || it.x < -20 || it.x > w + 20 }
        for (s in enemyShots) { s.x += s.vx * dt; s.y += s.vy * dt }
        enemyShots.removeAll { it.y > h + 20 || it.y < -40 || it.x < -20 || it.x > w + 20 }
        for (e in enemies) {
            when (e.kind) {
                0 -> { e.y += h * 0.18f * dt; e.x += sin(t * 2 + e.phase) * w * 0.15f * dt }
                1 -> { e.y += h * 0.12f * dt; e.x += sin(t * 3 + e.phase) * w * 0.35f * dt }
                2 -> { e.y += if (e.y < h * 0.25f) h * 0.15f * dt else 0f; e.x += sin(t + e.phase) * w * 0.2f * dt }
                3 -> { if (e.y < h * 0.18f) e.y += h * 0.1f * dt; e.x = w / 2 + sin(t * 0.8f) * w * 0.3f }
            }
            e.fire -= dt
            if (e.fire <= 0f && e.y > 0) {
                if (e.kind == 3) {
                    for (k in -3..3) enemyShots.add(Shot(e.x, e.y + 30 * dp, k * h * 0.08f, h * 0.35f))
                    e.fire = 1.3f
                } else if (e.kind >= 1) {
                    val dx = sx - e.x; val dy = sy - e.y; val d = hypot(dx, dy).coerceAtLeast(1f)
                    enemyShots.add(Shot(e.x, e.y, dx / d * h * 0.4f, dy / d * h * 0.4f))
                    e.fire = 1.6f + Random.nextFloat() * 1.5f
                } else e.fire = 99f
            }
        }
        // hits
        val sit = shots.iterator()
        while (sit.hasNext()) {
            val s = sit.next()
            val e = enemies.firstOrNull { abs(it.x - s.x) < size(it) && abs(it.y - s.y) < size(it) } ?: continue
            sit.remove(); e.hp--
            fx.burst(s.x, s.y, Pal.YELLOW, 3, 80 * dp, 2 * dp, 0.25f)
            if (e.hp <= 0) kill(e)
        }
        enemies.removeAll { it.hp <= 0 || it.y > h + 40 * dp }
        for (it in items) it.y += h * 0.15f * dt
        val iit = items.iterator()
        while (iit.hasNext()) {
            val i = iit.next()
            if (hypot(i.x - sx, i.y - sy) < 30 * dp) {
                if (i.kind == 0) power = (power + 1).coerceAtMost(3) else shield = 8f
                sfx(Sfx.COIN); iit.remove(); status()
            } else if (i.y > h) iit.remove()
        }
        if (hurt <= 0f) {
            val hit = enemyShots.firstOrNull { hypot(it.x - sx, it.y - sy) < 14 * dp }
                ?: enemies.firstOrNull { hypot(it.x - sx, it.y - sy) < size(it) + 10 * dp }?.also { if (it.kind != 3) { it.hp = 0; kill(it) } }
            if (hit != null) {
                if (hit is Shot) enemyShots.remove(hit)
                if (shield > 0f) { shield = 0f; sfx(Sfx.HIT) }
                else {
                    lives--; hurt = 1.5f; power = (power - 1).coerceAtLeast(1); sfx(Sfx.BOOM); buzz(80)
                    fx.burst(sx, sy, Pal.ORANGE, 24, 220 * dp, 4 * dp)
                    status()
                    if (lives <= 0) { gameOver(false, "Phi thuyền bị phá huỷ", "Đến đợt $wave · $points điểm", points); return }
                }
            }
        }
        if (enemies.isEmpty()) { waveT -= dt; if (waveT <= 0f) { waveT = 1.2f; nextWave() } }
    }

    private fun size(e: Enemy) = when (e.kind) { 3 -> 60 * dp; 2 -> 22 * dp; else -> 17 * dp }

    private fun kill(e: Enemy) {
        val pts = intArrayOf(10, 20, 40, 500)[e.kind]
        points += pts; score(points)
        fx.burst(e.x, e.y, color(e.kind), if (e.kind == 3) 60 else 16, (if (e.kind == 3) 320 else 180) * dp, 4 * dp)
        sfx(if (e.kind == 3) Sfx.WIN else Sfx.BOOM)
        if (Random.nextFloat() < (if (e.kind == 3) 1f else 0.07f)) items.add(Item(e.x, e.y, if (Random.nextFloat() < 0.6f) 0 else 1))
    }

    private fun color(kind: Int) = intArrayOf(Pal.PINK, Pal.ORANGE, Pal.PURPLE, Pal.RED)[kind]

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF070A1A.toInt(), 0xFF141033.toInt())
        for (s in stars) c.circle(s[0] * w, s[1] * h, s[2] * 1.6f * dp, Pal.alpha(Pal.WHITE, s[2] * 0.7f))
        for (i in items) {
            c.circle(i.x, i.y, 14 * dp, if (i.kind == 0) Pal.YELLOW else Pal.CYAN)
            c.text(if (i.kind == 0) "⚡" else "🛡", i.x, i.y, 14 * dp, Pal.WHITE)
        }
        for (e in enemies) {
            val s = size(e); val col = color(e.kind)
            when (e.kind) {
                0 -> { c.poly(col, e.x, e.y + s, e.x - s, e.y - s * 0.6f, e.x, e.y - s * 0.2f, e.x + s, e.y - s * 0.6f); c.circle(e.x, e.y, s * 0.3f, Pal.WHITE) }
                1 -> { c.poly(col, e.x - s, e.y, e.x, e.y - s * 0.7f, e.x + s, e.y, e.x, e.y + s); c.circle(e.x, e.y, s * 0.35f, Pal.YELLOW) }
                2 -> { c.rrect(e.x - s, e.y - s * 0.6f, e.x + s, e.y + s * 0.6f, s * 0.4f, col); c.circle(e.x - s * 0.4f, e.y, s * 0.2f, Pal.WHITE); c.circle(e.x + s * 0.4f, e.y, s * 0.2f, Pal.WHITE) }
                3 -> {
                    c.poly(Pal.shade(col, 0.6f), e.x - s * 1.4f, e.y - s * 0.3f, e.x + s * 1.4f, e.y - s * 0.3f, e.x + s * 0.8f, e.y + s * 0.5f, e.x - s * 0.8f, e.y + s * 0.5f)
                    c.circle(e.x, e.y, s * 0.7f, col); c.circle(e.x, e.y, s * 0.3f, Pal.YELLOW)
                    c.rrect(e.x - s, e.y - s * 0.95f, e.x - s + 2 * s * (e.hp / (60f + wave * 6)), e.y - s * 0.85f, 3 * dp, Pal.GREEN)
                }
            }
        }
        for (s in shots) c.rrect(s.x - 2 * dp, s.y - 8 * dp, s.x + 2 * dp, s.y + 8 * dp, 2 * dp, Pal.CYAN)
        for (s in enemyShots) { c.circle(s.x, s.y, 5 * dp, Pal.alpha(Pal.RED, 0.4f)); c.circle(s.x, s.y, 3 * dp, Pal.YELLOW) }
        if (hurt <= 0f || (hurt * 10).toInt() % 2 == 0) {
            val r = 18 * dp
            c.poly(Pal.alpha(Pal.ORANGE, 0.8f), sx - r * 0.3f, sy + r * 0.6f, sx + r * 0.3f, sy + r * 0.6f, sx, sy + r * (1.1f + 0.3f * sin(t * 30)))
            c.poly(0xFFB0BEC5.toInt(), sx, sy - r, sx + r, sy + r * 0.7f, sx, sy + r * 0.4f, sx - r, sy + r * 0.7f)
            c.poly(Pal.BLUE, sx, sy - r * 0.5f, sx + r * 0.3f, sy + r * 0.2f, sx - r * 0.3f, sy + r * 0.2f)
            if (shield > 0f) c.circle(sx, sy, r * 1.6f, Pal.alpha(Pal.CYAN, 0.25f + 0.1f * sin(t * 8)))
        }
        fx.draw(c)
    }
}
