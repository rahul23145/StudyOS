package com.studyos.cainter

import android.app.Application
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

    override fun onCreate() {
        super.onCreate()
        // Ensure database defaults are populated on first launch
        applicationScope.launch(Dispatchers.IO) {
            database.openHelper.writableDatabase
            database.ensureSeeded()
        }
    }
}
