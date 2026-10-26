package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class HoleIoGame : Game() {
    override val realtime = true
    override val help = "Kéo ngón tay để di chuyển hố đen. Nuốt các vật nhỏ hơn hố để lớn dần, rồi nuốt cả nhà và cây lớn. " +
        "Bạn có 2 phút, hãy lớn nhất có thể! Hố to hơn có thể nuốt hố nhỏ hơn."

    private class Thing(var x: Float, var y: Float, val size: Float, val kind: Int, var fall: Float = -1f)
    private class Hole(var x: Float, var y: Float, var r: Float, val color: Int, var vx: Float = 0f, var vy: Float = 0f, var alive: Boolean = true, var tx: Float = 0f, var ty: Float = 0f, var score: Int = 0)

    private val world = 2400f
    private val things = ArrayList<Thing>()
    private val holes = ArrayList<Hole>()
    private var time = 120f; private var dragX = 0f; private var dragY = 0f; private var dragging = false
    private var sx = 0f; private var sy = 0f
    private val emojis = arrayOf("🌳", "🚗", "🏠", "🚌", "🏢", "🌲", "⛲", "🚲", "🗑", "🚦")
    private val sizes = floatArrayOf(34f, 30f, 60f, 48f, 90f, 36f, 52f, 18f, 14f, 20f)

    override fun newGame() {
        things.clear(); holes.clear(); time = 120f
        val rnd = Random(Random.nextInt())
        repeat(520) {
            val k = rnd.nextInt(emojis.size)
            val s = sizes[k] * (0.8f + rnd.nextFloat() * 0.4f)
            things.add(Thing(rnd.nextFloat() * world, rnd.nextFloat() * world, s, k))
        }
        holes.add(Hole(world / 2, world / 2, 24f, Pal.ACCENT))
        val cols = intArrayOf(Pal.ORANGE, Pal.PINK, Pal.GREEN, Pal.CYAN)
        for (i in 0..3) holes.add(Hole(rnd.nextFloat() * world, rnd.nextFloat() * world, 24f, cols[i]))
        score(0)
    }

    private val scale get() = w / 520f

    override fun down(x: Float, y: Float) { dragging = true; sx = x; sy = y; dragX = 0f; dragY = 0f }
    override fun move(x: Float, y: Float) { dragX = x - sx; dragY = y - sy }
    override fun up(x: Float, y: Float) { dragging = false; dragX = 0f; dragY = 0f }

    override fun update(dt: Float) {
        time -= dt
        info("⏱ ${time.toInt().coerceAtLeast(0)}s · Cỡ ${holes[0].r.toInt()}")
        if (time <= 0f) {
            val rank = 1 + holes.drop(1).count { it.alive && it.score > holes[0].score }
            sfx(if (rank == 1) Sfx.WIN else Sfx.LOSE)
            gameOver(rank == 1, if (rank == 1) "Bạn là hố lớn nhất!" else "Hạng $rank", "Điểm ${holes[0].score}", holes[0].score); return
        }
        val me = holes[0]
        val d = hypot(dragX, dragY)
        val maxSp = 220f
        if (d > 4 * dp) { me.vx = dragX / d * maxSp * (d / (50 * dp)).coerceAtMost(1f); me.vy = dragY / d * maxSp * (d / (50 * dp)).coerceAtMost(1f) }
        else { me.vx *= 0.85f; me.vy *= 0.85f }
        for ((i, hl) in holes.withIndex()) {
            if (!hl.alive) continue
            if (i > 0) botMove(hl, dt)
            hl.x = (hl.x + hl.vx * dt).coerceIn(hl.r, world - hl.r); hl.y = (hl.y + hl.vy * dt).coerceIn(hl.r, world - hl.r)
        }
        val it = things.iterator()
        while (it.hasNext()) {
            val t = it.next()
            if (t.fall >= 0f) { t.fall += dt * 3f; if (t.fall >= 1f) it.remove(); continue }
            for ((i, hl) in holes.withIndex()) {
                if (!hl.alive) continue
                if (t.size < hl.r * 1.6f && hypot(t.x - hl.x, t.y - hl.y) < hl.r - t.size * 0.2f) {
                    t.fall = 0f; t.x = hl.x; t.y = hl.y
                    hl.r += t.size * 0.035f; hl.score += t.size.toInt()
                    if (i == 0) { sfx(Sfx.POP); score(hl.score) }
                    break
                }
            }
        }
        for (a in holes) for (b in holes) {
            if (a === b || !a.alive || !b.alive) continue
            if (a.r > b.r * 1.2f && hypot(a.x - b.x, a.y - b.y) < a.r - b.r * 0.4f) {
                b.alive = false; a.r += b.r * 0.4f; a.score += b.score / 2 + 100
                if (b === me) { sfx(Sfx.LOSE); buzz(100); gameOver(false, "Bạn bị nuốt mất!", "Điểm ${me.score}", me.score); return }
                if (a === me) { sfx(Sfx.WIN); score(me.score) }
            }
        }
        if (things.size < 380) repeat(5) { val k = Random.nextInt(emojis.size); things.add(Thing(Random.nextFloat() * world, Random.nextFloat() * world, sizes[k] * (0.8f + Random.nextFloat() * 0.4f), k)) }
    }

    private fun botMove(hl: Hole, dt: Float) {
        val me = holes[0]
        if (me.alive && me.r * 1.2f < hl.r && hypot(me.x - hl.x, me.y - hl.y) < 300f) { hl.tx = me.x; hl.ty = me.y }
        else if (me.r > hl.r * 1.2f && hypot(me.x - hl.x, me.y - hl.y) < 220f) { hl.tx = hl.x * 2 - me.x; hl.ty = hl.y * 2 - me.y }
        else if (hypot(hl.tx - hl.x, hl.ty - hl.y) < 20f || Random.nextFloat() < dt * 0.3f) {
            val t = things.filter { it.fall < 0 && it.size < hl.r * 1.5f }.minByOrNull { hypot(it.x - hl.x, it.y - hl.y) + Random.nextFloat() * 200f }
            if (t != null) { hl.tx = t.x; hl.ty = t.y } else { hl.tx = Random.nextFloat() * world; hl.ty = Random.nextFloat() * world }
        }
        val a = atan2(hl.ty - hl.y, hl.tx - hl.x)
        hl.vx = cos(a) * 170f; hl.vy = sin(a) * 170f
    }

    override fun draw(c: Canvas) {
        val me = holes[0]
        val k = scale * (36f / (me.r + 12f)).coerceIn(0.35f, 1f).let { 0.5f + it }
        val camX = me.x - w / 2 / k; val camY = me.y - h / 2 / k
        fun X(x: Float) = (x - camX) * k
        fun Y(y: Float) = (y - camY) * k
        c.rect(0f, 0f, w, h, 0xFF5D6D7E.toInt())
        c.rect(X(0f), Y(0f), X(world), Y(world), 0xFF9CCC65.toInt())
        // road grid
        var g = 0f
        while (g <= world) {
            c.rect(X(g) - 14 * k, Y(0f), X(g) + 14 * k, Y(world), 0xFF78909C.toInt())
            c.rect(X(0f), Y(g) - 14 * k, X(world), Y(g) + 14 * k, 0xFF78909C.toInt())
            g += 300f
        }
        for (hl in holes) if (hl.alive) {
            c.circle(X(hl.x), Y(hl.y), hl.r * k, 0xFF111111.toInt())
            c.ring(X(hl.x), Y(hl.y), hl.r * k, 3 * dp, hl.color)
        }
        for (t in things) {
            val x = X(t.x); val y = Y(t.y)
            if (x < -100 || x > w + 100 || y < -100 || y > h + 100) continue
            val s = if (t.fall >= 0f) t.size * (1 - t.fall) else t.size
            c.text(emojis[t.kind], x, y - s * k * 0.3f, s * k, Pal.WHITE)
        }
        for (hl in holes) if (hl.alive) c.text(if (hl === me) "BẠN" else "${hl.r.toInt()}", X(hl.x), Y(hl.y) - hl.r * k - 10 * dp, 11 * dp, Pal.WHITE)
        // mini leaderboard
        val board = holes.filter { it.alive }.sortedByDescending { it.score }
        c.rrect(8 * dp, 8 * dp, 120 * dp, 14 * dp + board.size * 16 * dp, 8 * dp, Pal.alpha(Pal.BLACK, 0.4f))
        board.forEachIndexed { i, hl -> c.text("${i + 1}. ${if (hl === me) "Bạn" else "Hố"} ${hl.score}", 16 * dp, 20 * dp + i * 16 * dp, 11 * dp, hl.color, android.graphics.Paint.Align.LEFT) }
    }
}
