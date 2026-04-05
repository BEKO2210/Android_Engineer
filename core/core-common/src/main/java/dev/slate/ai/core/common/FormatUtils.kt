package dev.slate.ai.core.common

import java.util.Locale

/**
 * Format bytes into a human-readable size string.
 */
fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(this.toDouble()) / Math.log10(1024.0)).toInt()
    val size = this / Math.pow(1024.0, digitGroups.toDouble())
    return String.format(Locale.US, "%.1f %s", size, units[digitGroups])
}

/**
 * Format a token generation rate.
 */
fun Float.formatTokenRate(): String {
    return String.format(Locale.US, "%.1f tok/s", this)
}

/**
 * Format a duration in milliseconds to a human-readable string.
 */
fun Long.formatDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
