package com.hgame.engine

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View

/** Hosts a [Game]: drives the frame loop with [Choreographer] and forwards touches. */
@SuppressLint("ViewConstructor")
class GameView(context: Context, val game: Game) : View(context), Choreographer.FrameCallback {
    private var looping = false
    private var lastFrame = 0L
    private var started = false
    var active = true
        set(v) { field = v; if (v) kick() }

    fun kick() {
        invalidate()
        if (!looping && active && started && game.wantsFrames()) {
            looping = true
            lastFrame = 0L
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun doFrame(frameTimeNanos: Long) {
        val dt = if (lastFrame == 0L) 1f / 60f else ((frameTimeNanos - lastFrame) / 1e9f).coerceIn(0f, 1f / 20f)
        lastFrame = frameTimeNanos
        game.tick(dt)
        invalidate()
        if (active && isAttachedToWindow && game.wantsFrames()) {
            Choreographer.getInstance().postFrameCallback(this)
        } else {
            looping = false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        game.w = w.toFloat()
        game.h = h.toFloat()
        if (w == 0 || h == 0) return
        if (!started) {
            started = true
            game.restart()
        } else {
            game.layout()
        }
        kick()
    }

    override fun onDraw(canvas: Canvas) {
        if (started) game.draw(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (!started || game.paused) return true
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                val i = e.actionIndex
                game.pointerDown(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) {
                game.pointerMove(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val i = e.actionIndex
                game.pointerUp(e.getPointerId(i), e.getX(i), e.getY(i))
            }
            MotionEvent.ACTION_CANCEL -> for (i in 0 until e.pointerCount) {
                game.pointerUp(e.getPointerId(i), e.getX(i), e.getY(i))
            }
        }
        kick()
        return true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Choreographer.getInstance().removeFrameCallback(this)
        looping = false
    }
}
