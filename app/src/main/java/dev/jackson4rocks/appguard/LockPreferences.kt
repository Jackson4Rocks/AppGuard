package dev.jackson4rocks.appguard

import android.content.Context

enum class LockTiming(
    val key: String,
    val label: String,
    val timeoutMs: Long
) {
    IMMEDIATELY("immediately", "Immediately", 0L),
    FIVE_SECONDS("five_seconds", "After 5 seconds", 5_000L),
    THIRTY_SECONDS("thirty_seconds", "After 30 seconds", 30_000L),
    ONE_MINUTE("one_minute", "After 1 minute", 60_000L);

    companion object {
        fun fromKey(key: String): LockTiming =
            entries.firstOrNull { it.key == key } ?: IMMEDIATELY
    }
}

class LockPreferences(context: Context) {
    private val prefs =
        context.getSharedPreferences("appguard_lock_preferences", Context.MODE_PRIVATE)

    fun timing(): LockTiming =
        LockTiming.fromKey(
            prefs.getString(KEY_TIMING, LockTiming.IMMEDIATELY.key).orEmpty()
        )

    fun setTiming(value: LockTiming) {
        prefs.edit().putString(KEY_TIMING, value.key).apply()
    }

    fun lockOnScreenOff(): Boolean =
        prefs.getBoolean(KEY_SCREEN_OFF, true)

    fun setLockOnScreenOff(value: Boolean) {
        prefs.edit().putBoolean(KEY_SCREEN_OFF, value).apply()
    }

    companion object {
        private const val KEY_TIMING = "timing"
        private const val KEY_SCREEN_OFF = "screen_off"
    }
}
