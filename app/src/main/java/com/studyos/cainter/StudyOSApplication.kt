package com.studyos.cainter

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.studyos.cainter.data.local.AppDatabase
import com.studyos.cainter.data.repository.ScheduleRepository
import com.studyos.cainter.data.repository.StudyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyOSApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val scheduleRepository by lazy {
        ScheduleRepository(
            database.scheduleDao(),
            database.completionDao(),
            database.scheduleTypeDao(),
            database.studySessionDao()
        )
    }
    val studyRepository by lazy {
        StudyRepository(
            database.lectureDao(),
            database.studySessionDao()
        )
    }
    val workoutRepository by lazy {
        com.studyos.cainter.data.repository.WorkoutRepository(
            database.workoutDao()
        )
    }
    val todoRepository by lazy {
        com.studyos.cainter.data.repository.TodoRepository(
            database.todoDao()
        )
    }
    val stepRepository by lazy {
        com.studyos.cainter.data.repository.StepRepository(
            database.stepDao()
        )
    }
    val distractionRepository by lazy {
        com.studyos.cainter.data.repository.DistractionRepository(
            database.distractionDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Lock app to clean, crisp light theme to prevent device dark mode from muddying selection colors
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Ensure database defaults are populated on first launch
        applicationScope.launch(Dispatchers.IO) {
            database.openHelper.writableDatabase
            database.ensureSeeded(this@StudyOSApplication)
        }

        // Initialize step tracking if enabled
        if (com.studyos.cainter.util.StepCounterManager.isTrackingEnabled(this)) {
            com.studyos.cainter.util.StepCounterManager.initialize(this)
        }
    }
}
