package com.speedometer.app.util

import java.util.Locale
import kotlin.math.roundToInt

object Format {

    fun speed(kmh: Float, mph: Boolean): String =
        String.format(Locale.US, "%.0f", if (mph) kmh * 0.621371f else kmh)

    fun distance(m: Double, kmUnit: Boolean): String {
        val value = if (kmUnit) m / 1000.0 else m * 3.28084
        val unit = if (kmUnit) "km" else "ft"
        return when {
            value >= 100 -> String.format(Locale.US, "%.1f %s", value, unit)
            value >= 1 -> String.format(Locale.US, "%.2f %s", value, unit)
            else -> String.format(Locale.US, "%.0f m", m)
        }
    }

    fun elapsed(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) {
            String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    fun accuracyM(m: Float): String = "${m.roundToInt()} m"
}
