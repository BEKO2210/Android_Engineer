package dev.slate.ai.core.common

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs

data class DeviceInfo(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val availableStorageMb: Long,
    val cpuCores: Int,
    val deviceTier: DeviceTier,
)

enum class DeviceTier {
    LOW,        // < 4 GB RAM — not recommended
    BASIC,      // 4-5 GB RAM — tiny models only
    STANDARD,   // 6-7 GB RAM — balanced models
    HIGH,       // 8+ GB RAM — all models
}

object DeviceCapability {

    fun getDeviceInfo(context: Context): DeviceInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val availableRamMb = memInfo.availMem / (1024 * 1024)
        val cpuCores = Runtime.getRuntime().availableProcessors()

        val storagePath = context.getExternalFilesDir(null)?.absolutePath
            ?: context.filesDir.absolutePath
        val stat = StatFs(storagePath)
        val availableStorageMb = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024)

        val tier = when {
            totalRamMb < 4096 -> DeviceTier.LOW
            totalRamMb < 6144 -> DeviceTier.BASIC
            totalRamMb < 8192 -> DeviceTier.STANDARD
            else -> DeviceTier.HIGH
        }

        return DeviceInfo(
            totalRamMb = totalRamMb,
            availableRamMb = availableRamMb,
            availableStorageMb = availableStorageMb,
            cpuCores = cpuCores,
            deviceTier = tier,
        )
    }

    fun getRecommendedModelId(tier: DeviceTier): String {
        return when (tier) {
            DeviceTier.LOW -> "smollm2-1.7b-q4"      // smallest, may still struggle
            DeviceTier.BASIC -> "smollm2-1.7b-q4"
            DeviceTier.STANDARD -> "qwen-2.5-3b-q4"
            DeviceTier.HIGH -> "phi-3-mini-q4"
        }
    }

    fun getTierDescription(tier: DeviceTier): String {
        return when (tier) {
            DeviceTier.LOW -> "Your device has limited memory. Smaller models may work, but performance could be slow."
            DeviceTier.BASIC -> "Your device can run lightweight models well."
            DeviceTier.STANDARD -> "Your device can run most models comfortably."
            DeviceTier.HIGH -> "Your device can run all available models."
        }
    }

    fun canRunModel(context: Context, minRamMb: Int): Boolean {
        val info = getDeviceInfo(context)
        return info.totalRamMb >= minRamMb
    }
}
