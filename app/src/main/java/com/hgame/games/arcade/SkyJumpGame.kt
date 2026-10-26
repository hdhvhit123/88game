package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.gradRect
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.random.Random

class SkyJumpGame : Game() {
    override val realtime = true
    override val help = "Nhân vật tự động nhảy khi chạm bậc. Giữ nửa trái/phải màn hình để di chuyển sang trái/phải. " +
        "Bậc xanh lá đứng yên, xanh dương di chuyển, nâu sẽ vỡ, có lò xo 🌀 bật thật cao. Đi ra mép này sẽ xuất hiện ở mép kia."

    private class Plat(var x: Float, val y: Float, val kind: Int, var vx: Float, var broken: Boolean = false, val spring: Boolean)

    private val plats = ArrayList<Plat>()
    private var px = 0f; private var py = 0f; private var vx = 0f; private var vy = 0f
    private var cam = 0f; private var maxH = 0f; private var steer = 0; private var face = 1
    private var topY = 0f
    private val pw get() = w * 0.18f
    private var started = false

    override fun newGame() {
        plats.clear(); cam = 0f; maxH = 0f; started = false; vx = 0f; vy = 0f
        topY = 0f
        score(0)
    }

    override fun layout() {
        if (plats.isEmpty()) {
            px = w / 2; py = h - 120 * dp
            plats.add(Plat(w / 2 - pw / 2, h - 60 * dp, 0, 0f, spring = false))
            topY = h - 60 * dp
            fill()
        }
    }

    private fun fill() {
        while (topY > cam - h) {
            val diff = (maxH / (3000 * dp)).coerceAtMost(1f)
            topY -= (55 + 70 * diff + Random.nextFloat() * 40) * dp
            val r = Random.nextFloat()
            val kind = when { r < 0.12f + 0.15f * diff -> 1; r < 0.2f + 0.2f * diff -> 2; else -> 0 }
            plats.add(Plat(Random.nextFloat() * (w - pw), topY, kind, if (kind == 1) (60 + Random.nextFloat() * 60) * dp else 0f, spring = kind == 0 && Random.nextFloat() < 0.08f))
            if (kind == 2) { // breakable platforms never count as the only path
                topY -= 10 * dp
                plats.add(Plat(Random.nextFloat() * (w - pw), topY, 0, 0f, spring = false))
            }
        }
        plats.removeAll { it.y > cam + h + 40 * dp }
    }

    override fun pointerDown(id: Int, x: Float, y: Float) { started = true; steer = if (x < w / 2) -1 else 1; face = steer }
    override fun pointerMove(id: Int, x: Float, y: Float) { steer = if (x < w / 2) -1 else 1; face = steer }
    override fun pointerUp(id: Int, x: Float, y: Float) { steer = 0 }

    override fun update(dt: Float) {
        if (!started) return
        val g = h * 2.4f
        vx += (steer * w * 2.2f - vx * 3f) * dt
        px += vx * dt
        if (px < -15 * dp) px += w + 30 * dp; if (px > w + 15 * dp) px -= w + 30 * dp
        val prevY = py
        vy += g * dt; py += vy * dt
        if (vy > 0) for (p in plats) {
            if (p.broken) continue
            if (prevY <= p.y && py >= p.y && px > p.x - 8 * dp && px < p.x + pw + 8 * dp) {
                if (p.kind == 2) { p.broken = true; sfx(Sfx.HIT); continue }
                py = p.y
                vy = if (p.spring && abs(px - (p.x + pw / 2)) < pw * 0.3f) { sfx(Sfx.COIN); -h * 2.2f } else { sfx(Sfx.JUMP); -h * 1.25f }
                break
            }
        }
        for (p in plats) if (p.kind == 1) { p.x += p.vx * dt; if (p.x < 0 || p.x > w - pw) p.vx = -p.vx }
        val screenY = py - cam
        if (screenY < h * 0.4f) cam = py - h * 0.4f
        val height = (h - 120 * dp - cam).coerceAtLeast(0f)
        if (height > maxH) { maxH = height; score((maxH / (10 * dp)).toInt()) }
        fill()
        if (py - cam > h + 40 * dp) { sfx(Sfx.LOSE); buzz(60); val s = (maxH / (10 * dp)).toInt(); gameOver(false, "Rơi mất rồi!", "Độ cao $s m", s) }
    }

    override fun draw(c: Canvas) {
        val k = (maxH / (6000 * dp)).coerceAtMost(1f)
        c.gradRect(0f, 0f, w, h, 0f, Pal.mix(0xFF81D4FA.toInt(), 0xFF1A237E.toInt(), k), Pal.mix(0xFFE1F5FE.toInt(), 0xFF5C6BC0.toInt(), k))
        val gridStep = 28 * dp
        var gy = -((cam % gridStep) + gridStep) % gridStep
        while (gy < h) { c.rect(0f, gy, w, gy + 1, Pal.alpha(Pal.WHITE, 0.25f)); gy += gridStep }
        for (p in plats) {
            if (p.broken) continue
            val y = p.y - cam
            val col = when (p.kind) { 1 -> Pal.BLUE; 2 -> 0xFF8D6E63.toInt(); else -> 0xFF43A047.toInt() }
            c.rrect(p.x, y + 3 * dp, p.x + pw, y + 15 * dp, 7 * dp, Pal.shade(col, 0.6f))
            c.rrect(p.x, y, p.x + pw, y + 12 * dp, 7 * dp, col)
            if (p.kind == 2) { c.rect(p.x + pw * 0.48f, y, p.x + pw * 0.52f, y + 12 * dp, 0xFF5D4037.toInt()) }
            if (p.spring) c.text("🌀", p.x + pw / 2, y - 9 * dp, 16 * dp, Pal.WHITE)
        }
        val y = py - cam
        c.rrect(px - 14 * dp, y - 34 * dp, px + 14 * dp, y - 4 * dp, 10 * dp, 0xFFFFB300.toInt())
        c.rrect(px - 14 * dp, y - 14 * dp, px + 14 * dp, y - 4 * dp, 4 * dp, 0xFF6D4C41.toInt())
        c.circle(px + face * 5 * dp, y - 25 * dp, 4.5f * dp, Pal.WHITE); c.circle(px + face * 6.5f * dp, y - 25 * dp, 2.2f * dp, Pal.BLACK)
        c.rect(px - 9 * dp, y - 4 * dp, px - 4 * dp, y, 0xFF3E2723.toInt()); c.rect(px + 4 * dp, y - 4 * dp, px + 9 * dp, y, 0xFF3E2723.toInt())
        c.text("${(maxH / (10 * dp)).toInt()} m", 16 * dp, 26 * dp, 18 * dp, 0xFF0D47A1.toInt(), android.graphics.Paint.Align.LEFT)
        if (!started) c.text("Chạm trái/phải để bắt đầu", w / 2, h * 0.5f, 18 * dp, 0xFF0D47A1.toInt())
    }
}
