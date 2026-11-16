package com.studyos.cainter.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.studyos.cainter.R
import com.studyos.cainter.databinding.ActivityMainBinding
import com.studyos.cainter.ui.analytics.AnalyticsFragment
import com.studyos.cainter.ui.dashboard.DashboardFragment
import com.studyos.cainter.ui.lectures.LecturesFragment
import com.studyos.cainter.ui.timetable.TimetableFragment
import com.studyos.cainter.ui.timer.TimerFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val dashboardFragment by lazy { DashboardFragment() }
    private val timetableFragment by lazy { TimetableFragment() }
    private val lecturesFragment by lazy { LecturesFragment() }
    private val timerFragment by lazy { TimerFragment() }
    private val analyticsFragment by lazy { AnalyticsFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        if (savedInstanceState == null) {
            switchFragment(dashboardFragment, "Dashboard")
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    switchFragment(dashboardFragment, "Dashboard")
                    binding.topAppBar.title = getString(R.string.app_name)
                    binding.topAppBar.subtitle = getString(R.string.app_tagline)
                    true
                }
                R.id.navigation_timetable -> {
                    switchFragment(timetableFragment, "Timetable")
                    binding.topAppBar.title = "Weekly Timetable"
                    binding.topAppBar.subtitle = "Editable Routine & Study Blocks"
                    true
                }
                R.id.navigation_lectures -> {
                    switchFragment(lecturesFragment, "Lectures")
                    binding.topAppBar.title = "Lecture Tracker"
                    binding.topAppBar.subtitle = "Costing & Auditing Progress"
                    true
                }
                R.id.navigation_timer -> {
                    switchFragment(timerFragment, "Timer")
                    binding.topAppBar.title = "Study Timer"
                    binding.topAppBar.subtitle = "2h Blocks & Presets"
                    true
                }
                R.id.navigation_analytics -> {
                    switchFragment(analyticsFragment, "Analytics")
                    binding.topAppBar.title = "Progress & Habits"
                    binding.topAppBar.subtitle = "Offline Completion Metrics"
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
}
