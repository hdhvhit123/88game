package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.DotsBoxes
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.min

class DotsAndBoxesGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Khó"), 1),
        GameOption("size", "Kích thước", listOf("4 × 4", "5 × 5", "6 × 6"), 1),
    )
    override val help = "Lần lượt nối hai chấm cạnh nhau. Ai vẽ cạnh thứ tư của một ô vuông sẽ chiếm ô đó và được đi tiếp. " +
        "Hết đường nối, ai chiếm nhiều ô hơn sẽ thắng."

    private var g = DotsBoxes(5, 5)
    private var turn = 1
    private var thinking = false
    private var gap = 0f; private var x0 = 0f; private var y0 = 0f
    private var lastLine = -1
    private val lineOwner = HashMap<Int, Int>()

    override fun newGame() {
        val n = intArrayOf(4, 5, 6)[opt("size")]
        g = DotsBoxes(n, n); turn = 1; thinking = false; lastLine = -1; lineOwner.clear()
        status()
    }

    override fun layout() {
        gap = min(w - 40 * dp, h - 140 * dp) / g.cols
        x0 = (w - gap * g.cols) / 2; y0 = 80 * dp + (h - 120 * dp - gap * g.rows) / 2
    }

    private fun status() {
        val vsAi = opt("mode") == 0
        info("${if (vsAi) "Bạn" else "Xanh"} ${g.score(1)} – ${g.score(2)} ${if (vsAi) "Máy" else "Hồng"}")
    }

    private fun lineAt(x: Float, y: Float): Int {
        var best = -1; var bestD = gap * 0.35f
        for (l in g.free()) {
            val (ax, ay, bx, by) = ends(l)
            val mx = (ax + bx) / 2; val my = (ay + by) / 2
            val d = if (ay == by) abs(y - ay) + maxOf(0f, abs(x - mx) - gap / 2) else abs(x - ax) + maxOf(0f, abs(y - my) - gap / 2)
            if (d < bestD) { bestD = d; best = l }
        }
        return best
    }

    private fun ends(l: Int): List<Float> = if (l < g.hCount) {
        val r = l / g.cols; val c = l % g.cols
        listOf(x0 + c * gap, y0 + r * gap, x0 + (c + 1) * gap, y0 + r * gap)
    } else {
        val k = l - g.hCount; val r = k / (g.cols + 1); val c = k % (g.cols + 1)
        listOf(x0 + c * gap, y0 + r * gap, x0 + c * gap, y0 + (r + 1) * gap)
    }

    override fun up(x: Float, y: Float) {
        if (over || thinking) return
        if (opt("mode") == 0 && turn != 1) return
        val l = lineAt(x, y)
        if (l >= 0) play(l)
    }

    private fun play(l: Int) {
        val made = g.play(l, turn)
        lineOwner[l] = turn; lastLine = l
        sfx(if (made > 0) Sfx.COIN else Sfx.TAP)
        if (made == 0) turn = 3 - turn
        status()
        if (g.done()) {
            val a = g.score(1); val b = g.score(2)
            val vsAi = opt("mode") == 0
            gameOver(a > b || (!vsAi && a != b), when { a == b -> "Hoà!"; !vsAi -> if (a > b) "Xanh thắng!" else "Hồng thắng!"; a > b -> "Bạn thắng!"; else -> "Máy thắng!" }, "$a – $b")
            return
        }
        if (opt("mode") == 0 && turn == 2) {
            thinking = true
            val copy = g.copy(); val skill = opt("level")
            background({ Thread.sleep(320); copy.bestMove(skill) }) { m -> thinking = false; play(m) }
        }
    }

    private fun color(p: Int) = if (p == 1) Pal.CYAN else Pal.PINK

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.text(when { over -> ""; thinking -> "Máy đang nghĩ…"; else -> if (turn == 1) (if (opt("mode") == 0) "Lượt của bạn" else "Lượt Xanh") else "Lượt Hồng" },
            w / 2, 36 * dp, 18 * dp, if (turn == 1) Pal.CYAN else Pal.PINK)
        for (b in g.owner.indices) {
            val o = g.owner[b]
            if (o == 0) continue
            val r = b / g.cols; val col = b % g.cols
            val x = x0 + col * gap; val y = y0 + r * gap
            c.rrect(x + 4 * dp, y + 4 * dp, x + gap - 4 * dp, y + gap - 4 * dp, 8 * dp, Pal.alpha(color(o), 0.3f))
            c.text(if (o == 1) "★" else "♥", x + gap / 2, y + gap / 2, gap * 0.4f, color(o))
        }
        for (l in g.lines.indices) {
            val (ax, ay, bx, by) = ends(l)
            if (g.lines[l]) c.line(ax, ay, bx, by, 5 * dp, if (l == lastLine) Pal.WHITE else color(lineOwner[l] ?: 1))
            else c.line(ax, ay, bx, by, 1.5f * dp, Pal.alpha(Pal.LINE, 0.6f))
        }
        for (r in 0..g.rows) for (col in 0..g.cols) c.circle(x0 + col * gap, y0 + r * gap, 6 * dp, Pal.TEXT)
    }
}
