package com.hgame.games.casual

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.hypot
import kotlin.random.Random

class FruitMergeGame : Game() {
    override val realtime = true
    override val help = "Kéo để chọn vị trí rồi thả tay để thả trái cây vào hộp. Hai quả giống nhau chạm vào nhau sẽ hợp thành quả lớn hơn. " +
        "Đừng để trái cây chồng quá vạch đỏ! Mục tiêu cuối cùng là quả dưa hấu khổng lồ 🍉."

    private val emoji = arrayOf("🍒", "🍓", "🍇", "🍊", "🍎", "🍐", "🍑", "🍍", "🍈", "🍉", "🎃")
    private val colors = intArrayOf(0xFFD32F2F.toInt(), 0xFFFF5252.toInt(), 0xFF8E24AA.toInt(), 0xFFFF9800.toInt(), 0xFFE53935.toInt(),
        0xFFCDDC39.toInt(), 0xFFFFAB91.toInt(), 0xFFFFD54F.toInt(), 0xFFAED581.toInt(), 0xFF43A047.toInt(), 0xFFFF6F00.toInt())
    private val radiusK = floatArrayOf(0.045f, 0.06f, 0.075f, 0.092f, 0.11f, 0.128f, 0.148f, 0.17f, 0.195f, 0.225f, 0.26f)

    private class Fruit(var x: Float, var y: Float, val lv: Int, var vx: Float = 0f, var vy: Float = 0f, var pop: Float = 0f, var dead: Boolean = false, var age: Float = 0f)
    private val fruits = ArrayList<Fruit>()
    private val fx = Particles()
    private var bl = 0f; private var br = 0f; private var bt = 0f; private var bb = 0f
    private var aimX = 0f; private var cur = 0; private var next = 0; private var cooldown = 0f; private var points = 0
    private var overT = 0f; private var aiming = false

    override fun newGame() { fruits.clear(); fx.clear(); points = 0; cur = Random.nextInt(4); next = Random.nextInt(4); cooldown = 0f; overT = 0f; score(0) }

    override fun layout() {
        val bw = minOf(w - 24 * dp, h * 0.68f)
        bl = (w - bw) / 2; br = bl + bw; bt = h * 0.2f; bb = h - 14 * dp
        if (aimX == 0f) aimX = w / 2
    }

    private fun rad(lv: Int) = radiusK[lv] * (br - bl)

    override fun down(x: Float, y: Float) { aiming = true; aimX = x.coerceIn(bl + rad(cur), br - rad(cur)) }
    override fun move(x: Float, y: Float) { aimX = x.coerceIn(bl + rad(cur), br - rad(cur)) }
    override fun up(x: Float, y: Float) {
        aiming = false
        if (over || cooldown > 0f) return
        fruits.add(Fruit(aimX, bt - rad(cur) * 0.5f, cur)); sfx(Sfx.TAP)
        cur = next; next = Random.nextInt(5); cooldown = 0.45f
        aimX = aimX.coerceIn(bl + rad(cur), br - rad(cur))
    }

