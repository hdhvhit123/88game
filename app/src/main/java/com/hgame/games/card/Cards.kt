package com.hgame.games.card

import android.graphics.Canvas
import com.hgame.engine.Pal
import com.hgame.engine.circle
import com.hgame.engine.rrect
import com.hgame.engine.rrectStroke
import com.hgame.engine.text
import kotlin.random.Random

/** A playing card. rank 1 (ace) .. 13 (king); suit 0 ♠, 1 ♥, 2 ♦, 3 ♣. */
class Card(val rank: Int, val suit: Int, var up: Boolean = false) {
    val red get() = suit == 1 || suit == 2
    // animated draw position
    var x = Float.NaN; var y = Float.NaN
    override fun toString() = Cards.RANKS[rank] + Cards.SUITS[suit]
}

object Cards {
    val SUITS = arrayOf("♠", "♥", "♦", "♣")
    val RANKS = arrayOf("", "A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K")

    fun deck(suits: IntArray = intArrayOf(0, 1, 2, 3), copies: Int = 1, rnd: Random = Random.Default): ArrayList<Card> {
        val list = ArrayList<Card>()
        repeat(copies) { for (s in suits) for (r in 1..13) list.add(Card(r, s)) }
        list.shuffle(rnd)
        return list
    }

    private const val RED = 0xFFD32F2F.toInt()
    private const val BLACK = 0xFF1C1C28.toInt()

    fun draw(c: Canvas, card: Card, x: Float, y: Float, w: Float, h: Float, highlight: Boolean = false, dim: Boolean = false) {
        val rad = w * 0.1f
        c.rrect(x, y + w * 0.03f, x + w, y + h + w * 0.03f, rad, 0x55000000)
        if (!card.up) { back(c, x, y, w, h); return }
        c.rrect(x, y, x + w, y + h, rad, if (dim) 0xFFCFCFD8.toInt() else 0xFFFDFDFD.toInt())
        c.rrectStroke(x, y, x + w, y + h, rad, 1f, 0xFFBDBDC8.toInt())
        val col = if (card.red) RED else BLACK
        val rs = w * 0.3f
        c.text(RANKS[card.rank], x + w * 0.05f, y + rs * 0.62f, rs, col, android.graphics.Paint.Align.LEFT)
        c.text(SUITS[card.suit], x + w * 0.95f, y + rs * 0.62f, rs * 0.85f, col, android.graphics.Paint.Align.RIGHT)
        if (h > w * 1.1f) {
            if (card.rank > 10) {
                c.rrect(x + w * 0.18f, y + h * 0.36f, x + w * 0.82f, y + h * 0.92f, w * 0.08f, if (card.red) 0xFFFFEBEE.toInt() else 0xFFE8EAF6.toInt())
                c.text(RANKS[card.rank], x + w / 2, y + h * 0.58f, w * 0.38f, col)
                c.text(SUITS[card.suit], x + w / 2, y + h * 0.8f, w * 0.26f, col)
            } else c.text(SUITS[card.suit], x + w / 2, y + h * 0.64f, w * 0.55f, col)
        }
        if (highlight) c.rrectStroke(x, y, x + w, y + h, rad, w * 0.06f, Pal.YELLOW)
    }

    fun back(c: Canvas, x: Float, y: Float, w: Float, h: Float) {
        val rad = w * 0.1f
        c.rrect(x, y, x + w, y + h, rad, 0xFFFDFDFD.toInt())
        c.rrect(x + w * 0.06f, y + w * 0.06f, x + w * 0.94f, y + h - w * 0.06f, rad * 0.7f, 0xFF3949AB.toInt())
        val step = w * 0.22f
        var yy = y + w * 0.2f
        var row = 0
        while (yy < y + h - w * 0.12f) {
            var xx = x + w * 0.2f + (row % 2) * step / 2
            while (xx < x + w * 0.85f) { c.circle(xx, yy, w * 0.04f, 0xFF5C6BC0.toInt()); xx += step }
            yy += step * 0.8f; row++
        }
    }

    fun slot(c: Canvas, x: Float, y: Float, w: Float, h: Float, label: String = "") {
        c.rrect(x, y, x + w, y + h, w * 0.1f, 0x22FFFFFF)
        c.rrectStroke(x, y, x + w, y + h, w * 0.1f, 1.5f, 0x44FFFFFF)
        if (label.isNotEmpty()) c.text(label, x + w / 2, y + h / 2, w * 0.4f, 0x66FFFFFF)
    }
}
