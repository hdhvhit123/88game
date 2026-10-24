package com.hgame.games.casual

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.gradRect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.min

class MemoryMatchGame : Game() {
    override val bestMode = BestMode.LOW
    override val options = listOf(GameOption("size", "Số thẻ", listOf("12 thẻ", "20 thẻ", "30 thẻ"), 1))
    override val help = "Lật hai thẻ mỗi lượt. Nếu hai thẻ giống nhau, chúng được giữ lại. Tìm hết các cặp nhanh nhất có thể."

    private val symbols = listOf("🍎", "🍋", "🍇", "🍉", "🍒", "🥝", "🍑", "🍍", "🥥", "🍓", "🌶", "🥕", "🌽", "🍄", "🥑")
    private var cards = IntArray(0)
    private var flip = FloatArray(0)      // 0 = back, 1 = face
    private var matched = BooleanArray(0)
    private val open = ArrayList<Int>()
    private var cols = 4; private var rows = 3
    private val fit = BoardFit()
    private var moves = 0
    private var busy = false

    override fun newGame() {
        val (c, r) = when (opt("size")) { 0 -> 3 to 4; 1 -> 4 to 5; else -> 5 to 6 }
        cols = c; rows = r
        val pairs = c * r / 2
        cards = (0 until pairs).flatMap { listOf(it, it) }.shuffled().toIntArray()
        val chosen = symbols.shuffled().take(pairs)
        symbolMap = chosen
        flip = FloatArray(cards.size); matched = BooleanArray(cards.size)
        open.clear(); moves = 0; busy = false
        info("Lượt: 0")
        score(0)
    }

    private var symbolMap: List<String> = emptyList()

    override fun layout() {
        val pad = 14 * dp
        fit.fit(cols, rows, pad, pad, w - pad, h - pad)
    }

    override fun up(x: Float, y: Float) {
        if (over || busy) return
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) return
        val i = r * cols + c
        if (matched[i] || i in open) return
        if (moves == 0 && open.isEmpty()) startClock()
        open.add(i)
        sfx(Sfx.CARD)
        animate(0.4f)
        if (open.size == 2) {
            moves++
            info("Lượt: $moves")
            val (a, b) = open
            if (cards[a] == cards[b]) {
                matched[a] = true; matched[b] = true; open.clear()
                after(0.25f) { sfx(Sfx.COIN) }
                if (matched.all { it }) gameOver(true, "Hoàn thành!", "$moves lượt lật", clockSeconds)
            } else {
                busy = true
                after(0.8f) { open.clear(); busy = false; animate(0.4f) }
            }
        }
    }

    override fun update(dt: Float) {
        for (i in cards.indices) {
            val target = if (matched[i] || i in open) 1f else 0f
            if (flip[i] != target) {
                flip[i] += (if (target > flip[i]) 1 else -1) * dt * 6f
                flip[i] = flip[i].coerceIn(0f, 1f)
            }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val cs = fit.cell
        val g = cs * 0.06f
        for (i in cards.indices) {
            val cx = fit.cx(i % cols); val cy = fit.cy(i / cols)
            val f = flip[i]
            val sx = abs(1 - 2 * f)      // horizontal squash for the flip
            val hw = (cs / 2 - g) * sx; val hh = cs / 2 - g * 1.4f
            val face = f > 0.5f
            if (face) {
                c.rrect(cx - hw, cy - hh + 3 * dp, cx + hw, cy + hh + 3 * dp, cs * 0.14f, 0xFFB8B4D9.toInt())
                c.rrect(cx - hw, cy - hh, cx + hw, cy + hh, cs * 0.14f, if (matched[i]) 0xFFE5FFF2.toInt() else Pal.WHITE)
                c.save(); c.scale(sx, 1f, cx, cy)
                c.text(symbolMap[cards[i]], cx, cy, cs * 0.5f, Pal.BLACK)
                c.restore()
            } else {
                c.rrect(cx - hw, cy - hh + 3 * dp, cx + hw, cy + hh + 3 * dp, cs * 0.14f, 0xFF3A2A85.toInt())
                c.gradRect(cx - hw, cy - hh, cx + hw, cy + hh, cs * 0.14f, 0xFF8E6BFF.toInt(), 0xFF5B3FD9.toInt())
                c.save(); c.scale(sx, 1f, cx, cy)
                c.text("?", cx, cy, cs * 0.4f, Pal.alpha(Pal.WHITE, 0.6f))
                c.restore()
            }
        }
    }
}
