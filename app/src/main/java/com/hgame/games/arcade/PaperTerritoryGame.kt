package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.Swipe
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.random.Random

class PaperTerritoryGame : Game() {
    override val realtime = true
    override val help = "Vuốt để đổi hướng. Rời lãnh thổ của bạn sẽ để lại vệt, quay về lãnh thổ để chiếm toàn bộ vùng bao quanh. " +
        "Cắt ngang vệt của đối thủ để hạ họ, nhưng đừng để ai chạm vào vệt của bạn! Chiếm 50% bản đồ để thắng."

    private val n = 60
    private val own = IntArray(n * n)      // 0 none, k = player k (1 = you)
    private val trail = IntArray(n * n)    // owner of trail on cell
    private class P(val id: Int, var r: Int, var c: Int, var dir: Int, val color: Int, var alive: Boolean = true, var turnIn: Int = 0, var respawn: Float = 0f)
    private val players = ArrayList<P>()
    private val swipe = Swipe(14f)
    private var acc = 0f; private var cell = 0f; private var started = false
    private var pending = -1
    private val dr = intArrayOf(-1, 0, 1, 0); private val dc = intArrayOf(0, 1, 0, -1)
    private var kills = 0

    override fun newGame() {
        own.fill(0); trail.fill(0); players.clear(); acc = 0f; started = false; pending = -1; kills = 0
        val colors = intArrayOf(Pal.CYAN, Pal.ORANGE, Pal.PINK, Pal.GREEN, Pal.YELLOW)
        val spots = listOf(n / 2 to n / 2, 10 to 10, 10 to n - 11, n - 11 to 10, n - 11 to n - 11)
        for (i in 0 until 5) { val p = P(i + 1, spots[i].first, spots[i].second, Random.nextInt(4), colors[i]); players.add(p); claimStart(p) }
        score(0); info("Vuốt để bắt đầu")
    }

    private fun claimStart(p: P) { for (r in p.r - 2..p.r + 2) for (c in p.c - 2..p.c + 2) if (r in 0 until n && c in 0 until n) own[r * n + c] = p.id }

    override fun layout() { cell = w / 26f }

    override fun down(x: Float, y: Float) = swipe.down(x, y)
    override fun move(x: Float, y: Float) { val d = swipe.move(x, y); if (d >= 0) steer(d) }
    override fun up(x: Float, y: Float) { val d = swipe.up(x, y); if (d >= 0) steer(d) }

    private fun steer(d: Int) {
        if (!started) { started = true }
        if (d != (players[0].dir + 2) % 4) pending = d
    }

    override fun update(dt: Float) {
        if (!started) return
        acc += dt
        while (acc > 0.085f && !over) { acc -= 0.085f; step() }
    }

    private fun botThink(p: P) {
        val home = own[p.r * n + p.c] == p.id
        val trailLen = trail.count { it == p.id }
        p.turnIn--
        var want = p.dir
        if (!home && trailLen > 14 + p.id * 3) {
            // head back to the nearest owned cell
            var best = Int.MAX_VALUE
            for (d in 0..3) {
                if (d == (p.dir + 2) % 4) continue
                val nr = p.r + dr[d] * 3; val nc = p.c + dc[d] * 3
                val dist = nearestOwn(p.id, nr, nc)
                if (dist < best) { best = dist; want = d }
            }
        } else if (p.turnIn <= 0) { want = (p.dir + if (Random.nextBoolean()) 1 else 3) % 4; p.turnIn = 4 + Random.nextInt(8) }
        // avoid walls and own trail
        val opts = listOf(want, p.dir, (p.dir + 1) % 4, (p.dir + 3) % 4).filter { it != (p.dir + 2) % 4 }
        p.dir = opts.firstOrNull { d -> val nr = p.r + dr[d]; val nc = p.c + dc[d]; nr in 0 until n && nc in 0 until n && trail[nr * n + nc] != p.id } ?: p.dir
    }

    private fun nearestOwn(id: Int, r: Int, c: Int): Int {
        var best = Int.MAX_VALUE
        var i = 0
        while (i < n * n) { if (own[i] == id) { val d = kotlin.math.abs(i / n - r) + kotlin.math.abs(i % n - c); if (d < best) best = d }; i += 3 }
        return best
    }

    private fun step() {
        if (pending >= 0) { players[0].dir = pending; pending = -1 }
        for (p in players) {
            if (!p.alive) { if (p.id != 1) { p.respawn -= 0.085f; if (p.respawn <= 0f) respawn(p) }; continue }
            if (p.id != 1) botThink(p)
            val nr = p.r + dr[p.dir]; val nc = p.c + dc[p.dir]
            if (nr !in 0 until n || nc !in 0 until n) { kill(p, null); continue }
            val tOwner = trail[nr * n + nc]
            if (tOwner == p.id) { kill(p, null); continue }
            if (tOwner != 0) players[tOwner - 1].let { if (it.alive) kill(it, p) }
            p.r = nr; p.c = nc
            val i = nr * n + nc
            if (own[i] == p.id) { if (trail.any { it == p.id }) capture(p) }
            else trail[i] = p.id
        }
        val mine = own.count { it == 1 }
        val pct = mine * 100 / (n * n)
        score(pct); info("Lãnh thổ $pct% · Hạ $kills")
        if (pct >= 50) { sfx(Sfx.WIN); gameOver(true, "Chiến thắng!", "Bạn chiếm $pct% bản đồ", pct) }
    }

