package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.gradRect
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.hypot

class BowlingGame : Game() {
    override val realtime = true
    override val help = "Kéo quả bóng sang trái/phải để chọn vị trí, rồi vuốt lên để lăn. Vuốt chéo để bóng đi xiên, vuốt càng nhanh càng mạnh. " +
        "10 frame, mỗi frame 2 lần ném. Strike (X) và Spare (/) được cộng điểm thưởng theo luật bowling chuẩn."

    private class Pin(var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f, var down: Boolean = false, var standing: Boolean = true)
    private val pins = ArrayList<Pin>()
    private var bx = 0f; private var by = 0f; private var bvx = 0f; private var bvy = 0f; private var rolling = false; private var spin = 0f
    private val rolls = ArrayList<Int>()
    private var frame = 0; private var rollInFrame = 0; private var settle = 0f
    private var laneL = 0f; private var laneR = 0f; private var foulY = 0f; private var pinTop = 0f
    private var sx = 0f; private var sy = 0f; private var st = 0L; private var placing = false
    private var msg = ""; private var msgT = 0f
    private val pinR get() = (laneR - laneL) * 0.045f
    private val ballR get() = (laneR - laneL) * 0.085f

    override fun newGame() { rolls.clear(); frame = 0; rollInFrame = 0; setPins(true); score(0); status() }

    override fun layout() {
        laneL = w * 0.18f; laneR = w * 0.82f; foulY = h * 0.78f; pinTop = h * 0.12f
        setPins(pins.isEmpty() || pins.all { it.standing })
    }

    private fun setPins(all: Boolean) {
        if (all) {
            pins.clear()
            val sp = (laneR - laneL) * 0.13f; val cx = (laneL + laneR) / 2
            for (row in 0 until 4) for (i in 0..row) pins.add(Pin(cx + (i - row / 2f) * sp, pinTop + (3 - row) * sp * 0.9f))
        } else {
            pins.removeAll { !it.standing }
        }
        bx = (laneL + laneR) / 2; by = h * 0.88f; rolling = false; settle = 0f; spin = 0f
    }

    private fun status() = info("Frame ${minOf(frame + 1, 10)} · Lần ${rollInFrame + 1}")

    override fun down(x: Float, y: Float) {
        if (rolling || over) return
        sx = x; sy = y; st = System.nanoTime(); placing = true
    }

    override fun move(x: Float, y: Float) {
        if (!placing || rolling) return
        if (sy - y < 20 * dp) bx = x.coerceIn(laneL + ballR, laneR - ballR)
    }

    override fun up(x: Float, y: Float) {
        if (!placing || rolling || over) return
        placing = false
        val dy = sy - y
        if (dy < 40 * dp) return
        val dt = ((System.nanoTime() - st) / 1e9f).coerceIn(0.06f, 0.8f)
        val speed = (dy / dt).coerceIn(500 * dp, 2200 * dp)
        bvy = -speed; bvx = (x - sx) / dy * speed * 0.35f
        spin = (x - sx) / dy * 200 * dp
        rolling = true; sfx(Sfx.TAP)
    }

