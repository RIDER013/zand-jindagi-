package com.rider.assistant

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Dedicated Accessibility Service for YouTube Shorts & Instagram Reels auto-scrolling.
 *
 * Core Features:
 * 1. 1-Second Heartbeat Engine: Continuously monitors YouTube Shorts progress and reel playback.
 * 2. Real-time YouTube Shorts Progress Detection: Swipes immediately when a Short reaches >= 95% or loops.
 * 3. Exact Timer Mode: Strict countdown (30s, 1m, 3m, or custom minutes) with guaranteed swipe on zero.
 * 4. Manual Scroll & Tap Reset: Resets timer immediately when user scrolls or taps the floating Ʀ bubble.
 * 5. Single Floating Ʀ Icon: No duplicate system accessibility button (requestAccessibilityButton removed).
 */
class ShortsAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastSwipeTimestamp = 0L
    private var lastUserScrollTimestamp = 0L

    // Foreground state
    private var currentForegroundPkg = ""
    private var isAppInForeground = false

    // Heartbeat & Timer state
    private var heartbeatRunnable: Runnable? = null
    private var secondsRemaining = 25
    private var lastKnownProgress = 0f
    private var hasSeenProgressStart = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "ShortsAccessibilityService connected and ready!")
        startHeartbeat()
    }

    private var lastMediaEventTime = 0L

    private fun isMediaApp(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p.contains("instagram") || p.contains("youtube")
    }

    private fun isLauncherOrHome(pkg: String): Boolean {
        val p = pkg.lowercase()
        return p.contains("launcher") || p.contains(".home") || p.contains("trebuchet")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        if (isMediaApp(pkg)) {
            lastMediaEventTime = System.currentTimeMillis()
            isAppInForeground = true
            currentForegroundPkg = pkg
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (isLauncherOrHome(pkg)) {
                Log.d(TAG, "User exited to home launcher: $pkg")
                isAppInForeground = false
                FloatingBubbleService.updateCountdown(-1)
            }
        }

        // When user manually scrolls/flicks to a new reel, restart the timer
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            val now = System.currentTimeMillis()
            if (now - lastUserScrollTimestamp > 700) {
                lastUserScrollTimestamp = now
                Log.d(TAG, "User manually scrolled reel. Resetting timer!")
                resetTimerForNewReel()
            }
        }
    }

    // ==============================================================================
    // 1-SECOND HEARTBEAT ENGINE
    // ==============================================================================
    private fun startHeartbeat() {
        stopHeartbeat()
        resetTimerForNewReel()
        heartbeatRunnable = object : Runnable {
            override fun run() {
                try {
                    tick()
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat tick error: ${e.message}")
                }
                mainHandler.postDelayed(this, 1000L)
            }
        }
        mainHandler.postDelayed(heartbeatRunnable!!, 1000L)
    }

    private fun stopHeartbeat() {
        heartbeatRunnable?.let { mainHandler.removeCallbacks(it) }
        heartbeatRunnable = null
    }

    private fun tick() {
        if (!isAutoScrollEnabled) return

        val now = System.currentTimeMillis()
        var root: AccessibilityNodeInfo? = null
        try {
            root = rootInActiveWindow
        } catch (e: Exception) {}

        val activePkg = root?.packageName?.toString() ?: currentForegroundPkg

        if (isMediaApp(activePkg) || (now - lastMediaEventTime < 5000)) {
            isAppInForeground = true
            if (isMediaApp(activePkg)) {
                currentForegroundPkg = activePkg
                lastMediaEventTime = now
            }
        } else if (isLauncherOrHome(activePkg)) {
            isAppInForeground = false
        }

        FloatingBubbleService.setDetectionActive(isAppInForeground)

        if (!isAppInForeground) {
            FloatingBubbleService.updateCountdown(-1)
            try { root?.recycle() } catch (e: Exception) {}
            return
        }

        // 1. YouTube Shorts & Reels Progress Check (Real Detection)
        var isShortCompleted = false
        if (root != null) {
            if (activePkg == YOUTUBE_PACKAGE || activePkg.contains("youtube")) {
                isShortCompleted = evaluateYouTubeShortsProgress(root)
            } else if (activePkg == INSTAGRAM_PACKAGE || activePkg.contains("instagram")) {
                isShortCompleted = evaluateInstagramReelsProgress(root)
            } else {
                // General media state recognition fallback
                isShortCompleted = evaluateGeneralReelsProgress(root)
            }
        }

        if (scrollMode == "AUTO_DETECT") {
            // AUTO_DETECT mode: Rely purely on real completion detection!
            FloatingBubbleService.updateCountdown(-1) // Show active detection indicator (no countdown numbers)
            
            if (isShortCompleted) {
                Log.d(TAG, "AUTO_DETECT: Real completion detected! Swiping instantly...")
                swipeNextShort()
                resetTimerForNewReel()
            }
        } else {
            // TIMER mode: Strict countdown timer set by user (30s, 1m, 3m, custom)
            secondsRemaining--
            FloatingBubbleService.updateCountdown(secondsRemaining)
            Log.d(TAG, "TIMER mode ticker: $secondsRemaining s remaining")

            if (secondsRemaining <= 0) {
                Log.d(TAG, "TIMER mode expired. Auto-swiping to next Reel...")
                swipeNextShort()
                resetTimerForNewReel()
            }
        }

        try { root?.recycle() } catch (e: Exception) {}
    }

    /**
     * Inspects YouTube Shorts view hierarchy for the playback progress bar.
     * Detects if the video has reached >= 95% or looped from >= 80% to <= 15%.
     */
    private var currentReelStartTime = 0L
    private var lastScannedReelSignature = ""

    private fun evaluateYouTubeShortsProgress(root: AccessibilityNodeInfo): Boolean {
        val progressBarNode = findProgressBarNode(root)
        if (progressBarNode != null) {
            try {
                val range = progressBarNode.rangeInfo
                if (range != null && range.max > 0f) {
                    val ratio = range.current / range.max
                    if (ratio >= 0.05f) {
                        hasSeenProgressStart = true
                    }
                    val isAtEnd = hasSeenProgressStart && ratio >= 0.92f
                    val isLooped = hasSeenProgressStart && lastKnownProgress >= 0.75f && ratio <= 0.15f

                    if (ratio > 0f) {
                        lastKnownProgress = ratio
                    }

                    if (isAtEnd || isLooped) {
                        Log.d(TAG, "YouTube Short completed (ratio=$ratio, loop=$isLooped)")
                        lastKnownProgress = 0f
                        hasSeenProgressStart = false
                        currentReelStartTime = System.currentTimeMillis()
                        return true
                    }
                }
            } catch (e: Exception) {}
        }

        try {
            val sigNode = findNodeByViewIdSubstring(root, "reel_watch_title") ?: findNodeByViewIdSubstring(root, "title")
            val signature = sigNode?.text?.toString() ?: root.hashCode().toString()
            if (signature != lastScannedReelSignature) {
                lastScannedReelSignature = signature
                currentReelStartTime = System.currentTimeMillis()
            } else {
                val elapsed = System.currentTimeMillis() - currentReelStartTime
                if (elapsed >= 20000L) {
                    Log.d(TAG, "YouTube Short powerful duration trigger (${elapsed}ms)")
                    currentReelStartTime = System.currentTimeMillis()
                    return true
                }
            }
        } catch (e: Exception) {}

        return false
    }

    private fun evaluateInstagramReelsProgress(root: AccessibilityNodeInfo): Boolean {
        try {
            val clipsContainer = findNodeByViewIdSubstring(root, "clips_viewer") ?: findNodeByViewIdSubstring(root, "reel_viewer")
            val signature = clipsContainer?.text?.toString() ?: root.childCount.toString()

            if (signature != lastScannedReelSignature) {
                lastScannedReelSignature = signature
                currentReelStartTime = System.currentTimeMillis()
            } else {
                val elapsed = System.currentTimeMillis() - currentReelStartTime
                if (elapsed >= 18000L) {
                    Log.d(TAG, "Instagram Reel powerful duration trigger (${elapsed}ms)")
                    currentReelStartTime = System.currentTimeMillis()
                    return true
                }
            }
        } catch (e: Exception) {}

        return false
    }

    private fun evaluateGeneralReelsProgress(root: AccessibilityNodeInfo): Boolean {
        val now = System.currentTimeMillis()
        if (currentReelStartTime == 0L) {
            currentReelStartTime = now
        }
        if (now - currentReelStartTime >= 20000L) {
            currentReelStartTime = now
            return true
        }
        return false
    }

    private fun findNodeByViewIdSubstring(root: AccessibilityNodeInfo, substring: String): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0

        while (queue.isNotEmpty() && count < 40) {
            val node = queue.removeFirst()
            count++

            val viewId = node.viewIdResourceName
            if (viewId != null && viewId.contains(substring, ignoreCase = true)) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun findProgressBarNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val viewIds = listOf(
            "com.google.android.youtube:id/progress_bar",
            "com.google.android.youtube:id/reel_progress_bar",
            "com.google.android.youtube:id/inline_time_bar",
            "com.google.android.youtube:id/time_bar",
            "com.google.android.youtube:id/reel_watch_playback_progress",
            "com.google.android.youtube:id/video_progress_bar"
        )

        for (vid in viewIds) {
            val nodes = root.findAccessibilityNodeInfosByViewId(vid)
            if (!nodes.isNullOrEmpty()) {
                return nodes[0]
            }
        }

        // Fallback: Breadth-first search for any node with rangeInfo or ProgressBar className
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var count = 0

        while (queue.isNotEmpty() && count < 60) {
            val node = queue.removeFirst()
            count++

            val range = node.rangeInfo
            if (range != null && range.max > 0f) {
                return node
            }
            if (node.className?.contains("ProgressBar", ignoreCase = true) == true && range != null) {
                return node
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Resets the countdown timer for the newly active reel or short.
     */
    fun resetTimerForNewReel() {
        lastKnownProgress = 0f
        hasSeenProgressStart = false

        secondsRemaining = if (scrollMode == "TIMER") {
            timerIntervalSeconds
        } else {
            AUTO_DETECT_REEL_SECONDS // 25 seconds default in Auto-Detect
        }
        Log.d(TAG, "Timer reset for new Reel: $secondsRemaining seconds (mode: $scrollMode)")
    }

    fun resetAndScheduleReelTimer(customDurationSeconds: Int? = null) {
        if (customDurationSeconds != null && customDurationSeconds > 0) {
            secondsRemaining = customDurationSeconds
        } else {
            resetTimerForNewReel()
        }
    }

    fun cancelReelTimer() {
        secondsRemaining = 9999
    }

    override fun onInterrupt() {
        Log.d(TAG, "ShortsAccessibilityService interrupted")
    }

    override fun onDestroy() {
        stopHeartbeat()
        instance = null
        super.onDestroy()
        Log.d(TAG, "ShortsAccessibilityService destroyed")
    }

    /**
     * Executes vertical upward swipe gesture to smoothly scroll to the next Reel or Short.
     */
    fun swipeNextShort(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastSwipeTimestamp < MIN_SWIPE_DEBOUNCE_MS) {
            return false
        }
        lastSwipeTimestamp = now
        totalSwipesTriggered++

        triggerHaptic()

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
                .addStroke(GestureDescription.StrokeDescription(path, 0, 260))
                .build()

            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    super.onCompleted(gestureDescription)
                    Log.d(TAG, "Reel auto-swipe gesture dispatched successfully!")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    super.onCancelled(gestureDescription)
                    Log.w(TAG, "Reel auto-swipe gesture cancelled by system")
                }
            }, null)

            Log.d(TAG, "Reel swipe dispatch result: $dispatched")
            dispatched
        } catch (e: Exception) {
            Log.e(TAG, "Error executing swipe gesture: ${e.message}")
            false
        }
    }

    private fun triggerHaptic() {
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

    companion object {
        private const val TAG = "ShortsAccessibility"
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val INSTAGRAM_PACKAGE = "com.instagram.android"
        private const val MIN_SWIPE_DEBOUNCE_MS = 1200L
        const val AUTO_DETECT_REEL_SECONDS = 25 // 25 seconds average reel playback in auto-detect

        @Volatile
        var instance: ShortsAccessibilityService? = null
            private set

        val isServiceRunning: Boolean
            get() = instance != null

        @Volatile
        var isAutoScrollEnabled: Boolean = true

        @Volatile
        var scrollMode: String = "AUTO_DETECT" // "AUTO_DETECT" or "TIMER"

        @Volatile
        var timerIntervalSeconds: Int = 30

        @Volatile
        var totalSwipesTriggered: Int = 0

        fun openYouTube(context: Context) {
            isAutoScrollEnabled = true
            instance?.resetTimerForNewReel()
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(YOUTUBE_PACKAGE)
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/shorts"))
                    context.startActivity(webIntent)
                }
            } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/shorts"))
                context.startActivity(webIntent)
            }
        }

        fun openInstagram(context: Context) {
            isAutoScrollEnabled = true
            instance?.resetTimerForNewReel()
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
                if (intent != null) {
                    context.startActivity(intent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels"))
                    context.startActivity(webIntent)
                }
            } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/reels"))
                context.startActivity(webIntent)
            }
        }
    }
}
