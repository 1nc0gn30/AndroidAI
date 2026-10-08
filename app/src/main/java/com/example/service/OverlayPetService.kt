package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class OverlayPetService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayLayout: View? = null

    companion object {
        const val ACTION_START = "com.example.action.START_PET_OVERLAY"
        const val ACTION_STOP = "com.example.action.STOP_PET_OVERLAY"
        const val EXTRA_SPEECH = "extra_pet_speech"
        const val CHANNEL_ID = "omnipet_overlay_channel"
        const val NOTIFICATION_ID = 1001

        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        val speech = intent?.getStringExtra(EXTRA_SPEECH) ?: "I'm watching over your apps! Tap me anytime."
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        isRunning = true

        if (Settings.canDrawOverlays(this)) {
            showFloatingPet(speech)
        }

        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingPet(speechText: String) {
        if (overlayLayout != null) {
            updateSpeechBubble(speechText)
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16, 16, 16, 16)
        }

        val speechBubble = TextView(this).apply {
            id = View.generateViewId()
            text = speechText
            textSize = 12f
            setTextColor(Color.WHITE)
            maxLines = 3
            val bg = GradientDrawable().apply {
                setColor(0xEE1E1B4B.toInt())
                cornerRadius = 24f
                setStroke(2, 0xFF6366F1.toInt())
            }
            background = bg
            setPadding(20, 12, 20, 12)
            elevation = 10f
        }

        val petAvatarContainer = FrameLayout(this).apply {
            val sizePx = (64 * resources.displayMetrics.density).toInt()
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                topMargin = 10
            }
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(0xFF6366F1.toInt(), 0xFF8B5CF6.toInt())
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = sizePx.toFloat()
                setStroke(4, 0xFFF472B6.toInt())
            }
            background = bg
            elevation = 12f

            val emojiView = TextView(context).apply {
                text = "🐱"
                textSize = 28f
                gravity = Gravity.CENTER
            }
            addView(emojiView)
        }

        rootLayout.addView(speechBubble)
        rootLayout.addView(petAvatarContainer)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoving = false

        rootLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoving = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        isMoving = true
                    }
                    params.x = initialX + deltaX
                    params.y = initialY + deltaY
                    windowManager?.updateViewLayout(rootLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoving) {
                        val appIntent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        startActivity(appIntent)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(rootLayout, params)
            overlayLayout = rootLayout
        } catch (_: Exception) {}
    }

    private fun updateSpeechBubble(newSpeech: String) {
        val root = overlayLayout as? LinearLayout ?: return
        val bubble = root.getChildAt(0) as? TextView
        bubble?.text = newSpeech
    }

    private fun stopOverlay() {
        if (overlayLayout != null && windowManager != null) {
            try {
                windowManager?.removeView(overlayLayout)
            } catch (_: Exception) {}
            overlayLayout = null
        }
        isRunning = false
    }

    override fun onDestroy() {
        stopOverlay()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OmniPet AI Assistant",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps your floating screen pet assistant active over apps"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OmniPet AI is Active")
            .setContentText("Tap to open assistant & task automator")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
