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

import com.studyos.cainter.data.local.entity.DistractionRecord
import com.studyos.cainter.data.local.entity.StepRecord
import com.studyos.cainter.data.local.entity.TodoItem
import com.studyos.cainter.data.local.entity.WorkoutEntry
import androidx.room.migration.Migration

@Database(
    entities = [
        ScheduleItem::class,
        CompletionRecord::class,
        LectureTracker::class,
        StudySession::class,
        ScheduleType::class,
        WorkoutEntry::class,
        TodoItem::class,
        StepRecord::class,
        DistractionRecord::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun scheduleDao(): ScheduleDao
    abstract fun completionDao(): CompletionDao
    abstract fun lectureDao(): LectureDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun scheduleTypeDao(): ScheduleTypeDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun todoDao(): TodoDao
    abstract fun stepDao(): StepDao
    abstract fun distractionDao(): DistractionDao

    suspend fun ensureSeeded(context: Context? = null) {
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

            // Auto-upgrade CA Inter schedule to v1.21.0 (Live Audit 8:45 AM + Afternoon Costing)
            context?.let { ctx ->
                val prefs = ctx.getSharedPreferences("studyos_db_version_prefs", Context.MODE_PRIVATE)
                val currentVersion = prefs.getInt("key_ca_schedule_schema_version", 0)
                if (currentVersion < 21 && !isCustom) {
                    // Update default schedule type 1
                    scheduleDao().clearForType(1L)
                    scheduleDao().insertAll(DefaultScheduleProvider.getScheduleForType(1L))
                    // Update lecture tracker: Audit = 1 daily target (105m), preserve completed lectures
                    val existingAudit = lectureDao().getTrackerSync("AUDIT")
                    val existingCosting = lectureDao().getTrackerSync("COSTING")
                    lectureDao().insertOrUpdate(
                        LectureTracker(
                            subject = "COSTING",
                            displayName = "Cost & Management Accounting (Paper 4)",
                            totalLectures = 85,
                            completedLectures = existingCosting?.completedLectures ?: 0,
                            dailyTarget = 2,
                            lectureDurationMinutes = 138
                        )
                    )
                    lectureDao().insertOrUpdate(
                        LectureTracker(
                            subject = "AUDIT",
                            displayName = "Auditing and Ethics (Paper 5)",
                            totalLectures = 65,
                            completedLectures = existingAudit?.completedLectures ?: 0,
                            dailyTarget = 1,
                            lectureDurationMinutes = 105
                        )
                    )
                    prefs.edit().putInt("key_ca_schedule_schema_version", 21).apply()
                }
            }

            // Always purge any duplicate entries
            scheduleDao().removeDuplicates()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `todo_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER,
                        `category` TEXT NOT NULL DEFAULT 'GENERAL'
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_step_records` (
                        `date` TEXT PRIMARY KEY NOT NULL,
                        `totalSteps` INTEGER NOT NULL DEFAULT 0,
                        `walkingSteps` INTEGER NOT NULL DEFAULT 0,
                        `runningSteps` INTEGER NOT NULL DEFAULT 0,
                        `distanceMeters` REAL NOT NULL DEFAULT 0,
                        `caloriesBurned` REAL NOT NULL DEFAULT 0,
                        `lastUpdatedTimestamp` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `daily_distraction_records` (
                        `date` TEXT PRIMARY KEY NOT NULL,
                        `reelsCount` INTEGER NOT NULL DEFAULT 0,
                        `shortsCount` INTEGER NOT NULL DEFAULT 0,
                        `totalReelsSeconds` INTEGER NOT NULL DEFAULT 0,
                        `totalShortsSeconds` INTEGER NOT NULL DEFAULT 0,
                        `otherAppsSeconds` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "studyos_cainter_database.db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
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
