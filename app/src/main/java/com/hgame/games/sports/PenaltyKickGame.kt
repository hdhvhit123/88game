package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.gradRect
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

class PenaltyKickGame : Game() {
    override val realtime = true
    override val help = "Vuốt từ quả bóng về phía khung thành để sút: hướng vuốt quyết định góc sút, vuốt càng nhanh bóng càng mạnh và cong. " +
        "Thủ môn sẽ đoán hướng để cản phá. 10 lượt sút, mỗi bàn được điểm, sút vào góc chết có thưởng."

    private var bx = 0f; private var by = 0f; private var bz = 0f   // ball position on screen + height factor
    private var vx = 0f; private var vy = 0f; private var curve = 0f; private var flying = false; private var t = 0f
    private var keeperX = 0f; private var keeperTarget = 0f; private var keeperDive = 0f; private var keeperSpeed = 0f
    private var shots = 0; private var goals = 0; private var points = 0
    private var result = ""; private var resultT = 0f
    private var sx = 0f; private var sy = 0f; private var st = 0L
    private var goalL = 0f; private var goalR = 0f; private var goalT = 0f; private var goalB = 0f
    private val trail = ArrayList<FloatArray>()

    override fun newGame() { shots = 0; goals = 0; points = 0; reset(); score(0); status() }

    private fun status() = info("Lượt ${minOf(shots + 1, 10)}/10 · Bàn thắng: $goals")

    override fun layout() {
        goalL = w * 0.12f; goalR = w * 0.88f; goalT = h * 0.18f; goalB = h * 0.42f
        if (!flying) reset()
    }

    private fun reset() { bx = w / 2; by = h * 0.8f; bz = 1f; flying = false; keeperX = w / 2; keeperDive = 0f; trail.clear() }

    override fun down(x: Float, y: Float) { sx = x; sy = y; st = System.nanoTime() }

    override fun up(x: Float, y: Float) {
        if (flying || over || resultT > 0f) return
        val dy = sy - y; val dx = x - sx
        if (dy < 30 * dp || hypot(sx - bx, sy - by) > w * 0.3f) return
        val dt = ((System.nanoTime() - st) / 1e9f).coerceIn(0.05f, 0.6f)
        val speed = (hypot(dx, dy) / dt).coerceIn(600 * dp, 2600 * dp)
        // aim point at goal line
        val k = (by - (goalT + goalB) / 2) / dy
        val aimX = bx + dx * k
        val time = (by - goalB * 0.9f) / (speed * 0.55f)
        vx = (aimX - bx) / time; vy = -(by - goalB * 0.9f) / time
        curve = (Random.nextFloat() - 0.5f) * dx * 0.8f
        val power = (speed / (2600 * dp))
        lift = goalB - (goalB - goalT) * (0.1f + power * 0.95f).coerceAtMost(1.15f)
        flying = true; t = 0f; sfx(Sfx.HIT)
        // keeper guesses with some skill
        keeperTarget = if (Random.nextFloat() < 0.55f) aimX + (Random.nextFloat() - 0.5f) * w * 0.15f else w * (0.2f + Random.nextFloat() * 0.6f)
        keeperSpeed = w * (0.9f + shots * 0.05f)
    }

    private var lift = 0f

    override fun update(dt: Float) {
        if (resultT > 0f) { resultT -= dt; if (resultT <= 0f) { if (shots >= 10) end() else reset() }; return }
        if (!flying) { keeperX = w / 2 + kotlin.math.sin(System.nanoTime() / 4e8f) * w * 0.06f; return }
        t += dt
        bx += (vx + curve * t) * dt; by += vy * dt
        bz = 1f - ((h * 0.8f - by) / (h * 0.8f - goalB)).coerceIn(0f, 1f) * 0.55f
        trail.add(floatArrayOf(bx, screenY()))
        if (trail.size > 12) trail.removeAt(0)
        val dir = keeperTarget - keeperX
        keeperX += dir.coerceIn(-keeperSpeed * dt, keeperSpeed * dt)
        keeperDive = (abs(keeperX - w / 2) / (w * 0.3f)).coerceAtMost(1f) * (if (keeperX < w / 2) -1 else 1)
        if (by <= goalB) {
            flying = false; shots++
            val ballY = screenY()
            val inGoal = bx > goalL + 6 * dp && bx < goalR - 6 * dp && ballY > goalT + 4 * dp
            val keeperW = w * 0.16f; val keeperH = (goalB - goalT) * 0.75f
            val saved = inGoal && abs(bx - keeperX) < keeperW * 0.75f && ballY > goalB - keeperH - abs(keeperDive) * 10 * dp
            when {
                !inGoal -> { result = "Trượt khung thành!"; sfx(Sfx.ERROR) }
                saved -> { result = "Thủ môn cản phá!"; sfx(Sfx.HIT) }
                else -> {
                    goals++
                    val corner = (bx < goalL + w * 0.12f || bx > goalR - w * 0.12f) && ballY < goalT + (goalB - goalT) * 0.4f
                    points += if (corner) 3 else 1
                    result = if (corner) "GÓC CHẾT! +3" else "VÀOOO! +1"; sfx(Sfx.WIN); buzz(30)
                }
            }
            score(points); status(); resultT = 1.4f
        }
    }

    private fun screenY(): Float {
        val p = ((h * 0.8f - by) / (h * 0.8f - goalB)).coerceIn(0f, 1f)
        return by - (goalB - lift) * p * p * 1.0f * if (flying || resultT > 0) 1 else 0
    }

