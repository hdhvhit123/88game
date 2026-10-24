package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.Swipe
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.random.Random

class CrossRoadGame : Game() {
    override val realtime = true
    override val help = "Chạm để nhảy về phía trước, vuốt trái/phải/xuống để né. Băng qua đường tránh xe, nhảy lên khúc gỗ để qua sông " +
        "(rơi xuống nước là thua). Đừng đứng yên quá lâu, đại bàng sẽ bắt bạn!"

    private class Lane(val type: Int, val speed: Float, val items: ArrayList<Float>, val len: Float, val trees: Set<Int>) // 0 grass,1 road,2 river
    private val lanes = HashMap<Int, Lane>()
    private val cols = 9
    private var pc = 4f; private var pr = 0; private var hop = 0f; private var hopFrom = 0f; private var hopFromR = 0
    private var cam = 0f; private var best = 0; private var idle = 0f; private var t = 0f
    private val swipe = Swipe(18f)
    private var cell = 0f

    override fun newGame() { lanes.clear(); pc = 4f; pr = 0; hop = 0f; cam = 0f; best = 0; idle = 0f; score(0) }

    override fun layout() { cell = w / cols }

    private fun lane(r: Int): Lane = lanes.getOrPut(r) {
        if (r < 3) return@getOrPut Lane(0, 0f, arrayListOf(), 0f, if (r < 0) (0 until cols).toSet() else emptySet())
        val diff = (r / 60f).coerceAtMost(1f)
        val roll = Random.nextFloat()
        val type = when { roll < 0.3f -> 0; roll < 0.7f -> 1; else -> 2 }
        val prevRiverLanes = (1..2).count { lanes[r - it]?.type == 2 }
        val tp = if (type == 2 && prevRiverLanes >= 2) 1 else type
        when (tp) {
            0 -> Lane(0, 0f, arrayListOf(), 0f, (0 until cols).filter { it != 4 && Random.nextFloat() < 0.25f }.toSet())
            1 -> {
                val sp = (1.2f + Random.nextFloat() * 1.8f + diff * 2f) * (if (Random.nextBoolean()) 1 else -1)
                val len = if (Random.nextFloat() < 0.2f) 2.2f else 1.2f
                val items = arrayListOf<Float>(); var x = Random.nextFloat() * 4
                while (x < cols + 4) { items.add(x); x += len + 2.5f + Random.nextFloat() * 4 }
                Lane(1, sp, items, len, emptySet())
            }
            else -> {
                val sp = (0.8f + Random.nextFloat() * 1.2f + diff) * (if (r % 2 == 0) 1 else -1)
                val len = 2f + Random.nextInt(3)
                val items = arrayListOf<Float>(); var x = Random.nextFloat() * 2
                while (x < cols + 6) { items.add(x); x += len + 1.2f + Random.nextFloat() * 1.6f }
                Lane(2, sp, items, len, emptySet())
            }
        }
    }

    override fun down(x: Float, y: Float) = swipe.down(x, y)
    override fun up(x: Float, y: Float) {
        if (over || hop > 0f) return
        val d = swipe.up(x, y)
        val (dr, dc) = when (d) { 1 -> 0 to 1; 2 -> -1 to 0; 3 -> 0 to -1; else -> 1 to 0 }
        val nr = pr + dr; val nc = Math.round(pc) + dc
        if (nc < 0 || nc >= cols) return
        if (lane(nr).type == 0 && nc in lane(nr).trees) return
        hopFrom = pc; hopFromR = pr; pr = nr; pc = nc.toFloat(); hop = 1f; idle = 0f
        sfx(Sfx.JUMP)
        if (pr > best) { best = pr; score(best) }
    }

    private fun wrap(x: Float, total: Float) = ((x % total) + total) % total

    override fun update(dt: Float) {
        t += dt
        if (hop > 0f) hop = (hop - dt * 7f).coerceAtLeast(0f)
        val total = cols + 6f
        for (r in pr - 6..pr + 14) {
            val l = lane(r)
            if (l.type == 0) continue
            for (i in l.items.indices) l.items[i] = wrap(l.items[i] + l.speed * dt, total)
        }
        val target = (pr - 3).toFloat()
        cam += (target - cam) * (dt * 4f).coerceAtMost(1f)
        idle += dt
        val cur = lane(pr)
        if (hop == 0f) {
            if (cur.type == 2) {
                val log = cur.items.firstOrNull { val s = it - 3f; pc + 0.5f > s + 0.1f && pc + 0.5f < s + cur.len - 0.1f }
                if (log == null) return die("Rơi xuống sông!")
                pc += cur.speed * dt
                if (pc < -0.5f || pc > cols - 0.5f) return die("Trôi ra khỏi màn hình!")
            }
        }
        if (cur.type == 1 && hop < 0.5f) {
            for (car in cur.items) { val s = car - 3f; if (pc + 0.8f > s && pc + 0.2f < s + cur.len) return die("Bị xe tông!") }
        }
        if (idle > 7f || pr < cam - 1.5f) die("Đại bàng bắt mất rồi!")
    }

