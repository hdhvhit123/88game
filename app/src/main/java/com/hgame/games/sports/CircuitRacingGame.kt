package com.hgame.games.sports

import android.graphics.Canvas
import android.graphics.Paint
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class CircuitRacingGame : Game() {
    override val realtime = true
    override val help = "Đua 3 vòng với 3 xe máy trên đường đua khép kín. Xe tự tăng tốc; giữ nửa trái/phải màn hình để đánh lái. " +
        "Chạy ra khỏi đường nhựa sẽ bị chậm lại. Về nhất để thắng!"

    // Track centre line control points (world units), closed loop.
    private val ctrl = listOf(0f to 0f, 300f to -40f, 520f to 80f, 560f to 320f, 420f to 470f, 200f to 420f, 120f to 560f, -120f to 600f, -300f to 420f, -260f to 180f, -120f to 120f)
    private val track = ArrayList<Pair<Float, Float>>()
    private val halfW = 46f
    private class Car(var x: Float, var y: Float, var a: Float, val color: Int, val ai: Boolean, var v: Float = 0f, var idx: Int = 0, var progress: Int = 0, var lap: Int = 0, var finished: Float = -1f, val skill: Float = 1f)
    private val cars = ArrayList<Car>()
    private var steer = 0; private val touches = HashMap<Int, Int>()
    private var countdown = 3f; private var raceTime = 0f; private val laps = 3
    private var place = 1

    init {
        // Catmull-Rom sampling of the loop
        val n = ctrl.size
        for (i in 0 until n) {
            val p0 = ctrl[(i - 1 + n) % n]; val p1 = ctrl[i]; val p2 = ctrl[(i + 1) % n]; val p3 = ctrl[(i + 2) % n]
            for (k in 0 until 12) {
                val t = k / 12f; val t2 = t * t; val t3 = t2 * t
                fun cr(a: Float, b: Float, c: Float, d: Float) = 0.5f * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3)
                track.add(cr(p0.first, p1.first, p2.first, p3.first) to cr(p0.second, p1.second, p2.second, p3.second))
            }
        }
    }

    override fun newGame() {
        cars.clear(); countdown = 3f; raceTime = 0f; touches.clear(); steer = 0
        val (x0, y0) = track[0]; val (x1, y1) = track[1]
        val a = atan2(y1 - y0, x1 - x0)
        val nx = -sin(a); val ny = cos(a)
        val cols = intArrayOf(Pal.CYAN, Pal.ORANGE, Pal.PINK, Pal.GREEN)
        for (i in 0 until 4) {
            val back = (i / 2) * 40f + 20f; val side = if (i % 2 == 0) -18f else 18f
            cars.add(Car(x0 - cos(a) * back + nx * side, y0 - sin(a) * back + ny * side, a, cols[i], i != 0, skill = 0.88f + i * 0.04f + Random.nextFloat() * 0.03f))
        }
        score(0); info("Vòng 1/$laps")
    }

    override fun pointerDown(id: Int, x: Float, y: Float) { touches[id] = if (x < w / 2) -1 else 1; steer = touches.values.lastOrNull() ?: 0 }
    override fun pointerUp(id: Int, x: Float, y: Float) { touches.remove(id); steer = touches.values.lastOrNull() ?: 0 }

    private fun nearest(c: Car): Int {
        var best = c.idx; var bd = Float.MAX_VALUE
        for (k in -6..10) {
            val i = Math.floorMod(c.idx + k, track.size)
            val d = hypot(track[i].first - c.x, track[i].second - c.y)
            if (d < bd) { bd = d; best = i }
        }
        return best
    }

    private fun distToTrack(c: Car): Float { val i = nearest(c); return hypot(track[i].first - c.x, track[i].second - c.y) }

    override fun update(dt: Float) {
        if (countdown > 0f) { val before = countdown.toInt(); countdown -= dt; if (countdown.toInt() != before) sfx(if (countdown <= 0f) Sfx.WIN else Sfx.TICK); return }
        raceTime += dt
        for (c in cars) {
            if (c.finished >= 0f && !c.ai) continue
            val off = distToTrack(c) > halfW
            val top = (if (c.ai) 230f * c.skill else 240f) * (if (off) 0.45f else 1f)
            c.v += ((top - c.v) * 1.2f).coerceAtMost(160f) * dt
            val turn: Float
            if (c.ai) {
                val target = track[(c.idx + 6) % track.size]
                var d = atan2(target.second - c.y, target.first - c.x) - c.a
                while (d > Math.PI) d -= (2 * Math.PI).toFloat(); while (d < -Math.PI) d += (2 * Math.PI).toFloat()
                turn = d.coerceIn(-1f, 1f)
                if (kotlin.math.abs(d) > 0.5f) c.v *= (1 - dt * 0.8f)
            } else turn = steer.toFloat()
            c.a += turn * 2.6f * dt * (c.v / 240f).coerceIn(0.3f, 1f)
            c.x += cos(c.a) * c.v * dt; c.y += sin(c.a) * c.v * dt
            val ni = nearest(c)
            val step = Math.floorMod(ni - c.idx, track.size)
            if (step in 1 until track.size / 2) c.progress += step
            c.idx = ni
            val lap = c.progress / track.size
            if (lap > c.lap) {
                c.lap = lap
                if (!c.ai && lap < laps) { info("Vòng ${lap + 1}/$laps"); sfx(Sfx.COIN) }
                if (lap >= laps && c.finished < 0f) { c.finished = raceTime }
            }
        }
        // car-car bumps
        for (i in cars.indices) for (j in i + 1 until cars.size) {
            val a = cars[i]; val b = cars[j]; val d = hypot(a.x - b.x, a.y - b.y)
            if (d < 26f && d > 0.01f) { val nx = (b.x - a.x) / d; val ny = (b.y - a.y) / d; val p = (26f - d) / 2; a.x -= nx * p; a.y -= ny * p; b.x += nx * p; b.y += ny * p; a.v *= 0.98f; b.v *= 0.98f }
        }
        val me = cars[0]
        place = 1 + cars.drop(1).count { it.progress > me.progress || (it.finished >= 0f && (me.finished < 0f || it.finished < me.finished)) }
        if (me.finished >= 0f) {
            sfx(if (place == 1) Sfx.WIN else Sfx.LOSE); score(5 - place)
            gameOver(place == 1, if (place == 1) "Về nhất!" else "Về thứ $place", "Thời gian ${"%.1f".format(me.finished)}s", 5 - place)
        }
    }

    override fun draw(c: Canvas) {
        c.rect(0f, 0f, w, h, 0xFF4CAF50.toInt())
        val me = cars[0]
        val scale = w / 420f
        c.save()
        c.translate(w / 2, h * 0.62f); c.scale(scale, scale); c.rotate(-Math.toDegrees(me.a.toDouble()).toFloat() - 90f); c.translate(-me.x, -me.y)
        val p = Gfx.path; p.reset()
        track.forEachIndexed { i, (x, y) -> if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }; p.close()
        val st = Gfx.stroke.apply { shader = null; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        st.strokeWidth = halfW * 2 + 14f; st.color = Pal.WHITE; c.drawPath(p, st)
        st.strokeWidth = halfW * 2 + 6f; st.color = Pal.RED; c.drawPath(p, st)
        st.strokeWidth = halfW * 2; st.color = 0xFF505050.toInt(); c.drawPath(p, st)
        st.strokeWidth = 2f; st.color = Pal.alpha(Pal.WHITE, 0.5f); st.pathEffect = android.graphics.DashPathEffect(floatArrayOf(18f, 18f), 0f); c.drawPath(p, st); st.pathEffect = null
        // start/finish line
        val (x0, y0) = track[0]; val (x1, y1) = track[1]; val a = atan2(y1 - y0, x1 - x0)
        c.save(); c.translate(x0, y0); c.rotate(Math.toDegrees(a.toDouble()).toFloat())
        for (k in -5..4) for (r in 0..1) c.rect(r * 8f - 8f, k * 9f, r * 8f, (k + 1) * 9f, if ((k + r) % 2 == 0) Pal.WHITE else Pal.BLACK)
        c.restore()
        for (car in cars) {
            c.save(); c.translate(car.x, car.y); c.rotate(Math.toDegrees(car.a.toDouble()).toFloat())
            c.rrect(-14f + 2f, -8f + 3f, 14f + 2f, 8f + 3f, 4f, Pal.alpha(Pal.BLACK, 0.3f))
            c.rrect(-14f, -8f, 14f, 8f, 4f, car.color)
            c.rrect(-2f, -6f, 7f, 6f, 2f, 0xFF263238.toInt())
            c.rect(-14f, -9f, -9f, -6f, 0xFF212121.toInt()); c.rect(-14f, 6f, -9f, 9f, 0xFF212121.toInt()); c.rect(8f, -9f, 13f, -6f, 0xFF212121.toInt()); c.rect(8f, 6f, 13f, 9f, 0xFF212121.toInt())
            c.restore()
        }
        c.restore()
        // HUD
        c.rrect(10 * dp, 10 * dp, 120 * dp, 56 * dp, 12 * dp, Pal.alpha(Pal.BLACK, 0.5f))
        c.text("Hạng $place/4", 18 * dp, 24 * dp, 14 * dp, Pal.YELLOW, Paint.Align.LEFT)
        c.text("Vòng ${minOf(me.lap + 1, laps)}/$laps", 18 * dp, 44 * dp, 13 * dp, Pal.WHITE, Paint.Align.LEFT)
        c.text("${"%.1f".format(raceTime)}s", w - 16 * dp, 26 * dp, 16 * dp, Pal.WHITE, Paint.Align.RIGHT)
        // minimap
        val mm = 90 * dp; val mx = w - mm - 10 * dp; val my = 40 * dp
        c.rrect(mx, my, mx + mm, my + mm, 10 * dp, Pal.alpha(Pal.BLACK, 0.35f))
        val minX = track.minOf { it.first }; val maxX = track.maxOf { it.first }; val minY = track.minOf { it.second }; val maxY = track.maxOf { it.second }
        val k = (mm - 16 * dp) / maxOf(maxX - minX, maxY - minY)
        for (i in track.indices step 3) c.circle(mx + 8 * dp + (track[i].first - minX) * k, my + 8 * dp + (track[i].second - minY) * k, 1.5f * dp, Pal.alpha(Pal.WHITE, 0.6f))
        for (car in cars) c.circle(mx + 8 * dp + (car.x - minX) * k, my + 8 * dp + (car.y - minY) * k, 3.5f * dp, car.color)
        if (countdown > 0f) c.text(if (countdown > 0.01f) "${countdown.toInt() + 1}" else "ĐI!", w / 2, h * 0.35f, 64 * dp, Pal.YELLOW)
        else if (raceTime < 1f) c.text("ĐI!", w / 2, h * 0.35f, 64 * dp, Pal.alpha(Pal.YELLOW, 1 - raceTime))
        c.rrect(10 * dp, h - 70 * dp, 70 * dp, h - 10 * dp, 14 * dp, Pal.alpha(Pal.WHITE, if (steer < 0) 0.5f else 0.2f)); c.text("◀", 40 * dp, h - 40 * dp, 22 * dp, Pal.WHITE)
        c.rrect(w - 70 * dp, h - 70 * dp, w - 10 * dp, h - 10 * dp, 14 * dp, Pal.alpha(Pal.WHITE, if (steer > 0) 0.5f else 0.2f)); c.text("▶", w - 40 * dp, h - 40 * dp, 22 * dp, Pal.WHITE)
    }
}
