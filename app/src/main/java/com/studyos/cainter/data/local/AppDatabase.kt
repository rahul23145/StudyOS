package com.studyos.cainter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studyos.cainter.BuildConfig
import com.studyos.cainter.data.default.DefaultScheduleProvider
import com.studyos.cainter.data.local.entity.CompletionRecord
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.local.entity.ScheduleType
import com.studyos.cainter.data.local.entity.StudySession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.studyos.cainter.data.local.entity.WorkoutEntry

@Database(
    entities = [
        ScheduleItem::class,
        CompletionRecord::class,
        LectureTracker::class,
        StudySession::class,
        ScheduleType::class,
        WorkoutEntry::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun completionDao(): CompletionDao
    abstract fun lectureDao(): LectureDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun scheduleTypeDao(): ScheduleTypeDao
    abstract fun workoutDao(): WorkoutDao

    suspend fun ensureSeeded() {
        withContext(Dispatchers.IO) {
            val isCustom = BuildConfig.IS_CUSTOM_APP
            if (scheduleTypeDao().getScheduleTypeCount() == 0) {
                scheduleTypeDao().insertAll(DefaultScheduleProvider.getDefaultScheduleTypes(isCustom))
            }
            for (typeId in 1L..5L) {
                if (scheduleDao().getScheduleCountForType(typeId) == 0) {
                    val items = DefaultScheduleProvider.getScheduleForType(typeId)
                    scheduleDao().insertAll(items)
                }
            }
            if (lectureDao().getTrackerCount() == 0) {
                val trackers = if (isCustom) {
                    DefaultScheduleProvider.getCustomEditionTrackers()
                } else {
                    DefaultScheduleProvider.getDefaultTrackers()
                }
                lectureDao().insertAll(trackers)
            }
            // Always purge any duplicate entries
            scheduleDao().removeDuplicates()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studyos_cainter_database.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        database.ensureSeeded()
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        database.scheduleDao().removeDuplicates()
                    }
                }
            }
        }
    }
}
