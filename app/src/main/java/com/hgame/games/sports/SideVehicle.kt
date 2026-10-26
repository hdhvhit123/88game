package com.hgame.games.sports

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Small Verlet-integrated two-wheel vehicle for side-view driving games. World units are metres-ish,
 * y grows downwards. [ground] returns the terrain height at x.
 */
class SideVehicle(val wheelR: Float, val base: Float, val height: Float, private val ground: (Float) -> Float) {
    class P(var x: Float, var y: Float) { var px = x; var py = y }

    val rear = P(0f, 0f); val front = P(0f, 0f); val head = P(0f, 0f)
    var rearSpin = 0f; var frontSpin = 0f
    var rearOnGround = false; var frontOnGround = false
    var crashed = false
    private val pts get() = arrayOf(rear, front, head)
    private val lenRF get() = base
    private val lenRH get() = hypot(base * 0.35f, height)
    private val lenFH get() = hypot(base * 0.65f, height)

    fun place(x: Float) {
        val gy = ground(x)
        rear.x = x; rear.y = ground(x) - wheelR - 0.05f
        front.x = x + base; front.y = ground(x + base) - wheelR - 0.05f
        head.x = x + base * 0.35f; head.y = minOf(rear.y, front.y) - height
        for (p in pts) { p.px = p.x; p.py = p.y }
        crashed = false; rearSpin = 0f; frontSpin = 0f
        if (gy.isNaN()) crashed = true
    }

    val angle get() = atan2(front.y - rear.y, front.x - rear.x)
    val speed get() = (rear.x - rear.px + front.x - front.px) / 2f
    val x get() = (rear.x + front.x) / 2

    /** throttle in -1..1 (negative = brake/reverse). */
    fun step(dt: Float, throttle: Float, gravity: Float = 22f, power: Float = 14f, topSpeed: Float = 13f) {
        for (p in pts) {
            val vx = (p.x - p.px) * 0.9975f; val vy = (p.y - p.py) * 0.999f
            p.px = p.x; p.py = p.y
            p.x += vx; p.y += vy + gravity * dt * dt
        }
        // drive: push wheels along the ground tangent
        if (throttle != 0f) {
            val ang = angle
            val tx = cos(ang); val ty = sin(ang)
            if (rearOnGround || frontOnGround) {
                // engine torque fades towards top speed (in world units per second)
                val v = speed / dt
                val fade = if (v * throttle > 0) (1f - kotlin.math.abs(v) / topSpeed).coerceIn(0f, 1f) else 1f
                val acc = throttle * power * fade * dt * dt
                // traction moves the whole body along the chassis direction
                for (p in pts) { p.x += tx * acc; p.y += ty * acc }
            } else {
                // in the air the throttle rotates the vehicle (lean back on gas, forward on brake)
                val rot = -throttle * 3.2f * dt * dt
                val cx = (rear.x + front.x) / 2; val cy = (rear.y + front.y) / 2
                for (p in pts) { val dx = p.x - cx; val dy = p.y - cy; p.x += -dy * rot * 10f; p.y += dx * rot * 10f }
            }
        }
        repeat(6) {
            constrain(rear, front, lenRF); constrain(rear, head, lenRH); constrain(front, head, lenFH)
            rearOnGround = collide(rear); frontOnGround = collide(front)
        }
        rearSpin += (rear.x - rear.px) / wheelR; frontSpin += (front.x - front.px) / wheelR
        if (head.y > ground(head.x) - 0.15f) crashed = true
    }

    private fun constrain(a: P, b: P, len: Float) {
        val dx = b.x - a.x; val dy = b.y - a.y; val d = sqrt(dx * dx + dy * dy).coerceAtLeast(1e-4f)
        val diff = (d - len) / d * 0.5f
        a.x += dx * diff; a.y += dy * diff; b.x -= dx * diff; b.y -= dy * diff
    }

    private fun collide(p: P): Boolean {
        val e = 0.05f
        val gy = ground(p.x)
        val slope = (ground(p.x + e) - ground(p.x - e)) / (2 * e)
        val nl = sqrt(1 + slope * slope); val nx = slope / nl; val ny = -1f / nl   // normal pointing up
        val dist = (gy - p.y) / nl   // distance above ground along the normal (approx)
        if (dist < wheelR) {
            val pen = wheelR - dist
            p.x += nx * pen; p.y += ny * pen
            // friction: kill some tangential velocity and bounce
            val vx = p.x - p.px; val vy = p.y - p.py
            val vn = vx * nx + vy * ny
            if (vn < 0) { p.px += vn * nx * 1.2f; p.py += vn * ny * 1.2f }
            return true
        }
        return false
    }
}
