package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.Mancala
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class MancalaGame : Game() {
    override val bestMode = BestMode.HIGH
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Khó"), 1),
    )
    override val help = "Ô ăn quan kiểu Kalah: chọn một hố của bạn (hàng dưới) để rải hạt ngược chiều kim đồng hồ, mỗi hố một hạt, " +
        "kể cả kho của bạn (bên phải). Hạt cuối rơi vào kho thì được đi tiếp. Hạt cuối rơi vào hố trống của mình thì ăn cả hạt ở hố đối diện. " +
        "Hết hạt một bên thì ván kết thúc, ai nhiều hạt trong kho hơn sẽ thắng."

    private var m = Mancala()
    private var thinking = false
    private var animating = false
    private var shown = IntArray(14)
    private val px = FloatArray(14); private val py = FloatArray(14)
    private var pitR = 0f
    private var lastPit = -1

    override fun newGame() {
        m = Mancala(); thinking = false; animating = false; lastPit = -1
        shown = m.pits.copyOf()
        status()
    }

    override fun layout() {
        val boardW = w - 24 * dp
        val pitW = boardW / 8
        pitR = min(pitW * 0.42f, h * 0.09f)
        val cy = h * 0.5f
        for (i in 0..5) {
            px[i] = 12 * dp + pitW * (i + 1.5f); py[i] = cy + pitR * 1.25f
            px[12 - i] = px[i]; py[12 - i] = cy - pitR * 1.25f
        }
        px[6] = 12 * dp + pitW * 7.5f; py[6] = cy
        px[13] = 12 * dp + pitW * 0.5f; py[13] = cy
    }

    private fun status() {
        val vsAi = opt("mode") == 0
        info("${if (vsAi) "Bạn" else "Dưới"} ${m.pits[6]} – ${m.pits[13]} ${if (vsAi) "Máy" else "Trên"}")
    }

    override fun up(x: Float, y: Float) {
        if (over || thinking || animating) return
        if (opt("mode") == 0 && m.turn != 0) return
        for (i in m.legal()) {
            val dx = x - px[i]; val dy = y - py[i]
            if (dx * dx + dy * dy < pitR * pitR * 1.3f) { sow(i); return }
        }
    }

    private fun sow(pit: Int) {
        val before = m.pits.copyOf()
        val path = m.play(pit)
        val after = m.pits.copyOf()
        animating = true
        shown = before.copyOf()
        shown[pit] = 0
        lastPit = pit
        path.forEachIndexed { k, i ->
            after(0.12f * (k + 1)) { shown[i]++; lastPit = i; sfx(Sfx.TICK) }
        }
        after(0.12f * (path.size + 1) + 0.15f) {
            if (!shown.contentEquals(after)) sfx(Sfx.COIN)
            shown = after.copyOf()
            animating = false
            status()
            onTurnDone()
        }
    }

    private fun onTurnDone() {
        if (m.gameEnded()) {
            val a = m.pits[6]; val b = m.pits[13]
            val vsAi = opt("mode") == 0
            gameOver(a > b || (!vsAi && a != b), when { a == b -> "Hoà!"; !vsAi -> if (a > b) "Bên dưới thắng!" else "Bên trên thắng!"; a > b -> "Bạn thắng!"; else -> "Máy thắng!" },
                "$a – $b", if (vsAi) a else null)
            return
        }
        if (opt("mode") == 0 && m.turn == 1) {
            thinking = true
            val copy = m.copy()
            val depth = intArrayOf(1, 5, 9)[opt("level")]
            background({ Thread.sleep(300); copy.bestMove(depth) }) { p -> thinking = false; sow(p) }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.text(when { over -> ""; thinking -> "Máy đang nghĩ…"; animating -> ""; m.turn == 0 -> "Lượt bên dưới"; else -> "Lượt bên trên" },
            w / 2, h * 0.18f, 18 * dp, Pal.TEXT)
        c.rrect(8 * dp, py[12] - pitR * 1.4f, w - 8 * dp, py[0] + pitR * 1.4f, pitR, 0xFF8B5A2B.toInt())
        c.rrect(12 * dp, py[12] - pitR * 1.25f, w - 12 * dp, py[0] + pitR * 1.25f, pitR * 0.9f, 0xFFA8733F.toInt())
        val legal = if (!over && !thinking && !animating && (opt("mode") == 1 || m.turn == 0)) m.legal().toSet() else emptySet()
        for (i in 0 until 14) {
            val store = i == 6 || i == 13
            val rw = if (store) pitR * 0.85f else pitR
            val rh = if (store) pitR * 2.3f else pitR
            Gfx.rect.set(px[i] - rw, py[i] - rh, px[i] + rw, py[i] + rh)
            c.drawOval(Gfx.rect, Gfx.fillPaint(if (i in legal) 0xFF6B3F1A.toInt() else 0xFF5A3415.toInt()))
            if (i == lastPit && animating) c.drawOval(Gfx.rect, Gfx.stroke.apply { strokeWidth = 2 * dp; color = Pal.YELLOW; shader = null })
            val n = shown[i]
            // seeds in a little spiral
            for (k in 0 until min(n, 24)) {
                val a = k * 2.4f; val rr = (if (store) rw else rw) * 0.18f * kotlin.math.sqrt(k.toFloat())
                val sx = px[i] + cos(a) * min(rr, rw * 0.7f); val sy = py[i] + sin(a) * min(rr * (if (store) 2.2f else 1f), rh * 0.7f)
                c.ball(sx, sy, pitR * 0.13f, Pal.VIVID[k % 8])
            }
            val ty = if (store) py[i] else if (i < 6) py[i] + pitR * 1.0f + 10 * dp else py[i] - pitR * 1.0f - 10 * dp
            if (store) c.text(n.toString(), px[i], py[i] - rh - 14 * dp, 16 * dp, Pal.TEXT)
            else c.text(n.toString(), px[i], ty + (if (i < 6) 14 * dp else -14 * dp), 14 * dp, Pal.TEXT)
        }
    }
}
