package com.hgame.games.card

import android.graphics.Canvas
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlin.random.Random

class PlinkoGame : Game() {
    override val realtime = true
    override val options = listOf(GameOption("risk", "Mức rủi ro", listOf("Thấp", "Trung bình", "Cao"), 1))
    override val help = "Chọn mức cược rồi chạm để thả bóng từ đỉnh. Bóng nảy qua các chốt và rơi vào ô hệ số ở đáy, tiền thắng = cược × hệ số. " +
        "Có thể thả nhiều bóng cùng lúc. Đây là trò chơi giải trí với xu ảo."

    private val rows = 12
    private val multis = arrayOf(
        floatArrayOf(5f, 2f, 1.4f, 1.1f, 1f, 0.6f, 0.5f, 0.6f, 1f, 1.1f, 1.4f, 2f, 5f),
        floatArrayOf(15f, 5f, 2f, 1.4f, 0.8f, 0.5f, 0.3f, 0.5f, 0.8f, 1.4f, 2f, 5f, 15f),
        floatArrayOf(80f, 15f, 4f, 1.5f, 0.5f, 0.2f, 0.2f, 0.2f, 0.5f, 1.5f, 4f, 15f, 80f),
    )
    private class B(var x: Float, var y: Float, var vx: Float, var vy: Float, val bet: Int)
    private val balls = ArrayList<B>()
    private val flash = FloatArray(13)
    private var coins = 0; private var bet = 10
    private var gap = 0f; private var top = 0f; private var pegR = 0f; private var ballR = 0f; private var binY = 0f
    private val minus = Btn("−", Pal.SURFACE2); private val plus = Btn("+", Pal.SURFACE2); private val drop = Btn("Thả bóng", Pal.GREEN)
    private var lastWin = ""

    override fun newGame() {
        coins = host.loadInt("coins", 1000); if (coins < 10) coins = 1000
        balls.clear(); flash.fill(0f); lastWin = ""; score(coins); info("Xu: $coins")
    }

    override fun layout() {
        gap = minOf(w / (rows + 2.5f), (h - 190 * dp) / (rows + 1.5f)); top = 30 * dp
        pegR = gap * 0.1f; ballR = gap * 0.22f; binY = top + rows * gap + gap * 0.3f
        val by = h - 40 * dp
        minus.setCenter(w * 0.12f, by, 46 * dp, 46 * dp); plus.setCenter(w * 0.44f, by, 46 * dp, 46 * dp)
        drop.setCenter(w * 0.76f, by, w * 0.4f, 50 * dp)
    }

    private fun pegX(r: Int, i: Int) = w / 2 + (i - r / 2f) * gap
    private fun pegY(r: Int) = top + r * gap + gap

    override fun up(x: Float, y: Float) {
        if (minus.hit(x, y)) { bet = (bet / 2).coerceAtLeast(1); sfx(Sfx.CLICK); return }
        if (plus.hit(x, y)) { bet = (bet * 2).coerceAtMost(maxOf(1, coins)); sfx(Sfx.CLICK); return }
        if (drop.hit(x, y) || y < binY) {
            if (coins < bet) { sfx(Sfx.ERROR); if (coins < 1 && balls.isEmpty()) { coins = 1000; host.saveInt("coins", coins); info("Được cấp lại 1000 xu") }; return }
            coins -= bet; host.saveInt("coins", coins); score(coins)
            balls.add(B(w / 2 + (Random.nextFloat() - 0.5f) * gap * 0.3f, top, 0f, 0f, bet)); sfx(Sfx.TAP)
        }
    }

    override fun update(dt: Float) {
        for (i in flash.indices) if (flash[i] > 0) flash[i] -= dt * 2
        val g = gap * 22f
        val sub = 4; val sdt = dt / sub
        val it = balls.iterator()
        while (it.hasNext()) {
            val b = it.next()
            var landed = false
            repeat(sub) {
                if (landed) return@repeat
                b.vy += g * sdt; b.x += b.vx * sdt; b.y += b.vy * sdt
                val r = ((b.y - top - gap) / gap + 0.5f).toInt()
                for (rr in (r - 1)..(r + 1)) {
                    if (rr < 0 || rr >= rows) continue
                    for (i in 0..rr + 2) {
                        val px = pegX(rr + 2, i) ; val py = pegY(rr)
                        val dx = b.x - px; val dy = b.y - py; val d = hypot(dx, dy)
                        if (d < pegR + ballR && d > 0.01f) {
                            val nx = dx / d; val ny = dy / d
                            b.x = px + nx * (pegR + ballR); b.y = py + ny * (pegR + ballR)
                            val vn = b.vx * nx + b.vy * ny
                            if (vn < 0) { b.vx -= 1.5f * vn * nx; b.vy -= 1.5f * vn * ny }
                            b.vx += (Random.nextFloat() - 0.5f) * gap * 0.6f
                            b.vx *= 0.75f; b.vy *= 0.6f
                            if (Random.nextFloat() < 0.3f) sfx(Sfx.TICK)
                        }
                    }
                }
                val maxV = sqrt(2 * g * gap)
                if (b.vy > maxV) b.vy = maxV
                if (b.y > binY + gap * 0.3f) landed = true
            }
            if (landed) {
                val left = pegX(rows + 1, 0)
                val bin = ((b.x - left) / gap).toInt().coerceIn(0, 12)
                val m = multis[opt("risk")][bin]
                val win = (b.bet * m).toInt()
                coins += win; host.saveInt("coins", coins); score(coins); flash[bin] = 1f
                lastWin = "×${fmt(m)} → +$win"
                sfx(if (m >= 2f) Sfx.COIN else Sfx.POP)
                info("Xu: $coins")
                it.remove()
            }
        }
    }

    private fun fmt(m: Float) = if (m == m.toInt().toFloat()) "${m.toInt()}" else "$m"

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1A1446.toInt(), 0xFF0A0820.toInt())
        for (r in 0 until rows) for (i in 0..r + 2) c.circle(pegX(r + 2, i), pegY(r), pegR, Pal.alpha(Pal.WHITE, 0.85f))
        val m = multis[opt("risk")]
        val left = pegX(rows + 1, 0)
        for (i in 0 until 13) {
            val x = left + i * gap
            val k = kotlin.math.abs(i - 6) / 6f
            val col = Pal.mix(Pal.YELLOW, Pal.RED, k)
            val lift = flash[i].coerceAtLeast(0f) * 6 * dp
            c.rrect(x + 1.5f, binY - lift, x + gap - 1.5f, binY + gap * 0.8f - lift, gap * 0.15f, col)
            c.text(fmt(m[i]), x + gap / 2, binY + gap * 0.4f - lift, gap * 0.3f, 0xFF3E2723.toInt())
        }
        for (b in balls) c.ball(b.x, b.y, ballR, Pal.PINK)
        c.text("💰 $coins", 16 * dp, 16 * dp, 15 * dp, Pal.YELLOW, android.graphics.Paint.Align.LEFT)
        if (lastWin.isNotEmpty()) c.text(lastWin, w - 16 * dp, 16 * dp, 14 * dp, Pal.GREEN, android.graphics.Paint.Align.RIGHT)
        minus.draw(c, 24 * dp); plus.draw(c, 24 * dp); drop.draw(c, 16 * dp)
        c.text("Cược $bet", (minus.cx + plus.cx) / 2, minus.cy, 15 * dp, Pal.WHITE)
    }
}
