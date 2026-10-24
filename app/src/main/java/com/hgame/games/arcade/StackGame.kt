package com.hgame.games.arcade

import android.graphics.Canvas
import com.hgame.engine.Game
import com.hgame.engine.Pal
import com.hgame.engine.Sfx
import com.hgame.engine.background
import com.hgame.engine.poly
import com.hgame.engine.text
import kotlin.math.abs

class StackGame : Game() {
    override val realtime = true
    override val help = "Khối trượt qua lại phía trên tháp. Chạm để thả khối xuống: phần thừa ra ngoài sẽ bị cắt. " +
        "Thả trùng khít hoàn hảo để giữ nguyên kích thước. Xây tháp cao nhất có thể!"

    private class Slab(var x: Float, var z: Float, var wx: Float, var wz: Float, val hue: Float)
    private class Falling(val s: Slab, val level: Float, var y: Float = 0f, var vy: Float = 0f)

    private val stack = ArrayList<Slab>()
    private val falling = ArrayList<Falling>()
    private var cur: Slab? = null
    private var axisX = true; private var t = 0f; private var dir = 1f; private var cam = 0f
    private var perfect = 0; private var flash = 0f
    private val baseHue = (Math.random() * 360).toFloat()
    private val slabH = 1f

    override fun newGame() {
        stack.clear(); falling.clear(); cam = 0f; perfect = 0
        stack.add(Slab(0f, 0f, 6f, 6f, baseHue))
        spawn(); score(0)
    }

    private fun spawn() {
        val top = stack.last()
        axisX = !axisX
        cur = Slab(if (axisX) -10f else top.x, if (axisX) top.z else -10f, top.wx, top.wz, baseHue + stack.size * 6f)
        dir = 1f
    }

    override fun down(x: Float, y: Float) {
        if (over) return
        val s = cur ?: return
        val top = stack.last()
        val delta = if (axisX) s.x - top.x else s.z - top.z
        val size = if (axisX) s.wx else s.wz
        if (abs(delta) >= size) {
            falling.add(Falling(s, stack.size.toFloat())); cur = null; sfx(Sfx.LOSE); buzz(60)
            gameOver(false, "Trượt mất rồi!", "Tháp cao ${stack.size - 1} tầng", stack.size - 1); return
        }
        if (abs(delta) < 0.18f) {
            if (axisX) s.x = top.x else s.z = top.z
            perfect++; flash = 1f; sfx(Sfx.COIN)
            if (perfect >= 4) { if (axisX) s.wx = (s.wx + 0.3f).coerceAtMost(6f) else s.wz = (s.wz + 0.3f).coerceAtMost(6f) }
        } else {
            perfect = 0; sfx(Sfx.TAP)
            val keep = size - abs(delta)
            // cut piece
            val lvl = stack.size.toFloat()
            if (axisX) {
                val newX = top.x + delta / 2
                val cx = if (delta > 0) newX + keep / 2 + abs(delta) / 2 else newX - keep / 2 - abs(delta) / 2
                falling.add(Falling(Slab(cx, s.z, abs(delta), s.wz, s.hue), lvl))
                s.x = newX; s.wx = keep
            } else {
                val newZ = top.z + delta / 2
                val cz = if (delta > 0) newZ + keep / 2 + abs(delta) / 2 else newZ - keep / 2 - abs(delta) / 2
                falling.add(Falling(Slab(s.x, cz, s.wx, abs(delta), s.hue), lvl))
                s.z = newZ; s.wz = keep
            }
        }
        stack.add(s); score(stack.size - 1)
        spawn()
    }

    override fun update(dt: Float) {
        t += dt
        if (flash > 0) flash -= dt * 2
        cur?.let { s ->
            val speed = 7f + stack.size * 0.12f
            if (axisX) { s.x += dir * speed * dt; if (s.x > 10f) dir = -1f; if (s.x < -10f) dir = 1f }
            else { s.z += dir * speed * dt; if (s.z > 10f) dir = -1f; if (s.z < -10f) dir = 1f }
        }
        for (f in falling) { f.vy += 30f * dt; f.y += f.vy * dt }
        falling.removeAll { it.y > 40f }
        val target = (stack.size - 6).coerceAtLeast(0) * slabH
        cam += (target - cam) * (dt * 4f).coerceAtMost(1f)
    }

    private var u = 0f; private var ox = 0f; private var oy = 0f

    // isometric projection
    private fun px(x: Float, z: Float) = ox + (x - z) * u * 0.87f
    private fun py(x: Float, y: Float, z: Float) = oy + (x + z) * u * 0.5f - y * u

    private fun drawSlab(c: Canvas, s: Slab, level: Float) {
        val y = level * slabH - cam
        val x0 = s.x - s.wx / 2; val x1 = s.x + s.wx / 2; val z0 = s.z - s.wz / 2; val z1 = s.z + s.wz / 2
        val col = Pal.hsv(s.hue, 0.55f, 0.95f)
        c.poly(col, px(x0, z0), py(x0, y + slabH, z0), px(x1, z0), py(x1, y + slabH, z0), px(x1, z1), py(x1, y + slabH, z1), px(x0, z1), py(x0, y + slabH, z1))
        c.poly(Pal.shade(col, 0.8f), px(x0, z1), py(x0, y + slabH, z1), px(x1, z1), py(x1, y + slabH, z1), px(x1, z1), py(x1, y, z1), px(x0, z1), py(x0, y, z1))
        c.poly(Pal.shade(col, 0.62f), px(x1, z0), py(x1, y + slabH, z0), px(x1, z1), py(x1, y + slabH, z1), px(x1, z1), py(x1, y, z1), px(x1, z0), py(x1, y, z0))
    }

    override fun draw(c: Canvas) {
        val hue = baseHue + stack.size * 6f
        c.background(w, h, Pal.hsv(hue + 40, 0.35f, 0.45f), Pal.hsv(hue + 80, 0.45f, 0.18f))
        u = w / 22f; ox = w / 2; oy = h * 0.55f
        // base column
        val b = stack[0]
        for (k in -12..-1) drawSlab(c, b, k.toFloat())
        stack.forEachIndexed { i, s -> drawSlab(c, s, i.toFloat()) }
        cur?.let { drawSlab(c, it, stack.size.toFloat()) }
        for (f in falling) drawSlab(c, f.s, f.level - f.y)
        c.text("${stack.size - 1}", w / 2, h * 0.12f, 56 * dp, Pal.WHITE)
        if (flash > 0f) c.text(if (perfect >= 4) "HOÀN HẢO ×$perfect!" else "Hoàn hảo!", w / 2, h * 0.2f, 18 * dp, Pal.alpha(Pal.WHITE, flash))
        if (stack.size == 1 && !over) c.text("Chạm để thả khối", w / 2, h * 0.88f, 18 * dp, Pal.alpha(Pal.WHITE, 0.85f))
    }
}
