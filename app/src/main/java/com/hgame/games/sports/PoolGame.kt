package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

class PoolGame : Game() {
    override val realtime = true
    override val help = "Bi-a 8 bi đấu với máy. Kéo ngón tay ra xa bi trắng theo hướng ngược lại để ngắm, khoảng cách quyết định lực, thả tay để đánh. " +
        "Bi đầu tiên vào lỗ xác định nhóm của bạn (bi trơn 1–7 hoặc bi sọc 9–15). Vào bi nhóm mình được đánh tiếp. " +
        "Đánh hết nhóm rồi đưa bi 8 vào lỗ để thắng; bi 8 vào sớm là thua. Bi trắng rơi lỗ là phạm lỗi."

    private class B(val n: Int, var x: Float, var y: Float, var vx: Float = 0f, var vy: Float = 0f, var potted: Boolean = false)
    private val balls = ArrayList<B>()
    private val colors = intArrayOf(Pal.WHITE, 0xFFFDD835.toInt(), 0xFF1E88E5.toInt(), 0xFFE53935.toInt(), 0xFF8E24AA.toInt(), 0xFFFB8C00.toInt(), 0xFF43A047.toInt(), 0xFF6D4C41.toInt(), 0xFF111111.toInt())
    private var tl = 0f; private var tt = 0f; private var tr = 0f; private var tb = 0f; private var r = 0f
    private var turn = 0           // 0 you, 1 AI
    private var group = intArrayOf(0, 0) // 0 unassigned, 1 solids, 2 stripes
    private var aiming = false; private var ax = 0f; private var ay = 0f
    private var moving = false; private var pottedThisShot = ArrayList<Int>(); private var firstHit = 0
    private var msg = ""; private var msgT = 0f; private var aiThinking = false

    override fun newGame() {
        balls.clear(); turn = 0; group = intArrayOf(0, 0); moving = false; aiThinking = false
        msg = "Bạn phá bi"; msgT = 2f
        if (r > 0f) rack()
        score(0); status()
    }

    override fun layout() {
        val tw = w - 24 * dp; val th = minOf(h - 90 * dp, tw * 1.9f)
        tl = (w - tw) / 2; tr = tl + tw; tt = (h - th) / 2; tb = tt + th
        r = tw * 0.032f
        if (balls.isEmpty()) rack()
    }

    private fun rack() {
        balls.clear()
        balls.add(B(0, (tl + tr) / 2, tt + (tb - tt) * 0.75f))
        val order = intArrayOf(1, 9, 2, 10, 8, 3, 11, 4, 12, 5, 13, 6, 14, 7, 15)
        val shuffled = order.toMutableList().also { val eight = it.indexOf(8); it.removeAt(eight); it.shuffle(); it.add(4, 8) }
        var k = 0
        val apexY = tt + (tb - tt) * 0.28f
        for (row in 0 until 5) for (i in 0..row) {
            balls.add(B(shuffled[k++], (tl + tr) / 2 + (i - row / 2f) * r * 2.02f, apexY - row * r * 1.75f))
        }
    }

    private fun cue() = balls.first { it.n == 0 }
    private fun groupOf(n: Int) = when { n in 1..7 -> 1; n in 9..15 -> 2; else -> 0 }
    private fun remaining(g: Int) = balls.count { !it.potted && groupOf(it.n) == g }
    private fun groupName(g: Int) = when (g) { 1 -> "bi trơn"; 2 -> "bi sọc"; else -> "chưa chia" }

    private fun status() = info((if (turn == 0) "Lượt bạn" else "Lượt máy") + " · Bạn: ${groupName(group[0])}")

    private val pockets get() = listOf(tl to tt, tr to tt, tl to (tt + tb) / 2, tr to (tt + tb) / 2, tl to tb, tr to tb)

    override fun down(x: Float, y: Float) { if (!moving && turn == 0 && !over) { aiming = true; ax = x; ay = y } }
    override fun move(x: Float, y: Float) { ax = x; ay = y }
    override fun up(x: Float, y: Float) {
        if (!aiming) return
        aiming = false
        val c = cue(); val dx = c.x - x; val dy = c.y - y; val d = hypot(dx, dy)
        if (d < r * 1.5f) return
        val power = (d / ((tr - tl) * 0.6f)).coerceIn(0.05f, 1f)
        shoot(atan2(dy, dx), power)
    }

