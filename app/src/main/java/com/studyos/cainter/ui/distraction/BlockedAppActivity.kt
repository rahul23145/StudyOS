package com.studyos.cainter.ui.distraction

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.studyos.cainter.databinding.ActivityBlockedAppBinding
import com.studyos.cainter.ui.MainActivity

class BlockedAppActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBlockedAppBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlockedAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appName = intent.getStringExtra("BLOCKED_APP_NAME") ?: "This application"
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "Focus Session"
        val remaining = intent.getStringExtra("REMAINING_TIME") ?: "--:--"

        binding.tvBlockedAppName.text = "$appName is blocked during focus sessions!"
        binding.tvActiveTaskTitle.text = taskTitle
        binding.tvRemainingTime.text = remaining

        binding.btnBackToStudy.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("NAVIGATE_TO", "TIMER")
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NAVIGATE_TO", "TIMER")
        }
        startActivity(intent)
        finish()
    }
}
