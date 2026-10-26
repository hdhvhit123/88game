package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Particles
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.ring
import com.hgame.engine.rrect
import com.hgame.engine.rrectStroke
import com.hgame.engine.text
import kotlin.math.hypot

class TowerDefenseGame : Game() {
    override val realtime = true
    override val help = "Quái vật đi theo con đường đến căn cứ của bạn. Chọn loại tháp ở dưới rồi chạm vào ô cỏ để xây. " +
        "Chạm vào tháp đã xây để nâng cấp. Tiêu diệt quái được vàng. Để lọt 20 con là thua. Bấm ▶ để gọi đợt tiếp theo sớm."

    private val cols = 9; private val rows = 13
    private val path = listOf(0 to 2, 3 to 2, 3 to 6, 1 to 6, 1 to 10, 6 to 10, 6 to 7, 8 to 7, 8 to 3, 5 to 3, 5 to 0)
    private val pathCells = HashSet<Int>()
    private val waypoints = ArrayList<Pair<Float, Float>>()
    private class Tower(val c: Int, val r: Int, val type: Int, var level: Int = 1, var cd: Float = 0f)
    private class Enemy(var d: Float, var hp: Float, val maxHp: Float, val speed: Float, val kind: Int, var slow: Float = 0f, val reward: Int)
    private class Shot(var x: Float, var y: Float, val target: Enemy, val dmg: Float, val type: Int)
    private val towers = ArrayList<Tower>(); private val enemies = ArrayList<Enemy>(); private val shots = ArrayList<Shot>()
    private val fx = Particles()
    private val fit = BoardFit()
    private var gold = 120; private var lives = 20; private var wave = 0; private var toSpawn = 0; private var spawnT = 0f; private var between = 3f
    private var selType = 0; private var selTower: Tower? = null
    private val typeNames = arrayOf("Cung", "Pháo", "Băng")
    private val typeCost = intArrayOf(50, 80, 60)
    private val typeColor = intArrayOf(Pal.GREEN, Pal.ORANGE, Pal.CYAN)
    private val typeBtns = List(3) { Btn("", typeColor[it]) }
    private val waveBtn = Btn("▶", Pal.ACCENT)
    private var totalLen = 0f; private var kills = 0

    override fun newGame() {
        towers.clear(); enemies.clear(); shots.clear(); fx.clear()
        gold = 120; lives = 20; wave = 0; toSpawn = 0; between = 4f; selTower = null; kills = 0
        score(0); status()
    }

    private fun status() = info("💰 $gold · ❤ $lives · Đợt $wave")

    override fun layout() {
        fit.fit(cols, rows, 4 * dp, 4 * dp, w - 4 * dp, h - 80 * dp)
        pathCells.clear(); waypoints.clear()
        for (i in 0 until path.size - 1) {
            val (c1, r1) = path[i]; val (c2, r2) = path[i + 1]
            val dc = Integer.signum(c2 - c1); val dr = Integer.signum(r2 - r1)
            var c = c1; var r = r1
            while (true) { pathCells.add(r * cols + c); if (c == c2 && r == r2) break; c += dc; r += dr }
        }
        waypoints.add((fit.x0 - fit.cell) to fit.cy(path[0].second))
        for ((c, r) in path) waypoints.add(fit.cx(c) to fit.cy(r))
        waypoints.add(fit.cx(path.last().first) to (fit.y0 - fit.cell * 0.2f))   // the base sits at the top edge
        totalLen = 0f; for (i in 1 until waypoints.size) totalLen += dist(waypoints[i - 1], waypoints[i])
        val bw = (w - 5 * 8 * dp) / 4
        typeBtns.forEachIndexed { i, b -> b.set(8 * dp + i * (bw + 8 * dp), h - 70 * dp, 8 * dp + i * (bw + 8 * dp) + bw, h - 12 * dp) }
        waveBtn.set(8 * dp + 3 * (bw + 8 * dp), h - 70 * dp, w - 8 * dp, h - 12 * dp)
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>) = hypot(a.first - b.first, a.second - b.second)

    private fun posAt(d: Float): Pair<Float, Float> {
        var left = d
        for (i in 1 until waypoints.size) {
            val seg = dist(waypoints[i - 1], waypoints[i])
            if (left <= seg) { val t = left / seg; return (waypoints[i - 1].first + (waypoints[i].first - waypoints[i - 1].first) * t) to (waypoints[i - 1].second + (waypoints[i].second - waypoints[i - 1].second) * t) }
            left -= seg
        }
        return waypoints.last()
    }

    private fun range(t: Tower) = fit.cell * (when (t.type) { 0 -> 2.6f; 1 -> 2.1f; else -> 2.0f } + t.level * 0.25f)
    private fun upgradeCost(t: Tower) = typeCost[t.type] * t.level

