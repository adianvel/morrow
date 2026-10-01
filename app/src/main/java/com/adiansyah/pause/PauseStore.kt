package com.adiansyah.pause

import android.content.Context
import org.json.JSONObject
import java.time.LocalDate

class PauseStore(context: Context) {
    private val preferences = context.getSharedPreferences("pause", Context.MODE_PRIVATE)

    fun limits(): Map<String, Int> {
        val json = JSONObject(preferences.getString(KEY_LIMITS, "{}") ?: "{}")
        return buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val packageName = keys.next()
                val minutes = json.optInt(packageName, 0)
                if (minutes > 0) put(packageName, minutes)
            }
        }
    }

    fun saveLimits(limits: Map<String, Int>) {
        val json = JSONObject()
        limits.forEach { (packageName, minutes) ->
            if (minutes > 0) json.put(packageName, minutes.coerceIn(5, 240))
        }
        preferences.edit().putString(KEY_LIMITS, json.toString()).apply()
    }

    fun limitMinutes(packageName: String): Int? = limits()[packageName]

    fun usedMillis(packageName: String): Long {
        resetUsageForToday()
        return usageJson().optLong(packageName, 0L).coerceAtLeast(0L)
    }

    fun addUsage(packageName: String, millis: Long) {
        if (millis <= 0L) return
        resetUsageForToday()
        val usage = usageJson()
        usage.put(packageName, usage.optLong(packageName, 0L) + millis)
        preferences.edit()
            .putString(KEY_USAGE, usage.toString())
            .apply()
    }

    fun resetUsageForToday(): Boolean {
        val currentDay = today()
        if (preferences.getString(KEY_USAGE_DAY, null) == currentDay) return false
        preferences.edit()
            .putString(KEY_USAGE_DAY, currentDay)
            .putString(KEY_USAGE, "{}")
            .apply()
        return true
    }

    private fun usageJson() = JSONObject(preferences.getString(KEY_USAGE, "{}") ?: "{}")

    private fun today() = LocalDate.now().toString()

    private companion object {
        const val KEY_LIMITS = "limits"
        const val KEY_USAGE_DAY = "usage_day"
        const val KEY_USAGE = "usage"
    }
}

data class UsageAllowance(val usedMillis: Long, val limitMinutes: Int) {
    val limitMillis: Long get() = limitMinutes * MILLIS_PER_MINUTE
    val remainingMillis: Long get() = (limitMillis - usedMillis).coerceAtLeast(0L)
    val isExhausted: Boolean get() = usedMillis >= limitMillis
    val progress: Float get() = (usedMillis.toFloat() / limitMillis).coerceIn(0f, 1f)

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
