package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

class HanoiGame : Game() {
    override val bestMode = BestMode.NONE
    override val options = listOf(GameOption("disks", "Số đĩa", (3..8).map { "$it đĩa" }, 1))
    override val help = "Chuyển toàn bộ tháp sang cọc bên phải. Mỗi lần chỉ di chuyển một đĩa trên cùng, " +
        "và không được đặt đĩa lớn lên đĩa nhỏ hơn. Chạm cọc để nhấc đĩa, chạm cọc khác để đặt."

    private var disks = 4
    private val pegs = List(3) { ArrayList<Int>() }
    private var held = -1          // peg index whose top disk is lifted
    private var moves = 0
    private var pegX = FloatArray(3)
    private var baseY = 0f
    private var diskH = 0f
    private var maxW = 0f

    override fun newGame() {
        disks = opt("disks") + 3
        pegs.forEach { it.clear() }
        for (d in disks downTo 1) pegs[0].add(d)
        held = -1; moves = 0
        info("Bước: 0 · Tối thiểu: ${(1 shl disks) - 1}")
    }

    override fun layout() {
        for (i in 0..2) pegX[i] = w * (i + 0.5f) / 3
        baseY = h * 0.72f
        maxW = w / 3 - 14 * dp
        diskH = min(28 * dp, h * 0.45f / (disks + 1))
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        val p = (x / (w / 3)).toInt().coerceIn(0, 2)
        if (held < 0) {
            if (pegs[p].isNotEmpty()) { held = p; sfx(Sfx.CLICK) }
        } else {
            if (p == held) { held = -1; sfx(Sfx.CLICK) }
            else {
                val d = pegs[held].last()
                if (pegs[p].isEmpty() || pegs[p].last() > d) {
                    pegs[held].removeAt(pegs[held].size - 1); pegs[p].add(d)
                    moves++; held = -1; sfx(Sfx.TAP)
                    info("Bước: $moves · Tối thiểu: ${(1 shl disks) - 1}")
                    if (pegs[2].size == disks) {
                        val opt = (1 shl disks) - 1
                        gameOver(true, "Hoàn thành!", if (moves == opt) "Hoàn hảo: $moves bước!" else "$moves bước (tối thiểu $opt)")
                    }
                } else { sfx(Sfx.ERROR); buzz(40) }
            }
        }
        redraw()
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.rrect(10 * dp, baseY, w - 10 * dp, baseY + 16 * dp, 8 * dp, Pal.WOOD_DARK)
        val pegTop = baseY - diskH * (disks + 1.5f)
        for (i in 0..2) {
            c.rrect(pegX[i] - 5 * dp, pegTop, pegX[i] + 5 * dp, baseY + 2 * dp, 5 * dp, Pal.WOOD)
            c.text(listOf("A", "B", "C")[i], pegX[i], baseY + 40 * dp, 18 * dp, Pal.MUTED)
        }
        for (i in 0..2) {
            pegs[i].forEachIndexed { k, d ->
                val lifted = i == held && k == pegs[i].size - 1
                val y = if (lifted) pegTop - diskH * 1.4f else baseY - (k + 1) * diskH
                val dw = maxW * (0.3f + 0.7f * d / disks)
                val col = Pal.hsv(d * 360f / (disks + 1), 0.65f, 0.95f)
                c.rrect(pegX[i] - dw / 2, y + 3 * dp, pegX[i] + dw / 2, y + diskH - 1 * dp, diskH * 0.45f, Pal.shade(col, 0.6f))
                c.rrect(pegX[i] - dw / 2, y, pegX[i] + dw / 2, y + diskH - 4 * dp, diskH * 0.45f, col)
            }
        }
    }
}
