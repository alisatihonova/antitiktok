package com.example.antitiktok

import android.accessibilityservice.AccessibilityService
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.UUID

class SwipeAccessibilityService : AccessibilityService() {

    private data class BufferedEvent(val time: Long, val csv: String)
    private val buffer = mutableListOf<BufferedEvent>()

    private var sessionId: String = UUID.randomUUID().toString()
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var timeoutRunnable: Runnable? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !AppState.isRecording(this)) return

        buffer.add(
            BufferedEvent(
                System.currentTimeMillis(),
                AccessibilityEvent.eventTypeToString(event.eventType) + "," +
                        event.eventTime + "," +
                        (event.packageName ?: "") + "\n"
            )
        )
    }

    override fun onServiceConnected() {
        startTimer()
    }

    private fun startTimer() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (AppState.isRecording(this@SwipeAccessibilityService)) {
                    showOverlay()
                }
                handler.postDelayed(this, AppState.getCheckIntervalMin(this@SwipeAccessibilityService) * 60_000L)
            }
        }, AppState.getCheckIntervalMin(this) * 60_000L)
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = "Тип деятельности"
            textSize = 18f
        }

        container.addView(title)

        fun select(type: String) {
            flush(type)
            removeOverlay()
        }

        listOf(
            "Полезное" to "good",
            "Короткие видео" to "tiktok",
            "Бесполезное" to "doom"
        ).forEach { (label, value) ->
            container.addView(Button(this).apply {
                text = label
                setOnClickListener { select(value) }
            })
        }

        overlayView = container

        wm.addView(
            overlayView,
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }
        )

        timeoutRunnable = Runnable { select("unknown") }
        handler.postDelayed(timeoutRunnable!!, 10_000L)
    }

    private fun removeOverlay() {
        timeoutRunnable?.let { handler.removeCallbacks(it) }
        timeoutRunnable = null

        overlayView?.let {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(it)
        }
        overlayView = null
    }

    private fun flush(type: String) {
        val cutoff = System.currentTimeMillis() -
                AppState.getCheckIntervalMin(this) * 60_000L

        buffer.filter { it.time >= cutoff }.forEach {
            append("gestureActivity.csv", "$sessionId,$type,${it.csv}")
        }
        buffer.removeIf { it.time >= cutoff }
    }

    private fun append(fileName: String, text: String) {
        val resolver = contentResolver
        val uri = findFile(fileName)

        if (uri != null) {
            resolver.openOutputStream(uri, "wa")?.use {
                it.write(text.toByteArray())
            }
        } else {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            }
            resolver.insert(MediaStore.Files.getContentUri("external"), values)?.let {
                resolver.openOutputStream(it)?.use { os ->
                    os.write("sessionId,activityType,eventType,eventTime,package\n".toByteArray())
                    os.write(text.toByteArray())
                }
            }
        }
    }

    private fun findFile(name: String) =
        contentResolver.query(
            MediaStore.Files.getContentUri("external"),
            arrayOf(MediaStore.Files.FileColumns._ID),
            "${MediaStore.Files.FileColumns.DISPLAY_NAME}=?",
            arrayOf(name),
            null
        )?.use {
            if (it.moveToFirst())
                ContentUris.withAppendedId(
                    MediaStore.Files.getContentUri("external"),
                    it.getLong(0)
                )
            else null
        }

    override fun onInterrupt() {}
}
