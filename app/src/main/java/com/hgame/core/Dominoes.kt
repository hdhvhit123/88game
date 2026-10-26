package com.hgame.core

import kotlin.random.Random

/** Draw dominoes with a double-six set, two players (0 = human, 1 = AI). */
class Dominoes(rnd: Random = Random.Default) {
    data class Tile(val a: Int, val b: Int) { val pips get() = a + b }

    val boneyard = ArrayList<Tile>()
    val hands = listOf(ArrayList<Tile>(), ArrayList<Tile>())
    /** Chain of tiles in order, each oriented so chain[i].b == chain[i+1].a. */
    val chain = ArrayDeque<Tile>()
    var turn = 0
    var passes = 0

    init {
        for (a in 0..6) for (b in a..6) boneyard.add(Tile(a, b))
        boneyard.shuffle(rnd)
        repeat(7) { hands[0].add(boneyard.removeAt(boneyard.size - 1)); hands[1].add(boneyard.removeAt(boneyard.size - 1)) }
        // highest double starts; otherwise player 0
        val doubles = (0..1).flatMap { p -> hands[p].filter { it.a == it.b }.map { p to it } }
        turn = doubles.maxByOrNull { it.second.a }?.first ?: 0
    }

    val leftEnd get() = chain.firstOrNull()?.a ?: -1
    val rightEnd get() = chain.lastOrNull()?.b ?: -1

    fun canPlayLeft(t: Tile) = chain.isEmpty() || t.a == leftEnd || t.b == leftEnd
    fun canPlayRight(t: Tile) = chain.isEmpty() || t.a == rightEnd || t.b == rightEnd
    fun playable(p: Int) = hands[p].filter { canPlayLeft(it) || canPlayRight(it) }

    fun play(p: Int, t: Tile, left: Boolean) {
        hands[p].remove(t)
        if (chain.isEmpty()) chain.add(t)
        else if (left) chain.addFirst(if (t.b == leftEnd) t else Tile(t.b, t.a))
        else chain.addLast(if (t.a == rightEnd) t else Tile(t.b, t.a))
        passes = 0
        turn = 1 - p
    }

    /** Draws one tile for [p]; false if the boneyard is empty. */
    fun draw(p: Int): Boolean {
        if (boneyard.isEmpty()) return false
        hands[p].add(boneyard.removeAt(boneyard.size - 1))
        return true
    }

    fun pass(p: Int) { passes++; turn = 1 - p }

    /** -1 ongoing, 0/1 winner, 2 blocked draw. */
    fun winner(): Int {
        for (p in 0..1) if (hands[p].isEmpty()) return p
        if (passes >= 2) {
            val a = hands[0].sumOf { it.pips }; val b = hands[1].sumOf { it.pips }
            return if (a < b) 0 else if (b < a) 1 else 2
        }
        return -1
    }

    /** AI: play the heaviest playable tile, preferring doubles. */
    fun aiChoice(p: Int): Pair<Tile, Boolean>? {
        val opts = playable(p)
        if (opts.isEmpty()) return null
        val t = opts.maxByOrNull { it.pips * 2 + if (it.a == it.b) 3 else 0 }!!
        return t to canPlayLeft(t).let { l -> if (l && canPlayRight(t)) Random.nextBoolean() else l }
    }
}
