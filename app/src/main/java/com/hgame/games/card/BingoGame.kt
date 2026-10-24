package com.hgame.games.card

import android.graphics.Canvas
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.random.Random

class BingoGame : Game() {
    override val realtime = true
    override val help = "Số được gọi mỗi vài giây. Chạm vào ô có số vừa gọi trên phiếu của bạn để đánh dấu. " +
        "Khi có đủ 5 ô thẳng hàng (ngang, dọc hoặc chéo), bấm BINGO! trước 3 đối thủ máy. Bấm nhầm sẽ bị phạt dừng 3 giây."

    private class CardB(val nums: IntArray) { val marked = BooleanArray(25).also { it[12] = true } }
    private lateinit var mine: CardB
    private val bots = ArrayList<CardB>()
    private val called = ArrayList<Int>(); private var pool = ArrayList<Int>()
    private var callT = 0f; private var interval = 3.2f; private var penalty = 0f
    private val bingoBtn = Btn("BINGO!", Pal.PINK)
    private var gx = 0f; private var gy = 0f; private var cs = 0f
    private var wins = 0

    private fun makeCard(): CardB {
        val nums = IntArray(25)
        for (c in 0 until 5) { val col = (c * 15 + 1..c * 15 + 15).shuffled().take(5); for (r in 0 until 5) nums[r * 5 + c] = col[r] }
        nums[12] = 0
        return CardB(nums)
    }

    override fun newGame() {
        mine = makeCard(); bots.clear(); repeat(3) { bots.add(makeCard()) }
        called.clear(); pool = ArrayList((1..75).shuffled()); callT = 1.5f; penalty = 0f
        wins = host.loadInt("wins", 0); score(wins); info("Đang chuẩn bị…")
    }

    override fun layout() {
        cs = minOf((w - 24 * dp) / 5, (h * 0.55f) / 6)
        gx = (w - cs * 5) / 2; gy = h * 0.3f
        bingoBtn.setCenter(w / 2, gy + cs * 6 + 30 * dp, w * 0.6f, 52 * dp)
    }

    private fun letter(n: Int) = "BINGO"[(n - 1) / 15]

    private fun hasLine(m: BooleanArray): Boolean {
        for (i in 0 until 5) {
            if ((0 until 5).all { m[i * 5 + it] }) return true
            if ((0 until 5).all { m[it * 5 + i] }) return true
        }
        return (0 until 5).all { m[it * 6] } || (0 until 5).all { m[it * 4 + 4] }
    }

