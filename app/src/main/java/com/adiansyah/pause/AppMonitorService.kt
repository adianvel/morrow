package com.adiansyah.pause

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate
import java.time.ZoneId

class AppMonitorService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: PauseStore
    private var activePackage: String? = null
    private var activeSince = 0L
    private var limitTask: Runnable? = null
    private var midnightTask: Runnable? = null
    private var blockOverlay: View? = null
    private var overlayPackage: String? = null
    private var overlayMode: OverlayMode? = null
    private var launchCountdown: TextView? = null
    private var launchDeadline = 0L
    private var launchDelayTask: Runnable? = null
    private var ignoredPackageUntilExit: String? = null
    private lateinit var windowManager: WindowManager

    private enum class OverlayMode { LAUNCH_DELAY, LIMIT_REACHED }

    override fun onServiceConnected() {
        super.onServiceConnected()
        store = PauseStore(this)
        windowManager = getSystemService(WindowManager::class.java)
        scheduleMidnightReset()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == ignoredPackageUntilExit) return
        ignoredPackageUntilExit = null
        if (blockOverlay != null) {
            val previousPackage = overlayPackage
            if (packageName == previousPackage) return
            val wasInLaunchDelay = overlayMode == OverlayMode.LAUNCH_DELAY
            dismissBlockOverlay()
            if (wasInLaunchDelay && activePackage == previousPackage) {
                closeActiveSession()
                activePackage = null
                cancelLimitCheck()
            }
        }
        if (store.resetUsageForToday() && activePackage != null && activeSince != 0L) {
            activeSince = SystemClock.elapsedRealtime()
            scheduleLimitCheck()
        }
        val selectedPackage = packageName.takeIf {
            it != this.packageName && store.limitMinutes(it) != null
        }
        if (selectedPackage == activePackage) return

        closeActiveSession()
        activePackage = selectedPackage
        activeSince = 0L
        if (selectedPackage != null) {
            val minutes = store.limitMinutes(selectedPackage) ?: return
            if (UsageAllowance(store.usedMillis(selectedPackage), minutes).isExhausted) {
                block(selectedPackage)
            } else {
                startLaunchDelay(selectedPackage)
            }
        }
        scheduleLimitCheck()
    }

    override fun onInterrupt() {
        closeActiveSession()
        activePackage = null
        cancelLimitCheck()
        dismissBlockOverlay()
    }

    override fun onDestroy() {
        closeActiveSession()
        cancelLimitCheck()
        dismissBlockOverlay()
        midnightTask?.let(handler::removeCallbacks)
        super.onDestroy()
    }

    private fun scheduleLimitCheck() {
        cancelLimitCheck()
        val packageName = activePackage ?: return
        if (activeSince == 0L) return
        val minutes = store.limitMinutes(packageName) ?: return
        val elapsed = (SystemClock.elapsedRealtime() - activeSince).coerceAtLeast(0L)
        val allowance = UsageAllowance(store.usedMillis(packageName) + elapsed, minutes)
        if (allowance.isExhausted) {
            block(packageName)
            return
        }
        limitTask = Runnable {
            if (activePackage == packageName) scheduleLimitCheck()
        }.also { handler.postDelayed(it, allowance.remainingMillis) }
    }

    private fun block(packageName: String) {
        closeActiveSession()
        activePackage = null
        cancelLimitCheck()
        showOverlay(packageName, OverlayMode.LIMIT_REACHED)
    }

    private fun startLaunchDelay(packageName: String) {
        launchDeadline = SystemClock.elapsedRealtime() + LAUNCH_DELAY_MILLIS
        showOverlay(packageName, OverlayMode.LAUNCH_DELAY)
        updateLaunchCountdown()
    }

    private fun showOverlay(packageName: String, mode: OverlayMode) {
        if (blockOverlay != null) return
        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault("this app")
        val overlay = createBlockOverlay(appLabel, mode)
        overlayPackage = packageName
        overlayMode = mode
        blockOverlay = overlay
        try {
            windowManager.addView(
                overlay,
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply { gravity = Gravity.TOP or Gravity.START },
            )
        } catch (_: Exception) {
            activePackage = null
            activeSince = 0L
            dismissBlockOverlay()
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun createBlockOverlay(appLabel: String, mode: OverlayMode): View {
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(247, 248, 241))
            isFocusableInTouchMode = true
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                    goHome()
                    true
                } else false
            }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(28), dp(20), dp(28), dp(20))
        }
        root.addView(content, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        content.addView(ImageView(this).apply {
            setImageResource(R.drawable.focus_axolotl)
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(216), dp(216)).apply { bottomMargin = dp(22) })
        content.addView(TextView(this).apply {
            text = getString(
                if (mode == OverlayMode.LIMIT_REACHED) R.string.block_title else R.string.launch_title,
                appLabel,
            )
            setTextColor(Color.rgb(23, 61, 54))
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        content.addView(TextView(this).apply {
            text = getString(
                if (mode == OverlayMode.LIMIT_REACHED) R.string.block_reset else R.string.launch_body,
            )
            setTextColor(Color.rgb(100, 116, 109))
            textSize = 18f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })
        if (mode == OverlayMode.LAUNCH_DELAY) {
            launchCountdown = TextView(this).apply {
                setTextColor(Color.rgb(23, 61, 54))
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    setColor(Color.rgb(215, 239, 114))
                    cornerRadius = dp(18).toFloat()
                }
            }
            content.addView(launchCountdown, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(20) })
        }
        content.addView(Button(this).apply {
            text = getString(R.string.go_home)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.rgb(23, 61, 54))
                cornerRadius = dp(18).toFloat()
            }
            setOnClickListener { goHome() }
        }, LinearLayout.LayoutParams(-1, dp(56)).apply { topMargin = dp(28) })
        return root
    }

    private fun goHome() {
        ignoredPackageUntilExit = overlayPackage
        if (overlayMode == OverlayMode.LAUNCH_DELAY) {
            activePackage = null
            activeSince = 0L
            cancelLimitCheck()
        }
        performGlobalAction(GLOBAL_ACTION_HOME)
        dismissBlockOverlay()
    }

    private fun dismissBlockOverlay() {
        val overlay = blockOverlay ?: return
        launchDelayTask?.let(handler::removeCallbacks)
        launchDelayTask = null
        launchCountdown = null
        blockOverlay = null
        overlayPackage = null
        overlayMode = null
        runCatching { windowManager.removeView(overlay) }
    }

    private fun updateLaunchCountdown() {
        if (overlayMode != OverlayMode.LAUNCH_DELAY || blockOverlay == null) return
        val remainingMillis = (launchDeadline - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val seconds = ((remainingMillis + 999L) / 1_000L).toInt()
        launchCountdown?.text = resources.getQuantityString(R.plurals.launch_countdown, seconds, seconds)
        if (seconds == 0) {
            val packageName = overlayPackage
            if (overlayMode == OverlayMode.LAUNCH_DELAY && packageName != null && activePackage == packageName) {
                dismissBlockOverlay()
                activeSince = SystemClock.elapsedRealtime()
                scheduleLimitCheck()
            }
        } else {
            launchDelayTask = Runnable { updateLaunchCountdown() }
                .also { handler.postDelayed(it, 1_000L) }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun closeActiveSession() {
        val packageName = activePackage ?: return
        if (activeSince != 0L) {
            val elapsed = (SystemClock.elapsedRealtime() - activeSince).coerceAtLeast(0L)
            store.addUsage(packageName, elapsed)
        }
        activeSince = 0L
    }

    private fun cancelLimitCheck() {
        limitTask?.let(handler::removeCallbacks)
        limitTask = null
    }

    private fun scheduleMidnightReset() {
        midnightTask?.let(handler::removeCallbacks)
        val zone = ZoneId.systemDefault()
        val nextMidnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val delay = (nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L)
        midnightTask = Runnable {
            if (store.resetUsageForToday()) {
                val now = SystemClock.elapsedRealtime()
                if (overlayMode == OverlayMode.LIMIT_REACHED && overlayPackage != null) {
                    activePackage = overlayPackage
                    activeSince = now
                    dismissBlockOverlay()
                } else if (activePackage != null && activeSince != 0L) {
                    activeSince = now
                }
            }
            scheduleLimitCheck()
            scheduleMidnightReset()
        }.also { handler.postDelayed(it, delay) }
    }

    private companion object {
        const val LAUNCH_DELAY_MILLIS = 15_000L
    }
}