    private fun die(msg: String) { sfx(Sfx.LOSE); buzz(80); gameOver(false, msg, "Bạn đi được $best bước", best) }

    private fun rowY(r: Int) = h - (r - cam + 1.5f) * cell

    override fun draw(c: Canvas) {
        c.rect(0f, 0f, w, h, 0xFF7CB342.toInt())
        val first = (cam - 2).toInt(); val last = first + (h / cell).toInt() + 4
        for (r in first..last) {
            val l = lane(r); val y = rowY(r)
            when (l.type) {
                0 -> {
                    c.rect(0f, y, w, y + cell, if (r % 2 == 0) 0xFF8BC34A.toInt() else 0xFF7CB342.toInt())
                    for (tc in l.trees) {
                        val x = tc * cell
                        c.rect(x + cell * 0.42f, y + cell * 0.6f, x + cell * 0.58f, y + cell * 0.9f, 0xFF6D4C41.toInt())
                        c.circle(x + cell / 2, y + cell * 0.42f, cell * 0.36f, 0xFF2E7D32.toInt())
                        c.circle(x + cell * 0.42f, y + cell * 0.35f, cell * 0.14f, 0xFF43A047.toInt())
                    }
                }
                1 -> {
                    c.rect(0f, y, w, y + cell, 0xFF455A64.toInt())
                    var x = 0f; while (x < w) { c.rect(x, y + cell * 0.48f, x + cell * 0.5f, y + cell * 0.52f, Pal.alpha(Pal.WHITE, 0.5f)); x += cell }
                    l.items.forEachIndexed { i, car ->
                        val sx = (car - 3f) * cell; val ln = l.len * cell
                        val col = Pal.VIVID[(r * 3 + i) % 8]
                        c.rrect(sx, y + cell * 0.15f, sx + ln, y + cell * 0.85f, cell * 0.2f, col)
                        c.rrect(sx + ln * 0.25f, y + cell * 0.25f, sx + ln * 0.75f, y + cell * 0.75f, cell * 0.12f, Pal.alpha(0xFFB3E5FC.toInt(), 0.9f))
                        val fx = if (l.speed > 0) sx + ln - cell * 0.08f else sx + cell * 0.08f
                        c.circle(fx, y + cell * 0.3f, cell * 0.06f, Pal.YELLOW); c.circle(fx, y + cell * 0.7f, cell * 0.06f, Pal.YELLOW)
                    }
                }
                2 -> {
                    c.rect(0f, y, w, y + cell, 0xFF29B6F6.toInt())
                    c.rect(0f, y + cell * 0.3f + kotlin.math.sin(t * 2 + r) * 2 * dp, w, y + cell * 0.33f + kotlin.math.sin(t * 2 + r) * 2 * dp, Pal.alpha(Pal.WHITE, 0.3f))
                    for (log in l.items) {
                        val sx = (log - 3f) * cell
                        c.rrect(sx, y + cell * 0.15f, sx + l.len * cell, y + cell * 0.85f, cell * 0.3f, 0xFF8D6E63.toInt())
                        c.rrect(sx + cell * 0.2f, y + cell * 0.35f, sx + l.len * cell - cell * 0.2f, y + cell * 0.42f, cell * 0.05f, 0xFF6D4C41.toInt())
                    }
                }
            }
        }
        // player (chicken)
        val k = 1f - hop
        val drawC = hopFrom + (pc - hopFrom) * k
        val drawR = hopFromR + (pr - hopFromR) * k
        val x = drawC * cell + cell / 2
        val y = h - (drawR - cam + 1.5f) * cell + cell / 2 - kotlin.math.sin(k * Math.PI).toFloat() * cell * 0.35f * (if (hop > 0) 1 else 0)
        c.circle(x, y + cell * 0.38f, cell * 0.28f, Pal.alpha(Pal.BLACK, 0.2f))
        c.rrect(x - cell * 0.28f, y - cell * 0.3f, x + cell * 0.28f, y + cell * 0.32f, cell * 0.12f, Pal.WHITE)
        c.rect(x - cell * 0.06f, y - cell * 0.42f, x + cell * 0.06f, y - cell * 0.3f, Pal.RED)
        c.rect(x - cell * 0.07f, y - cell * 0.05f, x + cell * 0.07f, y + cell * 0.05f, Pal.ORANGE)
        c.circle(x - cell * 0.13f, y - cell * 0.15f, cell * 0.05f, Pal.BLACK); c.circle(x + cell * 0.13f, y - cell * 0.15f, cell * 0.05f, Pal.BLACK)
        if (idle > 4.5f && !over) c.text("Nhanh lên! 🦅", w / 2, 40 * dp, 18 * dp, Pal.RED)
        c.text("$best", w - 20 * dp, 30 * dp, 26 * dp, Pal.WHITE, android.graphics.Paint.Align.RIGHT)
        if (best == 0 && !over && idle < 4.5f) c.text("Chạm để nhảy tới", w / 2, h * 0.3f, 18 * dp, Pal.WHITE)
    }
}