    override fun update(dt: Float) {
        fx.update(dt); if (cooldown > 0) cooldown -= dt
        val g = h * 1.6f
        val sub = 4; val sdt = dt / sub
        repeat(sub) {
            for (f in fruits) {
                f.vy += g * sdt; f.x += f.vx * sdt; f.y += f.vy * sdt
                f.vx *= 0.995f
                val r = rad(f.lv)
                if (f.x < bl + r) { f.x = bl + r; f.vx = -f.vx * 0.3f }
                if (f.x > br - r) { f.x = br - r; f.vx = -f.vx * 0.3f }
                if (f.y > bb - r) { f.y = bb - r; f.vy = -f.vy * 0.2f; f.vx *= 0.9f }
            }
            // pairwise resolution
            for (i in fruits.indices) for (j in i + 1 until fruits.size) {
                val a = fruits[i]; val b = fruits[j]
                if (a.dead || b.dead) continue
                val dx = b.x - a.x; val dy = b.y - a.y; val d = hypot(dx, dy); val min = rad(a.lv) + rad(b.lv)
                if (d < min && d > 0.01f) {
                    if (a.lv == b.lv && a.lv < emoji.size - 1) { merge(a, b); continue }
                    val nx = dx / d; val ny = dy / d; val overlap = (min - d)
                    val ma = rad(a.lv) * rad(a.lv); val mb = rad(b.lv) * rad(b.lv); val tot = ma + mb
                    a.x -= nx * overlap * mb / tot; a.y -= ny * overlap * mb / tot
                    b.x += nx * overlap * ma / tot; b.y += ny * overlap * ma / tot
                    val rv = (b.vx - a.vx) * nx + (b.vy - a.vy) * ny
                    if (rv < 0) {
                        val imp = -1.2f * rv / (1 / ma + 1 / mb)
                        a.vx -= imp * nx / ma; a.vy -= imp * ny / ma; b.vx += imp * nx / mb; b.vy += imp * ny / mb
                    }
                }
            }
            fruits.removeAll { it.dead }
        }
        for (f in fruits) { f.age += dt; if (f.pop > 0) f.pop -= dt * 4 }
        val high = fruits.any { it.age > 1.2f && it.y - rad(it.lv) < bt + 4 * dp }
        overT = if (high) overT + dt else 0f
        if (overT > 2f) { sfx(Sfx.LOSE); gameOver(false, "Hộp đầy rồi!", "Bạn đạt $points điểm", points) }
    }

    private fun merge(a: Fruit, b: Fruit) {
        a.dead = true; b.dead = true
        val lv = a.lv + 1
        val nf = Fruit((a.x + b.x) / 2, (a.y + b.y) / 2, lv, (a.vx + b.vx) / 2, (a.vy + b.vy) / 2 - 60 * dp, 1f, age = 2f)
        fruits.add(nf)
        points += (lv + 1) * (lv + 2) / 2; score(points)
        fx.burst(nf.x, nf.y, colors[lv], 14, 200 * dp, 4 * dp)
        sfx(if (lv >= 8) Sfx.WIN else Sfx.MERGE); buzz(15)
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFFFFF3E0.toInt(), 0xFFFFE0B2.toInt())
        c.rrect(bl - 8 * dp, bt - 20 * dp, br + 8 * dp, bb + 8 * dp, 16 * dp, 0xFFD7A86E.toInt())
        c.rect(bl, bt - 20 * dp, br, bb, 0xFFFFF8E1.toInt())
        c.line(bl, bt, br, bt, 2 * dp, if (overT > 0f && (overT * 6).toInt() % 2 == 0) Pal.RED else Pal.alpha(Pal.RED, 0.5f))
        for (f in fruits) {
            val r = rad(f.lv) * (1f + f.pop * 0.15f)
            c.circle(f.x, f.y, r, colors[f.lv])
            c.circle(f.x - r * 0.3f, f.y - r * 0.35f, r * 0.25f, Pal.alpha(Pal.WHITE, 0.4f))
            c.text(emoji[f.lv], f.x, f.y, r * 1.25f, Pal.WHITE)
        }
        // aim
        val r = rad(cur)
        if (cooldown <= 0f && !over) {
            for (yy in generateSequence(bt) { it + 12 * dp }.takeWhile { it < bb }) c.rect(aimX - 1 * dp, yy, aimX + 1 * dp, yy + 6 * dp, Pal.alpha(0xFF8D6E63.toInt(), 0.4f))
            c.circle(aimX, bt - r - 22 * dp, r, colors[cur]); c.text(emoji[cur], aimX, bt - r - 22 * dp, r * 1.25f, Pal.WHITE)
        }
        c.text("Tiếp:", w - 70 * dp, 22 * dp, 13 * dp, 0xFF6D4C41.toInt())
        c.circle(w - 32 * dp, 22 * dp, 15 * dp, colors[next]); c.text(emoji[next], w - 32 * dp, 22 * dp, 18 * dp, Pal.WHITE)
        c.text("$points", 20 * dp, 24 * dp, 24 * dp, 0xFF6D4C41.toInt(), android.graphics.Paint.Align.LEFT)
        if (fruits.isEmpty()) c.text("Kéo và thả để rơi trái cây", w / 2, (bt + bb) / 2, 16 * dp, 0xFF8D6E63.toInt())
    }
}
