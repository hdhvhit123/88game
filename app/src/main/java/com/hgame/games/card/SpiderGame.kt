package com.hgame.games.card

import com.hgame.engine.GameOption
import com.hgame.engine.Sfx

class SpiderGame : SolitaireGame() {
    override val options = listOf(GameOption("suits", "Số chất", listOf("1 chất (dễ)", "2 chất", "4 chất (khó)"), 0))
    override val help = "Xếp bài giảm dần ở 10 cột (không cần xen kẽ màu). Một dãy cùng chất từ K xuống A sẽ tự động được thu về. " +
        "Chỉ di chuyển được nhóm bài cùng chất liên tiếp. Chạm chồng bài để chia thêm mỗi cột 1 lá (không được có cột trống)."

    private lateinit var stock: Pile

    override fun deal() {
        stock = Pile(Kind.STOCK, 0); piles.add(stock)
        repeat(8) { piles.add(Pile(Kind.FOUNDATION, it)) }
        val suits = when (opt("suits")) { 0 -> intArrayOf(0); 1 -> intArrayOf(0, 1); else -> intArrayOf(0, 1, 2, 3) }
        val deck = Cards.deck(suits, 8 / suits.size)
        val cols = List(10) { Pile(Kind.TABLEAU, it) }
        for (i in 0 until 54) cols[i % 10].cards.add(deck.removeAt(deck.size - 1))
        cols.forEach { it.top!!.up = true }
        piles.addAll(cols); stock.cards.addAll(deck)
    }

    override fun layoutPiles() {
        val gap = w * 0.01f
        cw = (w - gap * 11) / 10; ch = cw * 1.42f
        val top = 10 * dp
        stock.x = gap; stock.y = top
        for (p in piles) when (p.kind) {
            Kind.FOUNDATION -> { p.x = gap + (p.index + 2) * (cw + gap); p.y = top }
            Kind.TABLEAU -> { p.x = gap + p.index * (cw + gap); p.y = top + ch + 14 * dp; p.maxBottom = bottomLimit() }
            else -> {}
        }
    }

    override fun slotLabel(p: Pile) = if (p.kind == Kind.STOCK) "" else if (p.kind == Kind.FOUNDATION) "K" else ""

    override fun status() = info("Nước đi: $moves · Còn chia ${stock.cards.size / 10} lần")

    override fun tapPile(p: Pile): Boolean {
        if (p !== stock) return false
        if (stock.cards.isEmpty()) return true
        val cols = piles.filter { it.kind == Kind.TABLEAU }
        if (cols.any { it.cards.isEmpty() }) { info("Không được chia khi còn cột trống"); sfx(Sfx.ERROR); return true }
        snapshot()
        for (col in cols) col.cards.add(stock.cards.removeAt(stock.cards.size - 1).also { it.up = true })
        moves++; sfx(Sfx.CARD)
        for (col in cols) collect(col)
        status(); checkWin()
        return true
    }

    private fun isRun(cards: List<Card>) = cards.all { it.up } && cards.zipWithNext().all { (a, b) -> a.suit == b.suit && a.rank == b.rank + 1 }

    override fun canPick(p: Pile, i: Int) = p.kind == Kind.TABLEAU && isRun(p.cards.subList(i, p.cards.size))

    override fun canDrop(cards: List<Card>, from: Pile, to: Pile) =
        to.kind == Kind.TABLEAU && (to.top?.let { it.rank == cards[0].rank + 1 } ?: true)

    override fun afterMove(from: Pile, to: Pile) { collect(to); flipTops() }

    private fun collect(p: Pile) {
        if (p.cards.size < 13) return
        val run = p.cards.subList(p.cards.size - 13, p.cards.size)
        if (run[0].rank == 13 && isRun(run)) {
            val f = piles.first { it.kind == Kind.FOUNDATION && it.cards.isEmpty() }
            val taken = run.toList(); run.clear(); f.cards.addAll(taken)
            sfx(Sfx.COIN); flipTops()
        }
    }

    override fun isWon() = piles.filter { it.kind == Kind.FOUNDATION }.all { it.cards.size == 13 }
}
