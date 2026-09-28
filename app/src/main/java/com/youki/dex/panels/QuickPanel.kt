package com.youki.dex.panels

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.youki.dex.R
import com.youki.dex.utils.ColorUtils
import com.youki.dex.utils.EventJournal
import com.youki.dex.utils.ShizukoManager
import com.youki.dex.utils.Utils

/**
 * ClauDEX quick panel (owner, 28/09): the levels and connections one reaches
 * for while driving - brightness, media volume, Wi-Fi <-> mobile data,
 * Bluetooth and its paired devices - without pulling the notification shade.
 *
 * Kept out of PerfectServer.kt on purpose (that file is ~370 KB). The host
 * only opens and closes it.
 *
 * Privilege map, measured on a SM-A055M (Android 15): Wi-Fi, mobile data and
 * Bluetooth on/off are shell commands (cmd wifi set-wifi-enabled, svc data,
 * cmd bluetooth_manager) -> Shizuku; brightness and volume need no privilege;
 * connecting a paired device has no shell command, so it is tried through the
 * profile proxies and falls back to the Bluetooth settings screen.
 */
class QuickPanel(
    private val context: Context,
    private val windowManager: WindowManager,
    private val prefs: SharedPreferences,
    private val onOpenShade: () -> Unit
) {
    private var view: View? = null
    private val main = Handler(Looper.getMainLooper())
    private val profiles = HashMap<Int, BluetoothProfile>()

    val isShowing: Boolean get() = view != null

    fun toggle() = if (isShowing) dismiss() else show()

    fun dismiss() {
        view?.let { try { windowManager.removeView(it) } catch (e: Exception) {} }
        view = null
        val bt = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        profiles.forEach { (id, p) -> try { bt?.closeProfileProxy(id, p) } catch (e: Exception) {} }
        profiles.clear()
    }

    fun show() {
        if (isShowing) return
        val v = LayoutInflater.from(context).inflate(R.layout.quick_panel, null)
        ColorUtils.applyMainColor(context, prefs, v)
        val params = Utils.makeWindowParams(-2, -2, context, fitNavInsets = true)
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        params.gravity = Gravity.BOTTOM or Gravity.END
        params.y = Utils.dpToPx(context, 52)
        params.x = Utils.dpToPx(context, 8)
        v.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) dismiss()
            false
        }
        bindBrightness(v.findViewById(R.id.qp_brightness))
        bindVolume(v.findViewById(R.id.qp_volume))
        bindToggles(v)
        bindDevices(v.findViewById(R.id.qp_devices))
        try { windowManager.addView(v, params); view = v } catch (e: Exception) {}
        EventJournal.log(context, "quickpanel: open")
    }

    // ── levels (no privilege) ──────────────────────────────────────────────
    private fun bindBrightness(sb: SeekBar) {
        sb.progress = try { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) } catch (e: Exception) { 128 }
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                if (!fromUser) return
                try {
                    // a hand-set level means manual mode, or the sensor undoes it
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
                } catch (e: Exception) {}
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    private fun bindVolume(sb: SeekBar) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        sb.max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        sb.progress = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                if (fromUser) try { audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0) } catch (e: Exception) {}
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    // ── connections (Shizuku) ───────────────────────────────────────────────
    @Suppress("DEPRECATION")
    private fun wifiOn() = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).isWifiEnabled
    private fun dataOn() = Settings.Global.getInt(context.contentResolver, "mobile_data", 0) == 1
    private fun btOn() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter?.isEnabled == true

    private fun bindToggles(v: View) {
        val wifi = v.findViewById<TextView>(R.id.qp_wifi)
        val data = v.findViewById<TextView>(R.id.qp_data)
        val bt = v.findViewById<TextView>(R.id.qp_bluetooth)
        fun paint() {
            wifi.alpha = if (wifiOn()) 1f else 0.45f
            data.alpha = if (dataOn()) 1f else 0.45f
            bt.alpha = if (btOn()) 1f else 0.45f
        }
        paint()
        wifi.setOnClickListener {
            shell(if (wifiOn()) "cmd wifi set-wifi-enabled disabled" else "cmd wifi set-wifi-enabled enabled") { paint() }
        }
        // pick a specific network: the public system panel, no shade
        val q = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
        wifi.setOnLongClickListener {
            openActivity(Intent(if (q) Settings.Panel.ACTION_WIFI else Settings.ACTION_WIFI_SETTINGS)); true
        }
        data.setOnClickListener { shell(if (dataOn()) "svc data disable" else "svc data enable") { paint() } }
        data.setOnLongClickListener {
            openActivity(Intent(if (q) Settings.Panel.ACTION_INTERNET_CONNECTIVITY else Settings.ACTION_WIRELESS_SETTINGS)); true
        }
        bt.setOnClickListener {
            shell(if (btOn()) "cmd bluetooth_manager disable" else "cmd bluetooth_manager enable") {
                paint()
                view?.let { bindDevices(it.findViewById(R.id.qp_devices)) }
            }
        }
        bt.setOnLongClickListener { openActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); true }
        v.findViewById<TextView>(R.id.qp_notifications).setOnClickListener { dismiss(); onOpenShade() }
    }

    /** Runs [cmd] over Shizuku off the main thread, then [after] on it (state settles in ~1 s). */
    private fun shell(cmd: String, after: () -> Unit) {
        val shizuku = ShizukoManager.getInstance(context)
        if (!shizuku.hasPermission) {
            Toast.makeText(context, R.string.qp_needs_shizuku, Toast.LENGTH_SHORT).show()
            return
        }
        Thread {
            val out = shizuku.runShellSync(cmd)
            EventJournal.log(context, "quickpanel: $cmd -> ${out?.take(60)}")
            main.postDelayed({ if (isShowing) after() }, 900)
        }.start()
    }

    // ── paired Bluetooth devices ───────────────────────────────────────────
    @SuppressLint("MissingPermission")
    private fun bindDevices(box: LinearLayout) {
        box.removeAllViews()
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter ?: return
        if (!adapter.isEnabled) return
        val devices = try { adapter.bondedDevices.toList() } catch (e: SecurityException) { emptyList() }
        if (devices.isEmpty()) return
        val rows = devices.associateWith { d -> deviceRow(box, d) }
        // the proxies answer asynchronously; rows show "connected" once they do
        listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET).forEach { id ->
            try {
                adapter.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
                    override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                        profiles[profile] = proxy
                        rows.forEach { (d, row) -> if (isConnected(d)) row.text = "${label(d)}  ·  ${context.getString(R.string.qp_connected)}" }
                    }
                    override fun onServiceDisconnected(profile: Int) { profiles.remove(profile) }
                }, id)
            } catch (e: Exception) {}
        }
    }

    @SuppressLint("MissingPermission")
    private fun label(d: BluetoothDevice): String = try { d.name ?: d.address } catch (e: SecurityException) { d.address }

    @SuppressLint("MissingPermission")
    private fun isConnected(d: BluetoothDevice) = profiles.values.any {
        try { it.getConnectionState(d) == BluetoothProfile.STATE_CONNECTED } catch (e: Exception) { false }
    }

    private fun deviceRow(box: LinearLayout, d: BluetoothDevice): TextView {
        val row = TextView(context)
        row.text = label(d)
        row.setTextColor(0xFFFFFFFF.toInt())
        row.textSize = 15f
        row.gravity = Gravity.CENTER_VERTICAL
        row.minHeight = Utils.dpToPx(context, 48)
        row.setPadding(Utils.dpToPx(context, 8), 0, Utils.dpToPx(context, 8), 0)
        row.setOnClickListener { connect(d, row) }
        box.addView(row, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        return row
    }

    /**
     * Connects a paired device through the hidden connect() of the A2DP and
     * headset proxies. On recent Android that needs BLUETOOTH_PRIVILEGED, which
     * a regular app does not hold - so a refusal opens the Bluetooth screen
     * (one tap more, still no shade) and is journaled, to be measured.
     */
    private fun connect(d: BluetoothDevice, row: TextView) {
        row.text = "${label(d)}  ·  ${context.getString(R.string.qp_connecting)}"
        var ok = false
        var why = "no proxy"
        profiles.values.forEach { p ->
            try {
                val m = p.javaClass.getMethod("connect", BluetoothDevice::class.java)
                ok = (m.invoke(p, d) as? Boolean) == true || ok
                why = "ok=$ok"
            } catch (e: java.lang.reflect.InvocationTargetException) {
                why = e.targetException?.javaClass?.simpleName ?: "invoke"
            } catch (e: Exception) {
                why = e.javaClass.simpleName
            }
        }
        EventJournal.log(context, "quickpanel: connect ${d.address} -> $why")
        if (!ok) { openActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); return }
        // measured on a SM-A055M: connect() is accepted and the A2DP stack
        // really tries (CONNECTING -> DISCONNECTED when the device is off or
        // out of range) - so say how it ended instead of "connecting..." forever
        main.postDelayed({
            if (!isShowing) return@postDelayed
            val done = isConnected(d)
            row.text = "${label(d)}  ·  ${context.getString(if (done) R.string.qp_connected else R.string.qp_not_connected)}"
            EventJournal.log(context, "quickpanel: connect ${d.address} ended connected=$done")
        }, 6000)
    }

    private fun openActivity(intent: Intent) {
        dismiss()
        try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (e: Exception) {}
    }
}
