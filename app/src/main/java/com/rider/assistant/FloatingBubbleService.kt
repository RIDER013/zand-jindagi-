package com.rider.assistant

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

/**
 * Floating Ʀ Overlay Service that draws a draggable pink Ʀ button over YouTube Shorts & Instagram Reels.
 * When tapped, it directly calls ShortsAccessibilityService.instance?.swipeNextShort() to perform
 * real accessibility swipe gestures without the user having to switch back to the app!
 */
class FloatingBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var rootBubbleView: FrameLayout? = null
    private var txtBadge: TextView? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        showFloatingBubble()
    }

    private fun showFloatingBubble() {
        if (bubbleView != null) return

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            dpToPx(60),
            dpToPx(60),
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 500
        }

        val rootBubble = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(Color.parseColor("#0C2340"))
                setStroke(dpToPx(2), Color.parseColor("#38BDF8"))
                cornerRadius = dpToPx(16).toFloat()
            }
            background = bg
        }
        rootBubbleView = rootBubble

        // Display the exact Ʀ app icon (same as top header Ʀ icon)
        val textR = TextView(this).apply {
            text = "Ʀ"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#38BDF8"))
            setShadowLayer(14f, 0f, 0f, Color.parseColor("#38BDF8"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        rootBubble.addView(textR)

        // Live countdown seconds badge at bottom right of Ʀ bubble
        txtBadge = TextView(this).apply {
            text = ""
            textSize = 9.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            val badgeBg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#0284C7"))
                setStroke(dpToPx(1), Color.parseColor("#38BDF8"))
            }
            background = badgeBg
            val lp = FrameLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                marginEnd = dpToPx(2)
                bottomMargin = dpToPx(2)
            }
            layoutParams = lp
        }
        rootBubble.addView(txtBadge)

        // Drag and click handling
        rootBubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = true

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(bubbleView, params)
                        } catch (e: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            Log.d(TAG, "Floating Ʀ button tapped! Triggering accessibility swipe...")
                            val service = ShortsAccessibilityService.instance
                            if (service != null) {
                                service.swipeNextShort()
                                service.resetTimerForNewReel()
                            } else {
                                val riderService = RiderAccessibilityService.instance
                                if (riderService != null) {
                                    riderService.swipeUp()
                                } else {
                                    Log.w(TAG, "Neither ShortsAccessibilityService nor RiderAccessibilityService is bound yet")
                                }
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            windowManager?.addView(rootBubble, params)
            bubbleView = rootBubble
            Log.d(TAG, "Floating Ʀ Bubble added to WindowManager successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add floating bubble: ${e.message}")
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        if (bubbleView != null && windowManager != null) {
            try {
                windowManager?.removeView(bubbleView)
            } catch (e: Exception) {}
            bubbleView = null
        }
        instance = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FloatingBubbleService"
        var instance: FloatingBubbleService? = null

        fun startBubble(context: Context) {
            try {
                val intent = Intent(context, FloatingBubbleService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not start FloatingBubbleService: ${e.message}")
            }
        }

        fun stopBubble(context: Context) {
            try {
                val intent = Intent(context, FloatingBubbleService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Could not stop FloatingBubbleService: ${e.message}")
            }
        }

        fun updateCountdown(seconds: Int) {
            val s = instance ?: return
            s.mainHandler.post {
                try {
                    s.txtBadge?.text = if (seconds in 1..99) "$seconds" else ""
                } catch (e: Exception) {}
            }
        }

        fun setDetectionActive(isActive: Boolean) {
            val s = instance ?: return
            s.mainHandler.post {
                try {
                    val density = android.content.res.Resources.getSystem().displayMetrics.density
                    val dp3 = (3 * density).toInt()
                    val dp2 = (2 * density).toInt()
                    val dp16 = (16 * density).toFloat()

                    val bg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        if (isActive) {
                            // Sky blue active glow state
                            setColor(Color.parseColor("#0369A1"))
                            setStroke(dp3, Color.parseColor("#38BDF8"))
                        } else {
                            // Original inactive state
                            setColor(Color.parseColor("#0C2340"))
                            setStroke(dp2, Color.parseColor("#38BDF8"))
                        }
                        cornerRadius = dp16
                    }
                    s.rootBubbleView?.background = bg
                } catch (e: Exception) {}
            }
        }
    }
}
