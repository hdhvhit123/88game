package com.hgame.engine

import android.graphics.Canvas
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Lightweight particle bursts for hits, explosions and sparkles. */
class Particles {
    private class P(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, val max: Float, val color: Int, val size: Float)

    private val list = ArrayList<P>()
    var gravity = 0f

    fun burst(x: Float, y: Float, color: Int, count: Int = 14, speed: Float = 200f, size: Float = 6f, life: Float = 0.6f) {
        repeat(count) {
            val a = Random.nextFloat() * 6.2832f
            val s = speed * (0.3f + Random.nextFloat() * 0.7f)
            val l = life * (0.6f + Random.nextFloat() * 0.4f)
            list.add(P(x, y, cos(a) * s, sin(a) * s, l, l, color, size * (0.6f + Random.nextFloat() * 0.6f)))
        }
    }

    fun update(dt: Float) {
        val it = list.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) { it.remove(); continue }
            p.vy += gravity * dt
            p.x += p.vx * dt; p.y += p.vy * dt
            p.vx *= 0.98f; p.vy *= 0.98f
        }
    }

    fun draw(c: Canvas) {
        for (p in list) {
            val k = p.life / p.max
            c.circle(p.x, p.y, p.size * (0.4f + 0.6f * k), Pal.alpha(p.color, k))
        }
    }

    fun clear() = list.clear()
    val isEmpty get() = list.isEmpty()
}
