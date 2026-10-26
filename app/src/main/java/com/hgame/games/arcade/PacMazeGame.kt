package com.hgame.games.arcade

import android.graphics.Canvas
import android.graphics.RectF
import com.hgame.engine.BoardFit
import com.hgame.engine.Game
import com.hgame.engine.Gfx
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.Swipe
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.math.sin

class PacMazeGame : Game() {
    override val realtime = true
    override val help = "Vuốt để điều khiển, ăn hết các chấm trong mê cung. Tránh 4 con ma! Ăn viên năng lượng lớn để ma hoảng sợ " +
        "(màu xanh) và có thể ăn chúng lấy điểm thưởng."

    // Original maze layout: # wall, . dot, o power, - ghost door, space empty, G ghost spawn, P player
    private val map = arrayOf(
        "###################",
        "#........#........#",
        "#o##.###.#.###.##o#",
        "#.................#",
        "#.##.#.#####.#.##.#",
        "#....#...#...#....#",
        "####.### # ###.####",
        "   #.#       #.#   ",
        "####.# ##-## #.####",
        "    .  #GGG#  .    ",
        "####.# ##### #.####",
        "   #.#       #.#   ",
        "####.# ##### #.####",
        "#........#........#",
        "#.##.###.#.###.##.#",
        "#o.#.....P.....#.o#",
        "##.#.#.#####.#.#.##",
        "#....#...#...#....#",
        "#.######.#.######.#",
        "#.................#",
        "###################",
    )
    private val rows = map.size; private val cols = map[0].length
    private val walls = BooleanArray(rows * cols); private val dots = IntArray(rows * cols) // 1 dot, 2 power
    private val fit = BoardFit(); private val swipe = Swipe(14f)

    private inner class Mover(var x: Float, var y: Float, var dir: Int, val speed: Float) {
        val col get() = (x + 0.5f).toInt().mod(cols)
        val row get() = (y + 0.5f).toInt()
    }
    private val dx = intArrayOf(0, 1, 0, -1); private val dy = intArrayOf(-1, 0, 1, 0)
    private lateinit var pac: Mover
    private val ghosts = ArrayList<Mover>(); private val ghostHome = ArrayList<Pair<Int, Int>>()
    private val ghostColors = intArrayOf(Pal.RED, Pal.PINK, Pal.CYAN, Pal.ORANGE)
    private var ghostRelease = FloatArray(4); private var eaten = BooleanArray(4)
    private var want = 3; private var fright = 0f; private var lives = 3; private var points = 0; private var level = 1
    private var t = 0f; private var dying = 0f; private var ready = 0f; private var chain = 0
    private var startR = 0; private var startC = 0

    override fun newGame() { lives = 3; points = 0; level = 1; score(0); loadLevel() }

    private fun loadLevel() {
        ghostHome.clear()
        for (r in 0 until rows) for (c in 0 until cols) {
            val ch = map[r][c]; val i = r * cols + c
            walls[i] = ch == '#' || ch == '-'
            dots[i] = when (ch) { '.' -> 1; 'o' -> 2; else -> 0 }
            if (ch == 'P') { startR = r; startC = c }
            if (ch == 'G') ghostHome.add(r to c)
        }
        ghostHome.add(ghostHome[1])
        resetActors()
    }

    private fun resetActors() {
        val sp = 5.5f + level * 0.3f
        pac = Mover(startC.toFloat(), startR.toFloat(), 3, sp)
        ghosts.clear()
        for (i in 0 until 4) ghosts.add(Mover(ghostHome[i].second.toFloat(), ghostHome[i].first.toFloat(), 0, sp * 0.88f))
        ghostRelease = floatArrayOf(0f, 3f, 6f, 9f); eaten.fill(false)
        want = 3; fright = 0f; dying = 0f; ready = 1.5f
        info("Màn $level · ❤ $lives")
    }

    override fun layout() { fit.fit(cols, rows, 4 * dp, 4 * dp, w - 4 * dp, h - 4 * dp) }

    override fun down(x: Float, y: Float) = swipe.down(x, y)
    override fun move(x: Float, y: Float) { val d = swipe.move(x, y); if (d >= 0) want = d }
    override fun up(x: Float, y: Float) { val d = swipe.up(x, y); if (d >= 0) want = d }

