package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.text
import kotlin.math.hypot
import kotlin.random.Random

class FruitSliceGame : Game() {
    override val realtime = true
    override val help = "Vuốt ngón tay để chém trái cây bay lên. Chém nhiều quả trong một nhát được thưởng combo. " +
        "Để rơi 3 quả là thua, chém trúng bom 💣 cũng thua!"

    private val fruits = arrayOf("🍎", "🍊", "🍋", "🍉", "🍇", "🍑", "🍍", "🥝", "🍓", "🍌")
    private val juice = intArrayOf(Pal.RED, Pal.ORANGE, Pal.YELLOW, Pal.PINK, Pal.PURPLE, 0xFFFFAB91.toInt(), Pal.YELLOW, Pal.GREEN, Pal.RED, Pal.YELLOW)
    private class Obj(var x: Float, var y: Float, var vx: Float, var vy: Float, val kind: Int, var rot: Float, val spin: Float, var cut: Boolean = false, var half: Int = 0)

    private val objs = ArrayList<Obj>()
    private val trail = ArrayList<FloatArray>()  // x, y, life
    private val fx = Particles().apply { gravity = 600f }
    private var spawnT = 1f; private var points = 0; private var misses = 0; private var t = 0f
    private var lastX = 0f; private var lastY = 0f; private var touching = false; private var sliceCount = 0
    private var comboText = ""; private var comboT = 0f; private var comboX = 0f; private var comboY = 0f

    override fun newGame() { objs.clear(); trail.clear(); fx.clear(); spawnT = 1f; points = 0; misses = 0; score(0); status() }

    private fun status() = info("Bỏ lỡ: " + "❌".repeat(misses) + "⭕".repeat(3 - misses))

    override fun down(x: Float, y: Float) { touching = true; lastX = x; lastY = y; sliceCount = 0; trail.add(floatArrayOf(x, y, 0.25f)) }

    override fun move(x: Float, y: Float) {
        if (!touching || over) return
        trail.add(floatArrayOf(x, y, 0.25f))
        val r = w * 0.075f
        val halves = ArrayList<Obj>()
        for (o in objs) {
            if (o.cut) continue
            if (segDist(o.x, o.y, lastX, lastY, x, y) < r) {
                if (o.kind < 0) {
                    o.cut = true; sfx(Sfx.BOOM); buzz(150)
                    fx.burst(o.x, o.y, Pal.ORANGE, 40, 400 * dp, 5 * dp)
                    gameOver(false, "Bùm! Trúng bom", "Bạn ghi được $points điểm", points); return
                }
                o.cut = true; sliceCount++; points++
                val a = Math.atan2((y - lastY).toDouble(), (x - lastX).toDouble())
                halves.add(Obj(o.x, o.y, o.vx - (Math.sin(a) * 120 * dp).toFloat(), o.vy, o.kind, o.rot, o.spin - 3, true, 1))
                o.half = 2; o.vx += (Math.sin(a) * 120 * dp).toFloat()
                fx.burst(o.x, o.y, juice[o.kind], 14, 220 * dp, 4 * dp)
                sfx(Sfx.POP)
            }
        }
        objs.addAll(halves)
        if (sliceCount >= 3) { comboText = "Combo ×$sliceCount  +$sliceCount"; comboT = 1f; comboX = x; comboY = y; points += sliceCount; sliceCount = -100; sfx(Sfx.COIN) }
        score(points)
        lastX = x; lastY = y
    }

    override fun up(x: Float, y: Float) { touching = false }

    private fun segDist(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax; val dy = by - ay; val l2 = dx * dx + dy * dy
        val tt = if (l2 == 0f) 0f else (((px - ax) * dx + (py - ay) * dy) / l2).coerceIn(0f, 1f)
        return hypot(px - (ax + tt * dx), py - (ay + tt * dy))
    }

    override fun update(dt: Float) {
        t += dt; fx.update(dt); if (comboT > 0) comboT -= dt
        trail.removeAll { it[2] -= dt; it[2] <= 0 }
        spawnT -= dt
        if (spawnT <= 0f) {
            val diff = (points / 80f).coerceAtMost(1f)
            spawnT = 1.4f - diff * 0.7f
            val n = 1 + Random.nextInt(2 + (diff * 3).toInt())
            repeat(n) {
                val bomb = points > 5 && Random.nextFloat() < 0.12f + diff * 0.08f
                val x = w * (0.15f + Random.nextFloat() * 0.7f)
                objs.add(Obj(x, h + 30 * dp, (w / 2 - x) * (0.4f + Random.nextFloat() * 0.6f), -h * (1.05f + Random.nextFloat() * 0.3f),
                    if (bomb) -1 else Random.nextInt(fruits.size), 0f, (Random.nextFloat() - 0.5f) * 8))
            }
            sfx(Sfx.TICK)
        }
        val g = h * 1.1f
        for (o in objs) { o.vy += g * dt; o.x += o.vx * dt; o.y += o.vy * dt; o.rot += o.spin * dt }
        val it = objs.iterator()
        while (it.hasNext()) {
            val o = it.next()
            if (o.y > h + 60 * dp && o.vy > 0) {
                if (!o.cut && o.kind >= 0) { misses++; status(); sfx(Sfx.ERROR); if (misses >= 3) { it.remove(); gameOver(false, "Rơi mất 3 quả!", "Bạn ghi được $points điểm", points); return } }
                it.remove()
            }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF4E342E.toInt(), 0xFF2E1B14.toInt())
        for (k in 0 until 6) c.circle(w * (k / 5f), h * 0.5f + (k % 2) * 30 * dp, w * 0.4f, Pal.alpha(0xFF3E2723.toInt(), 0.25f))
        val size = w * 0.16f
        for (o in objs) {
            c.save(); c.rotate(Math.toDegrees(o.rot.toDouble()).toFloat(), o.x, o.y)
            if (o.half != 0) { c.clipRect(if (o.half == 1) o.x - size else o.x, o.y - size, if (o.half == 1) o.x else o.x + size, o.y + size) }
            if (o.kind < 0) {
                c.circle(o.x, o.y, size * 0.4f, 0xFF212121.toInt())
                c.text("💣", o.x, o.y, size * 0.9f, Pal.WHITE)
                if ((t * 10).toInt() % 2 == 0) c.circle(o.x + size * 0.28f, o.y - size * 0.38f, size * 0.08f, Pal.YELLOW)
            } else c.text(fruits[o.kind], o.x, o.y, size, Pal.WHITE)
            c.restore()
        }
        fx.draw(c)
        if (trail.size > 1) {
            val p = Gfx.stroke.apply { shader = null; strokeCap = android.graphics.Paint.Cap.ROUND }
            for (i in 1 until trail.size) {
                val a = trail[i - 1]; val b = trail[i]
                p.strokeWidth = 10 * dp * (b[2] / 0.25f); p.color = Pal.alpha(Pal.WHITE, b[2] / 0.25f * 0.9f)
                c.drawLine(a[0], a[1], b[0], b[1], p)
            }
        }
        if (comboT > 0f) c.text(comboText, comboX.coerceIn(80 * dp, w - 80 * dp), comboY - 30 * dp, 20 * dp, Pal.alpha(Pal.YELLOW, comboT))
        c.text("$points", 20 * dp, 34 * dp, 30 * dp, Pal.YELLOW, android.graphics.Paint.Align.LEFT)
    }
}
