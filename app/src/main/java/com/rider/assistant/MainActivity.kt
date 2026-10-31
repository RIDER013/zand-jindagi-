package com.rider.assistant

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity matching the exact web UI layout of ƦƗƉƐƦ Reels Manager:
 * - Neon dark aesthetic with crowned portrait background
 * - Top header with glowing pink ƦƗƉƐƦ and status Ʀ button
 * - Clean open center viewport showcasing background artwork
 * - Small glowing EDIT button opening Customize Auto-Scroll modal
 * - YouTube and Instagram app launch buttons
 * - "CREATED BY RIDER" footer
 */
class MainActivity : AppCompatActivity() {

    // Configuration State
    private var isTimerMode = true
    private var timerSeconds = 30
    private var customMinutes = 2
    private var isFloatingBubbleOn = true
    private var isRunning = false

    // UI elements
    private lateinit var btnAccessibilityIndicator: FrameLayout
    private lateinit var txtAccessibilityIcon: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            buildUi()
            ShortsAccessibilityService.isAutoScrollEnabled = true
            ShortsAccessibilityService.scrollMode = if (isTimerMode) "TIMER" else "AUTO_DETECT"
            ShortsAccessibilityService.timerIntervalSeconds = timerSeconds
            checkAndStartFloatingBubble()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
        }
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityIndicatorUi()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return ShortsAccessibilityService.isServiceRunning || RiderAccessibilityService.isServiceRunning
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'ƦƗƉƐƦ Reels & Shorts Auto-Scroll' in Accessibility Settings", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Settings error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndStartFloatingBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                    Toast.makeText(this, "Enable 'Display over other apps' for Floating Ʀ Button", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivity(intent)
                }
                return
            }
        }
        FloatingBubbleService.startBubble(this)
    }

    private fun updateAccessibilityIndicatorUi() {
        if (!::btnAccessibilityIndicator.isInitialized) return
        val isServiceOn = isAccessibilityServiceEnabled()

        btnAccessibilityIndicator.background = createRoundedDrawable(
            if (isServiceOn) "#0C2340" else "#0F172A",
            if (isServiceOn) "#38BDF8" else "#334155",
            dpToPx(12).toFloat()
        )
        txtAccessibilityIcon.setTextColor(
            if (isServiceOn) Color.parseColor("#38BDF8") else Color.parseColor("#64748B")
        )
        if (isServiceOn) {
            txtAccessibilityIcon.setShadowLayer(10f, 0f, 0f, Color.parseColor("#38BDF8"))
        } else {
            txtAccessibilityIcon.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
        }
    }

    private fun buildUi() {
        val rootLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#080C14"))
        }

        // 1. Background image (matches web background artwork)
        val bgImageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            try {
                setImageResource(R.drawable.bg_rider)
            } catch (e: Exception) {
                setBackgroundColor(Color.parseColor("#080C14"))
            }
            alpha = 0.50f
        }
        rootLayout.addView(bgImageView)

        // 2. Dark scrim overlay
        val scrimView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80080C14"))
        }
        rootLayout.addView(scrimView)

        // 3. Top subtle gradient overlay
        val topGradient = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(130),
                Gravity.TOP
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#E6080C14"), Color.parseColor("#4D080C14"), Color.TRANSPARENT)
            )
        }
        rootLayout.addView(topGradient)

        // 4. Bottom subtle gradient overlay
        val bottomGradient = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dpToPx(180),
                Gravity.BOTTOM
            )
            background = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(Color.parseColor("#F2080C14"), Color.parseColor("#80080C14"), Color.TRANSPARENT)
            )
        }
        rootLayout.addView(bottomGradient)

        // 5. Main content container
        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(18))
        }

        // ==============================================================
        // TOP HEADER: ƦƗƉƐƦ above Reels Manager + Ʀ Status Icon
        // ==============================================================
        val headerLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(10)
            }
            gravity = Gravity.CENTER_VERTICAL
        }

        // Title container: ƦƗƉƐƦ (bigger) above Reels Manager (smaller)
        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        val riderLabel = TextView(this).apply {
            text = "ƦƗƉƐƦ"
            textSize = 26f
            setTextColor(Color.parseColor("#FF2A85"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.12f
            setShadowLayer(14f, 0f, 0f, Color.parseColor("#FF2A85"))
        }
        titleContainer.addView(riderLabel)

        val titleText = TextView(this).apply {
            text = "Reels Manager"
            textSize = 13.5f
            setTextColor(Color.parseColor("#E2E8F0"))
            setTypeface(null, Typeface.BOLD)
            letterSpacing = 0.05f
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(2)
            }
            layoutParams = lp
        }
        titleContainer.addView(titleText)
        headerLayout.addView(titleContainer)

        // Right Header: Ʀ Icon Only (Glowing Blue when ON, No ON/OFF text)
        btnAccessibilityIndicator = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(38), dpToPx(38))
            background = createRoundedDrawable("#0F172A", "#334155", dpToPx(12).toFloat())
            setOnClickListener { openAccessibilitySettings() }
        }

        txtAccessibilityIcon = TextView(this).apply {
            text = "Ʀ"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        btnAccessibilityIndicator.addView(txtAccessibilityIcon)
        headerLayout.addView(btnAccessibilityIndicator)

        contentLayout.addView(headerLayout)

        // ==============================================================
        // CENTER SECTION: Clean open viewport showing background artwork
        // ==============================================================
        val centerSpacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        contentLayout.addView(centerSpacer)

        // ==============================================================
        // BOTTOM SECTION: EDIT BUTTON + 2 APP ICONS + FOOTER
        // ==============================================================
        val bottomContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // Small Glowing EDIT Button Centered Above YouTube & Instagram
        val btnEdit = Button(this).apply {
            text = "⚙  EDIT"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FF2A85"))
            letterSpacing = 0.12f
            background = createRoundedDrawable("#0F172A", "#FF2A85", dpToPx(20).toFloat())
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dpToPx(38)
            ).apply {
                bottomMargin = dpToPx(14)
            }
            layoutParams = lp
            setPadding(dpToPx(24), 0, dpToPx(24), 0)
            setOnClickListener {
                showCustomizeModal()
            }
        }
        bottomContainer.addView(btnEdit)

        // 2 App Buttons: YouTube & Instagram in a 2-column row
        val appsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(14)
            }
            weightSum = 2f
        }

        // 1. YouTube Button
        val btnYouTube = Button(this).apply {
            text = "▶  YouTube"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createGradientDrawable("#DC2626", "#EF4444", dpToPx(16).toFloat())
            val lp = LinearLayout.LayoutParams(0, dpToPx(48), 1f).apply {
                marginEnd = dpToPx(7)
            }
            layoutParams = lp
            setOnClickListener {
                ShortsAccessibilityService.openYouTube(this@MainActivity)
            }
        }
        appsRow.addView(btnYouTube)

        // 2. Instagram Button
        val btnInstagram = Button(this).apply {
            text = "📷  Instagram"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createGradientDrawable(
                intArrayOf(Color.parseColor("#D6249F"), Color.parseColor("#FD5949"), Color.parseColor("#F97316")),
                dpToPx(16).toFloat()
            )
            val lp = LinearLayout.LayoutParams(0, dpToPx(48), 1f).apply {
                marginStart = dpToPx(7)
            }
            layoutParams = lp
            setOnClickListener {
                ShortsAccessibilityService.openInstagram(this@MainActivity)
            }
        }
        appsRow.addView(btnInstagram)
        bottomContainer.addView(appsRow)

        // Footer: Created by Rider (smaller and lower)
        val footerText = TextView(this).apply {
            text = "CREATED BY RIDER"
            textSize = 8.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#64748B"))
            letterSpacing = 0.22f
            gravity = Gravity.CENTER
            setPadding(0, dpToPx(8), 0, dpToPx(2))
        }
        bottomContainer.addView(footerText)

        contentLayout.addView(bottomContainer)
        rootLayout.addView(contentLayout)

        setContentView(rootLayout)
        updateAccessibilityIndicatorUi()
    }

    /**
     * Start / Customize Auto-Scroll Modal matching the web modal:
     * - Option 1: Timer (30s, 1m, 3m, Custom [1-5m])
     * - Option 2: Auto-Detect
     * - Done & Start Button -> opens Quick Launch Dialog
     */
    private fun showCustomizeModal() {
        var selectedMode = if (isTimerMode) "TIMER" else "AUTO_DETECT"
        var selectedSeconds = timerSeconds
        var selectedCustomMinutes = customMinutes

        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(20))
            background = createRoundedDrawable("#101827", "#334155", dpToPx(24).toFloat())
        }

        // Modal Header
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(16))
        }

        val titleText = TextView(this).apply {
            text = "Customize Auto-Scroll"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(titleText)
        dialogView.addView(headerRow)

        // Containers for Options
        lateinit var cardTimer: LinearLayout
        lateinit var cardAutoDetect: LinearLayout
        lateinit var radioTimer: TextView
        lateinit var radioAutoDetect: TextView
        lateinit var timerOptionsLayout: LinearLayout
        val presetButtons = mutableListOf<Button>()
        val customChips = mutableListOf<Button>()

        fun updateSelectionUi() {
            val isTimer = selectedMode == "TIMER"

            // Timer card style
            cardTimer.background = createRoundedDrawable(
                if (isTimer) "#0C2340" else "#0F172A",
                if (isTimer) "#38BDF8" else "#1E293B",
                dpToPx(16).toFloat()
            )
            radioTimer.text = if (isTimer) "●" else "○"
            radioTimer.setTextColor(if (isTimer) Color.parseColor("#38BDF8") else Color.parseColor("#64748B"))
            timerOptionsLayout.visibility = if (isTimer) View.VISIBLE else View.GONE

            // Auto-detect card style
            cardAutoDetect.background = createRoundedDrawable(
                if (!isTimer) "#3B0764" else "#0F172A",
                if (!isTimer) "#F43F5E" else "#1E293B",
                dpToPx(16).toFloat()
            )
            radioAutoDetect.text = if (!isTimer) "●" else "○"
            radioAutoDetect.setTextColor(if (!isTimer) Color.parseColor("#F43F5E") else Color.parseColor("#64748B"))
        }

        // ==========================================================
        // 1. TIMER CARD
        // ==========================================================
        cardTimer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(12)
            }
            setOnClickListener {
                selectedMode = "TIMER"
                updateSelectionUi()
            }
        }

        val timerHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        radioTimer = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, dpToPx(10), 0)
        }
        timerHeaderRow.addView(radioTimer)

        val timerTitle = TextView(this).apply {
            text = "Timer"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        timerHeaderRow.addView(timerTitle)

        val timerIcon = TextView(this).apply {
            text = "⏱"
            textSize = 15f
        }
        timerHeaderRow.addView(timerIcon)
        cardTimer.addView(timerHeaderRow)

        // Presets container: 30s, 1m, 3m, Custom
        timerOptionsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(10), dpToPx(12), 0, dpToPx(4))
        }

        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            weightSum = 4f
        }

        val presetsList = listOf("30s" to 30, "1m" to 60, "3m" to 180, "Custom" to -1)
        val customContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dpToPx(10), 0, 0)
        }

        for ((label, sec) in presetsList) {
            val btn = Button(this).apply {
                text = label
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                val isSelected = if (sec == -1) (selectedSeconds !in listOf(30, 60, 180)) else (selectedSeconds == sec)
                background = createRoundedDrawable(
                    if (isSelected) "#0284C7" else "#1E293B",
                    if (isSelected) "#38BDF8" else "#334155",
                    dpToPx(10).toFloat()
                )
                val lp = LinearLayout.LayoutParams(0, dpToPx(38), 1f).apply {
                    marginEnd = dpToPx(4)
                }
                layoutParams = lp
                setOnClickListener {
                    if (sec == -1) {
                        selectedSeconds = selectedCustomMinutes * 60
                        customContainer.visibility = View.VISIBLE
                    } else {
                        selectedSeconds = sec
                        customContainer.visibility = View.GONE
                    }
                    presetButtons.forEach { b ->
                        val isCurr = b.text == label
                        b.background = createRoundedDrawable(
                            if (isCurr) "#0284C7" else "#1E293B",
                            if (isCurr) "#38BDF8" else "#334155",
                            dpToPx(10).toFloat()
                        )
                    }
                }
            }
            presetButtons.add(btn)
            presetsRow.addView(btn)
        }
        timerOptionsLayout.addView(presetsRow)

        // Custom minutes chips: 1m, 2m, 3m, 4m, 5m (strictly max 5 mins)
        val customLabel = TextView(this).apply {
            text = "Custom (Max 5 min):"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dpToPx(6), 0, dpToPx(6))
        }
        customContainer.addView(customLabel)

        val chipsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            weightSum = 5f
        }

        for (m in 1..5) {
            val chip = Button(this).apply {
                text = "${m}m"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.WHITE)
                val isChipSelected = selectedCustomMinutes == m
                background = createRoundedDrawable(
                    if (isChipSelected) "#0284C7" else "#1E293B",
                    if (isChipSelected) "#38BDF8" else "#334155",
                    dpToPx(8).toFloat()
                )
                val lp = LinearLayout.LayoutParams(0, dpToPx(34), 1f).apply {
                    marginEnd = dpToPx(3)
                }
                layoutParams = lp
                setOnClickListener {
                    selectedCustomMinutes = m
                    selectedSeconds = m * 60
                    customChips.forEach { c ->
                        val isCurrent = c.text == "${m}m"
                        c.background = createRoundedDrawable(
                            if (isCurrent) "#0284C7" else "#1E293B",
                            if (isCurrent) "#38BDF8" else "#334155",
                            dpToPx(8).toFloat()
                        )
                    }
                }
            }
            customChips.add(chip)
            chipsRow.addView(chip)
        }
        customContainer.addView(chipsRow)
        customContainer.visibility = if (selectedSeconds !in listOf(30, 60, 180)) View.VISIBLE else View.GONE
        timerOptionsLayout.addView(customContainer)

        cardTimer.addView(timerOptionsLayout)
        dialogView.addView(cardTimer)

        // ==========================================================
        // 2. AUTO-DETECT CARD
        // ==========================================================
        cardAutoDetect = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(16)
            }
            setOnClickListener {
                selectedMode = "AUTO_DETECT"
                updateSelectionUi()
            }
        }

        radioAutoDetect = TextView(this).apply {
            textSize = 16f
            setPadding(0, 0, dpToPx(10), 0)
        }
        cardAutoDetect.addView(radioAutoDetect)

        val autoDetectTitle = TextView(this).apply {
            text = "Auto-Detect"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        cardAutoDetect.addView(autoDetectTitle)

        val autoDetectIcon = TextView(this).apply {
            text = "⚡"
            textSize = 15f
        }
        cardAutoDetect.addView(autoDetectIcon)
        dialogView.addView(cardAutoDetect)

        updateSelectionUi()

        val alert = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // ==========================================================
        // ACTION: DONE & START BUTTON
        // ==========================================================
        val btnDone = Button(this).apply {
            text = "Done & Start"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createGradientDrawable("#0284C7", "#38BDF8", dpToPx(14).toFloat())
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )
            setOnClickListener {
                alert.dismiss()

                ShortsAccessibilityService.isAutoScrollEnabled = true
                if (selectedMode == "TIMER") {
                    isTimerMode = true
                    timerSeconds = selectedSeconds
                    customMinutes = selectedCustomMinutes
                    ShortsAccessibilityService.scrollMode = "TIMER"
                    ShortsAccessibilityService.timerIntervalSeconds = timerSeconds
                } else {
                    isTimerMode = false
                    ShortsAccessibilityService.scrollMode = "AUTO_DETECT"
                }

                ShortsAccessibilityService.instance?.resetAndScheduleReelTimer()

                isRunning = true
                isFloatingBubbleOn = true
                checkAndStartFloatingBubble()

                if (!ShortsAccessibilityService.isServiceRunning) {
                    openAccessibilitySettings()
                }

                updateAccessibilityIndicatorUi()
                Toast.makeText(this@MainActivity, "Auto-Scroll Started!", Toast.LENGTH_SHORT).show()

                // Show Quick Launch Dialog
                showQuickLaunchDialog()
            }
        }
        dialogView.addView(btnDone)

        alert.show()
    }

    /**
     * Quick App Launch Dialog matching web design:
     * - Open YouTube Shorts
     * - Open Instagram Reels
     * - Done (Keep in Background)
     */
    private fun showQuickLaunchDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(20))
            background = createRoundedDrawable("#101827", "#334155", dpToPx(24).toFloat())
        }

        // Green checkmark badge
        val badge = TextView(this).apply {
            text = "✓"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#34D399"))
            gravity = Gravity.CENTER
            background = createRoundedDrawable("#064E3B", "#10B981", dpToPx(24).toFloat())
            val lp = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = dpToPx(12)
            }
            layoutParams = lp
        }
        dialogView.addView(badge)

        val title = TextView(this).apply {
            text = "Auto-Scroll Activated!"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(6))
        }
        dialogView.addView(title)

        val desc = TextView(this).apply {
            text = "Open YouTube Shorts or Instagram Reels to start auto-scrolling:"
            textSize = 12f
            setTextColor(Color.parseColor("#94A3B8"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dpToPx(16))
        }
        dialogView.addView(desc)

        val alert = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        alert.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // YouTube Shorts Button
        val btnYt = Button(this).apply {
            text = "▶  Open YouTube Shorts"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createGradientDrawable("#DC2626", "#EF4444", dpToPx(16).toFloat())
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(46)).apply {
                bottomMargin = dpToPx(10)
            }
            layoutParams = lp
            setOnClickListener {
                alert.dismiss()
                ShortsAccessibilityService.openYouTube(this@MainActivity)
            }
        }
        dialogView.addView(btnYt)

        // Instagram Reels Button
        val btnIg = Button(this).apply {
            text = "📷  Open Instagram Reels"
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            background = createGradientDrawable(
                intArrayOf(Color.parseColor("#D6249F"), Color.parseColor("#FD5949"), Color.parseColor("#F97316")),
                dpToPx(16).toFloat()
            )
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(46)).apply {
                bottomMargin = dpToPx(10)
            }
            layoutParams = lp
            setOnClickListener {
                alert.dismiss()
                ShortsAccessibilityService.openInstagram(this@MainActivity)
            }
        }
        dialogView.addView(btnIg)

        // Done (Keep in Background)
        val btnDismiss = Button(this).apply {
            text = "Done (Keep in Background)"
            textSize = 12.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#94A3B8"))
            background = createRoundedDrawable("#1E293B", "#334155", dpToPx(14).toFloat())
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(42)).apply {
                topMargin = dpToPx(4)
            }
            layoutParams = lp
            setOnClickListener {
                alert.dismiss()
                moveTaskToBack(true)
            }
        }
        dialogView.addView(btnDismiss)

        alert.show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun createRoundedDrawable(fillHex: String, strokeHex: String, cornerRadius: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor(fillHex))
            setStroke(dpToPx(1), Color.parseColor(strokeHex))
            this.cornerRadius = cornerRadius
        }
    }

    private fun createGradientDrawable(startHex: String, endHex: String, cornerRadius: Float): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(Color.parseColor(startHex), Color.parseColor(endHex))
        ).apply {
            this.cornerRadius = cornerRadius
        }
    }

    private fun createGradientDrawable(colors: IntArray, cornerRadius: Float): GradientDrawable {
        return GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            colors
        ).apply {
            this.cornerRadius = cornerRadius
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}