package com.hgame.games.board

import android.graphics.Canvas
import com.hgame.core.BattleshipAi
import com.hgame.core.Fleet
import com.hgame.engine.BestMode
import com.hgame.engine.BoardFit
import com.hgame.engine.Btn
import com.hgame.engine.Game
import com.hgame.engine.GameOption
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.circle
import com.hgame.engine.line
import com.hgame.engine.rect
import com.hgame.engine.rrect
import com.hgame.engine.text
import kotlin.math.min

class BattleshipGame : Game() {
    override val bestMode = BestMode.HIGH
    override val options = listOf(GameOption("level", "Độ khó", listOf("Dễ", "Khó"), 1))
    override val help = "Bắn vào lưới của đối phương (phía trên) để tìm và đánh chìm 5 tàu của họ. " +
        "Hạm đội của bạn ở lưới nhỏ phía dưới. Trước phát bắn đầu tiên có thể bấm Xếp lại để đổi vị trí tàu. Bắn chìm hết tàu địch trước để thắng."

    private var mine = Fleet(); private var enemy = Fleet()
    private val ai = BattleshipAi()
    private val big = BoardFit(); private val small = BoardFit()
    private var started = false
    private var enemyTurn = false
    private val shuffleBtn = Btn("🔀 Xếp lại", Pal.SURFACE2)
    private var shots = 0
    private var lastEnemy = -1; private var lastMine = -1
    private var msg = ""

    override fun newGame() {
        mine = Fleet().apply { randomize() }; enemy = Fleet().apply { randomize() }
        started = false; enemyTurn = false; shots = 0; lastEnemy = -1; lastMine = -1
        msg = "Chọn ô để bắn"
        status()
    }

    override fun layout() {
        val pad = 12 * dp
        val bigSize = min(w - pad * 2, h * 0.56f)
        big.fit(10, 10, (w - bigSize) / 2, 34 * dp, (w + bigSize) / 2, 34 * dp + bigSize)
        val smallSize = min(h - big.bottom - 70 * dp, w * 0.5f)
        small.fit(10, 10, pad + 4 * dp, big.bottom + 36 * dp, pad + 4 * dp + smallSize, big.bottom + 36 * dp + smallSize)
        shuffleBtn.set(small.right + 16 * dp, small.y0 + 30 * dp, w - pad, small.y0 + 76 * dp)
    }

    private fun status() = info("Tàu địch còn ${enemy.ships.indices.count { !enemy.sunk(it) }} · Tàu ta còn ${mine.ships.indices.count { !mine.sunk(it) }}")

    override fun up(x: Float, y: Float) {
        if (over || enemyTurn) return
        if (!started && shuffleBtn.hit(x, y)) { mine.randomize(); sfx(Sfx.CLICK); redraw(); return }
        val c = big.colAt(x); val r = big.rowAt(y)
        if (c < 0 || r < 0) return
        val i = r * 10 + c
        if (enemy.shot[i]) return
        started = true; shots++
        lastEnemy = i
        val res = enemy.fire(i)
        when (res) {
            0 -> { msg = "Trượt!"; sfx(Sfx.TAP) }
            1 -> { msg = "Trúng!"; sfx(Sfx.HIT); buzz(30) }
            else -> { msg = "Đánh chìm tàu ${enemy.lengths[enemy.ship[i]]} ô!"; sfx(Sfx.BOOM); buzz(60) }
        }
        status()
        if (enemy.allSunk()) {
            score(maxOf(10, 120 - shots))
            gameOver(true, "Chiến thắng!", "Bạn bắn $shots phát")
            return
        }
        if (res == 0) enemyFire()
        redraw()
    }

    private fun enemyFire() {
        enemyTurn = true
        val smart = opt("level") == 1
        after(0.7f) {
            val i = ai.choose(mine, smart)
            lastMine = i
            val res = mine.fire(i)
            sfx(if (res == 0) Sfx.TAP else Sfx.BOOM)
            if (res > 0) msg = if (res == 2) "Địch đánh chìm tàu của bạn!" else "Địch bắn trúng tàu bạn!"
            status()
            if (mine.allSunk()) { enemyTurn = false; gameOver(false, "Thất bại", "Hạm đội của bạn đã bị đánh chìm"); return@after }
            if (res > 0) enemyFire() else { enemyTurn = false; msg = "Đến lượt bạn" }
        }
    }

    private fun drawGrid(c: Canvas, f: BoardFit, fleet: Fleet, reveal: Boolean, last: Int) {
        val cs = f.cell
        c.rrect(f.x0 - 3 * dp, f.y0 - 3 * dp, f.right + 3 * dp, f.bottom + 3 * dp, 8 * dp, 0xFF0F3D6E.toInt())
        c.rect(f.x0, f.y0, f.right, f.bottom, 0xFF1565C0.toInt())
        for (k in 0..10) {
            c.line(f.x0 + k * cs, f.y0, f.x0 + k * cs, f.bottom, 1f, 0x553FA9F5)
            c.line(f.x0, f.y0 + k * cs, f.right, f.y0 + k * cs, 1f, 0x553FA9F5)
        }
        for (id in fleet.ships.indices) {
            val cells = fleet.ships[id]
            if (!reveal && !fleet.sunk(id)) continue
            val r0 = cells.minOf { it / 10 }; val r1 = cells.maxOf { it / 10 }
            val c0 = cells.minOf { it % 10 }; val c1 = cells.maxOf { it % 10 }
            c.rrect(f.x0 + c0 * cs + cs * 0.12f, f.y0 + r0 * cs + cs * 0.12f, f.x0 + (c1 + 1) * cs - cs * 0.12f, f.y0 + (r1 + 1) * cs - cs * 0.12f,
                cs * 0.4f, if (fleet.sunk(id)) 0xFF5D4037.toInt() else 0xFF90A4AE.toInt())
        }
        for (i in 0 until 100) {
            if (!fleet.shot[i]) continue
            val cx = f.cx(i % 10); val cy = f.cy(i / 10)
            if (fleet.ship[i] >= 0) {
                c.circle(cx, cy, cs * 0.32f, Pal.ORANGE); c.circle(cx, cy, cs * 0.18f, Pal.RED)
            } else c.circle(cx, cy, cs * 0.12f, Pal.alpha(Pal.WHITE, 0.8f))
            if (i == last) c.rrect(cx - cs / 2, cy - cs / 2, cx + cs / 2, cy + cs / 2, 3 * dp, Pal.alpha(Pal.YELLOW, 0.35f))
        }
    }

    override fun draw(c: Canvas) {
        c.background(w, h)
        c.text(if (enemyTurn) "Địch đang bắn…" else msg, w / 2, 17 * dp, 15 * dp, if (enemyTurn) Pal.ORANGE else Pal.TEXT)
        drawGrid(c, big, enemy, over, lastEnemy)
        c.text("Hạm đội của bạn", small.x0, small.y0 - 14 * dp, 13 * dp, Pal.MUTED, android.graphics.Paint.Align.LEFT)
        drawGrid(c, small, mine, true, lastMine)
        if (!started) shuffleBtn.draw(c, 14 * dp)
        else {
            val x = (small.right + 16 * dp + w - 12 * dp) / 2
            c.text("Phát bắn", x, small.y0 + 30 * dp, 13 * dp, Pal.MUTED, bold = false)
            c.text(shots.toString(), x, small.y0 + 60 * dp, 26 * dp, Pal.TEXT)
        }
    }
}
