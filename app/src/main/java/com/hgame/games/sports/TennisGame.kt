package com.hgame.games.sports

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.ball
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.poly
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.abs
import kotlin.random.Random

/** Top-down tennis with a pseudo-3D ball height. You play at the bottom. */
class TennisGame : Game() {
    override val realtime = true
    override val options = listOf(GameOption("level", "Độ khó", listOf("Dễ", "Vừa", "Khó"), 1))
    override val help = "Kéo ngón tay để di chuyển tay vợt ở nửa sân dưới. Chạm vào bóng sau khi bóng nảy để đánh trả; vị trí chạm trên vợt quyết định góc bóng. " +
        "Bóng chạm đất 2 lần ở sân bạn hoặc ra ngoài sau cú đánh của bạn là mất điểm. Thắng 1 set 4 game (mỗi game 4 điểm)."

    private var px = 0f; private var py = 0f; private var tx = 0f
    private var ox = 0f; private var oy = 0f
    private var bx = 0f; private var by = 0f; private var bz = 0f; private var vx = 0f; private var vy = 0f; private var vz = 0f
    private var bounces = 0; private var lastHitter = 0; private var serving = true; private var server = 0
    private val pts = IntArray(2); private val games = IntArray(2)
    private var msg = ""; private var msgT = 0f; private var live = false; private var aiMiss = false
    private var cl = 0f; private var cr = 0f; private var ct = 0f; private var cb = 0f; private var netY = 0f

    override fun newGame() { pts.fill(0); games.fill(0); server = 0; prepServe(); info(scoreText()) }

    override fun layout() {
        cl = w * 0.1f; cr = w * 0.9f; ct = h * 0.08f; cb = h * 0.92f; netY = (ct + cb) / 2
        if (px == 0f) { px = w / 2; py = cb - 20 * dp; tx = px; ox = w / 2; oy = ct + 20 * dp; prepServe() }
    }

    private fun prepServe() {
        live = false; bounces = 0; serving = true
        if (server == 0) { bx = px; by = py - 20 * dp } else { bx = ox; by = oy + 20 * dp }
        bz = 30f; vx = 0f; vy = 0f; vz = 0f
        if (server == 1) after(1.0f) { aiServe() }
    }

    private fun scoreText(): String {
        val names = arrayOf("0", "15", "30", "40", "AD")
        return "Game ${games[0]}–${games[1]} · Điểm ${names[minOf(pts[0], 4)]}–${names[minOf(pts[1], 4)]}"
    }

    override fun down(x: Float, y: Float) { tx = x; if (serving && server == 0 && !over) serve() }
    override fun move(x: Float, y: Float) { tx = x }

    private fun serve() {
        serving = false; live = true; lastHitter = 0; bounces = 0; aiMiss = false
        bx = px; by = py - 20 * dp; bz = 40f
        val target = w / 2 + (Random.nextFloat() - 0.5f) * (cr - cl) * 0.6f
        val time = 1.1f
        vx = (target - bx) / time; vy = -((by - (ct + (netY - ct) * 0.45f)) / time); vz = 120f
        sfx(Sfx.HIT)
    }

    private fun aiServe() {
        if (over || !serving || server != 1) return
        serving = false; live = true; lastHitter = 1; bounces = 0
        bx = ox; by = oy + 20 * dp; bz = 40f
        val target = w / 2 + (Random.nextFloat() - 0.5f) * (cr - cl) * 0.6f
        val time = 1.2f
        vx = (target - bx) / time; vy = ((cb - (cb - netY) * 0.45f) - by) / time; vz = 120f
        sfx(Sfx.HIT)
    }

    override fun update(dt: Float) {
        if (msgT > 0) msgT -= dt
        px += (tx.coerceIn(cl - 20 * dp, cr + 20 * dp) - px) * (dt * 14f).coerceAtMost(1f)
        // AI movement: track the ball when it heads its way
        val skill = floatArrayOf(0.55f, 0.8f, 1.05f)[opt("level")]
        val targetX = if (vy < 0 && live) bx + vx * ((by - oy) / -vy).coerceIn(0f, 1.5f) * 0.9f else w / 2
        val sp = w * 0.9f * skill
        ox += (targetX - ox).coerceIn(-sp * dt, sp * dt)
        if (serving) { if (server == 0) { bx = px; by = py - 20 * dp } ; return }
        if (!live) return
        bx += vx * dt; by += vy * dt
        vz -= 380f * dt; bz += vz * dt
        if (bz <= 0f) {
            bz = 0f; vz = -vz * 0.7f; bounces++
            sfx(Sfx.TICK)
            val inCourt = bx in cl..cr && by in ct..cb
            val side = if (by > netY) 0 else 1
            if (bounces == 1 && (!inCourt || side == lastHitter)) { point(1 - lastHitter, if (!inCourt) "Bóng ra ngoài!" else "Lỗi!"); return }
            if (bounces >= 2) { point(1 - side, "Bóng nảy 2 lần"); return }
        }
        // player hit
        val racketW = 60 * dp
        if (vy > 0 && lastHitter == 1 && bounces >= 1 && abs(by - py) < 22 * dp && abs(bx - px) < racketW / 2 + 8 * dp && bz < 60f) {
            hit(0, (bx - px) / (racketW / 2))
        }
        if (vy < 0 && lastHitter == 0 && bounces >= 1 && abs(by - oy) < 22 * dp && bz < 60f) {
            if (abs(bx - ox) < racketW / 2 + 8 * dp && !aiMiss) hit(1, (Random.nextFloat() - 0.5f) * 1.6f)
        }
        if (by < ct - 80 * dp || by > cb + 80 * dp || bx < -40 * dp || bx > w + 40 * dp) {
            val lastSide = if (by > netY) 0 else 1
            if (bounces == 0) point(1 - lastHitter, "Bóng ra ngoài!") else point(1 - lastSide, "Không đỡ được")
        }
    }

