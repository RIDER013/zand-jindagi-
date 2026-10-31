package com.rider.assistant

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class RiderAccessibilityService : AccessibilityService() {

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private var isWatchingReels = false
    private var lastSwipeTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // If ShortsAccessibilityService is active, it handles detection
        if (ShortsAccessibilityService.instance != null) return

        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        if (pkg == "com.instagram.android" || pkg == "com.google.android.youtube") {
            if (!isWatchingReels) {
                isWatchingReels = true
                scheduleFallbackTimer()
            }
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                scheduleFallbackTimer()
            }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (pkg != "android" && pkg != "com.android.systemui" && pkg != packageName) {
                isWatchingReels = false
                autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
            }
        }
    }

    private fun scheduleFallbackTimer() {
        autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
        autoScrollRunnable = Runnable {
            if (isWatchingReels) {
                Log.d(TAG, "Fallback auto-scroll: swiping Reel!")
                swipeUp()
                scheduleFallbackTimer()
            }
        }
        mainHandler.postDelayed(autoScrollRunnable!!, 15000L)
    }

    override fun onInterrupt() {
        Log.d(TAG, "RiderAccessibilityService interrupted")
        autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
    }

    /**
     * Intercepts Bluetooth earbud touch sensor taps (Media keys like Headset Hook, Play/Pause, Next, Skip)
     * system-wide at Accessibility Service level to trigger instant upward swipe.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val keyCode = event.keyCode
            val isEarbudTouch = when (keyCode) {
                KeyEvent.KEYCODE_HEADSETHOOK,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE,
                KeyEvent.KEYCODE_MEDIA_NEXT,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS,
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_STOP,
                KeyEvent.KEYCODE_VOICE_ASSIST,
                KeyEvent.KEYCODE_CALL -> true
                else -> false
            }
            if (isEarbudTouch) {
                Log.d(TAG, "Bluetooth earbud touch sensor intercepted (KeyCode: $keyCode)! Triggering swipeUp()...")
                swipeUp()
                return true // Consumes the key event to avoid unwanted audio playback
            }
        }
        return super.onKeyEvent(event)
    }

    /**
     * Executes the upward swipe gesture on the center of the screen
     * Duration: 350ms
     * Start: Center X, 82% Y (bottom area)
     * End: Center X, 25% Y (top area)
     * Operates system-wide, even when Rider Assistant is minimized or closed.
     */
    fun swipeUp(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastSwipeTime < 400) {
            // Debounce rapid duplicate taps
            return false
        }
        lastSwipeTime = now

        triggerHapticFeedback()

        return try {
            val metrics = resources.displayMetrics
            val x = metrics.widthPixels / 2f
            val startY = metrics.heightPixels * 0.72f
            val endY = metrics.heightPixels * 0.22f

            val path = Path().apply {
                moveTo(x, startY)
                lineTo(x, endY)
            }

            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(path, 0, 260)
                )
                .build()

            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "Swipe gesture completed successfully")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Swipe gesture cancelled")
                }
            }, null)

            Log.d(TAG, "Swipe gesture dispatched: $dispatched")
            dispatched
        } catch (e: Exception) {
            Log.e(TAG, "Error executing swipe: ${e.message}")
            false
        }
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(45)
                }
            }
        } catch (e: Exception) {}
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "RiderAccessibilityService connected successfully")
    }

    override fun onDestroy() {
        autoScrollRunnable?.let { mainHandler.removeCallbacks(it) }
        autoScrollRunnable = null
        instance = null
        super.onDestroy()
        Log.d(TAG, "RiderAccessibilityService destroyed")
    }

    companion object {
        private const val TAG = "RiderAccessibility"

        @Volatile
        var instance: RiderAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null
    }
}
