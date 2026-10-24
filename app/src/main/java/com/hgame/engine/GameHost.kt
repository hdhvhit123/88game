package com.hgame.engine

/** Services the hosting screen provides to a running [Game]. */
interface GameHost {
    val density: Float
    fun redraw()
    /** Runs [block] on the UI thread after [ms] milliseconds. */
    fun schedule(ms: Long, block: () -> Unit)
    fun setScore(value: Int)
    fun setInfo(text: String)
    fun gameOver(win: Boolean, title: String, message: String, score: Int?)
    fun play(s: Sfx)
    fun vibrate(ms: Long)
    fun option(key: String, default: Int): Int
    /** Small persistent per-game storage (levels reached, stats, ...). */
    fun loadInt(key: String, default: Int): Int
    fun saveInt(key: String, value: Int)
}
