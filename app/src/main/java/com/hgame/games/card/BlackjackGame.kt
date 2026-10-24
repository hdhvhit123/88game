package com.hgame.games.card

import android.graphics.Canvas
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.text

class BlackjackGame : Game() {
    override val help = "Đặt cược rồi rút bài để tổng điểm gần 21 nhất mà không vượt quá. J, Q, K = 10; A = 1 hoặc 11. " +
        "Rút (Hit) để lấy thêm lá, Dừng (Stand) để nhà cái chơi, Gấp đôi (Double) để nhân đôi cược và rút đúng 1 lá. " +
        "Blackjack (A + lá 10 điểm) thắng 3:2. Nhà cái rút đến khi đạt 17. Hết tiền sẽ được cấp lại 1000."

    private var shoe = ArrayList<Card>()
    private val player = ArrayList<Card>(); private val dealer = ArrayList<Card>()
    private var bank = 1000; private var bet = 0; private var phase = 0   // 0 betting, 1 playing, 2 dealer, 3 result
    private var result = ""; private var resultColor = Pal.WHITE
    private val chipVals = intArrayOf(10, 50, 100, 500)
    private val chipCols = intArrayOf(Pal.BLUE, Pal.RED, Pal.GREEN, Pal.PURPLE)
    private val chipBtns = List(4) { Btn("", chipCols[it]) }
    private val dealBtn = Btn("Chia bài", Pal.ACCENT); private val clearBtn = Btn("Xoá cược", Pal.SURFACE2)
    private val hitBtn = Btn("Rút", Pal.GREEN); private val standBtn = Btn("Dừng", Pal.RED); private val dblBtn = Btn("Gấp đôi", Pal.ORANGE)
    private val againBtn = Btn("Ván mới", Pal.ACCENT)
    private var cw = 0f; private var ch = 0f; private var lastBet = 50

    override fun newGame() {
        bank = host.loadInt("bank", 1000); if (bank < 10) bank = 1000
        player.clear(); dealer.clear(); bet = 0; phase = 0; result = ""
        score(bank); info("Đặt cược để bắt đầu")
    }

    override fun layout() {
        cw = minOf(w / 5.5f, h / 9f); ch = cw * 1.42f
        val y = h - 50 * dp; val bw = (w - 50 * dp) / 4
        chipBtns.forEachIndexed { i, b -> b.setCenter(10 * dp + bw / 2 + i * (bw + 10 * dp), h - 120 * dp, minOf(bw, 64 * dp), minOf(bw, 64 * dp)) }
        dealBtn.setCenter(w * 0.68f, y, w * 0.5f, 48 * dp); clearBtn.setCenter(w * 0.2f, y, w * 0.3f, 48 * dp)
        hitBtn.setCenter(w / 6, y, w * 0.28f, 50 * dp); standBtn.setCenter(w / 2, y, w * 0.28f, 50 * dp); dblBtn.setCenter(w * 5 / 6, y, w * 0.28f, 50 * dp)
        againBtn.setCenter(w / 2, y, w * 0.6f, 50 * dp)
    }

    private fun draw1(up: Boolean = true): Card {
        if (shoe.size < 15) shoe = Cards.deck(copies = 4)
        return shoe.removeAt(shoe.size - 1).also { it.up = up }
    }

    private fun value(hand: List<Card>): Int {
        var v = 0; var aces = 0
        for (c in hand) if (c.up) { v += minOf(c.rank, 10); if (c.rank == 1) aces++ }
        while (aces > 0 && v + 10 <= 21) { v += 10; aces-- }
        return v
    }

    override fun up(x: Float, y: Float) {
        when (phase) {
            0 -> {
                chipBtns.forEachIndexed { i, b -> if (b.hit(x, y) && bet + chipVals[i] <= bank) { bet += chipVals[i]; sfx(Sfx.COIN); info("Cược: $bet") } }
                if (clearBtn.hit(x, y)) { bet = 0; sfx(Sfx.CLICK) }
                if (dealBtn.hit(x, y)) { if (bet == 0) bet = minOf(lastBet, bank); if (bet > 0) startRound() }
            }
            1 -> {
                if (hitBtn.hit(x, y)) { player.add(draw1()); sfx(Sfx.CARD); if (value(player) > 21) finish() else if (value(player) == 21) dealerTurn() }
                else if (standBtn.hit(x, y)) dealerTurn()
                else if (dblBtn.hit(x, y) && player.size == 2 && bank >= bet * 2) { bet *= 2; player.add(draw1()); sfx(Sfx.CARD); if (value(player) > 21) finish() else dealerTurn() }
            }
            3 -> if (againBtn.hit(x, y)) { player.clear(); dealer.clear(); phase = 0; bet = 0; result = ""; info("Đặt cược để bắt đầu"); if (bank < 10) { bank = 1000; host.saveInt("bank", bank); score(bank) } }
        }
        redraw()
    }

    private fun startRound() {
        lastBet = bet; phase = 1; player.clear(); dealer.clear()
        player.add(draw1()); dealer.add(draw1()); player.add(draw1()); dealer.add(draw1(false))
        sfx(Sfx.CARD); info("Cược: $bet")
        val pBj = value(player) == 21
        dealer[1].up = true; val dBj = value(dealer) == 21; dealer[1].up = false
        if (pBj || dBj) { dealer[1].up = true; finish() }
    }

