package com.studyos.cainter.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.studyos.cainter.data.default.DefaultScheduleProvider
import com.studyos.cainter.data.local.entity.CompletionRecord
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.local.entity.ScheduleType
import com.studyos.cainter.data.local.entity.StudySession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ScheduleItem::class,
        CompletionRecord::class,
        LectureTracker::class,
        StudySession::class,
        ScheduleType::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun completionDao(): CompletionDao
    abstract fun lectureDao(): LectureDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun scheduleTypeDao(): ScheduleTypeDao

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
                        populateDatabase(database)
                    }
                }
            }

            suspend fun populateDatabase(db: AppDatabase) {
                if (db.scheduleDao().getScheduleCount() == 0) {
                    val schedule = if (com.studyos.cainter.BuildConfig.IS_CUSTOM_APP) {
                        DefaultScheduleProvider.getCustomEditionSchedule()
                    } else {
                        DefaultScheduleProvider.getDefaultSchedule()
                    }
                    db.scheduleDao().insertAll(schedule)
                }
                if (db.lectureDao().getTrackerCount() == 0) {
                    val trackers = if (com.studyos.cainter.BuildConfig.IS_CUSTOM_APP) {
                        DefaultScheduleProvider.getCustomEditionTrackers()
                    } else {
                        DefaultScheduleProvider.getDefaultTrackers()
                    }
                    db.lectureDao().insertAll(trackers)
                }
                if (db.scheduleTypeDao().getScheduleTypeCount() == 0) {
                    db.scheduleTypeDao().insertAll(DefaultScheduleProvider.getDefaultScheduleTypes())
                }
            }
        }
    }
}
