package com.tibarra.gymhelper.util

import android.app.Activity
import android.view.WindowManager

object WakeLockManager {
    fun keepScreenOn(activity: Activity, on: Boolean) {
        if (on) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