    private fun respawn(p: P) {
        val r = 5 + Random.nextInt(n - 10); val c = 5 + Random.nextInt(n - 10)
        p.r = r; p.c = c; p.alive = true; p.dir = Random.nextInt(4); claimStart(p)
    }

    private fun kill(p: P, by: P?) {
        p.alive = false; p.respawn = 3f
        for (i in own.indices) { if (trail[i] == p.id) trail[i] = 0; if (own[i] == p.id) own[i] = 0 }
        if (p.id == 1) {
            sfx(Sfx.LOSE); buzz(100)
            val pct = own.count { it == 1 } * 100 / (n * n)
            gameOver(false, "Bạn đã bị hạ!", "Đạt tối đa $pct% · Hạ $kills đối thủ")
        } else { if (by?.id == 1) { kills++; sfx(Sfx.BOOM) } }
    }

    /** Converts the trail to territory and fills every region enclosed by it. */
    private fun capture(p: P) {
        for (i in trail.indices) if (trail[i] == p.id) { trail[i] = 0; own[i] = p.id }
        // flood from the border through non-owned cells; everything unreached is enclosed
        val outside = BooleanArray(n * n); val q = ArrayDeque<Int>()
        for (k in 0 until n) for (i in intArrayOf(k, (n - 1) * n + k, k * n, k * n + n - 1)) if (own[i] != p.id && !outside[i]) { outside[i] = true; q.add(i) }
        while (q.isNotEmpty()) {
            val i = q.removeFirst(); val r = i / n; val c = i % n
            for (d in 0..3) { val nr = r + dr[d]; val nc = c + dc[d]; if (nr in 0 until n && nc in 0 until n) { val j = nr * n + nc; if (!outside[j] && own[j] != p.id) { outside[j] = true; q.add(j) } } }
        }
        var gained = 0
        for (i in own.indices) if (!outside[i] && own[i] != p.id) { own[i] = p.id; gained++; if (trail[i] != 0) { players[trail[i] - 1].let { if (it.alive && it.id != p.id) kill(it, p) } } }
        // players standing inside captured land are eliminated
        for (o in players) if (o !== p && o.alive && !outside[o.r * n + o.c] && own[o.r * n + o.c] == p.id && trail.any { it == o.id }) kill(o, p)
        if (p.id == 1) sfx(Sfx.MERGE)
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        val me = players[0]
        val ox = w / 2 - (me.c + 0.5f) * cell; val oy = h / 2 - (me.r + 0.5f) * cell
        c.rect(ox, oy, ox + n * cell, oy + n * cell, 0xFFECEFF1.toInt())
        val r0 = ((-oy) / cell).toInt().coerceAtLeast(0); val r1 = ((h - oy) / cell).toInt().coerceAtMost(n - 1)
        val c0 = ((-ox) / cell).toInt().coerceAtLeast(0); val c1 = ((w - ox) / cell).toInt().coerceAtMost(n - 1)
        for (r in r0..r1) for (col in c0..c1) {
            val i = r * n + col; val x = ox + col * cell; val y = oy + r * cell
            if (own[i] != 0) c.rect(x, y, x + cell + 0.5f, y + cell + 0.5f, players[own[i] - 1].color)
            else if ((r + col) % 2 == 0) c.rect(x, y, x + cell, y + cell, 0xFFE3E7EA.toInt())
            if (trail[i] != 0) c.rect(x + cell * 0.2f, y + cell * 0.2f, x + cell * 0.8f, y + cell * 0.8f, Pal.alpha(players[trail[i] - 1].color, 0.6f))
        }
        for (p in players) if (p.alive) {
            val x = ox + (p.c + 0.5f) * cell; val y = oy + (p.r + 0.5f) * cell
            c.rrect(x - cell * 0.6f, y - cell * 0.6f + 3 * dp, x + cell * 0.6f, y + cell * 0.6f + 3 * dp, cell * 0.25f, Pal.shade(p.color, 0.5f))
            c.rrect(x - cell * 0.6f, y - cell * 0.6f, x + cell * 0.6f, y + cell * 0.6f, cell * 0.25f, Pal.tint(p.color, 0.2f))
            if (p.id == 1) c.circle(x, y, cell * 0.2f, Pal.WHITE)
        }
        if (!started) c.text("Vuốt để bắt đầu", w / 2, h * 0.3f, 20 * dp, 0xFF263238.toInt())
    }
}
