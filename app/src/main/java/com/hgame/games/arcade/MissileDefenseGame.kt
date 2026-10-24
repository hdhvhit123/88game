package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.poly
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.hypot
import kotlin.random.Random

class MissileDefenseGame : Game() {
    override val realtime = true
    override val help = "Tên lửa địch đang lao xuống các thành phố. Chạm vào bầu trời để bắn tên lửa đánh chặn, vụ nổ sẽ phá huỷ mọi thứ bay vào. " +
        "Mỗi bệ phóng có số đạn giới hạn mỗi đợt. Mất hết 6 thành phố là thua."

    private class Missile(val sx: Float, val sy: Float, val tx: Float, val ty: Float, val speed: Float, var p: Float = 0f) {
        val len = hypot(tx - sx, ty - sy)
        val x get() = sx + (tx - sx) * p
        val y get() = sy + (ty - sy) * p
    }
    private class Blast(val x: Float, val y: Float, var t: Float = 0f, val friendly: Boolean)

    private val incoming = ArrayList<Missile>(); private val mine = ArrayList<Missile>(); private val blasts = ArrayList<Blast>()
    private val cities = BooleanArray(6); private val ammo = IntArray(3)
    private var ground = 0f; private var wave = 0; private var toSpawn = 0; private var spawnT = 0f
    private var points = 0; private var pause = 0f

    override fun newGame() {
        incoming.clear(); mine.clear(); blasts.clear(); cities.fill(true); wave = 0; points = 0; score(0)
        startWave()
    }

    private fun startWave() {
        wave++; toSpawn = 8 + wave * 3; spawnT = 1f; ammo.fill(10); pause = 0f
        info("Đợt $wave")
    }

    override fun layout() { ground = h - 50 * dp }

    private fun cityX(i: Int) = w * (listOf(0.14f, 0.24f, 0.34f, 0.66f, 0.76f, 0.86f)[i])
    private fun baseX(i: Int) = w * floatArrayOf(0.04f, 0.5f, 0.96f)[i]

    override fun down(x: Float, y: Float) {
        if (over || y > ground - 30 * dp) return
        val order = (0..2).sortedBy { kotlin.math.abs(baseX(it) - x) }
        val b = order.firstOrNull { ammo[it] > 0 } ?: run { sfx(Sfx.ERROR); return }
        ammo[b]--
        mine.add(Missile(baseX(b), ground - 20 * dp, x, y, h * 1.4f))
        sfx(Sfx.SHOOT)
    }

    override fun update(dt: Float) {
        if (toSpawn > 0) {
            spawnT -= dt
            if (spawnT <= 0f) {
                spawnT = (1.6f - wave * 0.1f).coerceAtLeast(0.45f) * (0.5f + Random.nextFloat())
                toSpawn--
                val targets = (0 until 6).filter { cities[it] }.map { cityX(it) } + (0..2).map { baseX(it) }
                incoming.add(Missile(Random.nextFloat() * w, -10f, targets.random(), ground, h * (0.07f + wave * 0.012f)))
            }
        }
        for (m in incoming) m.p += m.speed * dt / m.len
        for (m in mine) m.p += m.speed * dt / m.len
        mine.filter { it.p >= 1f }.forEach { blasts.add(Blast(it.tx, it.ty, friendly = true)); sfx(Sfx.BOOM) }
        mine.removeAll { it.p >= 1f }
        incoming.filter { it.p >= 1f }.forEach { m ->
            blasts.add(Blast(m.tx, m.ty, friendly = false)); sfx(Sfx.HIT); buzz(40)
            for (i in 0 until 6) if (cities[i] && kotlin.math.abs(cityX(i) - m.tx) < 20 * dp) cities[i] = false
            for (i in 0..2) if (kotlin.math.abs(baseX(i) - m.tx) < 20 * dp) ammo[i] = 0
        }
        incoming.removeAll { it.p >= 1f }
        for (b in blasts) b.t += dt
        blasts.removeAll { it.t > 1.4f }
        val dead = incoming.filter { m -> blasts.any { b -> b.friendly && hypot(m.x - b.x, m.y - b.y) < radius(b) } }
        for (m in dead) { blasts.add(Blast(m.x, m.y, friendly = true)); points += 25; score(points); sfx(Sfx.POP) }
        incoming.removeAll(dead)
        if (cities.none { it }) { gameOver(false, "Các thành phố đã sụp đổ", "Trụ được $wave đợt · $points điểm", points); return }
        if (toSpawn == 0 && incoming.isEmpty() && blasts.isEmpty() && mine.isEmpty()) {
            pause += dt
            if (pause > 0.6f) {
                val bonus = cities.count { it } * 100 + ammo.sum() * 5
                points += bonus; score(points); sfx(Sfx.WIN)
                info("Thưởng đợt $wave: +$bonus")
                startWave()
            }
        }
    }