    private fun startWave() {
        wave++; toSpawn = 6 + wave * 2; spawnT = 0f; status()
    }

    override fun up(x: Float, y: Float) {
        if (over) return
        typeBtns.forEachIndexed { i, b -> if (b.hit(x, y)) { selType = i; selTower = null; sfx(Sfx.CLICK); return } }
        if (waveBtn.hit(x, y)) { if (toSpawn == 0) { gold += (between * 5).toInt(); startWave(); sfx(Sfx.TAP) }; return }
        val c = fit.colAt(x); val r = fit.rowAt(y)
        if (c < 0 || r < 0) { selTower = null; return }
        val existing = towers.firstOrNull { it.c == c && it.r == r }
        if (existing != null) {
            if (selTower === existing) {
                val cost = upgradeCost(existing)
                if (existing.level < 3 && gold >= cost) { gold -= cost; existing.level++; sfx(Sfx.COIN); status() } else sfx(Sfx.ERROR)
            } else { selTower = existing; sfx(Sfx.CLICK) }
            return
        }
        selTower = null
        if (r * cols + c in pathCells) { sfx(Sfx.ERROR); return }
        if (gold < typeCost[selType]) { sfx(Sfx.ERROR); info("Không đủ vàng!"); return }
        gold -= typeCost[selType]; towers.add(Tower(c, r, selType)); sfx(Sfx.TAP); status()
    }