    override fun update(dt: Float) {
        if (msgT > 0) msgT -= dt
        if (!rolling) return
        val sub = 4; val sdt = dt / sub
        repeat(sub) {
            if (by > pinTop - 60 * dp) { bx += bvx * sdt; by += bvy * sdt; bvx += spin * sdt * (if (by < h * 0.45f) 1f else 0.2f) }
            if (bx < laneL + ballR * 0.6f || bx > laneR - ballR * 0.6f) { bvx = 0f; spin = 0f; bx = bx.coerceIn(laneL + ballR * 0.6f, laneR - ballR * 0.6f); bvy *= 0.98f } // gutter
            for (p in pins) {
                if (!p.standing && p.down) continue
                val d = hypot(p.x - bx, p.y - by)
                if (d < ballR + pinR && d > 0.01f) {
                    val nx = (p.x - bx) / d; val ny = (p.y - by) / d
                    val vb = bvx * nx + bvy * ny
                    p.vx += nx * -vb * 1.1f + bvx * 0.2f; p.vy += ny * -vb * 1.1f
                    if (p.standing) { p.standing = false; sfx(Sfx.HIT) }
                    bvx *= 0.97f; bvy *= 0.95f
                }
                p.x += p.vx * sdt; p.y += p.vy * sdt; p.vx *= 0.985f; p.vy *= 0.985f
            }
            for (i in pins.indices) for (j in i + 1 until pins.size) {
                val a = pins[i]; val b = pins[j]
                val d = hypot(a.x - b.x, a.y - b.y)
                if (d < pinR * 2 && d > 0.01f) {
                    val nx = (b.x - a.x) / d; val ny = (b.y - a.y) / d
                    val rv = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                    if (rv < 0) { a.vx += rv * nx; a.vy += rv * ny; b.vx -= rv * nx; b.vy -= rv * ny }
                    val push = (pinR * 2 - d) / 2; a.x -= nx * push; a.y -= ny * push; b.x += nx * push; b.y += ny * push
                    if (hypot(a.vx, a.vy) > 30 * dp) { if (b.standing) sfx(Sfx.TICK); b.standing = false }
                    if (hypot(b.vx, b.vy) > 30 * dp) { if (a.standing) sfx(Sfx.TICK); a.standing = false }
                }
            }
        }
        for (p in pins) if (!p.standing && (p.x < laneL || p.x > laneR || p.y < pinTop - 80 * dp)) p.down = true
        if (by < pinTop - 60 * dp || (bvy > -50 * dp)) {
            settle += dt
            if (settle > 1.3f) finishRoll()
        }
    }

    private fun finishRoll() {
        rolling = false
        val knocked = pins.count { !it.standing }
        rolls.add(knocked)
        val strike = rollInFrame == 0 && knocked == 10
        val standing = pins.count { it.standing }
        msg = when { strike -> "STRIKE!"; rollInFrame >= 1 && standing == 0 -> "SPARE!"; knocked == 0 -> "Bóng vào rãnh"; else -> "$knocked pin" }
        msgT = 1.3f
        sfx(if (strike || standing == 0) Sfx.WIN else Sfx.CLICK)
        score(total())
        // advance frame/roll
        if (frame < 9) {
            if (strike || rollInFrame == 1) { frame++; rollInFrame = 0; setPins(true) } else { rollInFrame++; setPins(false) }
        } else {
            rollInFrame++
            val fr = frameRolls(9)
            val doneTenth = (rollInFrame == 2 && fr.sum() < 10) || rollInFrame == 3
            if (doneTenth) { after(1f) { gameOver(true, "Kết thúc!", "Tổng điểm ${total()}", total()) }; return }
            if (standing == 0) setPins(true) else setPins(false)
        }
        status()
    }

    private fun frameRolls(f: Int): List<Int> {
        var i = 0; var fr = 0
        while (fr < f && i < rolls.size) { if (rolls[i] == 10) i++ else i += 2; fr++ }
        return rolls.drop(i)
    }

