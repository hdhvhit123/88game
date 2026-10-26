package com.hgame.ui

import android.os.Build
import android.view.View
import android.view.WindowInsets

/** Pads [view] by the system bar insets (the app draws edge-to-edge on Android 15+). */
fun applySystemBarPadding(view: View) {
    view.setOnApplyWindowInsetsListener { v, insets ->
        if (Build.VERSION.SDK_INT >= 30) {
            val i = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            v.setPadding(i.left, i.top, i.right, i.bottom)
        } else {
            @Suppress("DEPRECATION")
            v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
        }
        insets
    }
    view.requestApplyInsets()
}
