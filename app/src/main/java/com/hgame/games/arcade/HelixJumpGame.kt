package com.hgame.games.arcade

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.rect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.random.Random

/**
 * Helix tower seen from the front: each floor is a ring of 12 segments drawn as a squashed ellipse.
 * Drag horizontally to rotate the tower; the ball bounces and drops through gaps.
 */
class HelixJumpGame : Game() {
    override val realtime = true
    override val help = "Kéo ngang để xoay tháp. Quả bóng nảy liên tục, hãy xoay để bóng rơi qua khe hở xuống tầng dưới. " +
        "Tránh các ô màu đỏ! Rơi liên tiếp qua 3 tầng sẽ có sức mạnh phá vỡ ô đỏ."

    private val seg = 12
    private class Floor(val kinds: IntArray, var broken: Boolean = false) // 0 normal, 1 gap, 2 danger
    private val floors = ArrayList<Floor>()
    private var rot = 0f; private var lastX = 0f
    private var ballY = 0f; private var vy = 0f; private var cam = 0f; private var passed = 0; private var streak = 0
    private var points = 0; private var level = 1
    private val gapH = 3.2f   // world units between floors
    private val fx = Particles()
    private var hue = 0f

    override fun newGame() { level = 1; points = 0; score(0); build() }

    private fun build() {
        floors.clear(); fx.clear(); hue = Random.nextFloat() * 360
        val n = 10 + level * 2
        for (i in 0 until n) {
            val k = IntArray(seg)
            if (i == n - 1) { floors.add(Floor(IntArray(seg) { 3 })); continue }
            val gapStart = Random.nextInt(seg); val gapLen = 2 + Random.nextInt(2)
            for (j in 0 until gapLen) k[(gapStart + j) % seg] = 1
            if (i > 0) {
                val dangers = (1 + level / 3).coerceAtMost(4)
                repeat(dangers) { val j = Random.nextInt(seg); if (k[j] == 0 && i > 1) k[j] = 2 }
            } else {
                // make sure the first landing spot under the ball is solid
                k[frontIndex(0f)] = 0
            }
            floors.add(Floor(k))
        }
        ballY = -1.5f; vy = 0f; cam = 0f; passed = 0; streak = 0; rot = 0f
        info("Màn $level")
    }

    private fun frontIndex(r: Float): Int {
        // segment currently at the front (where the ball is): angle 90° in screen space
        val a = ((90f - r) % 360f + 360f) % 360f
        return (a / (360f / seg)).toInt() % seg
    }

    override fun down(x: Float, y: Float) { lastX = x }
    override fun move(x: Float, y: Float) { rot += (x - lastX) / w * 300f; lastX = x }

    override fun update(dt: Float) {
        fx.update(dt)
        vy += 26f * dt; if (vy > 14f) vy = 14f
        val prev = ballY
        ballY += vy * dt
        // floor i sits at y = i * gapH; ball bottom collides when crossing it
        for (i in floors.indices) {
            val fy = i * gapH
            if (prev < fy && ballY >= fy) {
                val f = floors[i]
                if (f.broken) continue
                val k = f.kinds[frontIndex(rot)]
                if (k == 3) { level++; points += 20; score(points); sfx(Sfx.WIN); build(); return }
                if (k == 1) { passed++; streak++; points += streak; score(points); f.broken = true; sfx(Sfx.POP); continue }
                if (streak >= 3) {
                    f.broken = true; streak = 0; sfx(Sfx.BOOM); buzz(40)
                    fx.burst(w / 2, toScreen(fy), Pal.hsv(hue, 0.7f, 1f), 24, 260 * dp, 4 * dp); vy = -9f; ballY = fy - 0.01f; return
                }
                if (k == 2) {
                    sfx(Sfx.LOSE); buzz(80)
                    gameOver(false, "Chạm ô đỏ!", "Màn $level · $points điểm", points); return
                }
                ballY = fy - 0.01f; vy = -9f; streak = 0; sfx(Sfx.TICK)
                fx.burst(w / 2, toScreen(fy), Pal.hsv(hue + 180, 0.8f, 1f), 5, 80 * dp, 2.5f * dp, 0.3f)
                break
            }
        }
        val target = ballY - 1.5f
        if (target > cam) cam += (target - cam) * (dt * 8f).coerceAtMost(1f)
    }

    private val unit get() = h / 12f
    private fun toScreen(y: Float) = h * 0.3f + (y - cam) * unit

    private val oval = RectF()
    override fun draw(c: Canvas) {
        c.background(w, h, Pal.hsv(hue, 0.4f, 0.35f), Pal.hsv(hue + 40, 0.5f, 0.12f))
        val cx = w / 2; val rx = w * 0.38f; val ry = rx * 0.32f
        c.rect(cx - rx * 0.16f, 0f, cx + rx * 0.16f, h, Pal.alpha(Pal.WHITE, 0.25f))
        val p = Gfx.fill.apply { shader = null }
        for (i in floors.indices.reversed()) {
            val f = floors[i]; if (f.broken) continue
            val sy = toScreen(i * gapH)
            if (sy < -ry * 2 || sy > h + ry * 2) continue
            oval.set(cx - rx, sy - ry, cx + rx, sy + ry)
            val step = 360f / seg
            // back half first, then front half so the front overlaps
            for (pass in 0..1) for (j in 0 until seg) {
                val kind = f.kinds[j]; if (kind == 1) continue
                val start = j * step + rot
                val mid = ((start + step / 2) % 360f + 360f) % 360f
                val front = mid in 0f..180f
                if ((pass == 0) == front) continue
                p.color = when (kind) {
                    2 -> if (front) Pal.RED else Pal.shade(Pal.RED, 0.6f)
                    3 -> if (front) Pal.GREEN else Pal.shade(Pal.GREEN, 0.6f)
                    else -> Pal.hsv(hue + 180, 0.55f, if (front) 0.95f else 0.6f)
                }
                c.drawArc(oval, start, step + 0.6f, true, p)
            }
            oval.set(cx - rx * 0.16f, sy - ry * 0.16f, cx + rx * 0.16f, sy + ry * 0.16f)
            c.drawOval(oval, Gfx.fillPaint(Pal.alpha(Pal.WHITE, 0.4f)))
        }
        val by = toScreen(ballY) - 10 * dp
        c.ball(cx, by + ry * 0.75f, 10 * dp, if (streak >= 3) Pal.ORANGE else Pal.hsv(hue, 0.8f, 1f))
        fx.draw(c)
        c.text("$points", w / 2, 34 * dp, 30 * dp, Pal.WHITE)
        if (streak >= 3) c.text("🔥 Phá vỡ!", w / 2, 64 * dp, 16 * dp, Pal.ORANGE)
        if (abs(rot) < 0.01f && passed == 0 && !over) c.text("Kéo ngang để xoay tháp", w / 2, h * 0.9f, 16 * dp, Pal.WHITE, Paint.Align.CENTER)
    }
}