    private fun total(): Int {
        var s = 0; var i = 0
        for (f in 0 until 10) {
            if (i >= rolls.size) break
            if (rolls[i] == 10) { s += 10 + rolls.getOrElse(i + 1) { 0 } + rolls.getOrElse(i + 2) { 0 }; i++ }
            else { val a = rolls[i]; val b = rolls.getOrElse(i + 1) { 0 }; s += a + b; if (a + b == 10) s += rolls.getOrElse(i + 2) { 0 }; i += 2 }
        }
        return s
    }

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h, 0f, 0xFF1A1A2E.toInt(), 0xFF0F0F1A.toInt())
        c.rect(laneL - w * 0.06f, 0f, laneR + w * 0.06f, h, 0xFF37474F.toInt())
        c.gradRect(laneL, 0f, laneR, h, 0f, 0xFFD7A86E.toInt(), 0xFFF0C890.toInt())
        for (i in 1 until 10) c.rect(laneL + (laneR - laneL) * i / 10f - 0.5f, 0f, laneL + (laneR - laneL) * i / 10f + 0.5f, h, Pal.alpha(0xFF8D6E63.toInt(), 0.35f))
        for (i in 0 until 7) { val x = laneL + (laneR - laneL) * (i + 1) / 8f; c.text("▲", x, h * 0.62f + kotlin.math.abs(i - 3) * 8 * dp, 10 * dp, 0xFF8D6E63.toInt()) }
        c.rect(laneL, foulY, laneR, foulY + 2 * dp, Pal.RED)
        for (p in pins.sortedBy { it.y }) {
            if (p.down) continue
            if (p.standing) {
                c.circle(p.x, p.y + pinR * 0.3f, pinR * 1.1f, Pal.alpha(Pal.BLACK, 0.2f))
                c.circle(p.x, p.y, pinR, Pal.WHITE); c.circle(p.x, p.y, pinR * 0.45f, Pal.RED); c.circle(p.x - pinR * 0.3f, p.y - pinR * 0.3f, pinR * 0.25f, Pal.alpha(Pal.WHITE, 0.8f))
            } else {
                val a = Math.toDegrees(kotlin.math.atan2(p.vy.toDouble(), p.vx.toDouble())).toFloat()
                c.save(); c.rotate(a + 90f, p.x, p.y)
                c.rrect(p.x - pinR * 0.7f, p.y - pinR * 1.8f, p.x + pinR * 0.7f, p.y + pinR * 1.8f, pinR * 0.7f, 0xFFEEEEEE.toInt())
                c.rect(p.x - pinR * 0.7f, p.y - pinR * 0.9f, p.x + pinR * 0.7f, p.y - pinR * 0.6f, Pal.RED)
                c.restore()
            }
        }
        c.ball(bx, by, ballR, 0xFF3949AB.toInt())
        c.circle(bx - ballR * 0.2f, by - ballR * 0.35f, ballR * 0.12f, 0xFF1A237E.toInt()); c.circle(bx + ballR * 0.2f, by - ballR * 0.35f, ballR * 0.12f, 0xFF1A237E.toInt()); c.circle(bx, by - ballR * 0.05f, ballR * 0.12f, 0xFF1A237E.toInt())
        // scoreboard
        val fw = w / 10f
        c.rect(0f, h - 34 * dp, w, h, Pal.alpha(Pal.BLACK, 0.6f))
        var i = 0; var running = 0
        for (f in 0 until 10) {
            val x = f * fw
            c.rect(x, h - 34 * dp, x + 1, h, Pal.LINE)
            var label = ""
            if (i < rolls.size) {
                if (f < 9) {
                    if (rolls[i] == 10) { label = "X"; i++ } else { val a = rolls[i]; val b = rolls.getOrNull(i + 1); label = "$a" + (b?.let { if (a + it == 10) "/" else "$it" } ?: ""); i += 2 }
                } else label = rolls.drop(i).joinToString("") { if (it == 10) "X" else "$it" }
            }
            c.text(label, x + fw / 2, h - 24 * dp, 10 * dp, Pal.YELLOW)
            if (f == frame && !over) c.rect(x + 2, h - 3 * dp, x + fw - 2, h - 1, Pal.CYAN)
        }
        running = total()
        c.text("Tổng: $running", w / 2, h - 10 * dp, 11 * dp, Pal.WHITE)
        if (msgT > 0f) c.text(msg, w / 2, h * 0.5f, 30 * dp, Pal.alpha(if (msg.endsWith("!") && !msg.startsWith("Bóng")) Pal.PINK else Pal.WHITE, msgT.coerceAtMost(1f)))
        if (!rolling && rolls.isEmpty()) c.text("Kéo ngang để chọn vị trí, vuốt lên để ném", w / 2, h * 0.7f, 13 * dp, 0xFF3E2723.toInt())
    }
}
