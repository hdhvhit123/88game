package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.poly
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.hypot
import kotlin.math.min

class MiniGolfGame : Game() {
    override val realtime = true
    override val bestMode = com.hgame.engine.BestMode.LOW
    override val help = "Kéo ngược từ quả bóng (giống ná cao su) để chọn hướng và lực, thả tay để đánh. Đưa bóng vào lỗ với ít gậy nhất. " +
        "9 lỗ, mỗi lỗ có số gậy chuẩn (par). Vùng cát làm bóng chậm lại, nước sẽ đưa bóng về chỗ cũ và phạt 1 gậy."

    // Course coordinates are in a 10 x 16 unit field. Walls: x1,y1,x2,y2 rectangles. Sand/water rects. Moving bar optional.
    private class Hole(val par: Int, val start: Pair<Float, Float>, val cup: Pair<Float, Float>, val walls: List<FloatArray>, val sand: List<FloatArray> = emptyList(), val water: List<FloatArray> = emptyList(), val mover: FloatArray? = null)
    private val holes = listOf(
        Hole(2, 5f to 14f, 5f to 2.5f, listOf()),
        Hole(2, 2f to 14f, 8f to 2.5f, listOf(floatArrayOf(0f, 7f, 6.5f, 7.6f))),
        Hole(3, 5f to 14.5f, 5f to 2f, listOf(floatArrayOf(2.5f, 10f, 10f, 10.6f), floatArrayOf(0f, 5.5f, 7.5f, 6.1f))),
        Hole(3, 1.5f to 14f, 8.5f to 2f, listOf(floatArrayOf(4.5f, 4f, 5.5f, 16f)), sand = listOf(floatArrayOf(6f, 6f, 10f, 9f))),
        Hole(3, 5f to 14.5f, 5f to 2f, listOf(floatArrayOf(3.5f, 7.5f, 6.5f, 8.5f)), water = listOf(floatArrayOf(0f, 4f, 3f, 6f), floatArrayOf(7f, 4f, 10f, 6f))),
        Hole(3, 5f to 14.5f, 5f to 2f, listOf(), mover = floatArrayOf(8f, 1.5f)),
        Hole(4, 1.5f to 14.5f, 1.5f to 2f, listOf(floatArrayOf(0f, 11f, 7f, 11.6f), floatArrayOf(3f, 6.5f, 10f, 7.1f), floatArrayOf(0f, 2.8f, 6.5f, 3.4f))),
        Hole(3, 5f to 14.5f, 5f to 3f, listOf(floatArrayOf(3f, 4f, 3.6f, 9f), floatArrayOf(6.4f, 4f, 7f, 9f), floatArrayOf(3f, 4f, 4.2f, 4.6f), floatArrayOf(5.8f, 4f, 7f, 4.6f)), sand = listOf(floatArrayOf(0f, 10f, 10f, 11.5f))),
        Hole(4, 8.5f to 14.5f, 1.5f to 1.5f, listOf(floatArrayOf(6f, 9f, 6.6f, 16f), floatArrayOf(3.4f, 0f, 4f, 7f)), water = listOf(floatArrayOf(0f, 9f, 6f, 10.5f)), mover = floatArrayOf(10f, 1.3f)),
    )

    private var hole = 0; private var strokes = 0; private var total = 0; private var totalPar = 0
    private var bx = 0f; private var by = 0f; private var vx = 0f; private var vy = 0f; private var lastX = 0f; private var lastY = 0f
    private var aiming = false; private var ax = 0f; private var ay = 0f; private var sinking = 0f
    private var u = 0f; private var ox = 0f; private var oy = 0f; private var t = 0f
    private val card = ArrayList<Int>()
    private var msg = ""; private var msgT = 0f

    override fun newGame() { hole = 0; total = 0; totalPar = 0; card.clear(); loadHole(); score(0) }

    private fun loadHole() { val hl = holes[hole]; bx = hl.start.first; by = hl.start.second; vx = 0f; vy = 0f; strokes = 0; sinking = 0f; status() }

    private fun status() = info("Lỗ ${hole + 1}/9 · Par ${holes[hole].par} · Gậy $strokes · Tổng ${if (total - totalPar >= 0) "+" else ""}${total - totalPar}")

