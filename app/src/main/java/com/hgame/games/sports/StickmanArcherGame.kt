package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.gradRect
import com.hgame.engine.line
import com.hgame.engine.poly
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class StickmanArcherGame : Game() {
    override val realtime = true
    override val help = "Đấu cung với người que máy. Kéo ngược ra sau để giương cung: hướng kéo là góc bắn, độ dài là lực. Thả tay để bắn. " +
        "Trúng đầu gây sát thương lớn. Hai bên luân phiên bắn, ai hết máu trước sẽ thua. Mỗi đối thủ hạ gục được 1 điểm."

    private class Arrow(var x: Float, var y: Float, var vx: Float, var vy: Float, val mine: Boolean, var stuck: Boolean = false, var ang: Float = 0f)
    private val arrows = ArrayList<Arrow>()
    private var meX = 0f; private var foeX = 0f; private var groundY = 0f
    private var myHp = 100; private var foeHp = 100; private var turn = 0; private var kills = 0
    private var aiming = false; private var sx = 0f; private var sy = 0f; private var cx = 0f; private var cy = 0f
    private var camX = 0f; private var flying: Arrow? = null; private var wind = 0f
    private var msg = ""; private var msgT = 0f; private var aiErr = 0.25f
    private val fx = Particles().apply { gravity = 500f }

    override fun newGame() { kills = 0; myHp = 100; score(0); newFoe() }

    private fun newFoe() {
        foeHp = 100; arrows.clear(); flying = null; turn = 0
        meX = 0f; foeX = 600f + Random.nextFloat() * 300f + kills * 30f
        wind = (Random.nextFloat() - 0.5f) * 60f * (kills.coerceAtMost(5) / 5f)
        aiErr = (0.3f - kills * 0.03f).coerceAtLeast(0.06f)
        status()
    }

    private fun status() = info("Hạ gục: $kills · Gió ${if (wind >= 0) "→" else "←"} ${kotlin.math.abs(wind).toInt()}")

    override fun layout() { groundY = h * 0.72f }

    private val s get() = w / 520f   // world -> screen scale

    override fun down(x: Float, y: Float) { if (turn == 0 && flying == null && !over) { aiming = true; sx = x; sy = y; cx = x; cy = y } }
    override fun move(x: Float, y: Float) { cx = x; cy = y }
    override fun up(x: Float, y: Float) {
        if (!aiming) return
        aiming = false
        val dx = sx - cx; val dy = sy - cy; val d = hypot(dx, dy)
        if (d < 15 * dp) return
        val power = (d / (w * 0.4f)).coerceIn(0.1f, 1f) * 900f
        val a = atan2(dy, dx)
        shoot(meX + 10f, -110f, cos(a) * power, sin(a) * power, true)
    }

    private fun shoot(x: Float, y: Float, vx: Float, vy: Float, mine: Boolean) {
        val ar = Arrow(x, y, vx, vy, mine); arrows.add(ar); flying = ar; sfx(Sfx.SHOOT)
    }

    private fun aiShoot() {
        // solve for a launch speed that lands near the player, then add error
        val dx = meX - (foeX - 10f); val ang = Math.toRadians(180.0 + 40 + Random.nextDouble() * 10)
        val g = 600f
        val cosA = cos(ang).toFloat(); val sinA = sin(ang).toFloat()
        // range on flat ground: dx = v^2 * sin(2a) / g  (dx and cos are negative here)
        var v = kotlin.math.sqrt(kotlin.math.abs(g * dx / (2 * sinA * cosA)))
        if (v.isNaN() || v < 100f) v = 700f
        v *= 1f + (Random.nextFloat() - 0.5f) * aiErr - wind / 2500f
        shoot(foeX - 10f, -110f, cosA * v, sinA * v, false)
    }

    override fun update(dt: Float) {
        fx.update(dt); if (msgT > 0) msgT -= dt
        val target = when { flying != null -> flying!!.x - 260f; turn == 1 -> foeX - 400f; else -> meX - 80f }
        camX += (target - camX) * (dt * 3f).coerceAtMost(1f)
        val ar = flying ?: return
        val g = 600f
        val sub = 4; val sdt = dt / sub
        repeat(sub) {
            if (ar.stuck) return@repeat
            ar.vy += g * sdt; ar.vx += wind * sdt
            ar.x += ar.vx * sdt; ar.y += ar.vy * sdt
            ar.ang = atan2(ar.vy, ar.vx)
            val tx = if (ar.mine) foeX else meX
            // hit test against the stickman (head circle at -150, body line -130..-60)
            val headHit = hypot(ar.x - tx, ar.y + 150f) < 16f
            val bodyHit = kotlin.math.abs(ar.x - tx) < 10f && ar.y > -135f && ar.y < 0f
            if (headHit || bodyHit) {
                ar.stuck = true
                val dmg = if (headHit) 50 + Random.nextInt(15) else 20 + Random.nextInt(15)
                if (ar.mine) foeHp -= dmg else myHp -= dmg
                msg = if (headHit) "TRÚNG ĐẦU! −$dmg" else "Trúng! −$dmg"; msgT = 1.2f
                sfx(Sfx.HIT); buzz(if (ar.mine) 20 else 60)
                fx.burst(sx(ar.x), sy(ar.y), Pal.RED, 14, 160 * dp, 3 * dp)
            } else if (ar.y >= 0f) { ar.stuck = true; ar.y = 0f; sfx(Sfx.TICK) }
        }
        if (ar.stuck || ar.x < meX - 600f || ar.x > foeX + 600f) { flying = null; after(0.7f) { nextTurn(ar.mine) } }
    }

    private fun nextTurn(wasMine: Boolean) {
        if (foeHp <= 0) {
            kills++; score(kills); sfx(Sfx.WIN); msg = "Hạ gục đối thủ!"; msgT = 1.5f
            myHp = (myHp + 30).coerceAtMost(100)
            after(1.2f) { newFoe() }
            return
        }
        if (myHp <= 0) { sfx(Sfx.LOSE); gameOver(false, "Bạn đã bị hạ!", "Hạ gục $kills đối thủ", kills); return }
        turn = if (wasMine) 1 else 0
        if (turn == 1) after(0.9f) { aiShoot() }
    }

    private fun sx(x: Float) = (x - camX) * s
    private fun sy(y: Float) = groundY + y * s

    private fun stickman(c: Canvas, x: Float, col: Int, bowAngle: Float, hp: Int) {
        val X = sx(x); val lw = 3.5f * dp
        c.circle(X, sy(-150f), 14 * s, col)
        c.line(X, sy(-135f), X, sy(-70f), lw, col)
        c.line(X, sy(-70f), X - 14 * s, sy(0f), lw, col); c.line(X, sy(-70f), X + 14 * s, sy(0f), lw, col)
        val ax = X + cos(bowAngle) * 30 * s; val ay = sy(-115f) + sin(bowAngle) * 30 * s
        c.line(X, sy(-115f), ax, ay, lw, col)
        // bow arc
        val p = com.hgame.engine.Gfx.stroke.apply { shader = null; strokeWidth = 3 * dp; color = 0xFF8D6E63.toInt() }
        val oval = android.graphics.RectF(ax - 22 * s, ay - 22 * s, ax + 22 * s, ay + 22 * s)
        c.drawArc(oval, Math.toDegrees(bowAngle.toDouble()).toFloat() - 70f, 140f, false, p)
        // HP bar
        c.rrect(X - 30 * s, sy(-190f), X + 30 * s, sy(-182f), 3 * dp, Pal.alpha(Pal.BLACK, 0.4f))
        c.rrect(X - 30 * s, sy(-190f), X - 30 * s + 60 * s * (hp.coerceAtLeast(0) / 100f), sy(-182f), 3 * dp, if (hp > 40) Pal.GREEN else Pal.RED)
    }

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h, 0f, 0xFF90CAF9.toInt(), 0xFFFFE0B2.toInt())
        for (i in -2..12) { val mx = (i * 300f - camX * 0.3f) * s; c.poly(0xFFA1887F.toInt(), mx, groundY, mx + 150 * s, groundY - 140 * s, mx + 300 * s, groundY) }
        c.rect(0f, groundY, w, h, 0xFF7CB342.toInt()); c.rect(0f, groundY, w, groundY + 6 * dp, 0xFF558B2F.toInt())
        val myAim = if (aiming) atan2(sy - cy, sx - cx) else -0.6f
        stickman(c, meX, 0xFF1A237E.toInt(), myAim, myHp)
        stickman(c, foeX, 0xFFB71C1C.toInt(), (Math.PI + 0.6).toFloat(), foeHp)
        for (a in arrows) {
            val x = sx(a.x); val y = sy(a.y); val l = 34 * s
            c.line(x - cos(a.ang) * l, y - sin(a.ang) * l, x, y, 2.5f * dp, 0xFF5D4037.toInt())
            c.poly(0xFF424242.toInt(), x + cos(a.ang) * 8 * s, y + sin(a.ang) * 8 * s, x - sin(a.ang) * 4 * s, y + cos(a.ang) * 4 * s, x + sin(a.ang) * 4 * s, y - cos(a.ang) * 4 * s)
        }
        if (aiming) {
            val dx = sx - cx; val dy = sy - cy; val pw = (hypot(dx, dy) / (w * 0.4f)).coerceIn(0.1f, 1f)
            // dotted trajectory preview (first part only)
            val a = atan2(dy, dx); var x = meX + 10f; var y = -110f; var vx = cos(a) * pw * 900f; var vy = sin(a) * pw * 900f
            for (i in 0 until 12) { for (k in 0 until 4) { vy += 600f * 0.01f; vx += wind * 0.01f; x += vx * 0.01f; y += vy * 0.01f }; c.circle(sx(x), sy(y), 2.5f * dp, Pal.alpha(Pal.WHITE, 0.8f - i * 0.06f)) }
            c.rrect(16 * dp, h - 30 * dp, 16 * dp + (w - 32 * dp) * pw, h - 22 * dp, 4 * dp, Pal.mix(Pal.GREEN, Pal.RED, pw))
        }
        // off-screen opponent indicator
        val fxs = sx(foeX)
        if (fxs > w - 10 * dp) {
            c.rrect(w - 92 * dp, sy(-150f) - 16 * dp, w - 8 * dp, sy(-150f) + 16 * dp, 10 * dp, Pal.alpha(0xFFB71C1C.toInt(), 0.85f))
            c.text("${(foeX - meX).toInt() / 10} m ▶", w - 50 * dp, sy(-150f), 13 * dp, Pal.WHITE)
        }
        fx.draw(c)
        if (msgT > 0f) c.text(msg, w / 2, h * 0.2f, 20 * dp, Pal.alpha(0xFFB71C1C.toInt(), msgT.coerceAtMost(1f)))
        if (turn == 1 && flying == null && msgT <= 0f) c.text("Đối thủ đang ngắm…", w / 2, h * 0.2f, 16 * dp, 0xFF4E342E.toInt())
        if (kills == 0 && arrows.isEmpty() && !aiming) c.text("Kéo ngược ra sau để giương cung", w / 2, h * 0.85f, 15 * dp, 0xFF1B5E20.toInt())
    }
}