    private fun dealerTurn() {
        phase = 2; dealer[1].up = true; sfx(Sfx.CARD)
        step()
    }

    private fun step() {
        after(0.6f) {
            val v = value(dealer)
            if (v < 17) { dealer.add(draw1()); sfx(Sfx.CARD); step() } else finish()
        }
    }

    private fun finish() {
        dealer.forEach { it.up = true }
        val p = value(player); val d = value(dealer)
        val pBj = p == 21 && player.size == 2; val dBj = d == 21 && dealer.size == 2
        val delta: Int
        when {
            p > 21 -> { delta = -bet; result = "Quắc! Bạn thua $bet"; resultColor = Pal.RED }
            pBj && !dBj -> { delta = bet * 3 / 2; result = "BLACKJACK! +$delta"; resultColor = Pal.YELLOW }
            dBj && !pBj -> { delta = -bet; result = "Nhà cái Blackjack. -$bet"; resultColor = Pal.RED }
            d > 21 -> { delta = bet; result = "Nhà cái quắc! +$bet"; resultColor = Pal.GREEN }
            p > d -> { delta = bet; result = "Bạn thắng! +$bet"; resultColor = Pal.GREEN }
            p < d -> { delta = -bet; result = "Bạn thua. -$bet"; resultColor = Pal.RED }
            else -> { delta = 0; result = "Hoà, hoàn cược"; resultColor = Pal.WHITE }
        }
        bank += delta; host.saveInt("bank", bank); score(bank)
        sfx(if (delta > 0) Sfx.WIN else if (delta < 0) Sfx.LOSE else Sfx.CLICK)
        phase = 3
        info("Tiền: $bank")
    }

    private fun hand(c: Canvas, cards: List<Card>, y: Float) {
        val n = cards.size
        val step = minOf(cw * 0.75f, (w - 40 * dp - cw) / maxOf(1, n - 1))
        val x0 = w / 2 - (step * (n - 1) + cw) / 2
        cards.forEachIndexed { i, card -> Cards.draw(c, card, x0 + i * step, y, cw, ch) }
    }

    private fun chip(c: Canvas, x: Float, y: Float, r: Float, col: Int, label: String) {
        c.circle(x, y + r * 0.12f, r, Pal.shade(col, 0.5f)); c.circle(x, y, r, col)
        c.ring(x, y, r * 0.75f, r * 0.12f, Pal.alpha(Pal.WHITE, 0.8f))
        c.text(label, x, y, r * 0.55f, Pal.WHITE)
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1B6B4A.toInt(), 0xFF0E3F2B.toInt())
        c.rrect(w * 0.1f, h * 0.03f, w * 0.9f, h * 0.03f + 22 * dp, 11 * dp, Pal.alpha(Pal.BLACK, 0.25f))
        c.text("NHÀ CÁI RÚT ĐẾN 17 · BLACKJACK TRẢ 3:2", w / 2, h * 0.03f + 11 * dp, 10 * dp, Pal.alpha(Pal.WHITE, 0.7f))
        val dy = h * 0.1f; val py = h * 0.45f
        if (dealer.isNotEmpty()) { hand(c, dealer, dy); c.text("Nhà cái: ${value(dealer)}", w / 2, dy + ch + 16 * dp, 15 * dp, Pal.WHITE) }
        if (player.isNotEmpty()) { hand(c, player, py); c.text("Bạn: ${value(player)}", w / 2, py + ch + 16 * dp, 16 * dp, Pal.YELLOW) }
        c.text("💰 $bank", 16 * dp, h * 0.38f, 16 * dp, Pal.YELLOW, android.graphics.Paint.Align.LEFT)
        if (bet > 0) chip(c, w - 40 * dp, h * 0.38f, 20 * dp, Pal.ORANGE, "$bet")
        when (phase) {
            0 -> {
                chipBtns.forEachIndexed { i, b -> chip(c, b.cx, b.cy, (b.r - b.l) / 2, if (bet + chipVals[i] <= bank) chipCols[i] else Pal.SURFACE2, "${chipVals[i]}") }
                dealBtn.label = if (bet == 0) "Chia bài (cược ${minOf(lastBet, bank)})" else "Chia bài · $bet"
                dealBtn.draw(c, 15 * dp); clearBtn.draw(c, 14 * dp)
                c.text("Chạm phỉnh để đặt cược", w / 2, h - 170 * dp, 14 * dp, Pal.alpha(Pal.WHITE, 0.8f))
            }
            1 -> { dblBtn.enabled = player.size == 2 && bank >= bet * 2; hitBtn.draw(c); standBtn.draw(c); dblBtn.draw(c, 15 * dp) }
            2 -> c.text("Nhà cái đang rút…", w / 2, h - 50 * dp, 16 * dp, Pal.WHITE)
            3 -> {
                c.rrect(16 * dp, h * 0.72f - 26 * dp, w - 16 * dp, h * 0.72f + 26 * dp, 16 * dp, Pal.alpha(Pal.BLACK, 0.45f))
                c.text(result, w / 2, h * 0.72f, 20 * dp, resultColor)
                againBtn.draw(c)
            }
        }
    }
}
