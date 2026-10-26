package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.ConnectFour
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text

class ConnectFourGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Đấu với máy", "2 người")),
        GameOption("level", "Độ khó", listOf("Dễ", "Thường", "Khó"), 1),
    )
    override val help = "Thả đĩa vào các cột. Ai xếp được 4 đĩa liền nhau theo hàng ngang, dọc hoặc chéo trước sẽ thắng."

    private var g = ConnectFour()
    private val fit = BoardFit()
    private var turn = 1
    private var thinking = false
    private var hover = -1
    private var dropCol = -1; private var dropRow = -1; private var dropY = 0f; private var dropV = 0f
    private var win: IntArray? = null
    private var aiFirst = true
    private var score1 = 0; private var score2 = 0

    override fun newGame() {
        g = ConnectFour(); turn = 1; thinking = false; win = null; dropCol = -1
        aiFirst = !aiFirst
        status()
        if (opt("mode") == 0 && aiFirst) aiMove()
    }

    override fun layout() {
        val pad = 14 * dp
        fit.fit(7, 6, pad, 90 * dp, w - pad, h - pad)
    }

    private fun status() = info(if (opt("mode") == 0) "Bạn $score1 – $score2 Máy" else "Đỏ $score1 – $score2 Vàng")

    private fun humanTurn() = opt("mode") == 1 || (turn == 1) != aiFirst

    override fun down(x: Float, y: Float) { hover = fit.colAt(x) }
    override fun move(x: Float, y: Float) { hover = fit.colAt(x) }

    override fun up(x: Float, y: Float) {
        val col = fit.colAt(x)
        hover = -1
        if (over || thinking || dropCol >= 0 || !humanTurn() || col < 0 || !g.canDrop(col)) { redraw(); return }
        play(col)
    }

    private fun play(col: Int) {
        val row = g.drop(col, turn)
        dropCol = col; dropRow = row; dropY = -1f; dropV = 0f
        animate(2f)
        sfx(Sfx.TAP)
    }

    private fun landed() {
        val who = turn
        dropCol = -1
        sfx(Sfx.HIT)
        win = g.winLine()
        if (win != null) {
            val vsAi = opt("mode") == 0
            if (who == 1) score1++ else score2++
            val humanWon = !vsAi || (who == 1) != aiFirst
            val title = if (!vsAi) (if (who == 1) "Đỏ thắng!" else "Vàng thắng!") else if (humanWon) "Bạn thắng!" else "Máy thắng!"
            fixScores(who, vsAi)
            status()
            gameOver(humanWon, title, "4 đĩa thẳng hàng")
            return
        }
        if (g.full()) { gameOver(false, "Hoà!", "Bàn cờ đã đầy"); return }
        turn = 3 - turn
        if (opt("mode") == 0 && !humanTurn()) aiMove()
    }

    /** Scores are tracked per person (human vs AI), not per colour. */
    private fun fixScores(who: Int, vsAi: Boolean) {
        if (!vsAi) return
        // undo the colour-based increment and count it for human/AI instead
        if (who == 1) score1-- else score2--
        val humanColor = if (aiFirst) 2 else 1
        if (who == humanColor) score1++ else score2++
    }

    private fun aiMove() {
        thinking = true
        val copy = g.copy(); val me = turn
        val depth = intArrayOf(2, 5, 7)[opt("level")]
        background({
            Thread.sleep(250)
            if (opt("level") == 0 && Math.random() < 0.35) (0..6).filter { copy.canDrop(it) }.random() else copy.bestMove(me, depth)
        }) { col -> thinking = false; if (col >= 0) play(col) }
    }

    override fun update(dt: Float) {
        if (dropCol < 0) return
        animate(0.2f)
        dropV += dt * 40f
        dropY += dropV * dt
        if (dropY >= dropRow) {
            dropY = dropRow.toFloat()
            if (dropV > 6f) dropV = -dropV * 0.3f else landed()
        }
    }

    private fun color(p: Int) = if (p == 1) Pal.RED else Pal.YELLOW

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        val label = when {
            over -> ""
            thinking -> "Máy đang nghĩ…"
            opt("mode") == 0 -> "Lượt của bạn"
            else -> if (turn == 1) "Lượt Đỏ" else "Lượt Vàng"
        }
        c.text(label, w / 2, 30 * dp, 18 * dp, Pal.TEXT)
        if (!over && !thinking && dropCol < 0 && humanTurn()) {
            val hc = if (hover >= 0) hover else 3
            c.ball(fit.cx(hc), fit.y0 - cs * 0.55f, cs * 0.38f, color(turn))
        }
        // falling disc behind the board front
        if (dropCol >= 0) c.ball(fit.cx(dropCol), fit.y0 + (dropY + 0.5f) * cs, cs * 0.4f, color(turn))
        c.rrect(fit.x0 - 8 * dp, fit.y0 - 6 * dp, fit.right + 8 * dp, fit.bottom + 8 * dp, 18 * dp, 0xFF2F4BD6.toInt())
        for (r in 0..5) for (col in 0..6) {
            val p = g.cells[r * 7 + col]
            val x = fit.cx(col); val y = fit.cy(r)
            val isDropping = dropCol == col && r == dropRow
            if (p != 0 && !isDropping) c.ball(x, y, cs * 0.4f, color(p))
            else c.circle(x, y, cs * 0.4f, 0xFF141833.toInt())
            c.ring(x, y, cs * 0.42f, 2 * dp, 0xFF2440B8.toInt())
        }
        win?.forEach { c.ring(fit.cx(it % 7), fit.cy(it / 7), cs * 0.3f, 4 * dp, Pal.WHITE) }
    }
}
