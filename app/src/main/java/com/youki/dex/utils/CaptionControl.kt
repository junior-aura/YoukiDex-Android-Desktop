package com.youki.dex.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityNodeInfo

/**
 * ClauDEX: the window buttons the system draws on each freeform window, reached
 * as accessibility nodes.
 *
 * Measured on a SM-A055M (One UI, Android 15) with uiautomator: the caption pill
 * is com.android.systemui:id/caption_handle; tapping it opens a menu with
 * minimize_window, maximize_window, close_window, split_window and
 * caption_pin_window. "Maximize" turns the task into mode=fullscreen - which the
 * shell cannot do (`am start --windowingMode 1` on a freeform task is ignored).
 *
 * Capability, not model: where those ids do not exist (another ROM, a tablet
 * with its own caption) press() reports false and the caller falls back.
 * Main thread only.
 */
object CaptionControl {
    private const val SYSUI = "com.android.systemui:id/"
    const val MAXIMIZE = "maximize_window"
    const val MINIMIZE = "minimize_window"

    /** Presses [button] in the caption menu of the window at [taskBounds]; [done] gets whether it was pressed. */
    fun press(service: AccessibilityService, taskBounds: Rect, button: String, done: (Boolean) -> Unit) {
        val handle = findHandle(service, taskBounds)
        if (handle == null || !handle.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { done(false); return }
        // the menu is a separate window that appears a moment after the tap
        val h = Handler(Looper.getMainLooper())
        var tries = 0
        fun look() {
            val b = nodes(service, SYSUI + button).firstOrNull()
            when {
                b != null -> done(b.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                ++tries < 8 -> h.postDelayed({ look() }, 120)
                else -> done(false)
            }
        }
        h.postDelayed({ look() }, 150)
    }

    /**
     * The caption handle of the window at [taskBounds]: centered on it, at its
     * top edge (measured: handle [675,35][841,79] over a window [0,55][1516,720]).
     */
    private fun findHandle(service: AccessibilityService, taskBounds: Rect): AccessibilityNodeInfo? {
        val r = Rect()
        fun distance(n: AccessibilityNodeInfo): Int {
            n.getBoundsInScreen(r)
            return Math.abs(r.centerX() - taskBounds.centerX()) + Math.abs(r.centerY() - taskBounds.top)
        }
        val best = nodes(service, SYSUI + "caption_handle").minByOrNull { distance(it) } ?: return null
        best.getBoundsInScreen(r)
        val near = Math.abs(r.centerX() - taskBounds.centerX()) < taskBounds.width() / 4 &&
            Math.abs(r.centerY() - taskBounds.top) < 120
        return if (near) best else null
    }

    private fun nodes(service: AccessibilityService, id: String): List<AccessibilityNodeInfo> {
        val windows = try { service.windows } catch (e: Exception) { return emptyList() }
        return windows.flatMap { w ->
            try { w.root?.findAccessibilityNodeInfosByViewId(id) ?: emptyList() } catch (e: Exception) { emptyList() }
        }
    }
}
