package com.example.antitiktok

import android.content.Context

object AppState {

    private const val PREFS = "app_state"
    private const val KEY_RECORDING = "recording_enabled"
    private const val KEY_INTERVAL = "check_interval_min"

    fun isRecording(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_RECORDING, false)

    fun setRecording(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_RECORDING, value)
            .apply()
    }

    fun getCheckIntervalMin(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_INTERVAL, 5)

    fun setCheckIntervalMin(context: Context, value: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL, value.coerceIn(0, 20))
            .apply()
    }
}
