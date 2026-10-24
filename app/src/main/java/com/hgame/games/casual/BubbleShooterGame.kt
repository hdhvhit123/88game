package com.hgame.games.casual

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class BubbleShooterGame : Game() {
    override val realtime = true
    override val help = "Kéo để ngắm, thả tay để bắn bóng. Ghép 3 bóng cùng màu trở lên để nổ, các bóng lơ lửng không còn bám trần sẽ rơi theo. " +
        "Sau mỗi 6 phát trượt, trần sẽ hạ xuống một hàng. Bóng chạm vạch dưới là thua."

    private val cols = 10
    private val colors = intArrayOf(Pal.RED, Pal.BLUE, Pal.GREEN, Pal.YELLOW, Pal.PURPLE, Pal.CYAN)
    private val grid = HashMap<Int, Int>()   // key row*cols+col -> colour index
    private var rowOffset = 0 // parity of the top row (for hex layout after the ceiling drops)
    private var r = 0f; private var top = 0f
    private var shotX = 0f; private var shotY = 0f; private var vx = 0f; private var vy = 0f; private var flying = false
    private var cur = 0; private var next = 0; private var aim = -Math.PI.toFloat() / 2; private var aiming = false
    private var misses = 0; private var points = 0; private var level = 1; private var nColors = 4
    private val fx = Particles().apply { gravity = 900f }
    private class Falling(var x: Float, var y: Float, var vy: Float, val col: Int)
    private val falling = ArrayList<Falling>()

    override fun newGame() { level = 1; points = 0; score(0); setup() }

    private fun setup() {
        grid.clear(); falling.clear(); fx.clear(); rowOffset = 0; misses = 0; flying = false
        nColors = (3 + level).coerceAtMost(6)
        for (row in 0 until 5 + level.coerceAtMost(4)) for (c in 0 until rowLen(row)) grid[row * cols + c] = Random.nextInt(nColors)
        cur = pick(); next = pick()
        info("Màn $level")
    }

    private fun rowLen(row: Int) = if ((row + rowOffset) % 2 == 0) cols else cols - 1
    private fun cx(row: Int, c: Int) = r + c * 2 * r + if ((row + rowOffset) % 2 == 1) r else 0f
    private fun cy(row: Int) = top + r + row * r * 1.732f

    private fun pick(): Int {
        val present = grid.values.toSet()
        return if (present.isEmpty()) Random.nextInt(nColors) else present.random()
    }

    override fun layout() { r = w / (cols * 2f); top = 6 * dp }

    private val shooterY get() = h - 60 * dp
    private val deadLine get() = shooterY - 2.2f * r

    override fun down(x: Float, y: Float) { aiming = true; setAim(x, y) }
    override fun move(x: Float, y: Float) { setAim(x, y) }
    override fun up(x: Float, y: Float) {
        aiming = false
        if (flying || over || y > shooterY + 20 * dp) return
        setAim(x, y)
        flying = true; shotX = w / 2; shotY = shooterY
        val sp = h * 1.6f; vx = cos(aim) * sp; vy = sin(aim) * sp
        sfx(Sfx.SHOOT)
    }

    private fun setAim(x: Float, y: Float) { aim = atan2(y - shooterY, x - w / 2).coerceIn(-Math.PI.toFloat() + 0.15f, -0.15f) }

    override fun update(dt: Float) {
        fx.update(dt)
        for (f in falling) { f.vy += h * 2f * dt; f.y += f.vy * dt }
        falling.removeAll { it.y > h + r }
        if (!flying) return
        val steps = 6
        repeat(steps) {
            if (!flying) return@repeat
            shotX += vx * dt / steps; shotY += vy * dt / steps
            if (shotX < r) { shotX = r; vx = -vx }; if (shotX > w - r) { shotX = w - r; vx = -vx }
            if (shotY < top + r) { snap(); return@repeat }
            for ((k, _) in grid) {
                val row = k / cols; val c = k % cols
                if (hypot(cx(row, c) - shotX, cy(row) - shotY) < r * 1.75f) { snap(); return@repeat }
            }
        }
    }

    private fun snap() {
        flying = false
        // nearest free cell
        var best = -1; var bd = Float.MAX_VALUE
        val approxRow = ((shotY - top - r) / (r * 1.732f)).toInt().coerceAtLeast(0)
        for (row in (approxRow - 1).coerceAtLeast(0)..approxRow + 1) for (c in 0 until rowLen(row)) {
            val k = row * cols + c
            if (grid.containsKey(k)) continue
            val d = hypot(cx(row, c) - shotX, cy(row) - shotY)
            if (d < bd) { bd = d; best = k }
        }
        if (best < 0) return
        grid[best] = cur
        val group = flood(best) { grid[it] == cur }
        if (group.size >= 3) {
            for (k in group) { pop(k); grid.remove(k) }
            points += group.size * 10; sfx(Sfx.POP)
            dropFloating()
        } else {
            sfx(Sfx.TAP); misses++
            if (misses >= 6) { misses = 0; lowerCeiling() }
        }
        score(points)
        cur = next; next = pick()
        if (grid.isEmpty()) { level++; points += 200; score(points); sfx(Sfx.WIN); setup(); return }
        if (grid.keys.any { cy(it / cols) + r > deadLine }) { sfx(Sfx.LOSE); gameOver(false, "Bóng chạm vạch!", "Màn $level · $points điểm", points) }
    }

    private fun pop(k: Int) = fx.burst(cx(k / cols, k % cols), cy(k / cols), colors[grid[k] ?: 0], 8, 180 * dp, 3 * dp, 0.5f)

    private fun neighbors(k: Int): List<Int> {
        val row = k / cols; val c = k % cols
        val odd = (row + rowOffset) % 2 == 1
        val list = arrayListOf(row to c - 1, row to c + 1)
        val dcs = if (odd) intArrayOf(0, 1) else intArrayOf(-1, 0)
        for (dr in intArrayOf(-1, 1)) for (d in dcs) list.add(row + dr to c + d)
        return list.filter { (rr, cc) -> rr >= 0 && cc >= 0 && cc < rowLen(rr) }.map { (rr, cc) -> rr * cols + cc }
    }

    private fun flood(start: Int, ok: (Int) -> Boolean): Set<Int> {
        val seen = hashSetOf(start); val q = ArrayDeque(listOf(start))
        while (q.isNotEmpty()) { val k = q.removeFirst(); for (n in neighbors(k)) if (n !in seen && grid.containsKey(n) && ok(n)) { seen.add(n); q.add(n) } }
        return seen
    }

    private fun dropFloating() {
        val anchored = HashSet<Int>()
        for (c in 0 until cols) if (grid.containsKey(c) && c !in anchored) anchored.addAll(flood(c) { true })
        val loose = grid.keys.filter { it !in anchored }
        for (k in loose) { falling.add(Falling(cx(k / cols, k % cols), cy(k / cols), -100 * dp, colors[grid[k]!!])); grid.remove(k) }
        if (loose.isNotEmpty()) { points += loose.size * 20; sfx(Sfx.COIN) }
    }

    private fun lowerCeiling() {
        val old = HashMap(grid); grid.clear()
        rowOffset = (rowOffset + 1) % 2
        for ((k, v) in old) grid[(k / cols + 1) * cols + k % cols] = v
        for (c in 0 until rowLen(0)) grid[c] = Random.nextInt(nColors)
        sfx(Sfx.HIT); buzz(30)
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1E2A4A.toInt(), 0xFF0F1426.toInt())
        c.line(0f, deadLine, w, deadLine, 1.5f * dp, Pal.alpha(Pal.RED, 0.5f))
        for ((k, v) in grid) c.ball(cx(k / cols, k % cols), cy(k / cols), r * 0.95f, colors[v])
        for (f in falling) c.ball(f.x, f.y, r * 0.95f, f.col)
        // aim guide with one wall bounce
        if (!flying && !over) {
            var x = w / 2; var y = shooterY; var dx = cos(aim); val dy = sin(aim)
            var i = 0
            while (i < 40 && y > top) {
                x += dx * 14 * dp; y += dy * 14 * dp
                if (x < r || x > w - r) dx = -dx
                if (i % 2 == 0) c.circle(x, y, 2.5f * dp, Pal.alpha(colors[cur], if (aiming) 0.8f else 0.35f))
                i++
            }
        }
        if (flying) c.ball(shotX, shotY, r * 0.95f, colors[cur]) else c.ball(w / 2, shooterY, r * 0.95f, colors[cur])
        c.ball(w / 2 + 3 * r, shooterY + r * 0.6f, r * 0.6f, colors[next])
        c.text("Tiếp", w / 2 + 3 * r, shooterY + r * 1.6f, 10 * dp, Pal.MUTED)
        for (i in 0 until 6) c.circle(20 * dp + i * 12 * dp, h - 20 * dp, 4 * dp, if (i < 6 - misses) Pal.MUTED else Pal.RED)
        fx.draw(c)
    }
}