    override fun layout() { u = min((w - 16 * dp) / 10f, (h - 16 * dp) / 16f); ox = (w - u * 10) / 2; oy = (h - u * 16) / 2 }

    private fun moving() = hypot(vx, vy) > 0.05f

    override fun down(x: Float, y: Float) { if (!moving() && sinking == 0f && !over) { aiming = true; ax = x; ay = y } }
    override fun move(x: Float, y: Float) { ax = x; ay = y }
    override fun up(x: Float, y: Float) {
        if (!aiming) return
        aiming = false
        val dx = (ox + bx * u - x) / u; val dy = (oy + by * u - y) / u
        val d = hypot(dx, dy); if (d < 0.3f) return
        val power = min(d, 5f) * 4.2f
        vx = dx / d * power; vy = dy / d * power
        lastX = bx; lastY = by; strokes++; status(); sfx(Sfx.TAP)
    }

    private fun inRect(x: Float, y: Float, r: FloatArray, pad: Float = 0f) = x > r[0] - pad && x < r[2] + pad && y > r[1] - pad && y < r[3] + pad

    private fun moverRect(): FloatArray? {
        val m = holes[hole].mover ?: return null
        val cx = 5f + kotlin.math.sin(t * m[1]) * 3.2f
        return floatArrayOf(cx - 1.5f, m[0] - 0.3f, cx + 1.5f, m[0] + 0.3f)
    }

    override fun update(dt: Float) {
        t += dt; if (msgT > 0) msgT -= dt
        val hl = holes[hole]
        if (sinking > 0f) {
            sinking += dt
            if (sinking > 0.8f) finishHole()
            return
        }
        if (!moving()) { vx = 0f; vy = 0f; return }
        val r = 0.22f
        val sub = 6; val sdt = dt / sub
        repeat(sub) {
            var nx = bx + vx * sdt; var ny = by + vy * sdt
            // outer bounds
            if (nx < r) { nx = r; vx = -vx * 0.8f }; if (nx > 10 - r) { nx = 10 - r; vx = -vx * 0.8f }
            if (ny < r) { ny = r; vy = -vy * 0.8f }; if (ny > 16 - r) { ny = 16 - r; vy = -vy * 0.8f }
            val walls = hl.walls + listOfNotNull(moverRect())
            for (wl in walls) {
                if (inRect(nx, ny, wl, r)) {
                    // decide which axis we came from
                    if (!inRect(bx, ny, wl, r)) { vx = -vx * 0.8f; nx = bx }
                    else if (!inRect(nx, by, wl, r)) { vy = -vy * 0.8f; ny = by }
                    else { vx = -vx * 0.8f; vy = -vy * 0.8f; nx = bx; ny = by }
                    sfx(Sfx.TICK)
                }
            }
            bx = nx; by = ny
        }
        val sand = hl.sand.any { inRect(bx, by, it) }
        val fr = if (sand) 0.12f else 0.55f
        val sp = hypot(vx, vy)
        val ns = (sp - (if (sand) 6f else 1.4f) * dt).coerceAtLeast(0f) * (1 - fr * dt)
        if (sp > 0) { vx *= ns / sp; vy *= ns / sp }
        if (hl.water.any { inRect(bx, by, it) }) {
            strokes++; bx = lastX; by = lastY; vx = 0f; vy = 0f; msg = "Xuống nước! +1 gậy phạt"; msgT = 1.5f; sfx(Sfx.ERROR); status()
        }
        val dc = hypot(bx - hl.cup.first, by - hl.cup.second)
        if (dc < 0.35f && hypot(vx, vy) < 9f) { sinking = 0.01f; bx = hl.cup.first; by = hl.cup.second; vx = 0f; vy = 0f; sfx(Sfx.COIN) }
        else if (dc < 0.6f) { vx += (hl.cup.first - bx) * 2f * dt; vy += (hl.cup.second - by) * 2f * dt }
    }

