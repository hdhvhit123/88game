package com.hgame.games.card

import com.hgame.engine.GameOption
import com.hgame.engine.Sfx

class KlondikeGame : SolitaireGame() {
    override val options = listOf(GameOption("draw", "Rút bài", listOf("Rút 1 lá", "Rút 3 lá"), 0))
    override val help = "Xếp toàn bộ 52 lá lên 4 ô nền theo chất, từ A đến K. Ở 7 cột, xếp bài giảm dần xen kẽ đỏ–đen; " +
        "chỉ quân K được đặt vào cột trống. Chạm chồng bài để rút. Chạm một lá để tự động di chuyển, hoặc kéo thả."

    private lateinit var stock: Pile; private lateinit var waste: Pile

    override fun deal() {
        stock = Pile(Kind.STOCK, 0); waste = Pile(Kind.WASTE, 0)
        waste.fanX = opt("draw") == 1
        piles.add(stock); piles.add(waste)
        repeat(4) { piles.add(Pile(Kind.FOUNDATION, it)) }
        val deck = Cards.deck()
        for (t in 0 until 7) {
            val p = Pile(Kind.TABLEAU, t)
            for (k in 0..t) p.cards.add(deck.removeAt(deck.size - 1).also { it.up = k == t })
            piles.add(p)
        }
        stock.cards.addAll(deck)
    }

    override fun layoutPiles() {
        val gap = w * 0.015f
        cw = (w - gap * 8) / 7; ch = cw * 1.42f
        val top = 10 * dp
        stock.x = gap; stock.y = top
        waste.x = gap * 2 + cw; waste.y = top
        piles.filter { it.kind == Kind.FOUNDATION }.forEach { it.x = gap + (it.index + 3) * (cw + gap); it.y = top }
        piles.filter { it.kind == Kind.TABLEAU }.forEach { it.x = gap + it.index * (cw + gap); it.y = top + ch + 14 * dp; it.maxBottom = bottomLimit() }
    }

    override fun tapPile(p: Pile): Boolean {
        if (p !== stock) return false
        snapshot()
        if (stock.cards.isEmpty()) {
            if (waste.cards.isEmpty()) return true
            while (waste.cards.isNotEmpty()) stock.cards.add(waste.cards.removeAt(waste.cards.size - 1).also { it.up = false })
        } else {
            repeat(if (opt("draw") == 1) 3 else 1) {
                if (stock.cards.isNotEmpty()) waste.cards.add(stock.cards.removeAt(stock.cards.size - 1).also { it.up = true })
            }
        }
        moves++; status(); sfx(Sfx.CARD)
        return true
    }

    override fun canPick(p: Pile, i: Int): Boolean = when (p.kind) {
        Kind.WASTE, Kind.FOUNDATION -> i == p.cards.size - 1
        Kind.TABLEAU -> p.cards[i].up
        else -> false
    }

    override fun canDrop(cards: List<Card>, from: Pile, to: Pile): Boolean {
        val c = cards[0]
        return when (to.kind) {
            Kind.FOUNDATION -> cards.size == 1 && (to.top?.let { it.suit == c.suit && it.rank == c.rank - 1 } ?: (c.rank == 1))
            Kind.TABLEAU -> to.top?.let { it.up && it.red != c.red && it.rank == c.rank + 1 } ?: (c.rank == 13)
            else -> false
        }
    }

    override fun isWon() = piles.filter { it.kind == Kind.FOUNDATION }.sumOf { it.cards.size } == 52

    override fun canAutoFinish() = stock.cards.isEmpty() && waste.cards.isEmpty() &&
        piles.filter { it.kind == Kind.TABLEAU }.all { p -> p.cards.all { it.up } }
}
