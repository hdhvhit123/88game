package com.hgame.games.card

import com.hgame.engine.Sfx

class PyramidGame : SolitaireGame() {
    override val help = "Loại bỏ toàn bộ kim tự tháp bằng cách ghép hai lá có tổng điểm bằng 13 (A=1, J=11, Q=12, K=13). " +
        "Quân K tự loại bỏ khi chạm vào. Chỉ dùng được lá không bị lá khác đè lên. Chạm chồng bài để lật thêm (được xào lại 2 lần)."

    private lateinit var stock: Pile; private lateinit var waste: Pile; private lateinit var discard: Pile
    private val pyr = ArrayList<Pile>()
    override var extraState = 0   // redeals used

    private fun row(k: Int): Int { var r = 0; var start = 0; while (k >= start + r + 1) { start += r + 1; r++ }; return r }
    private fun startOf(r: Int) = r * (r + 1) / 2

    override fun deal() {
        stock = Pile(Kind.STOCK, 0); waste = Pile(Kind.WASTE, 0); discard = Pile(Kind.FOUNDATION, 0)
        piles.add(stock); piles.add(waste); piles.add(discard)
        pyr.clear(); extraState = 0
        val deck = Cards.deck()
        for (k in 0 until 28) { val p = Pile(Kind.TABLEAU, k); p.fan = false; p.showSlot = false; p.cards.add(deck.removeAt(deck.size - 1).also { it.up = true }); pyr.add(p) }
        piles.addAll(pyr)
        stock.cards.addAll(deck)
    }

    override fun layoutPiles() {
        cw = minOf(w / 7.6f, (h - 80 * dp) / (1.42f * 5.6f)); ch = cw * 1.42f
        val gap = cw * 0.08f
        val top = 10 * dp
        for (k in 0 until 28) {
            val r = row(k); val i = k - startOf(r)
            pyr[k].x = w / 2 - (r + 1) * (cw + gap) / 2 + i * (cw + gap); pyr[k].y = top + r * ch * 0.5f
        }
        val by = top + 6 * ch * 0.5f + ch + 18 * dp
        stock.x = w / 2 - cw * 1.7f; stock.y = by
        waste.x = w / 2 - cw * 0.5f; waste.y = by
        discard.x = w / 2 + cw * 0.7f; discard.y = by
    }

    override fun slotLabel(p: Pile) = when (p.kind) { Kind.STOCK -> if (extraState < 2) "↻" else "✕"; Kind.FOUNDATION -> "13"; else -> "" }

    override fun status() = info("Nước đi: $moves · Xào lại còn ${2 - extraState}")

    private fun free(p: Pile): Boolean {
        if (p.kind == Kind.WASTE) return p.cards.isNotEmpty()
        if (p.kind != Kind.TABLEAU || p.cards.isEmpty()) return false
        val r = row(p.index); if (r == 6) return true
        val i = p.index - startOf(r)
        val below = startOf(r + 1) + i
        return pyr[below].cards.isEmpty() && pyr[below + 1].cards.isEmpty()
    }

    override fun tapPile(p: Pile): Boolean {
        if (p === stock) {
            if (stock.cards.isEmpty()) {
                if (extraState >= 2 || waste.cards.isEmpty()) { sfx(Sfx.ERROR); return true }
                snapshot(); extraState++
                while (waste.cards.isNotEmpty()) stock.cards.add(waste.cards.removeAt(waste.cards.size - 1).also { it.up = false })
            } else { snapshot(); waste.cards.add(stock.cards.removeAt(stock.cards.size - 1).also { it.up = true }) }
            moves++; sfx(Sfx.CARD); status(); checkStuck()
            return true
        }
        return false
    }

    override fun canPick(p: Pile, i: Int) = i == p.cards.size - 1 && free(p)

    override fun canDrop(cards: List<Card>, from: Pile, to: Pile): Boolean {
        if (cards.size != 1) return false
        val c = cards[0]
        if (to === discard) return c.rank == 13
        if (to === from || !free(to)) return false
        return to.top!!.rank + c.rank == 13
    }

    override fun performDrop(cards: List<Card>, from: Pile, to: Pile) {
        from.cards.removeAt(from.cards.size - 1)
        discard.cards.add(cards[0])
        if (to !== discard) discard.cards.add(to.cards.removeAt(to.cards.size - 1))
    }

    override fun afterMove(from: Pile, to: Pile) { sfx(Sfx.POP); checkStuck() }

    override fun bestTarget(cards: List<Card>, from: Pile): Pile? {
        if (cards[0].rank == 13) return discard
        return (pyr + waste).firstOrNull { it !== from && canDrop(cards, from, it) }
    }

    private fun checkStuck() {
        if (isWon() || stock.cards.isNotEmpty() || extraState < 2 && waste.cards.isNotEmpty()) return
        val frees = (pyr + waste).filter { free(it) }
        val any = frees.any { a -> a.top!!.rank == 13 || frees.any { b -> a !== b && a.top!!.rank + b.top!!.rank == 13 } }
        if (!any) after(0.6f) { stopClock(); gameOver(false, "Hết nước đi", "Còn ${pyr.count { it.cards.isNotEmpty() }} lá trên kim tự tháp") }
    }

    override fun isWon() = pyr.all { it.cards.isEmpty() }
}
