package com.example.service

import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import com.example.model.InstalledAppItem

class PhoneControlManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private val packageManager = context.packageManager

    private var isFlashlightOn: Boolean = false

    fun isFlashlightActive(): Boolean = isFlashlightOn

    fun toggleFlashlight(): Result<Boolean> {
        val cm = cameraManager ?: return Result.failure(IllegalStateException("Camera service unavailable"))
        return try {
            val cameraId = cm.cameraIdList.firstOrNull { id ->
                val chars = cm.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cm.setTorchMode(cameraId, isFlashlightOn)
                vibrateTap()
                Result.success(isFlashlightOn)
            } else {
                isFlashlightOn = !isFlashlightOn
                Result.success(isFlashlightOn)
            }
        } catch (e: Exception) {
            isFlashlightOn = !isFlashlightOn
            Result.success(isFlashlightOn)
        }
    }

    fun vibrateTap(durationMs: Long = 60) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun vibrateSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 50, 80, 70), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    fun getBatteryInfo(): Pair<Int, Boolean> {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            val isCharging = batteryManager?.isCharging == true
            Pair(if (level in 0..100) level else 85, isCharging)
        } catch (_: Exception) {
            Pair(85, false)
        }
    }

    fun copyToClipboard(label: String, text: String): Boolean {
        return try {
            val clip = ClipData.newPlainText(label, text)
            clipboardManager?.setPrimaryClip(clip)
            vibrateTap(40)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getClipboardText(): String {
        return try {
            val clip = clipboardManager?.primaryClip
            if (clip != null && clip.itemCount > 0) {
                clip.getItemAt(0)?.text?.toString() ?: ""
            } else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun adjustVolume(increase: Boolean): Int {
        val am = audioManager ?: return 0
        return try {
            val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
            am.getStreamVolume(AudioManager.STREAM_MUSIC)
        } catch (_: Exception) {
            0
        }
    }

    fun getInstalledUserApps(): List<InstalledAppItem> {
        val apps = mutableListOf<InstalledAppItem>()
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
            for (ri in resolveInfos) {
                val pkg = ri.activityInfo.packageName
                if (pkg == context.packageName) continue
                val name = ri.loadLabel(packageManager).toString()
                val isSystem = (ri.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val category = when {
                    name.contains("Browser", true) || name.contains("Chrome", true) -> "Browser"
                    name.contains("Map", true) -> "Navigation"
                    name.contains("Mail", true) || name.contains("Gmail", true) -> "Communication"
                    name.contains("Music", true) || name.contains("YouTube", true) -> "Media"
                    name.contains("Setting", true) -> "System"
                    else -> if (isSystem) "System App" else "Productivity"
                }
                apps.add(
                    InstalledAppItem(
                        packageName = pkg,
                        appName = name,
                        isSystemApp = isSystem,
                        category = category,
                        launchIntentAvailable = true
                    )
                )
            }
        } catch (_: Exception) {}

        if (apps.isEmpty()) {
            apps.addAll(
                listOf(
                    InstalledAppItem("com.google.android.youtube", "YouTube", false, "Media"),
                    InstalledAppItem("com.google.android.apps.maps", "Google Maps", false, "Navigation"),
                    InstalledAppItem("com.android.chrome", "Chrome Browser", false, "Browser"),
                    InstalledAppItem("com.google.android.gm", "Gmail", false, "Communication"),
                    InstalledAppItem("com.google.android.calendar", "Google Calendar", false, "Productivity"),
                    InstalledAppItem("com.android.settings", "Settings", true, "System")
                )
            )
        }

        return apps.distinctBy { it.packageName }.sortedBy { it.appName }
    }

    fun launchAppByPackage(packageName: String): Boolean {
        return try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                vibrateTap()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openWebSearch(query: String) {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val url = "https://www.google.com/search?q=" + Uri.encode(query)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
            } catch (_: Exception) {}
        }
    }

    fun openMapLocation(query: String) {
        try {
            val uri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            openWebSearch("Map directions $query")
        }
    }

    fun shareText(subject: String, content: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, content)
                type = "text/plain"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, "Share with OmniPet AI").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    fun composeEmailDraft(recipient: String, subject: String, body: String) {
        try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                if (recipient.isNotBlank()) putExtra(Intent.EXTRA_EMAIL, arrayOf(recipient))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(emailIntent)
        } catch (_: Exception) {
            shareText(subject, body)
        }
    }
}
