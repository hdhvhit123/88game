package com.hgame.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.hgame.engine.BestMode
import com.hgame.engine.Game
import com.hgame.engine.GameHost
import com.hgame.engine.GameView
import com.hgame.engine.Sfx
import com.hgame.engine.SoundBank
import com.hgame.games.Catalog
import com.hgame.games.GameInfo
import com.hgame.ui.Overlay
import com.hgame.ui.TopBar
import com.hgame.ui.applySystemBarPadding

/** Hosts one game: header, game canvas and the result/pause overlay. */
class GameActivity : Activity(), GameHost {
    private lateinit var info: GameInfo
    private lateinit var game: Game
    private lateinit var view: GameView
    private lateinit var bar: TopBar
    private lateinit var overlay: Overlay
    private var currentScore: Int? = null
    private var bestAtStart: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val found = Catalog.byId(intent.getStringExtra(EXTRA_ID) ?: "")
        if (found == null) { finish(); return }
        info = found
        game = info.factory()
        game.host = this
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val accent = info.color1.toInt()
        val root = FrameLayout(this)
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        bar = TopBar(this, info.title, accent, game.options.isNotEmpty())
        view = GameView(this, game)
        column.addView(bar, LinearLayout.LayoutParams(-1, -2))
        column.addView(view, LinearLayout.LayoutParams(-1, 0, 1f))
        overlay = Overlay(this, accent)
        root.addView(column, FrameLayout.LayoutParams(-1, -1))
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        applySystemBarPadding(root)

        bar.onAction = { a ->
            when (a) {
                TopBar.Action.BACK -> finish()
                TopBar.Action.RESTART -> restart()
                TopBar.Action.HELP -> showHelp()
                TopBar.Action.OPTIONS -> showOptions()
            }
        }
        showBest()
        bestAtStart = Prefs.best(info.id)
        if (intent.getBooleanExtra(SelfTest.EXTRA, false)) {
            SelfTest.drive(this, view, info.id) { setResult(RESULT_OK); finish() }
            return
        }
        Prefs.pushRecent(info.id)
        Prefs.addPlay(info.id)
    }

    private fun restart() {
        overlay.hide()
        currentScore = null
        bestAtStart = Prefs.best(info.id)
        bar.score = ""
        bar.info = ""
        game.restart()
        view.kick()
    }

    private fun showBest() {
        val b = Prefs.best(info.id)
        bar.best = if (b == null || game.bestMode == BestMode.NONE) "" else formatScore(b)
    }

    private fun formatScore(v: Int): String =
        if (game.bestMode == BestMode.LOW) "%d:%02d".format(v / 60, v % 60) else v.toString()

    private fun showHelp() {
        pauseGame(showPanel = false)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(info.title)
            .setMessage(game.help.ifEmpty { info.tagline })
            .setPositiveButton("Đã hiểu", null)
            .setOnDismissListener { resumeGame() }
            .show()
    }

    private fun showOptions() {
        pauseGame(showPanel = false)
        val opts = game.options
        val labels = opts.map { "${it.label}: ${it.choices[game.opt(it.key)]}" }.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Tuỳ chọn")
            .setItems(labels) { _, which ->
                val o = opts[which]
                AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(o.label)
                    .setSingleChoiceItems(o.choices.toTypedArray(), game.opt(o.key)) { dlg, choice ->
                        Prefs.setOption(info.id, o.key, choice)
                        dlg.dismiss()
                        restart()
                    }
                    .setOnDismissListener { resumeGame() }
                    .show()
            }
            .setNegativeButton("Đóng", null)
            .setOnCancelListener { resumeGame() }
            .setOnDismissListener { resumeGame() }
            .show()
    }

    private fun pauseGame(showPanel: Boolean) {
        if (game.over || overlay.showing || game.paused) return
        game.onPause()
        game.pauseClock()
        if (game.realtime) {
            game.paused = true
            if (showPanel) {
                overlay.show("⏸", "Tạm dừng", listOf("Chạm Tiếp tục để chơi tiếp"), "Tiếp tục", "Chơi lại",
                    onPrimary = { overlay.hide(); resumeGame() }, onSecondary = { restart() })
            }
        }
    }

    private fun resumeGame() {
        if (overlay.showing) return
        game.paused = false
        game.resumeClock()
        view.kick()
    }

    override fun onPause() {
        super.onPause()
        view.active = false
        pauseGame(showPanel = true)
    }

    override fun onResume() {
        super.onResume()
        view.active = true
        if (!game.realtime && !overlay.showing) resumeGame()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (overlay.showing && !game.over) { overlay.hide(); resumeGame(); return }
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    // ---- GameHost ----
    override val density: Float get() = resources.displayMetrics.density

    override fun redraw() { if (::view.isInitialized) view.kick() }

    override fun schedule(ms: Long, block: () -> Unit) { view.postDelayed({ if (!isFinishing) block() }, ms) }

    override fun setScore(value: Int) {
        currentScore = value
        bar.score = if (game.bestMode == BestMode.LOW) "⏱ ${formatScore(value)}" else "Điểm $value"
        if (game.bestMode == BestMode.HIGH) {
            val b = Prefs.best(info.id)
            if (b == null || value > b) { Prefs.setBest(info.id, value); showBest() }
        }
    }

    override fun setInfo(text: String) { bar.info = text }

    override fun gameOver(win: Boolean, title: String, message: String, score: Int?) {
        val s = score ?: currentScore
        var record = false
        if (s != null && game.bestMode != BestMode.NONE) {
            val b = Prefs.best(info.id)
            if (game.bestMode == BestMode.HIGH) {
                record = s > 0 && (bestAtStart == null || s > bestAtStart!!)
                if (b == null || s > b) Prefs.setBest(info.id, s)
            } else if (win && (b == null || s < b)) {
                Prefs.setBest(info.id, s); record = true
            }
            showBest()
        }
        play(if (win) Sfx.WIN else Sfx.LOSE)
        vibrate(if (win) 30 else 60)
        val lines = buildList {
            if (message.isNotEmpty()) add(message)
            if (s != null && game.bestMode != BestMode.NONE) {
                add((if (game.bestMode == BestMode.LOW) "Thời gian: " else "Điểm: ") + formatScore(s) + if (record) "  ★ Kỷ lục!" else "")
            }
        }
        val emoji = if (win) "🏆" else "💥"
        view.postDelayed({
            if (!isFinishing) overlay.show(emoji, title, lines, "Chơi lại", "Xem bàn",
                onPrimary = { restart() }, onSecondary = { overlay.hide() })
        }, 450)
    }

    override fun play(s: Sfx) = SoundBank.play(s)

    override fun vibrate(ms: Long) {
        if (!Prefs.vibration) return
        if (ms <= 15) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            return
        }
        val v = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        else @Suppress("DEPRECATION") v.vibrate(ms)
    }

    override fun option(key: String, default: Int) = Prefs.option(info.id, key, default)
    override fun loadInt(key: String, default: Int) = Prefs.getInt(info.id, key, default)
    override fun saveInt(key: String, value: Int) = Prefs.putInt(info.id, key, value)

    companion object { const val EXTRA_ID = "game_id" }
}
