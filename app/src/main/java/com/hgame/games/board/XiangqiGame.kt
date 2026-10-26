package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.Xiangqi
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.min

class XiangqiGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(
        GameOption("mode", "Chế độ", listOf("Cầm quân Đỏ", "Cầm quân Đen", "2 người")),
        GameOption("level", "Độ khó máy", listOf("Dễ", "Trung bình", "Khó"), 1),
    )
    override val help = "Cờ tướng: Đỏ đi trước. Tướng và Sĩ ở trong cung, Tượng không qua sông, Mã bị cản chân, " +
        "Pháo ăn quân phải nhảy qua đúng một quân, Tốt qua sông được đi ngang. Hai Tướng không được đối mặt. " +
        "Chiếu bí hoặc làm đối phương hết nước đi để thắng."

    private val redChars = arrayOf("", "帥", "仕", "相", "傌", "俥", "炮", "兵")
    private val blackChars = arrayOf("", "將", "士", "象", "馬", "車", "砲", "卒")

    private var x = Xiangqi()
    private val undoStack = ArrayList<Xiangqi>()
    private var sel = -1
    private var targets: List<Int> = emptyList()
    private var lastFrom = -1; private var lastTo = -1
    private var thinking = false
    private var flip = false
    private var cell = 0f; private var x0 = 0f; private var y0 = 0f
    private val undoBtn = Btn("↶ Đi lại", Pal.SURFACE2)
    private var animM = 0; private var animT = 1f

    private val humanRed get() = opt("mode") != 1
    private val vsAi get() = opt("mode") != 2

    override fun newGame() {
        x = Xiangqi(); undoStack.clear(); sel = -1; targets = emptyList(); lastFrom = -1; lastTo = -1
        thinking = false; animT = 1f
        flip = opt("mode") == 1
        status()
        if (vsAi && !humanRed) aiMove()
    }

    override fun layout() {
        cell = min((w - 24 * dp) / 9, (h - 110 * dp) / 10)
        x0 = (w - cell * 8) / 2
        y0 = (h - 70 * dp - cell * 9) / 2 + 4 * dp
        undoBtn.setCenter(w / 2, h - 32 * dp, 150 * dp, 44 * dp)
    }

    private fun px(sq: Int) = x0 + (if (flip) 8 - sq % 9 else sq % 9) * cell
    private fun py(sq: Int) = y0 + (if (flip) sq / 9 else 9 - sq / 9) * cell

    private fun sqAt(tx: Float, ty: Float): Int {
        val f = ((tx - x0) / cell + 0.5f).toInt(); val r = ((ty - y0) / cell + 0.5f).toInt()
        if (tx < x0 - cell / 2 || ty < y0 - cell / 2 || f !in 0..8 || r !in 0..9) return -1
        val file = if (flip) 8 - f else f
        val rank = if (flip) r else 9 - r
        return rank * 9 + file
    }

    private fun status() {
        val side = if (x.red) "Đỏ" else "Đen"
        info(when { thinking -> "Máy đang nghĩ…"; x.inCheck() -> "$side đang bị chiếu!"; else -> "Lượt: $side" })
    }

    override fun up(tx: Float, ty: Float) {
        if (undoBtn.hit(tx, ty)) { undo(); return }
        if (over || thinking || animT < 1f) return
        if (vsAi && x.red != humanRed) return
        val sq = sqAt(tx, ty)
        if (sq < 0) return
        if (sel >= 0 && sq in targets) {
            doMove(Xiangqi.move(sel, sq))
            if (!over && vsAi) aiMove()
            return
        }
        val p = x.b[sq]
        if (p != 0 && (p > 0) == x.red) {
            sel = sq
            targets = x.legalMoves().filter { Xiangqi.from(it) == sq }.map { Xiangqi.dest(it) }
            sfx(Sfx.CLICK)
        } else { sel = -1; targets = emptyList() }
        redraw()
    }

    private fun undo() {
        if (thinking || undoStack.isEmpty()) return
        x = undoStack.removeAt(undoStack.size - 1)
        if (vsAi) while (x.red != humanRed && undoStack.isNotEmpty()) x = undoStack.removeAt(undoStack.size - 1)
        sel = -1; targets = emptyList(); lastFrom = -1; lastTo = -1; animT = 1f
        if (over) resumePlay()
        sfx(Sfx.TAP); status(); redraw()
        if (vsAi && x.red != humanRed) aiMove()
    }

    private fun doMove(m: Int) {
        undoStack.add(x.copy())
        val cap = x.make(m)
        lastFrom = Xiangqi.from(m); lastTo = Xiangqi.dest(m)
        animM = m; animT = 0f; animate(0.3f)
        sel = -1; targets = emptyList()
        sfx(if (cap != 0) Sfx.HIT else Sfx.TAP)
        val r = x.result()
        if (r != 0) {
            val redWins = r == 1
            val title = if (!vsAi) (if (redWins) "Đỏ thắng!" else "Đen thắng!") else if (redWins == humanRed) "Bạn thắng!" else "Máy thắng"
            gameOver(!vsAi || redWins == humanRed, title, "Chiếu bí")
        } else if (x.inCheck()) { sfx(Sfx.ERROR); buzz(30) }
        status()
    }

    private fun aiMove() {
        thinking = true; status()
        val pos = x.copy()
        val (d, ms, rnd) = when (opt("level")) { 0 -> Triple(1, 400L, 60); 1 -> Triple(2, 1200L, 15); else -> Triple(4, 3000L, 0) }
        background({ pos.bestMove(d, ms, rnd) }) { m ->
            thinking = false
            if (m >= 0) doMove(m)
            status()
        }
    }

    override fun update(dt: Float) { animT = min(1f, animT + dt / 0.18f) }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val pad = cell * 0.6f
        c.rrect(x0 - pad, y0 - pad, x0 + cell * 8 + pad, y0 + cell * 9 + pad, 12 * dp, 0xFFE8C88E.toInt())
        val lc = 0xFF6B4423.toInt(); val lw = 1.4f * dp
        for (r in 0..9) c.line(x0, y0 + r * cell, x0 + 8 * cell, y0 + r * cell, lw, lc)
        for (f in 0..8) {
            if (f == 0 || f == 8) c.line(x0 + f * cell, y0, x0 + f * cell, y0 + 9 * cell, lw, lc)
            else {
                c.line(x0 + f * cell, y0, x0 + f * cell, y0 + 4 * cell, lw, lc)
                c.line(x0 + f * cell, y0 + 5 * cell, x0 + f * cell, y0 + 9 * cell, lw, lc)
            }
        }
        // palaces
        for (top in intArrayOf(0, 7)) {
            c.line(x0 + 3 * cell, y0 + top * cell, x0 + 5 * cell, y0 + (top + 2) * cell, lw, lc)
            c.line(x0 + 5 * cell, y0 + top * cell, x0 + 3 * cell, y0 + (top + 2) * cell, lw, lc)
        }
        c.text("楚 河", x0 + 2 * cell, y0 + 4.5f * cell, cell * 0.45f, Pal.alpha(lc, 0.7f))
        c.text("漢 界", x0 + 6 * cell, y0 + 4.5f * cell, cell * 0.45f, Pal.alpha(lc, 0.7f))
        if (lastFrom >= 0) {
            c.ring(px(lastFrom), py(lastFrom), cell * 0.18f, 2 * dp, Pal.alpha(Pal.BLUE, 0.7f))
        }
        val animTo = if (animT < 1f) Xiangqi.dest(animM) else -1
        for (sq in 0 until 90) {
            val p = x.b[sq]
            if (p == 0 || sq == animTo) continue
            drawPiece(c, p, px(sq), py(sq), sq == sel, sq == lastTo)
        }
        if (animTo >= 0) {
            val f = Xiangqi.from(animM); val e = 1 - (1 - animT) * (1 - animT)
            drawPiece(c, x.b[animTo], px(f) + (px(animTo) - px(f)) * e, py(f) + (py(animTo) - py(f)) * e, false, true)
        }
        for (t in targets) {
            if (x.b[t] != 0) c.ring(px(t), py(t), cell * 0.48f, 3 * dp, Pal.GREEN)
            else c.circle(px(t), py(t), cell * 0.14f, Pal.alpha(0xFF1E8C4E.toInt(), 0.8f))
        }
        undoBtn.enabled = undoStack.isNotEmpty() && !thinking
        undoBtn.draw(c, 15 * dp)
    }

    private fun drawPiece(c: Canvas, p: Int, cx: Float, cy: Float, selected: Boolean, last: Boolean) {
        val r = cell * 0.44f
        val red = p > 0
        val lift = if (selected) 4 * dp else 0f
        c.circle(cx, cy + 3 * dp, r, 0x66000000)
        c.circle(cx, cy - lift, r, 0xFFFFF3D6.toInt())
        c.ring(cx, cy - lift, r * 0.82f, 1.6f * dp, if (red) 0xFFC62828.toInt() else 0xFF263238.toInt())
        if (last || selected) c.ring(cx, cy - lift, r, 2.5f * dp, if (selected) Pal.GREEN else Pal.alpha(Pal.BLUE, 0.8f))
        val ch = if (red) redChars[abs(p)] else blackChars[abs(p)]
        c.save()
        if (flip) c.rotate(0f)
        c.text(ch, cx, cy - lift, r * 1.0f, if (red) 0xFFC62828.toInt() else 0xFF1B1F24.toInt())
        c.restore()
    }
}