    private fun end() { sfx(if (goals >= 6) Sfx.WIN else Sfx.LOSE); gameOver(goals >= 6, "Kết thúc loạt sút", "$goals/10 bàn · $points điểm", points) }

    override fun draw(c: Canvas) {
        c.gradRect(0f, 0f, w, h * 0.3f, 0f, 0xFF1A237E.toInt(), 0xFF3949AB.toInt())
        for (i in 0..7) c.circle(w * (i / 7f), h * 0.1f, 12 * dp, Pal.alpha(Pal.WHITE, 0.6f))
        c.rect(0f, h * 0.12f, w, h * 0.3f, 0xFF283593.toInt())
        for (i in 0 until 40) c.circle((i * 37 % 100) / 100f * w, h * 0.15f + (i % 4) * h * 0.035f, 4 * dp, Pal.VIVID[i % 8])
        // pitch stripes
        for (i in 0 until 8) c.rect(0f, h * 0.3f + i * h * 0.0875f, w, h * 0.3f + (i + 1) * h * 0.0875f, if (i % 2 == 0) 0xFF43A047.toInt() else 0xFF388E3C.toInt())
        // goal
        c.rect(goalL, goalT, goalR, goalB, Pal.alpha(Pal.WHITE, 0.15f))
        var nx = goalL; while (nx < goalR) { c.line(nx, goalT, nx, goalB, 1f, Pal.alpha(Pal.WHITE, 0.3f)); nx += 10 * dp }
        var ny = goalT; while (ny < goalB) { c.line(goalL, ny, goalR, ny, 1f, Pal.alpha(Pal.WHITE, 0.3f)); ny += 10 * dp }
        c.rect(goalL - 5 * dp, goalT - 5 * dp, goalR + 5 * dp, goalT, Pal.WHITE)
        c.rect(goalL - 5 * dp, goalT, goalL, goalB, Pal.WHITE); c.rect(goalR, goalT, goalR + 5 * dp, goalB, Pal.WHITE)
        // box lines
        c.line(w * 0.02f, goalB + h * 0.12f, w * 0.98f, goalB + h * 0.12f, 2 * dp, Pal.alpha(Pal.WHITE, 0.7f))
        c.line(0f, goalB, w, goalB, 2 * dp, Pal.alpha(Pal.WHITE, 0.7f))
        c.circle(w / 2, h * 0.8f + 6 * dp, 4 * dp, Pal.WHITE)
        // keeper
        val kh = (goalB - goalT) * 0.75f; val kw = w * 0.1f
        c.save(); c.rotate(keeperDive * 55f, keeperX, goalB)
        c.rrect(keeperX - kw / 2, goalB - kh, keeperX + kw / 2, goalB - kh * 0.25f, kw * 0.3f, Pal.ORANGE)
        c.circle(keeperX, goalB - kh - kw * 0.3f, kw * 0.32f, 0xFFFFCC80.toInt())
        c.rect(keeperX - kw * 0.9f, goalB - kh * 0.95f, keeperX - kw * 0.45f, goalB - kh * 0.8f, Pal.ORANGE)
        c.rect(keeperX + kw * 0.45f, goalB - kh * 0.95f, keeperX + kw * 0.9f, goalB - kh * 0.8f, Pal.ORANGE)
        c.circle(keeperX - kw * 0.95f, goalB - kh * 0.88f, kw * 0.15f, Pal.GREEN); c.circle(keeperX + kw * 0.95f, goalB - kh * 0.88f, kw * 0.15f, Pal.GREEN)
        c.rect(keeperX - kw * 0.4f, goalB - kh * 0.25f, keeperX - kw * 0.1f, goalB, 0xFF212121.toInt()); c.rect(keeperX + kw * 0.1f, goalB - kh * 0.25f, keeperX + kw * 0.4f, goalB, 0xFF212121.toInt())
        c.restore()
        // ball
        trail.forEachIndexed { i, p -> c.circle(p[0], p[1], 10 * dp * bz * i / trail.size, Pal.alpha(Pal.WHITE, 0.25f * i / trail.size)) }
        val ys = if (flying || resultT > 0f) screenY() else by
        c.circle(bx, if (flying) by + 4 * dp else by + 12 * dp * bz, 12 * dp * bz, Pal.alpha(Pal.BLACK, 0.25f))
        c.ball(bx, ys, 14 * dp * bz, Pal.WHITE)
        c.circle(bx, ys, 5 * dp * bz, 0xFF212121.toInt())
        // shot markers
        for (i in 0 until 10) c.circle(w / 2 - 4.5f * 16 * dp + i * 16 * dp, h - 24 * dp, 5 * dp, if (i < shots) Pal.alpha(Pal.WHITE, 0.4f) else Pal.WHITE)
        if (resultT > 0f) {
            c.rrect(w * 0.1f, h * 0.55f - 26 * dp, w * 0.9f, h * 0.55f + 26 * dp, 16 * dp, Pal.alpha(Pal.BLACK, 0.5f))
            c.text(result, w / 2, h * 0.55f, 22 * dp, if (result.startsWith("V") || result.startsWith("G")) Pal.YELLOW else Pal.WHITE)
        } else if (!flying && shots == 0) c.text("Vuốt quả bóng về phía khung thành", w / 2, h * 0.62f, 15 * dp, Pal.WHITE)
    }
}
