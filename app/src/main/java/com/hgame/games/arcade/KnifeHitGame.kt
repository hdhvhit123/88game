package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.poly
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class KnifeHitGame : Game() {
    override val realtime = true
    override val help = "Chạm để phóng dao vào khúc gỗ đang xoay. Đừng để dao chạm vào dao đã cắm trước đó! " +
        "Phóng hết số dao để qua màn. Chém trúng 🍎 để thêm điểm. Mỗi 5 màn có trùm xoay khó đoán."

    private val knives = ArrayList<Float>()      // angles (deg, log space) of stuck knives
    private val apples = ArrayList<Float>()
    private var rot = 0f; private var speed = 0f; private var level = 1; private var left = 0; private var points = 0
    private var flyY = -1f; private var bounce: FloatArray? = null
    private var t = 0f; private var shake = 0f; private var breakT = 0f
    private val fx = Particles()
    private var logR = 0f; private var logY = 0f; private var knifeLen = 0f

    override fun newGame() { level = 1; points = 0; score(0); setup() }

    private fun setup() {
        knives.clear(); apples.clear(); fx.clear(); flyY = -1f; bounce = null; breakT = 0f
        left = 5 + level.coerceAtMost(6)
        val pre = ((level - 1) / 2).coerceAtMost(4)
        repeat(pre) { knives.add(Random.nextFloat() * 360f) }
        if (Random.nextFloat() < 0.7f) apples.add(Random.nextFloat() * 360f)
        speed = 90f + level * 12f
        info(if (level % 5 == 0) "⚔ Màn trùm $level" else "Màn $level")
    }

    override fun layout() { logR = w * 0.22f; logY = h * 0.32f; knifeLen = logR * 0.9f }

    override fun down(x: Float, y: Float) {
        if (over || flyY >= 0f || bounce != null || breakT > 0f || left <= 0) return
        flyY = h * 0.82f; left--; sfx(Sfx.SHOOT)
    }

    private fun angDiff(a: Float, b: Float): Float { var d = (a - b) % 360f; if (d < -180) d += 360; if (d > 180) d -= 360; return abs(d) }

    override fun update(dt: Float) {
        t += dt; fx.update(dt); if (shake > 0) shake -= dt
        val boss = level % 5 == 0
        val sp = if (boss) speed * (1.2f + sin(t * 1.3f) * 1.4f) else speed * (if (level > 3) 1f + 0.4f * sin(t * 0.9f) else 1f)
        rot = (rot + sp * dt) % 360f
        if (breakT > 0f) { breakT -= dt; if (breakT <= 0f) { level++; setup() }; return }
        if (flyY >= 0f) {
            flyY -= h * 3.2f * dt
            if (flyY <= logY + logR) {
                val hitAngle = ((90f - rot) % 360f + 360f) % 360f   // angle of the log point facing down
                if (knives.any { angDiff(it, hitAngle) < 9f }) {
                    bounce = floatArrayOf(w / 2, logY + logR + knifeLen * 0.3f, 0f)
                    flyY = -1f; sfx(Sfx.HIT); buzz(80)
                    return
                }
                knives.add(hitAngle); flyY = -1f; shake = 0.1f; sfx(Sfx.TAP); points++; score(points)
                fx.burst(w / 2, logY + logR, 0xFFBCAAA4.toInt(), 6, 120 * dp, 2.5f * dp, 0.4f)
                apples.removeAll { a ->
                    (angDiff(a, hitAngle) < 12f).also { if (it) { points += 2; score(points); sfx(Sfx.COIN); fx.burst(w / 2, logY + logR, Pal.RED, 14, 160 * dp, 3 * dp) } }
                }
                if (left == 0) {
                    breakT = 1.2f; sfx(Sfx.WIN)
                    fx.burst(w / 2, logY, 0xFF8D6E63.toInt(), 40, 320 * dp, 5 * dp, 0.9f)
                }
            }
        }
        bounce?.let { b ->
            b[1] += h * 1.4f * dt; b[2] += 720f * dt
            if (b[1] > h + 50 * dp) { bounce = null; gameOver(false, "Dao va nhau!", "Màn $level · $points điểm", points) }
        }
    }

    private fun knife(c: Canvas, x: Float, y: Float, deg: Float, len: Float) {
        c.save(); c.rotate(deg, x, y)
        val bw = len * 0.09f
        c.poly(0xFFE0E0E0.toInt(), x - bw, y, x + bw, y, x + bw, y + len * 0.55f, x, y + len * 0.62f, x - bw, y + len * 0.55f)
        c.rect(x - bw * 0.2f, y + len * 0.05f, x + bw * 0.3f, y + len * 0.5f, Pal.alpha(Pal.WHITE, 0.7f))
        c.rect(x - bw * 1.6f, y - len * 0.03f, x + bw * 1.6f, y + len * 0.02f, 0xFF616161.toInt())
        c.rect(x - bw * 1.1f, y - len * 0.4f, x + bw * 1.1f, y - len * 0.03f, 0xFF5D4037.toInt())
        c.restore()
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF2B1B3D.toInt(), 0xFF120B1E.toInt())
        val sx = if (shake > 0) (Random.nextFloat() - 0.5f) * 6 * dp else 0f
        val cx = w / 2 + sx; val cy = logY - if (shake > 0) 4 * dp else 0f
        if (breakT <= 0f) {
            for (k in knives) {
                val a = Math.toRadians((k + rot).toDouble())
                val ex = cx + cos(a).toFloat() * logR * 0.85f; val ey = cy + sin(a).toFloat() * logR * 0.85f
                knife(c, ex, ey, k + rot - 90f, knifeLen)
            }
            val boss = level % 5 == 0
            c.circle(cx, cy, logR, if (boss) 0xFF5E35B1.toInt() else 0xFF8D6E63.toInt())
            c.circle(cx, cy, logR * 0.82f, if (boss) 0xFF7E57C2.toInt() else 0xFFA1887F.toInt())
            for (k in 1..3) c.ring(cx, cy, logR * k / 4.2f, 1.5f * dp, Pal.alpha(Pal.BLACK, 0.15f))
            for (k in 0 until 6) {
                val a = Math.toRadians((rot + k * 60).toDouble())
                c.circle(cx + cos(a).toFloat() * logR * 0.55f, cy + sin(a).toFloat() * logR * 0.55f, logR * 0.05f, Pal.alpha(Pal.BLACK, 0.2f))
            }
            for (ap in apples) {
                val a = Math.toRadians((ap + rot).toDouble())
                c.text("🍎", cx + cos(a).toFloat() * logR * 1.12f, cy + sin(a).toFloat() * logR * 1.12f, logR * 0.3f, Pal.RED)
            }
        }
        if (flyY >= 0f) knife(c, w / 2, flyY, 180f, knifeLen)
        else if (left > 0 && bounce == null && !over && breakT <= 0f) knife(c, w / 2, h * 0.82f, 180f, knifeLen)
        bounce?.let { knife(c, it[0], it[1], 180f + it[2], knifeLen) }
        fx.draw(c)
        // remaining knives
        for (i in 0 until left) c.text("🗡", 22 * dp, h - 30 * dp - i * 22 * dp, 18 * dp, Pal.WHITE)
        c.text("$points", w / 2, 36 * dp, 32 * dp, Pal.YELLOW)
    }
}
