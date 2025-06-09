package com.yourdomain.chaosoul.services

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import com.yourdomain.chaosoul.data.database.AppDatabase
import com.yourdomain.chaosoul.data.model.TypingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KeyboardMonitorService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private lateinit var db: AppDatabase
    private var isKeyboardVisible = false
    private var keyboardVisibleTimestamp: Long = 0

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = serviceInfo ?: AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        var isImeUp = false
        for (window in windows) {
            if (window.type == AccessibilityServiceInfo.FLAG_INPUT_METHOD_EDITOR) {
                isImeUp = true
                break
            }
        }

        if (isImeUp) {
            if (!isKeyboardVisible) {
                isKeyboardVisible = true
                keyboardVisibleTimestamp = System.currentTimeMillis()
            }
        } else {
            if (isKeyboardVisible) {
                isKeyboardVisible = false
                val duration = System.currentTimeMillis() - keyboardVisibleTimestamp
                if (duration > 300) {
                    scope.launch {
                        db.typingEventDao().insert(TypingEvent(timestamp = System.currentTimeMillis(), durationMillis = duration))
                    }
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}