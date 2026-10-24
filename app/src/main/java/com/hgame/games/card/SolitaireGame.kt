package com.hgame.games.card

import android.graphics.Canvas
import com.hgame.engine.BestMode
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.rrectStroke
import kotlin.math.abs

/**
 * Shared machinery for patience games: piles, card fanning, drag & drop, tap-to-move,
 * undo snapshots and smooth card motion.
 */
abstract class SolitaireGame : Game() {
    override val bestMode = BestMode.LOW

    enum class Kind { STOCK, WASTE, FOUNDATION, TABLEAU, CELL }

    inner class Pile(val kind: Kind, val index: Int) {
        val cards = ArrayList<Card>()
        var x = 0f; var y = 0f
        var fan = kind == Kind.TABLEAU
        var fanX = false          // waste in draw-3 fans horizontally
        var maxBottom = 0f
        var showSlot = true
        val top get() = cards.lastOrNull()
        /** y offset of card [i] in this pile. */
        fun offsets(): FloatArray {
            val out = FloatArray(cards.size)
            if (!fan && !fanX) return out
            if (fanX) { val n = cards.size; for (i in cards.indices) out[i] = maxOf(0, i - (n - 3)) * cw * 0.32f; return out }
            var down = ch * 0.12f; var up = ch * 0.26f
            var total = 0f
            for (i in 0 until cards.size - 1) total += if (cards[i].up) up else down
            val avail = maxBottom - y - ch
            if (total > avail && total > 0) { val k = avail / total; down *= k; up *= k }
            var acc = 0f
            for (i in cards.indices) { out[i] = acc; acc += if (cards[i].up) up else down }
            return out
        }
    }

    val piles = ArrayList<Pile>()
    var cw = 0f; var ch = 0f
    protected var moves = 0
    private val undoStack = ArrayList<Snapshot>()
    private class Snapshot(val piles: List<List<Pair<Card, Boolean>>>, val moves: Int, val extra: Int)
    private var drag: List<Card>? = null; private var dragFrom: Pile? = null
    private var dragDX = 0f; private var dragDY = 0f; private var dragX = 0f; private var dragY = 0f
    private var downX = 0f; private var downY = 0f; private var moved = false
    private var pressPile: Pile? = null; private var pressIndex = -1
    protected val undoBtn = Btn("↶ Hoàn tác", Pal.SURFACE2)
    protected val hintBtn = Btn("💡 Gợi ý", Pal.SURFACE2)
    private var hint: Pair<Card, Pile>? = null; private var hintT = 0f
    protected var autoFinishing = false

    abstract fun deal()
    abstract fun layoutPiles()
    abstract fun canPick(p: Pile, i: Int): Boolean
    abstract fun canDrop(cards: List<Card>, from: Pile, to: Pile): Boolean
    abstract fun isWon(): Boolean
    /** Taps on a pile that are not card moves (e.g. the stock). Return true if handled. */
    open fun tapPile(p: Pile): Boolean = false
    /** Extra integer state saved with undo (e.g. stock passes). */
    open var extraState: Int = 0
    open fun afterMove(from: Pile, to: Pile) { flipTops() }
    open fun performDrop(cards: List<Card>, from: Pile, to: Pile) {
        repeat(cards.size) { from.cards.removeAt(from.cards.size - 1) }
        to.cards.addAll(cards)
    }

    protected fun flipTops() { for (p in piles) if (p.kind == Kind.TABLEAU) p.top?.let { if (!it.up) { it.up = true } } }

    override fun newGame() {
        piles.clear(); undoStack.clear(); moves = 0; drag = null; hint = null; autoFinishing = false
        deal()
        for (p in piles) for (c in p.cards) { c.x = Float.NaN; c.y = Float.NaN }
        startClock(); status()
    }

    open fun status() = info("Nước đi: $moves")

    override fun onClock(seconds: Int) = score(seconds)

    override fun layout() {
        layoutPiles()
        val by = h - 30 * dp
        undoBtn.setCenter(w * 0.3f, by, w * 0.36f, 40 * dp); hintBtn.setCenter(w * 0.7f, by, w * 0.36f, 40 * dp)
        animate(0.5f)
    }

    protected fun bottomLimit() = h - 60 * dp

    fun pos(p: Pile, i: Int, offs: FloatArray = p.offsets()): Pair<Float, Float> =
        if (p.fanX) (p.x + offs[i]) to p.y else p.x to (p.y + offs[i])