    private fun finishHole() {
        val par = holes[hole].par
        total += strokes; totalPar += par; card.add(strokes)
        val diff = strokes - par
        msg = when { strokes == 1 -> "HOLE IN ONE!"; diff <= -2 -> "Eagle!"; diff == -1 -> "Birdie!"; diff == 0 -> "Par"; diff == 1 -> "Bogey"; else -> "+$diff" }
        msgT = 1.6f; sfx(if (diff <= 0) Sfx.WIN else Sfx.CLICK)
        score(total)
        if (hole == holes.size - 1) { after(1f) { gameOver(total <= totalPar, "Hoàn thành 9 lỗ", "$total gậy (par $totalPar)", total) }; sinking = 0f; return }
        hole++; loadHole()
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1B5E20.toInt(), 0xFF0B3D10.toInt())
        val hl = holes[hole]
        fun X(x: Float) = ox + x * u
        fun Y(y: Float) = oy + y * u
        c.rrect(X(0f) - 6 * dp, Y(0f) - 6 * dp, X(10f) + 6 * dp, Y(16f) + 6 * dp, 10 * dp, 0xFF795548.toInt())
        c.rect(X(0f), Y(0f), X(10f), Y(16f), 0xFF66BB6A.toInt())
        for (i in 0 until 16) if (i % 2 == 0) c.rect(X(0f), Y(i.toFloat()), X(10f), Y(i + 1f), 0xFF5CB860.toInt())
        for (s in hl.sand) c.rrect(X(s[0]), Y(s[1]), X(s[2]), Y(s[3]), u * 0.5f, 0xFFF0D9A0.toInt())
        for (s in hl.water) { c.rrect(X(s[0]), Y(s[1]), X(s[2]), Y(s[3]), u * 0.4f, 0xFF29B6F6.toInt()); c.line(X(s[0]) + u * 0.4f, Y((s[1] + s[3]) / 2), X(s[2]) - u * 0.4f, Y((s[1] + s[3]) / 2), 1.5f * dp, Pal.alpha(Pal.WHITE, 0.5f)) }
        for (wl in hl.walls + listOfNotNull(moverRect())) { c.rect(X(wl[0]), Y(wl[1]) + 3 * dp, X(wl[2]), Y(wl[3]) + 3 * dp, 0xFF4E342E.toInt()); c.rect(X(wl[0]), Y(wl[1]), X(wl[2]), Y(wl[3]), 0xFF8D6E63.toInt()) }
        // cup + flag
        val cx = X(hl.cup.first); val cy = Y(hl.cup.second)
        c.circle(cx, cy, u * 0.36f, 0xFF1B1B1B.toInt())
        c.line(cx, cy, cx, cy - u * 1.6f, 2 * dp, Pal.WHITE)
        c.poly(Pal.RED, cx, cy - u * 1.6f, cx + u * 0.9f, cy - u * 1.35f, cx, cy - u * 1.1f)
        c.text("${hole + 1}", cx + u * 0.35f, cy - u * 1.35f, u * 0.3f, Pal.WHITE)
        // ball and aim
        val bsx = X(bx); val bsy = Y(by)
        if (aiming) {
            val dx = bsx - ax; val dy = bsy - ay; val d = hypot(dx, dy).coerceAtMost(5f * u)
            val k = d / hypot(dx, dy).coerceAtLeast(1f)
            val pw = d / (5f * u)
            var i = 1
            while (i <= 8) { c.circle(bsx + dx * k * i / 8f, bsy + dy * k * i / 8f, 3 * dp, Pal.alpha(Pal.mix(Pal.GREEN, Pal.RED, pw), 1f - i / 10f)); i++ }
        }
        val s = if (sinking > 0f) (1 - sinking / 0.8f).coerceAtLeast(0f) else 1f
        c.circle(bsx + 2 * dp, bsy + 3 * dp, u * 0.22f * s, Pal.alpha(Pal.BLACK, 0.3f))
        c.ball(bsx, bsy, u * 0.22f * s, Pal.WHITE)
        if (msgT > 0f) {
            c.rrect(w * 0.15f, h * 0.45f - 24 * dp, w * 0.85f, h * 0.45f + 24 * dp, 14 * dp, Pal.alpha(Pal.BLACK, 0.5f))
            c.text(msg, w / 2, h * 0.45f, 20 * dp, Pal.YELLOW)
        }
        if (strokes == 0 && hole == 0 && !aiming) c.text("Kéo ngược từ bóng rồi thả để đánh", w / 2, Y(15.4f), 13 * dp, Pal.WHITE)
    }
}
