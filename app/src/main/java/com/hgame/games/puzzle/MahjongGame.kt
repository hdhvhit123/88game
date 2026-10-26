package com.hgame.games.puzzle

import android.graphics.Canvas
import com.hgame.core.Mahjong
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min
import kotlin.random.Random

class MahjongGame : Game() {
    override val bestMode = BestMode.LOW
    override val help = "Chọn hai quân mạt chược giống nhau đang 'tự do' để xoá chúng. Quân tự do là quân không bị quân khác đè lên " +
        "và có ít nhất một cạnh trái hoặc phải trống. Xoá hết các quân để thắng."

    private val faces = listOf("🀄", "🎋", "🌸", "🍀", "🐉", "🌙", "☀️", "⭐", "🔥", "💧", "🍁", "🐼", "🐰", "🐟", "🍑", "🎐", "🏮", "🌺", "🍵", "🎍", "🐢")
    private val faceColors = intArrayOf(0xFFC62828.toInt(), 0xFF2E7D32.toInt(), 0xFF1565C0.toInt())
    private var m = Mahjong()
    private var sel: Mahjong.Tile? = null
    private var hint: Pair<Mahjong.Tile, Mahjong.Tile>? = null
    private var tw = 0f; private var th = 0f; private var x0 = 0f; private var y0 = 0f; private var depth = 0f
    private val hintBtn = Btn("💡 Gợi ý", Pal.SURFACE2)
    private val shuffleBtn = Btn("🔀 Xáo", Pal.SURFACE2)
    private val history = ArrayList<Pair<Mahjong.Tile, Mahjong.Tile>>()
    private val undoBtn = Btn("↶", Pal.SURFACE2)

    override fun newGame() {
        m = Mahjong(); sel = null; hint = null; history.clear()
        startClock(); score(0); status()
    }

    private fun status() = info("Còn ${m.remaining()} quân")

    override fun layout() {
        tw = min((w - 24 * dp) / 8.3f, (h - 110 * dp) / 6.4f / 1.3f)
        th = tw * 1.3f
        depth = tw * 0.12f
        x0 = (w - tw * 8) / 2 + depth * 2
        y0 = 16 * dp + (h - 90 * dp - th * 6) / 2
        hintBtn.setCenter(w * 0.25f, h - 34 * dp, w * 0.3f, 46 * dp)
        undoBtn.setCenter(w * 0.5f, h - 34 * dp, w * 0.14f, 46 * dp)
        shuffleBtn.setCenter(w * 0.75f, h - 34 * dp, w * 0.3f, 46 * dp)
    }

    private fun tx(t: Mahjong.Tile) = x0 + t.col * tw - t.layer * depth
    private fun ty(t: Mahjong.Tile) = y0 + t.row * th - t.layer * depth * 1.2f + if (t.layer == 3) th * 0.5f else 0f

    override fun up(x: Float, y: Float) {
        if (hintBtn.hit(x, y)) { hint = m.hint(); sfx(Sfx.CLICK); redraw(); return }
        if (shuffleBtn.hit(x, y) && !over) { m.shuffleRemaining(Random.Default); sel = null; hint = null; sfx(Sfx.CARD); redraw(); return }
        if (undoBtn.hit(x, y) && history.isNotEmpty()) {
            val (a, b) = history.removeAt(history.size - 1); a.alive = true; b.alive = true
            if (over) resumePlay()
            sel = null; status(); sfx(Sfx.TAP); redraw(); return
        }
        if (over) return
        val t = m.tiles.filter { it.alive }.sortedWith(compareByDescending<Mahjong.Tile> { it.layer }).firstOrNull {
            x >= tx(it) && x < tx(it) + tw && y >= ty(it) && y < ty(it) + th
        } ?: run { sel = null; redraw(); return }
        if (!m.isFree(t)) { sfx(Sfx.ERROR); buzz(20); return }
        val s = sel
        if (s == null || s === t) { sel = if (s === t) null else t; sfx(Sfx.CLICK) }
        else if (s.kind == t.kind) {
            s.alive = false; t.alive = false; history.add(s to t)
            sel = null; hint = null
            sfx(Sfx.COIN)
            status()
            if (m.remaining() == 0) gameOver(true, "Dọn sạch bàn!", "", clockSeconds)
            else if (m.hint() == null) info("Hết cặp có thể xoá – hãy Xáo hoặc Đi lại")
        } else { sel = t; sfx(Sfx.CLICK) }
        redraw()
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1D5C45.toInt(), 0xFF0E3326.toInt())
        val order = m.tiles.filter { it.alive }.sortedWith(compareBy<Mahjong.Tile>({ it.layer }, { it.row }, { -it.col }))
        val h1 = hint
        for (t in order) {
            val x = tx(t); val y = ty(t)
            val free = m.isFree(t)
            c.rrect(x + depth, y + depth, x + tw + depth * 0.2f, y + th + depth * 0.2f, tw * 0.12f, 0xFF8C7A5B.toInt())
            c.rrect(x + depth * 0.5f, y + depth * 0.5f, x + tw, y + th, tw * 0.12f, 0xFFD9C9A3.toInt())
            val face = when {
                t === sel -> 0xFFFFE27A.toInt()
                h1 != null && (t === h1.first || t === h1.second) -> 0xFFB9F6CA.toInt()
                free -> 0xFFFFFBF0.toInt()
                else -> 0xFFE8E0CC.toInt()
            }
            c.rrect(x, y, x + tw - depth * 0.5f, y + th - depth * 0.5f, tw * 0.12f, face)
            c.text(faces[t.kind], x + tw * 0.47f, y + th * 0.47f, tw * 0.6f, faceColors[t.kind % 3])
        }
        hintBtn.draw(c, 15 * dp); shuffleBtn.draw(c, 15 * dp)
        undoBtn.enabled = history.isNotEmpty(); undoBtn.draw(c, 20 * dp)
    }
}
