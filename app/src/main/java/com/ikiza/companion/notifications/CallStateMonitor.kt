package com.ikiza.companion.notifications

import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Monitors cellular telephony call state changes using Android's TelephonyManager.
 * Compatible with modern TelephonyCallback (API 31+) and legacy PhoneStateListener.
 */
class CallStateMonitor(
    private val context: Context,
    private val onIncomingCall: (String) -> Unit
) {

    companion object {
        private const val TAG = "IKIZA_CallMonitor"
        private const val CALL_DEBOUNCE_MS = 10_000L
    }

    private var telephonyManager: TelephonyManager? = null
    private var lastRingingTime = 0L

    // For API 31+
    private var telephonyCallback: Any? = null

    // For API < 31
    private var legacyListener: PhoneStateListener? = null

    fun start() {
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val tm = telephonyManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleCallState(state, null)
                    }
                }
                tm.registerTelephonyCallback(context.mainExecutor, callback)
                telephonyCallback = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleCallState(state, phoneNumber)
                    }
                }
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyListener = listener
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "READ_PHONE_STATE permission not granted: ${e.message}")
        }
    }

    private fun handleCallState(state: Int, caller: String?) {
        if (state == TelephonyManager.CALL_STATE_RINGING) {
            val now = System.currentTimeMillis()
            if (now - lastRingingTime > CALL_DEBOUNCE_MS) {
                lastRingingTime = now
                val announcement = CallAnnouncementHelper.formatCallAnnouncement(caller)
                Log.i(TAG, "Telephony incoming call: $announcement")
                onIncomingCall(announcement)
            }
        }
    }

    fun stop() {
        val tm = telephonyManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = telephonyCallback as? TelephonyCallback
                if (callback != null) {
                    tm.unregisterTelephonyCallback(callback)
                }
            } else {
                val listener = legacyListener
                if (listener != null) {
                    @Suppress("DEPRECATION")
                    tm.listen(listener, PhoneStateListener.LISTEN_NONE)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering call monitor: ${e.message}")
        }
    }
}
