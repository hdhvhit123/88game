package com.hgame.games.card

import android.graphics.Canvas
import android.graphics.Paint
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rrect
import com.hgame.engine.rrectStroke
import com.hgame.engine.text
import kotlin.random.Random

class YahtzeeGame : Game() {
    override val help = "Mỗi lượt được tung 5 viên xúc xắc tối đa 3 lần; chạm vào xúc xắc để giữ lại. Sau đó chọn một ô trong bảng điểm để ghi. " +
        "Mỗi ô chỉ dùng một lần, 13 lượt. Tổng các ô 1–6 đạt từ 63 được thưởng 35. Năm viên giống nhau (Yahtzee) được 50 điểm."

    private val names = arrayOf("Một", "Hai", "Ba", "Bốn", "Năm", "Sáu", "Bộ ba", "Bộ bốn", "Cù lũ", "Sảnh nhỏ", "Sảnh lớn", "Yahtzee", "Cơ hội")
    private val dice = IntArray(5) { 1 }; private val held = BooleanArray(5)
    private val scores = IntArray(13) { -1 }
    private var rolls = 0; private var rollAnim = 0f
    private val rollBtn = Btn("🎲 Tung", Pal.ACCENT)
    private var dx = 0f; private var dy = 0f; private var ds = 0f
    private var rowH = 0f; private var tableTop = 0f

    override fun newGame() { scores.fill(-1); held.fill(false); rolls = 0; dice.fill(1); score(0); info("Lượt 1/13") }

    override fun layout() {
        ds = minOf(w / 6.5f, h * 0.09f); dx = (w - ds * 5 - 4 * ds * 0.2f) / 2; dy = h - ds - 90 * dp
        rollBtn.setCenter(w / 2, h - 40 * dp, w * 0.6f, 50 * dp)
        tableTop = 8 * dp; rowH = (dy - 30 * dp - tableTop) / 16.5f
    }

    private fun counts() = IntArray(7).also { for (d in dice) it[d]++ }

    private fun potential(cat: Int): Int {
        val cnt = counts(); val sum = dice.sum()
        return when (cat) {
            in 0..5 -> cnt[cat + 1] * (cat + 1)
            6 -> if (cnt.any { it >= 3 }) sum else 0
            7 -> if (cnt.any { it >= 4 }) sum else 0
            8 -> if (cnt.any { it == 3 } && cnt.any { it == 2 }) 25 else 0
            9 -> if (listOf(1..4, 2..5, 3..6).any { r -> r.all { cnt[it] > 0 } }) 30 else 0
            10 -> if (listOf(1..5, 2..6).any { r -> r.all { cnt[it] > 0 } }) 40 else 0
            11 -> if (cnt.any { it == 5 }) 50 else 0
            else -> sum
        }
    }

    private fun upper() = (0..5).sumOf { maxOf(0, scores[it]) }
    private fun total() = scores.sumOf { maxOf(0, it) } + if (upper() >= 63) 35 else 0

    override fun up(x: Float, y: Float) {
        if (over || rollAnim > 0f) return
        if (rollBtn.hit(x, y) && rolls < 3) {
            for (i in 0..4) if (!held[i]) dice[i] = 1 + Random.nextInt(6)
            rolls++; rollAnim = 0.5f; animate(0.5f); sfx(Sfx.SHOOT)
            after(0.5f) { rollAnim = 0f; if (counts().any { it == 5 }) sfx(Sfx.WIN) }
            return
        }
        for (i in 0..4) {
            val l = dx + i * ds * 1.2f
            if (rolls > 0 && x in l..l + ds && y in dy..dy + ds) { held[i] = !held[i]; sfx(Sfx.CLICK); redraw(); return }
        }
        if (rolls > 0 && y > tableTop + rowH && y < tableTop + rowH * 15) {
            var row = ((y - tableTop) / rowH).toInt() - 1
            // rows: 0..5 upper, 6 = bonus line (not selectable), 7..13 lower
            if (row == 6) return
            val cat = if (row < 6) row else row - 1
            if (cat !in 0..12 || scores[cat] >= 0) return
            scores[cat] = potential(cat)
            sfx(if (scores[cat] > 0) Sfx.COIN else Sfx.TAP)
            score(total())
            rolls = 0; held.fill(false)
            val done = scores.count { it >= 0 }
            if (done == 13) { sfx(Sfx.WIN); gameOver(true, "Hoàn thành!", "Tổng điểm ${total()}", total()) }
            else info("Lượt ${done + 1}/13")
            redraw()
        }
    }

