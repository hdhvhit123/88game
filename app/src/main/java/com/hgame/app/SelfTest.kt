package com.hgame.app

import android.app.Activity
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.View
import java.io.File
import java.io.FileOutputStream
import kotlin.random.Random

/**
 * QA harness: when a game is opened with the self-test extra, feeds it random taps and swipes,
 * then saves a screenshot. Started with
 * `am start -n com.hgame.arcade/com.hgame.app.MainActivity --ez selftest true [--ei from N]`.
 */
object SelfTest {
    const val EXTRA = "selftest"
    const val TAG = "HGameSelfTest"

    fun drive(activity: Activity, target: View, id: String, events: Int = 45, done: () -> Unit) {
        val rnd = Random(id.hashCode())
        var k = 0
        fun next() {
            if (activity.isFinishing) return
            if (k >= events) {
                target.postDelayed({ capture(activity, id); done() }, 600)
                return
            }
            k++
            val w = target.width.toFloat(); val h = target.height.toFloat()
            val x = rnd.nextFloat() * w * 0.9f + w * 0.05f
            val y = rnd.nextFloat() * h * 0.9f + h * 0.05f
            if (k % 5 == 0) {
                val dx = (rnd.nextFloat() - 0.5f) * w * 0.6f; val dy = (rnd.nextFloat() - 0.5f) * h * 0.4f
                gesture(target, x, y, x + dx, y + dy, 6)
            } else gesture(target, x, y, x, y, 1)
            target.postDelayed({ next() }, 140)
        }
        target.postDelayed({ next() }, 900)
    }

    private fun gesture(v: View, x0: Float, y0: Float, x1: Float, y1: Float, steps: Int) {
        val t = SystemClock.uptimeMillis()
        v.dispatchTouchEvent(MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, x0, y0, 0))
        for (i in 1..steps) {
            val f = i.toFloat() / steps
            v.dispatchTouchEvent(MotionEvent.obtain(t, t + i * 16, MotionEvent.ACTION_MOVE, x0 + (x1 - x0) * f, y0 + (y1 - y0) * f, 0))
        }
        v.dispatchTouchEvent(MotionEvent.obtain(t, t + steps * 16 + 30, MotionEvent.ACTION_UP, x1, y1, 0))
    }

    private fun capture(activity: Activity, id: String) {
        try {
            val root = activity.window.decorView
            val bmp = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            root.draw(android.graphics.Canvas(bmp))
            val dir = File(activity.getExternalFilesDir(null), "shots").apply { mkdirs() }
            FileOutputStream(File(dir, "$id.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
        } catch (e: Exception) {
            Log.w(TAG, "screenshot failed for $id", e)
        }
        Log.i(TAG, "ok $id")
    }
}