    private fun wall(r: Int, c: Int, ghost: Boolean = false): Boolean {
        if (r < 0 || r >= rows) return true
        val cc = c.mod(cols)
        val i = r * cols + cc
        if (ghost && map[r][cc] == '-') return false
        return walls[i]
    }

    /** Moves along the grid; turns only at cell centres. Returns true when at a centre this frame. */
    private fun advance(m: Mover, dist: Float, chooser: (Mover) -> Int, ghost: Boolean) {
        var left = dist
        var guard = 0
        while (left > 0f && guard++ < 8) {
            val cx = Math.round(m.x).toFloat(); val cy = Math.round(m.y).toFloat()
            val toCentre = abs(m.x - cx) + abs(m.y - cy)
            val atCentre = toCentre < 1e-3f
            if (atCentre) {
                m.x = cx; m.y = cy
                val nd = chooser(m)
                if (!wall(cy.toInt() + dy[nd], cx.toInt() + dx[nd], ghost)) m.dir = nd
                if (wall(cy.toInt() + dy[m.dir], cx.toInt() + dx[m.dir], ghost)) return
                val s = minOf(left, 1f); m.x += dx[m.dir] * s; m.y += dy[m.dir] * s; left -= s
            } else {
                // distance to the next centre in the current direction
                val nx = if (dx[m.dir] > 0) Math.floor(m.x.toDouble()).toFloat() + 1 else if (dx[m.dir] < 0) Math.ceil(m.x.toDouble()).toFloat() - 1 else m.x
                val ny = if (dy[m.dir] > 0) Math.floor(m.y.toDouble()).toFloat() + 1 else if (dy[m.dir] < 0) Math.ceil(m.y.toDouble()).toFloat() - 1 else m.y
                val need = abs(nx - m.x) + abs(ny - m.y)
                val s = minOf(left, need)
                // allow instant reversal between centres
                if (!ghost && want == (m.dir + 2) % 4) { m.dir = want }
                else { m.x += dx[m.dir] * s; m.y += dy[m.dir] * s; left -= s }
                if (s == 0f && need == 0f) { m.x = Math.round(m.x).toFloat(); m.y = Math.round(m.y).toFloat() }
            }
            if (m.x < -0.5f) m.x += cols; if (m.x > cols - 0.5f) m.x -= cols
        }
    }

    private fun ghostTarget(i: Int): Pair<Int, Int> {
        val pr = pac.row; val pc = pac.col
        return when (i) {
            0 -> pr to pc
            1 -> (pr + dy[pac.dir] * 4) to (pc + dx[pac.dir] * 4)
            2 -> (2 * pr - ghosts[0].row) to (2 * pc - ghosts[0].col)
            else -> if (abs(pr - ghosts[3].row) + abs(pc - ghosts[3].col) > 8) pr to pc else rows to 0
        }
    }