    protected fun snapshot() {
        undoStack.add(Snapshot(piles.map { p -> p.cards.map { it to it.up } }, moves, extraState))
        if (undoStack.size > 200) undoStack.removeAt(0)
    }

    private fun undo() {
        val s = undoStack.removeLastOrNull() ?: run { sfx(Sfx.ERROR); return }
        piles.forEachIndexed { i, p -> p.cards.clear(); for ((c, up) in s.piles[i]) { c.up = up; p.cards.add(c) } }
        moves = s.moves; extraState = s.extra
        if (over) resumePlay()
        sfx(Sfx.CARD); status(); animate(0.5f)
    }

    /** Executes a move with undo and bookkeeping. */
    fun doMove(cards: List<Card>, from: Pile, to: Pile) {
        snapshot()
        performDrop(cards, from, to)
        moves++
        afterMove(from, to)
        sfx(Sfx.CARD); status(); animate(0.5f)
        checkWin()
    }

    protected fun checkWin() {
        if (isWon()) { stopClock(); sfx(Sfx.WIN); after(0.6f) { gameOver(true, "Chiến thắng!", "$moves nước · ${clockSeconds}s", clockSeconds) } }
        else if (!autoFinishing && canAutoFinish()) { autoFinishing = true; autoStep() }
    }

    open fun canAutoFinish(): Boolean = false

    private fun autoStep() {
        after(0.12f) {
            for (p in piles) {
                if (p.kind == Kind.FOUNDATION) continue
                val c = p.top ?: continue
                val f = piles.firstOrNull { it.kind == Kind.FOUNDATION && canDrop(listOf(c), p, it) } ?: continue
                p.cards.removeAt(p.cards.size - 1); f.cards.add(c); moves++
                sfx(Sfx.CARD); animate(0.4f); status()
                if (isWon()) { checkWin(); return@after }
                autoStep(); return@after
            }
            autoFinishing = false
        }
    }

    /** Index of the card under (x, y) in pile [p], or -1. */
    private fun hitCard(p: Pile, x: Float, y: Float): Int {
        val offs = p.offsets()
        for (i in p.cards.indices.reversed()) {
            val (cx, cy) = pos(p, i, offs)
            if (x in cx..cx + cw && y in cy..cy + ch) return i
        }
        return if (p.showSlot && x in p.x..p.x + cw && y in p.y..p.y + ch) -2 else -1   // -2 = empty slot
    }

    override fun down(x: Float, y: Float) {
        if (over || autoFinishing) return
        downX = x; downY = y; moved = false; pressPile = null
        if (undoBtn.hit(x, y) || hintBtn.hit(x, y)) return
        for (p in piles.reversed()) {
            val i = hitCard(p, x, y)
            if (i == -1) continue
            pressPile = p; pressIndex = i
            if (i >= 0 && canPick(p, i)) {
                val cards = p.cards.subList(i, p.cards.size).toList()
                val (cx, cy) = pos(p, i)
                drag = cards; dragFrom = p; dragDX = x - cx; dragDY = y - cy; dragX = x; dragY = y
            }
            return
        }
    }

    override fun move(x: Float, y: Float) {
        if (abs(x - downX) + abs(y - downY) > 10 * dp) moved = true
        if (drag != null) { dragX = x; dragY = y; redraw() }
    }

    override fun up(x: Float, y: Float) {
        val cards = drag; val from = dragFrom
        drag = null
        if (over || autoFinishing) return
        if (!moved) {
            if (undoBtn.hit(x, y)) { undo(); return }
            if (hintBtn.hit(x, y)) { showHint(); return }
            val p = pressPile ?: return
            if (tapPile(p)) { animate(0.5f); return }
            if (cards != null && from != null) {
                val dest = bestTarget(cards, from)
                if (dest != null) doMove(cards, from, dest) else { sfx(Sfx.ERROR); animate(0.3f) }
            }
            return
        }
        if (cards == null || from == null) return
        val cx = x - dragDX; val cy = y - dragDY
        var best: Pile? = null; var bestOverlap = 0f
        for (p in piles) {
            if (p === from) continue
            val (px, py) = if (p.cards.isEmpty()) p.x to p.y else pos(p, p.cards.size - 1)
            val ox = (minOf(cx + cw, px + cw) - maxOf(cx, px)).coerceAtLeast(0f)
            val oy = (minOf(cy + ch, py + ch) - maxOf(cy, py)).coerceAtLeast(0f)
            val ov = ox * oy
            if (ov > bestOverlap && canDrop(cards, from, p)) { bestOverlap = ov; best = p }
        }
        if (best != null) doMove(cards, from, best) else { sfx(Sfx.ERROR); animate(0.4f) }
    }

