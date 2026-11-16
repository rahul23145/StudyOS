package com.studyos.cainter.ui

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.studyos.cainter.BuildConfig
import com.studyos.cainter.R
import com.studyos.cainter.databinding.ActivityMainBinding
import com.studyos.cainter.ui.analytics.AnalyticsFragment
import com.studyos.cainter.ui.dashboard.DashboardFragment
import com.studyos.cainter.ui.lectures.LecturesFragment
import com.studyos.cainter.ui.timetable.TimetableFragment
import com.studyos.cainter.ui.timer.TimerFragment
import com.studyos.cainter.util.BackupManager
import com.studyos.cainter.util.DateUtils
import com.studyos.cainter.util.MentorPdfReportGenerator
import com.studyos.cainter.util.StepCounterManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val dashboardFragment by lazy { DashboardFragment() }
    private val timetableFragment by lazy { TimetableFragment() }
    private val lecturesFragment by lazy { LecturesFragment() }
    private val timerFragment by lazy { TimerFragment() }
    private val analyticsFragment by lazy { AnalyticsFragment() }

    private val activityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            StepCounterManager.startListening()
        }
    }

    private val exportBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                try {
                    val json = BackupManager.createBackupJson(this@MainActivity)
                    contentResolver.openOutputStream(uri)?.use { os ->
                        os.write(json.toByteArray(Charsets.UTF_8))
                    }
                    android.widget.Toast.makeText(this@MainActivity, "✅ Offline backup exported successfully!", android.widget.Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@MainActivity, "❌ Failed to export backup: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val importBackupLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lifecycleScope.launch {
                try {
                    val json = contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""
                    val count = BackupManager.restoreBackupJson(this@MainActivity, json)
                    android.widget.Toast.makeText(this@MainActivity, "✅ Restored $count records successfully!", android.widget.Toast.LENGTH_LONG).show()
                    recreate()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this@MainActivity, "❌ Failed to restore backup: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (BuildConfig.IS_CUSTOM_APP) {
            binding.topAppBar.title = "StudyOS - Custom"
            binding.topAppBar.subtitle = "Personal Schedule & Routine Builder"
            binding.bottomNavigation.menu.findItem(R.id.navigation_lectures)?.title = "Subjects"
        } else {
            binding.topAppBar.title = "StudyOS - CA Inter"
            binding.topAppBar.subtitle = "CA Intermediate Preparation"
            binding.bottomNavigation.menu.findItem(R.id.navigation_lectures)?.title = "Lectures"
        }

        binding.topAppBar.inflateMenu(R.menu.main_top_menu)
        if (BuildConfig.IS_CUSTOM_APP) {
            binding.topAppBar.menu.findItem(R.id.action_mentor_pdf)?.isVisible = false
        }
        binding.topAppBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_mentor_pdf -> {
                    generateMentorPdfReport()
                    true
                }
                R.id.action_gym_ledger -> {
                    startActivity(android.content.Intent(this, com.studyos.cainter.ui.gym.GymLedgerActivity::class.java))
                    true
                }
                R.id.action_backup_restore -> {
                    showBackupRestoreDialog()
                    true
                }
                else -> false
            }
        }

        // Initialize hardware Step Counter
        StepCounterManager.initialize(this)
        requestActivityRecognitionPermissionIfNeeded()

        setupBottomNavigation()

        if (savedInstanceState == null) {
            switchFragment(dashboardFragment, "Dashboard")
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.getStringExtra("NAVIGATE_TO") == "TIMER") {
            binding.bottomNavigation.selectedItemId = R.id.navigation_timer
        }
    }

    private fun setupBottomNavigation() {
        try {
            binding.bottomNavigation.isItemActiveIndicatorEnabled = false
        } catch (_: Exception) {}
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    switchFragment(dashboardFragment, "Dashboard")
                    if (BuildConfig.IS_CUSTOM_APP) {
                        binding.topAppBar.title = "StudyOS - Custom"
                        binding.topAppBar.subtitle = "Personal Schedule & Routine Builder"
                    } else {
                        binding.topAppBar.title = "StudyOS - CA Inter"
                        binding.topAppBar.subtitle = "CA Intermediate Preparation"
                    }
                    true
                }
                R.id.navigation_timetable -> {
                    switchFragment(timetableFragment, "Timetable")
                    binding.topAppBar.title = "Weekly Timetable"
                    binding.topAppBar.subtitle = if (BuildConfig.IS_CUSTOM_APP) "Custom Routine & Time Blocks" else "CA Inter Routine & Study Blocks"
                    true
                }
                R.id.navigation_lectures -> {
                    switchFragment(lecturesFragment, "Lectures")
                    if (BuildConfig.IS_CUSTOM_APP) {
                        binding.topAppBar.title = "My Subjects & Targets"
                        binding.topAppBar.subtitle = "Custom Courses, Chapters & Milestones"
                    } else {
                        binding.topAppBar.title = "Lecture Tracker"
                        binding.topAppBar.subtitle = "Costing & Auditing Progress"
                    }
                    true
                }
                R.id.navigation_timer -> {
                    switchFragment(timerFragment, "Timer")
                    binding.topAppBar.title = "Study Timer"
                    binding.topAppBar.subtitle = "Focus Blocks & Presets"
                    true
                }
                R.id.navigation_analytics -> {
                    switchFragment(analyticsFragment, "Analytics")
                    binding.topAppBar.title = "Progress & Habits"
                    binding.topAppBar.subtitle = "7-Day Study Hours Graph & Consistency"
                    true
                }
                else -> false
            }
        }
    }

    private fun switchFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .commit()
    }

    fun openTimerWithPreset(title: String, durationMinutes: Int) {
        binding.bottomNavigation.selectedItemId = R.id.navigation_timer
        timerFragment.applyPreset(title, durationMinutes)
    }

    private fun showBackupRestoreDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Offline Data Backup & Restore")
            .setMessage("All your timetables, lecture trackers, study sessions, and workout ledger entries are stored offline.\n\nChoose an option:")
            .setPositiveButton("Export Backup") { _, _ ->
                val fileName = "StudyOS-Backup-${DateUtils.getTodayDateString()}.json"
                exportBackupLauncher.launch(fileName)
            }
            .setNeutralButton("Restore Backup") { _, _ ->
                importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun requestActivityRecognitionPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (!StepCounterManager.hasActivityRecognitionPermission(this)) {
                activityRecognitionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    private fun generateMentorPdfReport() {
        lifecycleScope.launch {
            try {
                android.widget.Toast.makeText(this@MainActivity, "⏳ Generating Weekly Report PDF...", android.widget.Toast.LENGTH_SHORT).show()
                val uri = MentorPdfReportGenerator.generateWeeklyReport(this@MainActivity)
                if (uri != null) {
                    com.google.android.material.snackbar.Snackbar.make(
                        binding.root,
                        "✅ Saved to Downloads: StudyOS Weekly Report PDF",
                        com.google.android.material.snackbar.Snackbar.LENGTH_LONG
                    ).setAction("Open PDF") {
                        MentorPdfReportGenerator.openPdfViewer(this@MainActivity, uri)
                    }.show()
                } else {
                    android.widget.Toast.makeText(this@MainActivity, "❌ Failed to save PDF to Downloads", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(this@MainActivity, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        StepCounterManager.stopListening()
    }
}
