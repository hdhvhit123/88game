package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
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

class StickHeroGame : Game() {
    override val realtime = true
    override val help = "Chạm và giữ để cây gậy dài ra, thả tay để gậy đổ xuống làm cầu. Gậy phải vừa đủ chạm cột kế tiếp. " +
        "Đặt đầu gậy trúng điểm đỏ giữa cột để được thêm điểm."

    private class Pillar(var x: Float, val w: Float)
    private val pillars = ArrayList<Pillar>()
    private var state = 0 // 0 idle, 1 growing, 2 falling, 3 walking, 4 scrolling, 5 dropping
    private var stick = 0f; private var angle = 0f; private var heroX = 0f; private var heroY = 0f
    private var scroll = 0f; private var scrollTarget = 0f; private var points = 0; private var bonusT = 0f
    private var top = 0f; private var t = 0f

    override fun newGame() {
        pillars.clear(); state = 0; points = 0; stick = 0f; angle = 0f; scroll = 0f; scrollTarget = 0f; heroY = 0f
        score(0)
    }

    override fun layout() {
        top = h * 0.66f
        if (pillars.isEmpty()) { pillars.add(Pillar(0f, w * 0.22f)); addPillar(); heroX = pillars[0].w - 14 * dp }
    }

    private fun addPillar() {
        val last = pillars.last()
        val gap = w * (0.12f + Random.nextFloat() * 0.45f)
        pillars.add(Pillar(last.x + last.w + gap, w * (0.06f + Random.nextFloat() * 0.14f)))
    }

    override fun down(x: Float, y: Float) { if (state == 0 && !over) { state = 1; stick = 0f; angle = 0f } }
    override fun up(x: Float, y: Float) { if (state == 1) { state = 2; sfx(Sfx.TAP) } }

    override fun update(dt: Float) {
        t += dt; if (bonusT > 0) bonusT -= dt
        val cur = pillars[pillars.size - 2]; val next = pillars.last()
        when (state) {
            1 -> { stick += h * 0.55f * dt; if (stick > h) stick = h }
            2 -> {
                angle += 220f * dt
                if (angle >= 90f) {
                    angle = 90f; state = 3; sfx(Sfx.TICK)
                }
            }
            3 -> {
                heroX += w * 0.55f * dt
                val tip = cur.x + cur.w + stick
                val ok = tip >= next.x && tip <= next.x + next.w
                val stopAt = if (ok) next.x + next.w - 14 * dp else tip
                if (heroX >= stopAt) {
                    heroX = stopAt
                    if (ok) {
                        val mid = next.x + next.w / 2
                        val bonus = kotlin.math.abs(tip - mid) < 5 * dp
                        points += if (bonus) 2 else 1; if (bonus) { bonusT = 1f; sfx(Sfx.COIN) } else sfx(Sfx.POP)
                        score(points); addPillar(); scrollTarget = next.x - w * 0.22f + next.w; state = 4
                    } else { state = 5; sfx(Sfx.LOSE) }
                }
            }
            4 -> {
                scroll += (scrollTarget - scroll) * (dt * 8f).coerceAtMost(1f)
                if (kotlin.math.abs(scrollTarget - scroll) < 1f) { scroll = scrollTarget; state = 0; stick = 0f; angle = 0f; pillars.removeAll { it.x + it.w < scroll - w } }
            }
            5 -> {
                heroY += h * 1.2f * dt; angle = (angle + 200f * dt).coerceAtMost(180f)
                if (heroY > h) { buzz(60); gameOver(false, "Rơi xuống vực!", "Bạn vượt qua $points cột", points) }
            }
        }
    }

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h, 0f, 0xFFFFCC80.toInt(), 0xFFFF8A65.toInt())
        c.circle(w * 0.75f, h * 0.25f, w * 0.14f, Pal.alpha(0xFFFFF3E0.toInt(), 0.8f))
        for (k in 0..3) {
            val mx = ((k * w * 0.45f - scroll * 0.3f) % (w * 1.8f) + w * 1.8f) % (w * 1.8f) - w * 0.3f
            c.circle(mx, top + h * 0.15f, w * 0.38f, Pal.alpha(0xFF8D6E63.toInt(), 0.35f))
        }
        c.save(); c.translate(-scroll, 0f)
        for (p in pillars) {
            c.rect(p.x, top, p.x + p.w, h, 0xFF263238.toInt())
            c.rect(p.x + p.w / 2 - 4 * dp, top, p.x + p.w / 2 + 4 * dp, top + 4 * dp, Pal.RED)
        }
        val cur = pillars[pillars.size - 2]
        if (state != 0 || stick > 0f) {
            val bx = cur.x + cur.w; val a = Math.toRadians((angle - 90).toDouble())
            c.line(bx, top, bx + (cos(a) * stick).toFloat(), top + (sin(a) * stick).toFloat(), 4 * dp, 0xFF3E2723.toInt())
        }
        // hero
        val hx = heroX; val hy = top + heroY
        val bob = if (state == 3) sin(t * 30) * 1.5f * dp else 0f
        c.rrect(hx - 9 * dp, hy - 30 * dp + bob, hx + 9 * dp, hy - 8 * dp + bob, 5 * dp, 0xFF212121.toInt())
        c.rect(hx - 9 * dp, hy - 26 * dp + bob, hx + 9 * dp, hy - 22 * dp + bob, Pal.RED)
        c.circle(hx + 4 * dp, hy - 22 * dp + bob, 2 * dp, Pal.WHITE)
        val leg = if (state == 3) ((t * 12).toInt() % 2) * 3 * dp else 0f
        c.rect(hx - 6 * dp, hy - 8 * dp, hx - 2 * dp, hy - leg, 0xFF212121.toInt()); c.rect(hx + 2 * dp, hy - 8 * dp, hx + 6 * dp, hy - 3 * dp + leg, 0xFF212121.toInt())
        c.restore()
        c.text("$points", w / 2, h * 0.12f, 48 * dp, Pal.WHITE)
        if (bonusT > 0f) c.text("+1 Chính giữa!", w / 2, h * 0.2f, 18 * dp, Pal.alpha(Pal.WHITE, bonusT))
        if (points == 0 && state == 0 && !over) c.text("Giữ để kéo dài gậy", w / 2, h * 0.4f, 18 * dp, 0xFF4E342E.toInt())
    }
}
