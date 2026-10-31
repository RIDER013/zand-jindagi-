package com.rider.assistant

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity for ZAND JINDAGI.
 * Directly renders the exact preview UI via high-performance WebView
 * with full hardware acceleration and native Android bridge integration.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set status bar and navigation bar colors to deep dark
        window.statusBarColor = Color.parseColor("#080C14")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.parseColor("#080C14")
        }

        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#080C14"))
        }

        webView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.TRANSPARENT)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e(TAG, "WebView error: ${error?.description}")
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    Log.d("WebConsole", "[${consoleMessage?.messageLevel()}] ${consoleMessage?.message()}")
                    return true
                }
            }

            // Expose the native Android Bridge to the Web UI
            val bridge = AndroidBridge(this@MainActivity)
            addJavascriptInterface(bridge, "AndroidBridge")
            addJavascriptInterface(bridge, "ShortsAccessibility")
        }

        rootLayout.addView(webView)
        setContentView(rootLayout)

        // Load the exact bundled web application
        webView.loadUrl("file:///android_asset/www/index.html")

        // Initialize background auto-scroll & overlay features
        ShortsAccessibilityService.isAutoScrollEnabled = true
        ShortsAccessibilityService.scrollMode = "TIMER"
        ShortsAccessibilityService.timerIntervalSeconds = 30
        checkAndStartFloatingBubble()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) {
            webView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) {
            webView.onPause()
        }
    }

    override fun onDestroy() {
        try {
            if (::webView.isInitialized) {
                webView.destroy()
            }
        } catch (e: Exception) {}
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            moveTaskToBack(true)
        }
    }

    fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Enable 'ZAND JINDAGI Reels & Shorts Auto-Scroll' in Accessibility Settings",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Settings error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndStartFloatingBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Enable 'Display over other apps' for Floating Bubble",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
                return
            }
        }
        FloatingBubbleService.startBubble(this)
    }

    /**
     * JavaScript Bridge providing full hardware and Android OS integration
     * to the exact Web UI preview.
     */
    inner class AndroidBridge(private val activity: MainActivity) {

        @JavascriptInterface
        fun openAccessibilitySettings() {
            activity.runOnUiThread {
                activity.openAccessibilitySettings()
            }
        }

        @JavascriptInterface
        fun startAutoScroll(mode: String?, intervalSeconds: Int) {
            activity.runOnUiThread {
                ShortsAccessibilityService.isAutoScrollEnabled = true
                ShortsAccessibilityService.scrollMode = if (mode?.lowercase() == "timer") "TIMER" else "AUTO_DETECT"
                ShortsAccessibilityService.timerIntervalSeconds = Math.max(3, intervalSeconds)
                ShortsAccessibilityService.instance?.resetTimerForNewReel()
                activity.checkAndStartFloatingBubble()
                Toast.makeText(activity, "Auto-Scroll Activated (${ShortsAccessibilityService.timerIntervalSeconds}s)", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun stopAutoScroll() {
            activity.runOnUiThread {
                ShortsAccessibilityService.isAutoScrollEnabled = false
                Toast.makeText(activity, "Auto-Scroll Stopped", Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun setAutoScrollMode(mode: String?) {
            ShortsAccessibilityService.scrollMode = if (mode?.lowercase() == "timer") "TIMER" else "AUTO_DETECT"
        }

        @JavascriptInterface
        fun swipeNextShort() {
            activity.runOnUiThread {
                ShortsAccessibilityService.instance?.performSwipeUp("Web Preview Tap")
            }
        }

        @JavascriptInterface
        fun openYouTube() {
            activity.runOnUiThread {
                ShortsAccessibilityService.openYouTube(activity)
            }
        }

        @JavascriptInterface
        fun openInstagram() {
            activity.runOnUiThread {
                ShortsAccessibilityService.openInstagram(activity)
            }
        }

        @JavascriptInterface
        fun openInstagramProfile(url: String?) {
            activity.runOnUiThread {
                val target = if (!url.isNullOrBlank()) url else "https://www.instagram.com/ru_rider__?rpxt=MWJyYmdiZjJqZzJ4dQ=="
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
                    activity.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(activity, "Opening $target", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}