    override fun update(dt: Float) { if (rollAnim > 0f) rollAnim -= dt }

    private val pipPos = arrayOf(
        floatArrayOf(), floatArrayOf(.5f, .5f), floatArrayOf(.27f, .27f, .73f, .73f), floatArrayOf(.25f, .25f, .5f, .5f, .75f, .75f),
        floatArrayOf(.27f, .27f, .73f, .27f, .27f, .73f, .73f, .73f), floatArrayOf(.25f, .25f, .75f, .25f, .5f, .5f, .25f, .75f, .75f, .75f),
        floatArrayOf(.27f, .23f, .73f, .23f, .27f, .5f, .73f, .5f, .27f, .77f, .73f, .77f),
    )

    private fun die(c: Canvas, x: Float, y: Float, s: Float, v: Int, hold: Boolean) {
        c.rrect(x, y + s * 0.06f, x + s, y + s * 1.06f, s * 0.2f, 0xFF9E9E9E.toInt())
        c.rrect(x, y, x + s, y + s, s * 0.2f, if (hold) 0xFFFFF59D.toInt() else Pal.WHITE)
        val p = pipPos[v]; var i = 0
        while (i < p.size) { c.circle(x + p[i] * s, y + p[i + 1] * s, s * 0.09f, if (v == 1) Pal.RED else 0xFF212121.toInt()); i += 2 }
        if (hold) c.rrectStroke(x, y, x + s, y + s, s * 0.2f, 3 * dp, Pal.ORANGE)
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF0D47A1.toInt(), 0xFF071A3D.toInt())
        // score table
        val l = 12 * dp; val r = w - 12 * dp
        c.rrect(l, tableTop, r, tableTop + rowH * 16, 12 * dp, Pal.alpha(Pal.WHITE, 0.08f))
        c.text("Hạng mục", l + 10 * dp, tableTop + rowH / 2, rowH * 0.45f, Pal.MUTED, Paint.Align.LEFT)
        c.text("Điểm", r - 10 * dp, tableTop + rowH / 2, rowH * 0.45f, Pal.MUTED, Paint.Align.RIGHT)
        for (row in 0 until 14) {
            val y = tableTop + (row + 1) * rowH
            if (row == 6) {
                c.text("Thưởng (≥63: +35)  ${upper()}/63", l + 10 * dp, y + rowH / 2, rowH * 0.42f, Pal.YELLOW, Paint.Align.LEFT)
                c.text(if (upper() >= 63) "35" else "–", r - 10 * dp, y + rowH / 2, rowH * 0.5f, Pal.YELLOW, Paint.Align.RIGHT)
                continue
            }
            val cat = if (row < 6) row else row - 1
            val used = scores[cat] >= 0
            if (!used && rolls > 0) c.rrect(l + 3 * dp, y + 2, r - 3 * dp, y + rowH - 2, rowH * 0.25f, Pal.alpha(Pal.ACCENT, 0.25f))
            c.text(names[cat], l + 10 * dp, y + rowH / 2, rowH * 0.48f, if (used) Pal.MUTED else Pal.WHITE, Paint.Align.LEFT)
            val v = if (used) "${scores[cat]}" else if (rolls > 0) "${potential(cat)}" else ""
            c.text(v, r - 10 * dp, y + rowH / 2, rowH * 0.52f, if (used) Pal.WHITE else Pal.CYAN, Paint.Align.RIGHT)
        }
        c.text("TỔNG: ${total()}", w / 2, tableTop + rowH * 15.5f, rowH * 0.6f, Pal.YELLOW)
        // dice
        for (i in 0..4) {
            val shakeY = if (rollAnim > 0f && !held[i]) (Random.nextFloat() - 0.5f) * 12 * dp else 0f
            val v = if (rollAnim > 0f && !held[i]) 1 + Random.nextInt(6) else dice[i]
            if (rolls == 0) { c.rrect(dx + i * ds * 1.2f, dy, dx + i * ds * 1.2f + ds, dy + ds, ds * 0.2f, Pal.alpha(Pal.WHITE, 0.15f)) }
            else die(c, dx + i * ds * 1.2f, dy + shakeY, ds, v, held[i])
        }
        rollBtn.enabled = rolls < 3
        rollBtn.label = if (rolls == 0) "🎲 Tung xúc xắc" else "🎲 Tung lại (${3 - rolls})"
        rollBtn.draw(c, 17 * dp)
        if (rolls > 0) c.text("Chạm xúc xắc để giữ · chạm một hạng mục để ghi điểm", w / 2, dy - 10 * dp, 11 * dp, Pal.MUTED)
    }
}