    private fun shoot(angle: Float, power: Float) {
        val c = cue(); val sp = power * (tb - tt) * 3.2f
        c.vx = cos(angle) * sp; c.vy = sin(angle) * sp
        moving = true; pottedThisShot.clear(); firstHit = 0
        sfx(Sfx.HIT)
    }

    override fun update(dt: Float) {
        if (msgT > 0) msgT -= dt
        if (!moving) return
        val sub = 8; val sdt = dt / sub
        repeat(sub) { physics(sdt) }
        if (balls.all { it.potted || hypot(it.vx, it.vy) < 2f }) { balls.forEach { it.vx = 0f; it.vy = 0f }; moving = false; endShot() }
    }

    private fun physics(dt: Float) {
        val live = balls.filter { !it.potted }
        for (b in live) {
            b.x += b.vx * dt; b.y += b.vy * dt
            val sp = hypot(b.vx, b.vy)
            if (sp > 0f) { val ns = (sp - (tb - tt) * 0.35f * dt).coerceAtLeast(0f); b.vx *= ns / sp; b.vy *= ns / sp }
            for ((px, py) in pockets) if (hypot(b.x - px, b.y - py) < r * 1.6f) {
                b.potted = true; b.vx = 0f; b.vy = 0f; pottedThisShot.add(b.n); sfx(Sfx.POP)
            }
            if (b.potted) continue
            if (b.x < tl + r) { b.x = tl + r; b.vx = -b.vx * 0.8f }
            if (b.x > tr - r) { b.x = tr - r; b.vx = -b.vx * 0.8f }
            if (b.y < tt + r) { b.y = tt + r; b.vy = -b.vy * 0.8f }
            if (b.y > tb - r) { b.y = tb - r; b.vy = -b.vy * 0.8f }
        }
        for (i in live.indices) for (j in i + 1 until live.size) {
            val a = live[i]; val b = live[j]
            if (a.potted || b.potted) continue
            val dx = b.x - a.x; val dy = b.y - a.y; val d = hypot(dx, dy)
            if (d < 2 * r && d > 0.001f) {
                val nx = dx / d; val ny = dy / d
                val ov = (2 * r - d) / 2; a.x -= nx * ov; a.y -= ny * ov; b.x += nx * ov; b.y += ny * ov
                val rv = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny
                if (rv > 0) {
                    a.vx -= rv * nx * 0.97f; a.vy -= rv * ny * 0.97f; b.vx += rv * nx * 0.97f; b.vy += rv * ny * 0.97f
                    if (firstHit == 0) { if (a.n == 0) firstHit = b.n else if (b.n == 0) firstHit = a.n }
                    if (rv > 40f) sfx(Sfx.TICK)
                }
            }
        }
    }

    private fun endShot() {
        val me = turn; val g = group[me]
        val scratch = 0 in pottedThisShot
        val eight = 8 in pottedThisShot
        if (eight) {
            val cleared = g != 0 && remaining(g) == 0
            val win = cleared && !scratch
            val youWin = (me == 0) == win
            sfx(if (youWin) Sfx.WIN else Sfx.LOSE)
            gameOver(youWin, if (youWin) "Bạn thắng!" else "Máy thắng!", if (win) "Bi 8 vào lỗ hợp lệ" else "Bi 8 vào lỗ khi chưa hợp lệ")
            return
        }
        // assign groups
        if (g == 0 && !scratch) {
            val first = pottedThisShot.firstOrNull { it != 0 }
            if (first != null) { group[me] = groupOf(first); group[1 - me] = 3 - group[me]; msg = (if (me == 0) "Bạn" else "Máy") + " chơi ${groupName(group[me])}"; msgT = 1.5f }
        }
        val gNow = group[me]
        val foulWrongFirst = gNow != 0 && firstHit != 0 && groupOf(firstHit) != gNow && !(remaining(gNow) == 0 && firstHit == 8)
        val pottedOwn = pottedThisShot.any { it != 0 && (gNow == 0 || groupOf(it) == gNow) }
        if (scratch) {
            val c = cue(); c.potted = false; c.x = (tl + tr) / 2; c.y = tt + (tb - tt) * 0.75f
            while (balls.any { it !== c && !it.potted && hypot(it.x - c.x, it.y - c.y) < 2 * r }) c.y += r
            msg = "Bi trắng rơi lỗ – phạm lỗi!"; msgT = 1.5f; sfx(Sfx.ERROR)
        } else if (firstHit == 0) { msg = "Không chạm bi nào – phạm lỗi"; msgT = 1.5f }
        else if (foulWrongFirst) { msg = "Chạm bi đối thủ trước – phạm lỗi"; msgT = 1.5f }
        val keep = pottedOwn && !scratch && !foulWrongFirst && firstHit != 0
        if (!keep) turn = 1 - turn
        score(if (group[0] == 0) 0 else 7 - remaining(group[0]))
        status()
        if (turn == 1) aiShot()
    }

