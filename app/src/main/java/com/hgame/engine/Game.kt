package com.hgame.engine

import android.graphics.Canvas

/** How the "best" record of a game is compared. */
enum class BestMode { HIGH, LOW, NONE }

/** A user-selectable setting shown in the game's settings dialog. */
class GameOption(val key: String, val label: String, val choices: List<String>, val default: Int = 0)

/**
 * Base class of every game. A game draws itself on a [Canvas] and receives touch input
 * through [down]/[move]/[up]. Real-time games set [realtime] so the host runs a frame loop
 * and calls [update] every frame; turn-based games only redraw when something changes,
 * unless they request frames with [animate] or [after].
 */
abstract class Game {
    lateinit var host: GameHost
    var w = 0f
    var h = 0f
    val dp: Float get() = host.density

    open val realtime: Boolean get() = false
    open val bestMode: BestMode get() = BestMode.HIGH
    open val options: List<GameOption> get() = emptyList()
    open val help: String get() = ""

    /** True while the game loop should not advance (pause overlay shown). */
    var paused = false
    /** True after [gameOver] until the next [newGame]. */
    var over = false
        private set

    private var animLeft = 0f
    private var generation = 0
    private var clockBase = 0L
    private var clockFrom = 0L
    private var clockRunning = false

    /** Starts a fresh round. Called once at start and on every restart. */
    abstract fun newGame()

    /** Recomputes geometry; called after a size change and after every [newGame]. */
    open fun layout() {}

    open fun update(dt: Float) {}
    abstract fun draw(c: Canvas)

    open fun down(x: Float, y: Float) {}
    open fun move(x: Float, y: Float) {}
    open fun up(x: Float, y: Float) {}

    /** Multi-touch hooks. By default only the first pointer is forwarded. */
    open fun pointerDown(id: Int, x: Float, y: Float) { if (id == 0) down(x, y) }
    open fun pointerMove(id: Int, x: Float, y: Float) { if (id == 0) move(x, y) }
    open fun pointerUp(id: Int, x: Float, y: Float) { if (id == 0) up(x, y) }

    open fun onPause() {}

    fun opt(key: String): Int {
        val o = options.first { it.key == key }
        return host.option(key, o.default).coerceIn(0, o.choices.size - 1)
    }

    fun redraw() = host.redraw()

    /** Keeps the frame loop alive for [seconds] so tweens can run. */
    fun animate(seconds: Float = 0.35f) {
        animLeft = maxOf(animLeft, seconds)
        host.redraw()
    }

    /** Runs [block] after [seconds] on the UI thread, unless the game was restarted meanwhile. */
    fun after(seconds: Float, block: () -> Unit) {
        val gen = generation
        host.schedule((seconds * 1000).toLong()) {
            if (gen == generation) { block(); host.redraw() }
        }
    }

    /**
     * Runs [task] on a worker thread (AI search) and hands the result to [done] on the UI thread,
     * unless the game was restarted meanwhile. [task] must only touch its own copies of state.
     */
    fun <T> background(task: () -> T, done: (T) -> Unit) {
        val gen = generation
        Thread {
            val r = task()
            host.schedule(0L) { if (gen == generation) { done(r); host.redraw() } }
        }.apply { priority = Thread.NORM_PRIORITY - 1 }.start()
    }

    /** Drops every pending [after] callback. */
    fun cancelTimers() { generation++ }

    // ---- stopwatch for timed puzzles ----
    /** Starts counting play time from zero; [onClock] is called every second. */
    fun startClock() {
        clockBase = 0L; clockFrom = android.os.SystemClock.elapsedRealtime(); clockRunning = true
        scheduleClock(generation)
    }

    fun stopClock() {
        if (clockRunning) { clockBase += android.os.SystemClock.elapsedRealtime() - clockFrom; clockRunning = false }
    }

    val clockSeconds: Int
        get() = ((clockBase + if (clockRunning) android.os.SystemClock.elapsedRealtime() - clockFrom else 0L) / 1000).toInt()

    /** Called each second while the clock runs. Timed games show the time as their score. */
    open fun onClock(seconds: Int) { if (bestMode == BestMode.LOW) score(seconds) }

    private var clockPaused = false
    internal fun pauseClock() { if (clockRunning) { stopClock(); clockPaused = true } }
    internal fun resumeClock() {
        if (clockPaused && !over) {
            clockPaused = false; clockFrom = android.os.SystemClock.elapsedRealtime(); clockRunning = true
            scheduleClock(generation)
        }
    }

    private fun scheduleClock(gen: Int) {
        host.schedule(1000L) {
            if (gen == generation && clockRunning && !over) { onClock(clockSeconds); scheduleClock(gen) }
        }
    }

    internal fun wantsFrames() = (realtime && !paused && !over) || animLeft > 0f

    internal fun tick(dt: Float) {
        if (animLeft > 0f) animLeft -= dt
        if (!paused && (realtime && !over || animLeft > 0f)) update(dt)
    }

    internal fun restart() {
        over = false
        paused = false
        animLeft = 0f
        generation++
        clockRunning = false; clockPaused = false; clockBase = 0L
        newGame()
        if (w > 0f) layout()
        host.redraw()
    }

    /** Ends the round and shows the result panel. */
    fun gameOver(win: Boolean, title: String, message: String = "", score: Int? = null) {
        if (over) return
        stopClock()
        over = true
        host.gameOver(win, title, message, score)
    }

    /** Lets play continue after [gameOver], e.g. when the player undoes the final move. */
    fun resumePlay() { over = false }

    fun score(value: Int) = host.setScore(value)
    fun info(text: String) = host.setInfo(text)
    fun sfx(s: Sfx) = host.play(s)
    fun buzz(ms: Long = 15) = host.vibrate(ms)
}
