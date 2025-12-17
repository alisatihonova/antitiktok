package com.example.antitiktok

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var serviceStatusText: TextView
    private lateinit var recordingStatusText: TextView
    private lateinit var intervalInput: EditText
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var clearButton: Button
    private lateinit var settingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        serviceStatusText = TextView(this).apply { textSize = 18f }
        recordingStatusText = TextView(this).apply { textSize = 18f }

        intervalInput = EditText(this).apply {
            hint = "Интервал проверки (мин)"
            inputType = InputType.TYPE_CLASS_NUMBER
            filters = arrayOf(InputFilter.LengthFilter(2))
            setText(AppState.getCheckIntervalMin(this@MainActivity).toString())
        }

        intervalInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toIntOrNull() ?: return
                AppState.setCheckIntervalMin(this@MainActivity, v)
            }
        })

        settingsButton = Button(this).apply {
            text = "Открыть настройки доступности"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        startButton = Button(this).apply {
            text = "Начать запись"
            setOnClickListener {
                AppState.setRecording(this@MainActivity, true)
                updateUI()
            }
        }

        stopButton = Button(this).apply {
            text = "Остановить запись"
            setOnClickListener {
                AppState.setRecording(this@MainActivity, false)
                updateUI()
            }
        }

        clearButton = Button(this).apply {
            text = "Очистить файл"
            setOnClickListener { clearGestureFile() }
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
            addView(serviceStatusText)
            addView(recordingStatusText)
            addView(intervalInput)
            addView(settingsButton)
            addView(startButton)
            addView(stopButton)
            addView(clearButton)
        }

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val serviceEnabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )?.contains(packageName) == true

        serviceStatusText.text =
            if (serviceEnabled) "Служба включена" else "Служба выключена"
        serviceStatusText.setTextColor(
            if (serviceEnabled) Color.parseColor("#2E7D32") else Color.DKGRAY
        )

        val recording = AppState.isRecording(this)
        recordingStatusText.text =
            if (recording) "Файл записывается" else "Запись остановлена"
        recordingStatusText.setTextColor(
            if (recording) Color.parseColor("#2E7D32") else Color.DKGRAY
        )

        startButton.isEnabled = serviceEnabled && !recording
        stopButton.isEnabled = serviceEnabled && recording
    }

    private fun clearGestureFile() {
        contentResolver.delete(
            MediaStore.Files.getContentUri("external"),
            "${MediaStore.Files.FileColumns.RELATIVE_PATH}=? AND ${MediaStore.Files.FileColumns.DISPLAY_NAME}=?",
            arrayOf("${Environment.DIRECTORY_DOCUMENTS}/", "gestureActivity.csv")
        )
    }
}
