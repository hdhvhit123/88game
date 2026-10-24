package com.hgame.app

import android.app.Application
import com.hgame.engine.SoundBank

class HGameApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        SoundBank.enabled = Prefs.sound
        // Synthesising the effects takes a few ms; keep it off the launch path.
        Thread { SoundBank.init(this) }.start()
    }
}