    override fun update(dt: Float) {
        fx.update(dt)
        if (toSpawn == 0 && enemies.isEmpty()) { between -= dt; if (between <= 0f) { between = 8f; startWave() } }
        if (toSpawn > 0) {
            spawnT -= dt
            if (spawnT <= 0f) {
                spawnT = (0.9f - wave * 0.03f).coerceAtLeast(0.35f)
                val kind = when { wave % 5 == 0 && toSpawn == 1 -> 2; toSpawn % 3 == 0 && wave > 2 -> 1; else -> 0 }
                val hp = (20f + wave * 9f) * floatArrayOf(1f, 0.6f, 8f)[kind]
                val sp = fit.cell * floatArrayOf(1.3f, 2.4f, 0.8f)[kind]
                enemies.add(Enemy(0f, hp, hp, sp, kind, reward = intArrayOf(6, 5, 60)[kind] + wave / 2))
                toSpawn--
            }
        }
        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val sp = if (e.slow > 0f) e.speed * 0.45f else e.speed
            if (e.slow > 0f) e.slow -= dt
            e.d += sp * dt
            if (e.d >= totalLen) {
                it.remove(); lives -= if (e.kind == 2) 5 else 1; sfx(Sfx.ERROR); buzz(40); status()
                if (lives <= 0) { lives = 0; sfx(Sfx.LOSE); gameOver(false, "Căn cứ thất thủ!", "Trụ được $wave đợt · hạ $kills quái", wave); return }
            }
        }
        for (t in towers) {
            t.cd -= dt
            if (t.cd > 0f) continue
            val tx = fit.cx(t.c); val ty = fit.cy(t.r); val rg = range(t)
            val target = enemies.filter { val (ex, ey) = posAt(it.d); hypot(ex - tx, ey - ty) < rg }.maxByOrNull { it.d } ?: continue
            val dmg = when (t.type) { 0 -> 9f; 1 -> 22f; else -> 4f } * (1 + (t.level - 1) * 0.7f)
            shots.add(Shot(tx, ty, target, dmg, t.type))
            t.cd = when (t.type) { 0 -> 0.45f; 1 -> 1.4f; else -> 0.8f } / (1 + (t.level - 1) * 0.15f)
            if (t.type == 0) sfx(Sfx.TICK) else if (t.type == 1) sfx(Sfx.SHOOT)
        }
        val sit = shots.iterator()
        while (sit.hasNext()) {
            val s = sit.next()
            val (ex, ey) = posAt(s.target.d)
            val dx = ex - s.x; val dy = ey - s.y; val d = hypot(dx, dy)
            val step = fit.cell * 9f * dt
            if (d <= step || s.target !in enemies) {
                sit.remove()
                if (s.target !in enemies) continue
                if (s.type == 1) {
                    fx.burst(ex, ey, Pal.ORANGE, 12, 140 * dp, 3 * dp, 0.35f)
                    for (e in enemies) { val (x2, y2) = posAt(e.d); if (hypot(x2 - ex, y2 - ey) < fit.cell * 1.1f) e.hp -= s.dmg }
                } else { s.target.hp -= s.dmg; if (s.type == 2) s.target.slow = 1.5f }
            } else { s.x += dx / d * step; s.y += dy / d * step }
        }
        val dead = enemies.filter { it.hp <= 0f }
        for (e in dead) { val (x, y) = posAt(e.d); fx.burst(x, y, Pal.RED, 8, 120 * dp, 2.5f * dp, 0.4f); gold += e.reward; kills++; sfx(Sfx.POP) }
        if (dead.isNotEmpty()) { enemies.removeAll(dead); status(); score(kills) }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF33691E.toInt(), 0xFF1B3A0F.toInt())
        val cs = fit.cell
        for (r in 0 until rows) for (col in 0 until cols) {
            val x = fit.x0 + col * cs; val y = fit.y0 + r * cs
            if (r * cols + col in pathCells) c.rect(x, y, x + cs, y + cs, 0xFFD7B98E.toInt())
            else c.rect(x, y, x + cs, y + cs, if ((r + col) % 2 == 0) 0xFF7CB342.toInt() else 0xFF74A93E.toInt())
        }
        // base
        val (bx, _) = waypoints.last()
        c.rrect(bx - cs * 0.6f, fit.y0 - 2, bx + cs * 0.6f, fit.y0 + cs * 0.5f, cs * 0.15f, Pal.BLUE)
        c.text("🏰", bx, fit.y0 + cs * 0.4f, cs * 0.7f, Pal.WHITE)
        for (t in towers) {
            val x = fit.cx(t.c); val y = fit.cy(t.r)
            if (t === selTower) c.circle(x, y, range(t), Pal.alpha(Pal.WHITE, 0.15f))
            c.rrect(x - cs * 0.4f, y - cs * 0.4f, x + cs * 0.4f, y + cs * 0.4f, cs * 0.15f, Pal.shade(typeColor[t.type], 0.6f))
            c.circle(x, y, cs * 0.28f, typeColor[t.type])
            c.text(arrayOf("🏹", "💣", "❄")[t.type], x, y, cs * 0.38f, Pal.WHITE)
            for (k in 0 until t.level) c.circle(x - cs * 0.2f + k * cs * 0.2f, y + cs * 0.32f, cs * 0.06f, Pal.YELLOW)
            if (t === selTower) c.rrectStroke(x - cs * 0.45f, y - cs * 0.45f, x + cs * 0.45f, y + cs * 0.45f, cs * 0.15f, 2 * dp, Pal.YELLOW)
        }
        for (e in enemies) {
            val (x, y) = posAt(e.d)
            val rr = cs * floatArrayOf(0.28f, 0.22f, 0.4f)[e.kind]
            c.circle(x, y, rr, if (e.slow > 0f) Pal.CYAN else intArrayOf(Pal.PURPLE, Pal.PINK, Pal.RED)[e.kind])
            c.circle(x - rr * 0.35f, y - rr * 0.2f, rr * 0.2f, Pal.WHITE); c.circle(x + rr * 0.35f, y - rr * 0.2f, rr * 0.2f, Pal.WHITE)
            c.rect(x - rr, y - rr - 6 * dp, x - rr + 2 * rr * (e.hp / e.maxHp).coerceAtLeast(0f), y - rr - 3 * dp, Pal.GREEN)
        }
        for (s in shots) c.circle(s.x, s.y, if (s.type == 1) 4 * dp else 2.5f * dp, if (s.type == 2) Pal.CYAN else if (s.type == 1) 0xFF212121.toInt() else Pal.YELLOW)
        fx.draw(c)
        typeBtns.forEachIndexed { i, b ->
            b.label = "${typeNames[i]} ${typeCost[i]}"
            b.color = if (gold >= typeCost[i]) typeColor[i] else Pal.SURFACE2
            b.draw(c, 13 * dp)
            if (i == selType && selTower == null) c.rrectStroke(b.l - 3, b.t - 3, b.r + 3, b.b + 3, 10 * dp, 2.5f * dp, Pal.WHITE)
        }
        waveBtn.label = if (toSpawn == 0 && enemies.isEmpty()) "▶ ${between.toInt() + 1}s" else "⚔"
        waveBtn.draw(c, 15 * dp)
        selTower?.let { t ->
            val msg = if (t.level >= 3) "Tháp cấp tối đa" else "Chạm lần nữa để nâng cấp (${upgradeCost(t)} vàng)"
            c.rrect(w * 0.05f, fit.bottom - 34 * dp, w * 0.95f, fit.bottom - 6 * dp, 10 * dp, Pal.alpha(Pal.BLACK, 0.6f))
            c.text(msg, w / 2, fit.bottom - 20 * dp, 12 * dp, Pal.YELLOW)
        }
        if (wave == 0) c.text("Chọn tháp rồi chạm ô cỏ để xây", w / 2, fit.cy(12), 14 * dp, Pal.WHITE)
    }
}