    private fun radius(b: Blast): Float {
        val k = if (b.t < 0.5f) b.t / 0.5f else (1.4f - b.t) / 0.9f
        return 42 * dp * k.coerceIn(0f, 1f)
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF0A0F2C.toInt(), 0xFF3A1C4A.toInt())
        c.rect(0f, ground, w, h, 0xFF4E342E.toInt())
        c.rect(0f, ground, w, ground + 4 * dp, 0xFF8D6E63.toInt())
        for (i in 0 until 6) {
            val x = cityX(i)
            if (cities[i]) {
                c.rect(x - 16 * dp, ground - 14 * dp, x - 6 * dp, ground, Pal.CYAN)
                c.rect(x - 6 * dp, ground - 24 * dp, x + 4 * dp, ground, Pal.BLUE)
                c.rect(x + 4 * dp, ground - 18 * dp, x + 16 * dp, ground, Pal.TEAL)
                for (k in 0..2) c.rect(x - 3 * dp, ground - (20 - k * 6) * dp, x + 1 * dp, ground - (18 - k * 6) * dp, Pal.YELLOW)
            } else c.poly(0xFF3E2723.toInt(), x - 16 * dp, ground, x - 8 * dp, ground - 6 * dp, x, ground - 3 * dp, x + 10 * dp, ground - 7 * dp, x + 16 * dp, ground)
        }
        for (i in 0..2) {
            val x = baseX(i)
            c.poly(0xFF6D4C41.toInt(), x - 22 * dp, ground, x - 10 * dp, ground - 20 * dp, x + 10 * dp, ground - 20 * dp, x + 22 * dp, ground)
            c.text("${ammo[i]}", x, ground + 22 * dp, 13 * dp, if (ammo[i] > 0) Pal.WHITE else Pal.RED)
        }
        for (m in incoming) { c.line(m.sx, m.sy, m.x, m.y, 1.5f * dp, Pal.alpha(Pal.RED, 0.7f)); c.circle(m.x, m.y, 3 * dp, Pal.WHITE) }
        for (m in mine) {
            c.line(m.sx, m.sy, m.x, m.y, 1.5f * dp, Pal.alpha(Pal.CYAN, 0.7f)); c.circle(m.x, m.y, 2.5f * dp, Pal.CYAN)
            c.line(m.tx - 5 * dp, m.ty - 5 * dp, m.tx + 5 * dp, m.ty + 5 * dp, 1.5f * dp, Pal.CYAN); c.line(m.tx - 5 * dp, m.ty + 5 * dp, m.tx + 5 * dp, m.ty - 5 * dp, 1.5f * dp, Pal.CYAN)
        }
        for (b in blasts) {
            val r = radius(b)
            c.circle(b.x, b.y, r, Pal.alpha(if (b.friendly) Pal.YELLOW else Pal.ORANGE, 0.75f))
            c.circle(b.x, b.y, r * 0.6f, Pal.alpha(Pal.WHITE, 0.7f))
        }
        if (wave == 1 && points == 0 && mine.isEmpty() && ammo.sum() == 30) {
            c.rrect(w * 0.12f, h * 0.4f - 22 * dp, w * 0.88f, h * 0.4f + 22 * dp, 12 * dp, Pal.alpha(Pal.BLACK, 0.4f))
            c.text("Chạm bầu trời để bắn chặn", w / 2, h * 0.4f, 16 * dp, Pal.WHITE)
        }
    }
}