    /** Preferred destination for a tapped card: foundation first, then a non-empty tableau, then an empty one. */
    open fun bestTarget(cards: List<Card>, from: Pile): Pile? {
        if (cards.size == 1) piles.firstOrNull { it.kind == Kind.FOUNDATION && it !== from && canDrop(cards, from, it) }?.let { return it }
        piles.firstOrNull { it.kind == Kind.TABLEAU && it !== from && it.cards.isNotEmpty() && canDrop(cards, from, it) }?.let { return it }
        piles.firstOrNull { it.kind == Kind.TABLEAU && it !== from && it.cards.isEmpty() && canDrop(cards, from, it) }?.let { return it }
        return piles.firstOrNull { it.kind == Kind.CELL && it !== from && canDrop(cards, from, it) }
    }

    private fun showHint() {
        for (p in piles) {
            if (p.kind == Kind.FOUNDATION) continue
            for (i in p.cards.indices) {
                if (!canPick(p, i)) continue
                val cards = p.cards.subList(i, p.cards.size).toList()
                for (t in piles) {
                    if (t === p || !canDrop(cards, p, t)) continue
                    // skip pointless moves: a whole column to an empty column, or king-led stacks off an empty spot
                    if (t.cards.isEmpty() && i == 0 && t.kind == Kind.TABLEAU) continue
                    if (t.kind == Kind.CELL) continue
                    hint = cards[0] to t; hintT = 1.5f; animate(1.5f); sfx(Sfx.CLICK); return
                }
            }
        }
        info(if (piles.any { it.kind == Kind.STOCK && it.cards.isNotEmpty() }) "Hãy rút thêm bài từ chồng bài" else "Không tìm thấy nước đi")
    }

    override fun update(dt: Float) {
        if (hintT > 0) hintT -= dt
        val k = (dt * 14f).coerceAtMost(1f)
        var moving = false
        for (p in piles) {
            val offs = p.offsets()
            p.cards.forEachIndexed { i, c ->
                val (tx, ty) = pos(p, i, offs)
                if (c.x.isNaN()) { c.x = tx; c.y = ty }
                c.x += (tx - c.x) * k; c.y += (ty - c.y) * k
                if (abs(tx - c.x) + abs(ty - c.y) > 0.5f) moving = true else { c.x = tx; c.y = ty }
            }
        }
        if (moving) animate(0.1f)
    }

    open fun drawExtras(c: Canvas) {}
    open fun slotLabel(p: Pile): String = when (p.kind) { Kind.FOUNDATION -> "A"; Kind.STOCK -> "↻"; else -> "" }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF1B6B4A.toInt(), 0xFF0E3F2B.toInt())
        for (p in piles) if (p.showSlot) Cards.slot(c, p.x, p.y, cw, ch, slotLabel(p))
        val dragging = drag
        val offsCache = HashMap<Pile, FloatArray>()
        for (p in piles) {
            val offs = offsCache.getOrPut(p) { p.offsets() }
            for ((i, card) in p.cards.withIndex()) {
                if (dragging != null && card in dragging) continue
                val (tx, ty) = pos(p, i, offs)
                val x = if (card.x.isNaN()) tx else card.x; val y = if (card.y.isNaN()) ty else card.y
                val hl = hintT > 0f && hint?.first === card
                Cards.draw(c, card, x, y, cw, ch, hl)
            }
            if (hintT > 0f && hint?.second === p) {
                val (hx, hy) = if (p.cards.isEmpty()) p.x to p.y else pos(p, p.cards.size - 1, offs)
                c.rrectStroke(hx - 3, hy - 3, hx + cw + 3, hy + ch + 3, cw * 0.12f, 3 * dp, Pal.YELLOW)
            }
        }
        drawExtras(c)
        if (dragging != null) {
            val step = if (dragging.size > 1) ch * 0.26f else 0f
            dragging.forEachIndexed { i, card ->
                val x = dragX - dragDX; val y = dragY - dragDY + i * step
                card.x = x; card.y = y
                Cards.draw(c, card, x, y, cw, ch)
            }
        }
        undoBtn.enabled = undoStack.isNotEmpty()
        undoBtn.draw(c, 14 * dp); hintBtn.draw(c, 14 * dp)
    }
}
