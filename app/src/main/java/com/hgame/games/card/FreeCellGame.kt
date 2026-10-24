package com.hgame.games.card

class FreeCellGame : SolitaireGame() {
    override val help = "Mọi lá bài đều ngửa. Xếp 52 lá lên 4 ô nền theo chất từ A đến K. 4 ô trống bên trái giữ tạm mỗi ô 1 lá. " +
        "Ở 8 cột xếp giảm dần xen kẽ đỏ–đen. Số lá di chuyển cùng lúc phụ thuộc số ô trống và cột trống."

    override fun deal() {
        repeat(4) { piles.add(Pile(Kind.CELL, it)) }
        repeat(4) { piles.add(Pile(Kind.FOUNDATION, it)) }
        val cols = List(8) { Pile(Kind.TABLEAU, it) }
        val deck = Cards.deck()
        deck.forEachIndexed { i, c -> c.up = true; cols[i % 8].cards.add(c) }
        piles.addAll(cols)
    }

    override fun layoutPiles() {
        val gap = w * 0.012f
        cw = (w - gap * 9) / 8; ch = cw * 1.42f
        val top = 10 * dp
        for (p in piles) when (p.kind) {
            Kind.CELL -> { p.x = gap + p.index * (cw + gap); p.y = top }
            Kind.FOUNDATION -> { p.x = gap + (p.index + 4) * (cw + gap); p.y = top }
            else -> { p.x = gap + p.index * (cw + gap); p.y = top + ch + 16 * dp; p.maxBottom = bottomLimit() }
        }
    }

    override fun slotLabel(p: Pile) = when (p.kind) { Kind.FOUNDATION -> "A"; Kind.CELL -> "◇"; else -> "" }

    private fun isRun(cards: List<Card>) = cards.zipWithNext().all { (a, b) -> a.red != b.red && a.rank == b.rank + 1 }

    private fun capacity(to: Pile): Int {
        val free = piles.count { it.kind == Kind.CELL && it.cards.isEmpty() }
        val empty = piles.count { it.kind == Kind.TABLEAU && it.cards.isEmpty() && it !== to }
        return (free + 1) shl empty
    }

    override fun canPick(p: Pile, i: Int): Boolean = when (p.kind) {
        Kind.CELL, Kind.FOUNDATION -> i == p.cards.size - 1
        Kind.TABLEAU -> isRun(p.cards.subList(i, p.cards.size))
        else -> false
    }

    override fun canDrop(cards: List<Card>, from: Pile, to: Pile): Boolean {
        val c = cards[0]
        return when (to.kind) {
            Kind.CELL -> cards.size == 1 && to.cards.isEmpty()
            Kind.FOUNDATION -> cards.size == 1 && (to.top?.let { it.suit == c.suit && it.rank == c.rank - 1 } ?: (c.rank == 1))
            Kind.TABLEAU -> cards.size <= capacity(to) && (to.top?.let { it.red != c.red && it.rank == c.rank + 1 } ?: true)
            else -> false
        }
    }

    override fun isWon() = piles.filter { it.kind == Kind.FOUNDATION }.sumOf { it.cards.size } == 52

    override fun canAutoFinish() = piles.filter { it.kind == Kind.TABLEAU }.all { p -> p.cards.zipWithNext().all { (a, b) -> a.rank > b.rank } }
}
