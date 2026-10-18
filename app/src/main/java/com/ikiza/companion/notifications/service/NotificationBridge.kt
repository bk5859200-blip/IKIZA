package com.ikiza.companion.notifications.service

import com.ikiza.companion.core.model.RawNotification
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory reactive event bridge connecting the Android NotificationListenerService
 * to the ViewModel and Companion UI.
 */
object NotificationBridge {
    private val _incomingNotifications = MutableSharedFlow<RawNotification>(extraBufferCapacity = 64)
    val incomingNotifications: SharedFlow<RawNotification> = _incomingNotifications.asSharedFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun postNotification(rawNotification: RawNotification) {
        _incomingNotifications.tryEmit(rawNotification)
    }

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }
}