    override fun update(dt: Float) {
        t += dt
        if (dying > 0f) {
            dying -= dt
            if (dying <= 0f) { if (lives <= 0) gameOver(false, "Bị ma bắt!", "Màn $level · $points điểm", points) else resetActors() }
            return
        }
        if (ready > 0f) { ready -= dt; return }
        if (fright > 0f) fright -= dt
        advance(pac, pac.speed * dt, { want }, false)
        val i = pac.row * cols + pac.col
        if (pac.row in 0 until rows && dots[i] > 0) {
            if (dots[i] == 2) { fright = (7f - level).coerceAtLeast(2.5f); chain = 0; sfx(Sfx.COIN); for (g in ghosts) g.dir = (g.dir + 2) % 4 }
            else if ((points / 10) % 2 == 0) sfx(Sfx.TICK)
            points += if (dots[i] == 2) 50 else 10; dots[i] = 0; score(points)
            if (dots.none { it > 0 }) { level++; sfx(Sfx.WIN); loadLevel(); return }
        }
        ghosts.forEachIndexed { k, g ->
            if (ghostRelease[k] > 0f) { ghostRelease[k] -= dt; return@forEachIndexed }
            val sp = if (eaten[k]) g.speed * 2f else if (fright > 0f) g.speed * 0.55f else g.speed
            advance(g, sp * dt, { m ->
                if (eaten[k] && m.row == ghostHome[k].first && m.col == ghostHome[k].second) eaten[k] = false
                val (tr, tc) = if (eaten[k]) ghostHome[k] else if (map[m.row][m.col] == 'G') (ghostHome[0].first - 2) to ghostHome[0].second else ghostTarget(k)
                val opts = (0..3).filter { it != (m.dir + 2) % 4 && !wall(m.row + dy[it], m.col + dx[it], true) }
                when {
                    opts.isEmpty() -> (m.dir + 2) % 4
                    fright > 0f && !eaten[k] -> opts.random()
                    else -> opts.minByOrNull { d -> val r = m.row + dy[d]; val c = m.col + dx[d]; (r - tr) * (r - tr) + (c - tc) * (c - tc) }!!
                }
            }, true)
            if (!eaten[k] && abs(g.x - pac.x) + abs(g.y - pac.y) < 0.8f) {
                if (fright > 0f) {
                    eaten[k] = true; chain++; points += 200 * chain; score(points); sfx(Sfx.MERGE)
                } else {
                    lives--; dying = 1.4f; sfx(Sfx.LOSE); buzz(100); info("Màn $level · ❤ $lives")
                }
            }
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h, 0xFF000010.toInt(), 0xFF050520.toInt())
        val cs = fit.cell
        for (r in 0 until rows) for (col in 0 until cols) {
            val i = r * cols + col
            val x = fit.x0 + col * cs; val y = fit.y0 + r * cs
            if (map[r][col] == '-') c.rect(x, y + cs * 0.4f, x + cs, y + cs * 0.6f, Pal.PINK)
            else if (walls[i]) c.rrect(x + cs * 0.08f, y + cs * 0.08f, x + cs * 0.92f, y + cs * 0.92f, cs * 0.25f, 0xFF2234B8.toInt())
            else if (dots[i] == 1) c.circle(x + cs / 2, y + cs / 2, cs * 0.1f, 0xFFFFE0B2.toInt())
            else if (dots[i] == 2 && (t * 4).toInt() % 2 == 0) c.circle(x + cs / 2, y + cs / 2, cs * 0.3f, 0xFFFFE0B2.toInt())
        }
        // pac
        val px = fit.x0 + (pac.x + 0.5f) * cs; val py = fit.y0 + (pac.y + 0.5f) * cs
        val mouth = if (dying > 0f) (1.4f - dying) / 1.4f * 180f else abs(sin(t * 14)) * 45f
        val start = intArrayOf(270, 0, 90, 180)[pac.dir] + mouth
        val rf = RectF(px - cs * 0.45f, py - cs * 0.45f, px + cs * 0.45f, py + cs * 0.45f)
        c.drawArc(rf, start, 360 - 2 * mouth, true, Gfx.fillPaint(Pal.YELLOW))
        if (dying <= 0f) ghosts.forEachIndexed { k, g ->
            val gx = fit.x0 + (g.x + 0.5f) * cs; val gy = fit.y0 + (g.y + 0.5f) * cs; val r = cs * 0.45f
            val blink = fright in 0f..2f && (t * 6).toInt() % 2 == 0
            val col = if (eaten[k]) 0 else if (fright > 0f) (if (blink) Pal.WHITE else 0xFF2962FF.toInt()) else ghostColors[k]
            if (col != 0) {
                c.circle(gx, gy - r * 0.1f, r, col)
                c.rect(gx - r, gy - r * 0.1f, gx + r, gy + r * 0.7f, col)
                for (s in 0..2) c.circle(gx - r + r * 0.33f + s * r * 0.67f, gy + r * 0.7f, r * 0.33f, col)
            }
            c.circle(gx - r * 0.35f, gy - r * 0.2f, r * 0.28f, Pal.WHITE); c.circle(gx + r * 0.35f, gy - r * 0.2f, r * 0.28f, Pal.WHITE)
            c.circle(gx - r * 0.35f + dx[g.dir] * r * 0.1f, gy - r * 0.2f + dy[g.dir] * r * 0.1f, r * 0.13f, 0xFF1A237E.toInt())
            c.circle(gx + r * 0.35f + dx[g.dir] * r * 0.1f, gy - r * 0.2f + dy[g.dir] * r * 0.1f, r * 0.13f, 0xFF1A237E.toInt())
        }
        if (ready > 0f) c.text("SẴN SÀNG!", fit.cx(cols / 2), fit.cy(11), cs * 0.9f, Pal.YELLOW)
    }
}