    private fun hit(who: Int, offset: Float) {
        lastHitter = who; bounces = 0
        if (who == 0) aiMiss = Random.nextFloat() < 0.4f - floatArrayOf(0.55f, 0.8f, 1.05f)[opt("level")] * 0.33f
        val dir = if (who == 0) -1f else 1f
        val depth = (cb - ct) * (0.62f + Random.nextFloat() * 0.25f)
        val time = 1.0f - abs(offset) * 0.15f
        vy = dir * depth / time
        vx = offset * (cr - cl) * 0.55f / time + (w / 2 - bx) * 0.3f / time
        vz = 140f; bz = maxOf(bz, 20f)
        sfx(Sfx.HIT); if (who == 0) buzz(15)
    }

    private fun point(winner: Int, why: String) {
        live = false; msg = (if (winner == 0) "Điểm cho bạn – " else "Điểm cho máy – ") + why; msgT = 1.4f
        pts[winner]++
        sfx(if (winner == 0) Sfx.COIN else Sfx.ERROR)
        val a = pts[winner]; val b = pts[1 - winner]
        if (a >= 4 && a - b >= 2) {
            games[winner]++; pts.fill(0); server = 1 - server
            if (games[winner] >= 4) {
                info(scoreText()); score(games[0])
                after(1.2f) { gameOver(winner == 0, if (winner == 0) "Bạn thắng set!" else "Máy thắng set!", "Tỉ số game ${games[0]}–${games[1]}", games[0]) }
                return
            }
        } else if (a == 4 && b == 4) { pts[0] = 3; pts[1] = 3 }
        info(scoreText()); score(games[0])
        after(1.2f) { prepServe() }
    }

    override fun draw(c: Canvas) {
        c.rect(0f, 0f, w, h, 0xFF2E7D32.toInt())
        c.rect(cl - 10 * dp, ct - 10 * dp, cr + 10 * dp, cb + 10 * dp, 0xFF1565C0.toInt())
        c.rect(cl, ct, cr, cb, 0xFF1E88E5.toInt())
        val lw = 2.5f * dp
        c.line(cl, ct, cr, ct, lw, Pal.WHITE); c.line(cl, cb, cr, cb, lw, Pal.WHITE); c.line(cl, ct, cl, cb, lw, Pal.WHITE); c.line(cr, ct, cr, cb, lw, Pal.WHITE)
        val sl = cl + (cr - cl) * 0.12f; val sr = cr - (cr - cl) * 0.12f
        c.line(sl, ct, sl, cb, lw, Pal.WHITE); c.line(sr, ct, sr, cb, lw, Pal.WHITE)
        c.line(sl, netY - (netY - ct) * 0.55f, sr, netY - (netY - ct) * 0.55f, lw, Pal.WHITE); c.line(sl, netY + (cb - netY) * 0.55f, sr, netY + (cb - netY) * 0.55f, lw, Pal.WHITE)
        c.line(w / 2, netY - (netY - ct) * 0.55f, w / 2, netY + (cb - netY) * 0.55f, lw, Pal.WHITE)
        c.rect(cl - 14 * dp, netY - 3 * dp, cr + 14 * dp, netY + 3 * dp, Pal.alpha(Pal.WHITE, 0.85f))
        // players
        player(c, ox, oy, Pal.ORANGE, true)
        // ball with shadow and height
        c.circle(bx, by, 6 * dp, Pal.alpha(Pal.BLACK, 0.3f))
        c.ball(bx, by - bz * dp * 0.35f, 6 * dp + bz * 0.02f * dp, 0xFFDCE775.toInt())
        player(c, px, py, Pal.PINK, false)
        if (msgT > 0f) { c.rrect(w * 0.05f, netY - 22 * dp, w * 0.95f, netY + 22 * dp, 12 * dp, Pal.alpha(Pal.BLACK, 0.55f)); c.text(msg, w / 2, netY, 14 * dp, Pal.YELLOW) }
        else if (serving && server == 0) c.text("Chạm để giao bóng", w / 2, netY + (cb - netY) * 0.3f, 15 * dp, Pal.WHITE)
    }

    private fun player(c: Canvas, x: Float, y: Float, col: Int, top: Boolean) {
        val rw = 60 * dp
        c.circle(x, y + 4 * dp, 14 * dp, Pal.alpha(Pal.BLACK, 0.25f))
        c.circle(x, y, 13 * dp, col); c.circle(x, y, 7 * dp, 0xFFFFCC80.toInt())
        val ry = if (top) y + 14 * dp else y - 14 * dp
        c.rrect(x - rw / 2, ry - 4 * dp, x + rw / 2, ry + 4 * dp, 4 * dp, Pal.alpha(Pal.WHITE, 0.85f))
        c.poly(0xFF424242.toInt(), x + rw / 2 - 4 * dp, ry - 2 * dp, x + rw / 2 + 10 * dp, ry, x + rw / 2 - 4 * dp, ry + 2 * dp)
    }
}
