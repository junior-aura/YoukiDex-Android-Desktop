package com.youki.dex.panels

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.youki.dex.R
import com.youki.dex.models.App
import com.youki.dex.utils.ColorUtils
import com.youki.dex.utils.EventJournal
import com.youki.dex.utils.Utils

/**
 * ClauDEX side rail (owner, 29/09): the portrait counterpart of the dock.
 *
 * In portrait the system navigation bar sits at the bottom, and a bottom dock
 * covered its Back and Recents buttons (measured on a SM-A055M). So in
 * portrait the dock steps aside and this rail takes over from the RIGHT edge:
 * a thin handle, a swipe inwards opens a column with the favorites, the
 * Windows switcher and the quick panel; it closes on a pick or a tap outside.
 *
 * A separate small window on purpose: upstream once supported a vertical dock
 * and removed it as unstable (see DockPositionUtils), so the landscape dock
 * is left untouched.
 */
class SideRail(
    private val context: Context,
    private val windowManager: WindowManager,
    private val prefs: SharedPreferences,
    private val favorites: () -> List<App>,
    private val onOpen: (App) -> Unit,
    private val onWindows: () -> Unit,
    private val onOptions: () -> Unit
) {
    private var handle: View? = null
    private var rail: View? = null

    val isEnabled: Boolean get() = handle != null

    fun enable() {
        if (handle != null) return
        val h = View(context)
        h.background = GradientDrawable().apply {
            cornerRadius = Utils.dpToPx(context, 6).toFloat()
            setColor(0x59FFFFFF)
        }
        val screenH = context.resources.displayMetrics.heightPixels
        val p = Utils.makeWindowParams(Utils.dpToPx(context, 12), (screenH * 0.30f).toInt(), context)
        p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        p.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        bindSwipe(h)
        try { windowManager.addView(h, p); handle = h } catch (e: Exception) {}
        EventJournal.log(context, "rail: on")
    }

    fun disable() {
        dismissRail()
        handle?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        if (handle != null) EventJournal.log(context, "rail: off")
        handle = null
    }

    /** A swipe inwards (right to left) of 16 dp on the handle opens the rail. */
    @SuppressLint("ClickableViewAccessibility")
    private fun bindSwipe(h: View) {
        val need = Utils.dpToPx(context, 16)
        var downX = 0f
        h.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> downX = e.rawX
                MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP ->
                    if (downX - e.rawX > need && rail == null) showRail()
            }
            true
        }
    }

    private fun showRail() {
        val col = LinearLayout(context)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        val pad = Utils.dpToPx(context, 6)
        col.setPadding(pad, pad, pad, pad)
        val size = Utils.dpToPx(context, 48)
        fun item(icon: android.graphics.drawable.Drawable?, res: Int, desc: String, action: () -> Unit) {
            val iv = ImageView(context)
            if (icon != null) iv.setImageDrawable(icon) else iv.setImageResource(res)
            iv.contentDescription = desc
            val ip = Utils.dpToPx(context, if (icon != null) 4 else 12)
            iv.setPadding(ip, ip, ip, ip)
            iv.setOnClickListener { dismissRail(); action() }
            col.addView(iv, LinearLayout.LayoutParams(size, size))
        }
        favorites().forEach { app -> item(app.icon, 0, app.name) { onOpen(app) } }
        item(null, R.drawable.ic_recents, context.getString(R.string.switcher_title)) { onWindows() }
        item(null, R.drawable.ic_brightness, context.getString(R.string.qp_brightness)) { onOptions() }

        // a long list scrolls inside 80% of the screen height
        val scroll = ScrollView(context)
        scroll.isVerticalScrollBarEnabled = false
        scroll.addView(col)
        scroll.setBackgroundResource(R.drawable.round_rect)
        ColorUtils.applyMainColor(context, prefs, scroll)
        col.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val maxH = (context.resources.displayMetrics.heightPixels * 0.8f).toInt()
        val p = Utils.makeWindowParams(-2, minOf(col.measuredHeight, maxH), context)
        p.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        p.gravity = Gravity.END or Gravity.CENTER_VERTICAL
        p.x = Utils.dpToPx(context, 8)
        scroll.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) dismissRail()
            false
        }
        try { windowManager.addView(scroll, p); rail = scroll } catch (e: Exception) {}
        EventJournal.log(context, "rail: open")
    }

    fun dismissRail() {
        rail?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        rail = null
    }
}