    private fun clearPath(x1: Float, y1: Float, x2: Float, y2: Float, ignore: Set<Int>): Boolean {
        val dx = x2 - x1; val dy = y2 - y1; val l2 = dx * dx + dy * dy
        for (b in balls) {
            if (b.potted || b.n in ignore) continue
            val t = (((b.x - x1) * dx + (b.y - y1) * dy) / l2).coerceIn(0f, 1f)
            if (hypot(b.x - (x1 + t * dx), b.y - (y1 + t * dy)) < 2 * r) return false
        }
        return true
    }

    private fun aiShot() {
        aiThinking = true
        after(0.9f) {
            aiThinking = false
            val c = cue(); val g = group[1]
            val targets = balls.filter { !it.potted && it.n != 0 && (if (g == 0) it.n != 8 else if (remaining(g) == 0) it.n == 8 else groupOf(it.n) == g) }
            var bestScore = -1f; var bestAngle = 0f; var bestPower = 0.6f
            for (t in targets) for ((px, py) in pockets) {
                val dpx = px - t.x; val dpy = py - t.y; val dl = hypot(dpx, dpy)
                val gx = t.x - dpx / dl * 2 * r; val gy = t.y - dpy / dl * 2 * r
                val cx = gx - c.x; val cy = gy - c.y; val cl = hypot(cx, cy)
                val cosA = (cx * dpx + cy * dpy) / (cl * dl)
                if (cosA < 0.25f) continue
                if (!clearPath(c.x, c.y, gx, gy, setOf(0, t.n)) || !clearPath(t.x, t.y, px, py, setOf(t.n))) continue
                val sc = cosA * 2 - (cl + dl) / (tb - tt)
                if (sc > bestScore) { bestScore = sc; bestAngle = atan2(cy, cx); bestPower = ((cl + dl) / (tb - tt) * 0.7f + 0.25f).coerceIn(0.3f, 0.95f) }
            }
            if (bestScore < 0f) {
                val t = targets.minByOrNull { hypot(it.x - c.x, it.y - c.y) } ?: balls.first { !it.potted && it.n != 0 }
                bestAngle = atan2(t.y - c.y, t.x - c.x); bestPower = 0.7f
            }
            shoot(bestAngle + (Random.nextFloat() - 0.5f) * 0.035f, bestPower)
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF2E1A12.toInt(), 0xFF1A0F0A.toInt())
        c.rrect(tl - 14 * dp, tt - 14 * dp, tr + 14 * dp, tb + 14 * dp, 16 * dp, 0xFF5D4037.toInt())
        c.rect(tl, tt, tr, tb, 0xFF1B7A4A.toInt())
        c.line(tl, tt + (tb - tt) * 0.75f, tr, tt + (tb - tt) * 0.75f, 1f * dp, Pal.alpha(Pal.WHITE, 0.25f))
        for ((px, py) in pockets) c.circle(px, py, r * 1.6f, 0xFF0B0B0B.toInt())
        val cb = cue()
        if (aiming && !moving) {
            val dx = cb.x - ax; val dy = cb.y - ay; val d = hypot(dx, dy)
            if (d > r) {
                val nx = dx / d; val ny = dy / d
                // find first ball along the path for a ghost-ball preview
                var hitT = Float.MAX_VALUE; var hitB: B? = null
                for (b in balls) {
                    if (b.potted || b.n == 0) continue
                    val bx = b.x - cb.x; val by = b.y - cb.y
                    val proj = bx * nx + by * ny; if (proj <= 0) continue
                    val perp2 = bx * bx + by * by - proj * proj
                    if (perp2 < 4 * r * r) { val tt2 = proj - kotlin.math.sqrt(4 * r * r - perp2); if (tt2 < hitT) { hitT = tt2; hitB = b } }
                }
                val len = if (hitB != null) hitT else (tb - tt)
                c.line(cb.x, cb.y, cb.x + nx * len, cb.y + ny * len, 1.5f * dp, Pal.alpha(Pal.WHITE, 0.7f))
                hitB?.let { b ->
                    val gx = cb.x + nx * hitT; val gy = cb.y + ny * hitT
                    c.circle(gx, gy, r, Pal.alpha(Pal.WHITE, 0.3f))
                    val ox = b.x - gx; val oy = b.y - gy; val ol = hypot(ox, oy)
                    c.line(b.x, b.y, b.x + ox / ol * r * 5, b.y + oy / ol * r * 5, 1.5f * dp, Pal.alpha(Pal.YELLOW, 0.8f))
                }
                // cue stick
                val pw = (d / ((tr - tl) * 0.6f)).coerceIn(0.05f, 1f)
                val back = r * 1.5f + pw * r * 4
                c.line(cb.x - nx * back, cb.y - ny * back, cb.x - nx * (back + (tb - tt) * 0.45f), cb.y - ny * (back + (tb - tt) * 0.45f), r * 0.5f, 0xFFD7A86E.toInt())
                c.rrect(tl, tb + 18 * dp, tl + (tr - tl) * pw, tb + 26 * dp, 4 * dp, Pal.mix(Pal.GREEN, Pal.RED, pw))
            }
        }
        for (b in balls) {
            if (b.potted) continue
            val col = colors[if (b.n > 8) b.n - 8 else b.n]
            c.circle(b.x + r * 0.15f, b.y + r * 0.2f, r, Pal.alpha(Pal.BLACK, 0.3f))
            if (b.n > 8) { c.ball(b.x, b.y, r, Pal.WHITE); c.save(); c.clipRect(b.x - r, b.y - r * 0.55f, b.x + r, b.y + r * 0.55f); c.ball(b.x, b.y, r, col); c.restore() }
            else c.ball(b.x, b.y, r, col)
            if (b.n != 0) { c.circle(b.x, b.y, r * 0.45f, Pal.WHITE); c.text("${b.n}", b.x, b.y, r * 0.6f, 0xFF111111.toInt()) }
        }
        // potted trays
        val mine = if (group[0] == 0) emptyList() else balls.filter { it.potted && groupOf(it.n) == group[0] }
        val theirs = if (group[1] == 0) emptyList() else balls.filter { it.potted && groupOf(it.n) == group[1] }
        mine.forEachIndexed { i, b -> c.circle(tl + r + i * r * 2.2f, tt - 26 * dp, r * 0.8f, colors[if (b.n > 8) b.n - 8 else b.n]) }
        theirs.forEachIndexed { i, b -> c.circle(tr - r - i * r * 2.2f, tt - 26 * dp, r * 0.8f, colors[if (b.n > 8) b.n - 8 else b.n]) }
        c.text("Bạn", tl, tt - 40 * dp, 10 * dp, Pal.CYAN, android.graphics.Paint.Align.LEFT)
        c.text("Máy", tr, tt - 40 * dp, 10 * dp, Pal.ORANGE, android.graphics.Paint.Align.RIGHT)
        if (msgT > 0f) { c.rrect(w * 0.1f, h / 2 - 22 * dp, w * 0.9f, h / 2 + 22 * dp, 12 * dp, Pal.alpha(Pal.BLACK, 0.55f)); c.text(msg, w / 2, h / 2, 16 * dp, Pal.YELLOW) }
        if (aiThinking) c.text("Máy đang ngắm…", w / 2, tb + 28 * dp, 14 * dp, Pal.ORANGE)
    }
}
