package com.hgame.games.sports

import android.graphics.Canvas
import android.graphics.RectF
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.poly
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class DartsGame : Game() {
    override val realtime = true
    override val help = "Chơi 501: trừ dần điểm từ 501 về đúng 0 trong ít lượt nhất. Mỗi lượt 3 phi tiêu. Kéo để ngắm (tâm ngắm rung nhẹ), thả tay để phóng. " +
        "Vòng ngoài hẹp = nhân đôi, vòng giữa hẹp = nhân ba, tâm xanh = 25, hồng tâm = 50. Trừ quá 0 hoặc còn 1 là 'bust', lượt đó không tính."

    private val order = intArrayOf(20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5)
    private var remaining = 501; private var turnStart = 501; private var dartsLeft = 3; private var turns = 1
    private val stuck = ArrayList<FloatArray>()
    private var aiming = false; private var ax = 0f; private var ay = 0f; private var t = 0f
    private var fly: FloatArray? = null   // x, y, progress
    private var msg = ""; private var msgT = 0f
    private var cx = 0f; private var cy = 0f; private var R = 0f
    private val oval = RectF()

    override fun newGame() { remaining = 501; turnStart = 501; dartsLeft = 3; turns = 1; stuck.clear(); fly = null; score(501); status() }

    override val bestMode get() = com.hgame.engine.BestMode.LOW

    private fun status() = info("Còn $remaining · Lượt $turns · Phi tiêu: ${"🎯".repeat(dartsLeft.coerceAtLeast(0))}")

    override fun layout() { R = minOf(w * 0.46f, h * 0.32f); cx = w / 2; cy = h * 0.38f }

    override fun down(x: Float, y: Float) { if (fly == null && !over && dartsLeft > 0) { aiming = true; ax = x; ay = y - 60 * dp } }
    override fun move(x: Float, y: Float) { ax = x; ay = y - 60 * dp }
    override fun up(x: Float, y: Float) {
        if (!aiming) return
        aiming = false
        val (wx, wy) = wobble()
        fly = floatArrayOf(wx + (Random.nextFloat() - 0.5f) * R * 0.08f, wy + (Random.nextFloat() - 0.5f) * R * 0.08f, 0f)
        sfx(Sfx.SHOOT)
    }

    private fun wobble(): Pair<Float, Float> = (ax + sin(t * 3.1f) * R * 0.05f + sin(t * 7.3f) * R * 0.02f) to (ay + cos(t * 2.3f) * R * 0.05f + sin(t * 5.7f) * R * 0.02f)

    /** Returns (base, multiplier) for a board hit. */
    private fun scoreAt(x: Float, y: Float): Pair<Int, Int> {
        val d = hypot(x - cx, y - cy) / R
        if (d < 0.05f) return 50 to 1
        if (d < 0.11f) return 25 to 1
        if (d > 1f) return 0 to 0
        var a = Math.toDegrees(atan2((y - cy).toDouble(), (x - cx).toDouble())).toFloat() + 90f + 9f
        a = ((a % 360f) + 360f) % 360f
        val num = order[(a / 18f).toInt() % 20]
        val mult = when { d in 0.58f..0.65f -> 3; d in 0.93f..1f -> 2; else -> 1 }
        return num to mult
    }

    override fun update(dt: Float) {
        t += dt; if (msgT > 0) msgT -= dt
        val f = fly ?: return
        f[2] += dt * 4f
        if (f[2] >= 1f) {
            fly = null
            stuck.add(floatArrayOf(f[0], f[1]))
            val (base, mult) = scoreAt(f[0], f[1])
            val pts = base * mult
            sfx(if (pts == 0) Sfx.ERROR else Sfx.HIT)
            val newRem = remaining - pts
            msg = when { pts == 0 -> "Trượt!"; base == 50 -> "HỒNG TÂM 50!"; mult == 3 -> "Nhân ba $base = $pts"; mult == 2 -> "Nhân đôi $base = $pts"; else -> "$pts" }
            msgT = 1f
            if (newRem < 0 || newRem == 1) {
                msg = "BUST! Lượt không tính"; remaining = turnStart; endTurn(); return
            }
            remaining = newRem; score(remaining)
            if (remaining == 0) { sfx(Sfx.WIN); gameOver(true, "Checkout!", "Về 0 sau $turns lượt", turns); return }
            dartsLeft--
            if (dartsLeft == 0) endTurn() else status()
        }
    }

    private fun endTurn() {
        after(0.9f) { stuck.clear(); dartsLeft = 3; turns++; turnStart = remaining; score(remaining); status() }
        dartsLeft = 0; status()
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF3E2723.toInt(), 0xFF1B0F0C.toInt())
        c.circle(cx, cy, R * 1.15f, 0xFF212121.toInt())
        val p = Gfx.fill.apply { shader = null }
        val rings = floatArrayOf(1f, 0.93f, 0.65f, 0.58f)
        for ((ri, rr) in rings.withIndex()) {
            oval.set(cx - R * rr, cy - R * rr, cx + R * rr, cy + R * rr)
            for (i in 0 until 20) {
                val dark = i % 2 == 0
                p.color = if (ri % 2 == 0) (if (dark) Pal.RED else Pal.GREEN) else (if (dark) 0xFF111111.toInt() else 0xFFF3E5C0.toInt())
                c.drawArc(oval, -90f - 9f + i * 18f, 18f, true, p)
            }
        }
        c.circle(cx, cy, R * 0.11f, Pal.GREEN); c.circle(cx, cy, R * 0.05f, Pal.RED)
        for (i in 0 until 20) {
            val a = Math.toRadians((-90.0 + i * 18))
            c.text("${order[i]}", cx + cos(a).toFloat() * R * 1.08f, cy + sin(a).toFloat() * R * 1.08f, R * 0.09f, Pal.WHITE)
            c.line(cx + cos(a - Math.toRadians(9.0)).toFloat() * R * 0.11f, cy + sin(a - Math.toRadians(9.0)).toFloat() * R * 0.11f,
                cx + cos(a - Math.toRadians(9.0)).toFloat() * R, cy + sin(a - Math.toRadians(9.0)).toFloat() * R, 1f, 0xFFB0BEC5.toInt())
        }
        for (s in stuck) dart(c, s[0], s[1], 1f)
        fly?.let { f -> val k = f[2]; dart(c, f[0], f[1] + (1 - k) * h * 0.5f, 1f + (1 - k) * 1.5f) }
        if (aiming) {
            val (wx, wy) = wobble()
            c.circle(wx, wy, R * 0.06f, Pal.alpha(Pal.WHITE, 0.25f))
            c.line(wx - R * 0.1f, wy, wx + R * 0.1f, wy, 2 * dp, Pal.YELLOW); c.line(wx, wy - R * 0.1f, wx, wy + R * 0.1f, 2 * dp, Pal.YELLOW)
        }
        c.rrect(w * 0.15f, h * 0.76f, w * 0.85f, h * 0.76f + 60 * dp, 16 * dp, Pal.alpha(Pal.BLACK, 0.35f))
        c.text("$remaining", w / 2, h * 0.76f + 30 * dp, 36 * dp, Pal.YELLOW)
        if (msgT > 0f) c.text(msg, w / 2, h * 0.72f, 18 * dp, Pal.alpha(Pal.WHITE, msgT.coerceAtMost(1f)))
        if (stuck.isEmpty() && turns == 1 && !aiming && fly == null) c.text("Kéo để ngắm, thả để phóng", w / 2, h * 0.9f, 15 * dp, Pal.MUTED)
    }

    private fun dart(c: Canvas, x: Float, y: Float, s: Float) {
        c.line(x, y, x + 6 * dp * s, y + 26 * dp * s, 3 * dp * s, 0xFFB0BEC5.toInt())
        c.poly(Pal.CYAN, x + 6 * dp * s, y + 20 * dp * s, x + 14 * dp * s, y + 34 * dp * s, x + 4 * dp * s, y + 30 * dp * s)
        c.circle(x, y, 2 * dp, Pal.YELLOW)
    }
}