    override fun update(dt: Float) {
        if (penalty > 0) penalty -= dt
        callT -= dt
        if (callT <= 0f) {
            if (pool.isEmpty()) { gameOver(false, "Hết số!", "Không ai Bingo"); return }
            callT = interval
            val n = pool.removeAt(0); called.add(n); sfx(Sfx.TICK)
            info("Số vừa gọi: ${letter(n)}-$n")
            for ((i, b) in bots.withIndex()) {
                val k = b.nums.indexOf(n)
                if (k >= 0) after(0.6f + Random.nextFloat() * 1.8f + i * 0.2f) {
                    if (over) return@after
                    b.marked[k] = true
                    if (hasLine(b.marked)) after(0.8f + Random.nextFloat()) {
                        if (!over) { sfx(Sfx.LOSE); gameOver(false, "Máy ${i + 1} BINGO trước!", "Số trận thắng: $wins") }
                    }
                }
            }
        }
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        if (penalty > 0f) { sfx(Sfx.ERROR); return }
        if (bingoBtn.hit(x, y)) {
            if (hasLine(mine.marked)) {
                wins++; host.saveInt("wins", wins); score(wins); sfx(Sfx.WIN)
                gameOver(true, "BINGO!", "Bạn thắng sau ${called.size} số", wins)
            } else { penalty = 3f; sfx(Sfx.ERROR); buzz(80) }
            return
        }
        val c = ((x - gx) / cs).toInt(); val r = ((y - gy - cs) / cs).toInt()
        if (x < gx || y < gy + cs || c !in 0..4 || r !in 0..4) return
        val k = r * 5 + c
        if (mine.marked[k]) return
        if (mine.nums[k] in called) { mine.marked[k] = true; sfx(Sfx.POP) } else { sfx(Sfx.ERROR); penalty = 1f }
        redraw()
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF4A148C.toInt(), 0xFF1A0533.toInt())
        // called balls
        val last = called.lastOrNull()
        if (last != null) {
            c.circle(w / 2, h * 0.11f, 40 * dp, Pal.shade(colorOf(last), 0.6f)); c.circle(w / 2, h * 0.11f - 2 * dp, 38 * dp, colorOf(last))
            c.circle(w / 2, h * 0.11f - 2 * dp, 26 * dp, Pal.WHITE)
            c.text("${letter(last)}", w / 2, h * 0.11f - 14 * dp, 12 * dp, 0xFF333333.toInt())
            c.text("$last", w / 2, h * 0.11f + 4 * dp, 20 * dp, 0xFF111111.toInt())
            val prev = called.dropLast(1).takeLast(6).reversed()
            prev.forEachIndexed { i, n ->
                val x = w / 2 + (if (i % 2 == 0) 1 else -1) * (64 + (i / 2) * 36) * dp
                c.circle(x, h * 0.11f, 15 * dp, colorOf(n)); c.text("$n", x, h * 0.11f, 12 * dp, Pal.WHITE)
            }
            // countdown bar
            c.rrect(w * 0.2f, h * 0.2f, w * 0.2f + w * 0.6f * (callT / interval), h * 0.2f + 4 * dp, 2 * dp, Pal.alpha(Pal.WHITE, 0.5f))
        }
        // bot progress
        bots.forEachIndexed { i, b ->
            val x = w * (0.2f + i * 0.3f)
            val best = bestLine(b.marked)
            c.text("Máy ${i + 1}: $best/5", x, h * 0.25f, 12 * dp, if (best >= 4) Pal.ORANGE else Pal.MUTED)
        }
        // card
        c.rrect(gx - 8 * dp, gy - 8 * dp, gx + cs * 5 + 8 * dp, gy + cs * 6 + 8 * dp, 14 * dp, 0xFFFDF7FF.toInt())
        for (k in 0 until 5) c.text("BINGO"[k].toString(), gx + k * cs + cs / 2, gy + cs / 2, cs * 0.55f, intArrayOf(Pal.BLUE, Pal.RED, 0xFF555555.toInt(), Pal.GREEN, Pal.ORANGE)[k])
        for (k in 0 until 25) {
            val x = gx + (k % 5) * cs; val y = gy + cs + (k / 5) * cs
            c.rrect(x + 2, y + 2, x + cs - 2, y + cs - 2, cs * 0.15f, if ((k / 5 + k % 5) % 2 == 0) 0xFFEDE7F6.toInt() else 0xFFF3E5F5.toInt())
            if (k == 12) c.text("★", x + cs / 2, y + cs / 2, cs * 0.5f, Pal.PURPLE)
            else c.text("${mine.nums[k]}", x + cs / 2, y + cs / 2, cs * 0.38f, 0xFF311B92.toInt())
            if (mine.marked[k]) c.circle(x + cs / 2, y + cs / 2, cs * 0.38f, Pal.alpha(Pal.PINK, 0.55f))
        }
        bingoBtn.enabled = penalty <= 0f
        bingoBtn.draw(c, 22 * dp)
        if (penalty > 0f) c.text("Bị phạt: ${penalty.toInt() + 1}s", w / 2, bingoBtn.b + 18 * dp, 13 * dp, Pal.ORANGE)
    }

    private fun bestLine(m: BooleanArray): Int {
        var best = 0
        for (i in 0 until 5) { best = maxOf(best, (0 until 5).count { m[i * 5 + it] }, (0 until 5).count { m[it * 5 + i] }) }
        return maxOf(best, (0 until 5).count { m[it * 6] }, (0 until 5).count { m[it * 4 + 4] })
    }

    private fun colorOf(n: Int) = intArrayOf(Pal.BLUE, Pal.RED, 0xFF757575.toInt(), Pal.GREEN, Pal.ORANGE)[(n - 1) / 15]
}
