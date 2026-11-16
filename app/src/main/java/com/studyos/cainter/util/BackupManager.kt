package com.studyos.cainter.util

import android.content.Context
import com.studyos.cainter.StudyOSApplication
import com.studyos.cainter.data.local.entity.CompletionRecord
import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.data.local.entity.ScheduleItem
import com.studyos.cainter.data.local.entity.ScheduleType
import com.studyos.cainter.data.local.entity.StudySession
import com.studyos.cainter.data.local.entity.WorkoutEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupManager {

    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val app = context.applicationContext as StudyOSApplication
        val db = app.database

        val schedules = db.scheduleDao().getAllScheduleItemsSync()
        val completions = db.completionDao().getAllCompletions()
        val lectures = db.lectureDao().getAllTrackersSync()
        val sessions = db.studySessionDao().getAllSessionsSync()
        val scheduleTypes = db.scheduleTypeDao().getAllScheduleTypesSync()
        val workouts = db.workoutDao().getAllWorkoutsSync()

        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "StudyOS")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("dateDisplay", DateUtils.getFormattedDisplayDate())

        // Schedules
        val schedArr = JSONArray()
        for (item in schedules) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("scheduleTypeId", item.scheduleTypeId)
            obj.put("dayOfWeek", item.dayOfWeek)
            obj.put("title", item.title)
            obj.put("category", item.category)
            obj.put("startTime", item.startTime)
            obj.put("endTime", item.endTime)
            obj.put("durationMinutes", item.durationMinutes)
            obj.put("notes", item.notes)
            obj.put("sortOrder", item.sortOrder)
            obj.put("isCustom", item.isCustom)
            schedArr.put(obj)
        }
        root.put("schedules", schedArr)

        // Completions
        val compArr = JSONArray()
        for (comp in completions) {
            val obj = JSONObject()
            obj.put("id", comp.id)
            obj.put("date", comp.date)
            obj.put("scheduleItemId", comp.scheduleItemId)
            obj.put("isCompleted", comp.isCompleted)
            obj.put("completedAt", comp.completedAt)
            compArr.put(obj)
        }
        root.put("completions", compArr)

        // Lectures
        val lecArr = JSONArray()
        for (lec in lectures) {
            val obj = JSONObject()
            obj.put("subject", lec.subject)
            obj.put("displayName", lec.displayName)
            obj.put("totalLectures", lec.totalLectures)
            obj.put("completedLectures", lec.completedLectures)
            obj.put("dailyTarget", lec.dailyTarget)
            obj.put("lectureDurationMinutes", lec.lectureDurationMinutes)
            obj.put("lastUpdatedDate", lec.lastUpdatedDate)
            lecArr.put(obj)
        }
        root.put("lectures", lecArr)

        // Study Sessions
        val sessArr = JSONArray()
        for (sess in sessions) {
            val obj = JSONObject()
            obj.put("id", sess.id)
            obj.put("subject", sess.subject)
            obj.put("category", sess.category)
            obj.put("durationMinutes", sess.durationMinutes)
            obj.put("timestamp", sess.timestamp)
            obj.put("notes", sess.notes)
            sessArr.put(obj)
        }
        root.put("studySessions", sessArr)

        // Schedule Types
        val typesArr = JSONArray()
        for (type in scheduleTypes) {
            val obj = JSONObject()
            obj.put("id", type.id)
            obj.put("name", type.name)
            obj.put("description", type.description)
            obj.put("targetDailyHours", type.targetDailyHours.toDouble())
            obj.put("wakeTime", type.wakeTime)
            obj.put("sleepTime", type.sleepTime)
            obj.put("isActive", type.isActive)
            typesArr.put(obj)
        }
        root.put("scheduleTypes", typesArr)

        // Workouts
        val workArr = JSONArray()
        for (w in workouts) {
            val obj = JSONObject()
            obj.put("id", w.id)
            obj.put("date", w.date)
            obj.put("timestamp", w.timestamp)
            obj.put("muscleGroup", w.muscleGroup)
            obj.put("exerciseName", w.exerciseName)
            obj.put("sets", w.sets)
            obj.put("reps", w.reps)
            obj.put("weightKg", w.weightKg.toDouble())
            obj.put("notes", w.notes)
            workArr.put(obj)
        }
        root.put("workouts", workArr)

        root.toString(2)
    }

    suspend fun restoreBackupJson(context: Context, jsonString: String): Int = withContext(Dispatchers.IO) {
        val app = context.applicationContext as StudyOSApplication
        val db = app.database

        val root = JSONObject(jsonString)
        var importedCount = 0

        // Schedule Types
        if (root.has("scheduleTypes")) {
            val arr = root.getJSONArray("scheduleTypes")
            val list = mutableListOf<ScheduleType>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ScheduleType(
                        id = o.optLong("id", 0L),
                        name = o.getString("name"),
                        description = o.optString("description", ""),
                        targetDailyHours = o.optDouble("targetDailyHours", 8.0).toFloat(),
                        wakeTime = o.optString("wakeTime", "07:00"),
                        sleepTime = o.optString("sleepTime", "23:30"),
                        isActive = o.optBoolean("isActive", false)
                    )
                )
            }
            if (list.isNotEmpty()) {
                db.scheduleTypeDao().insertAll(list)
                importedCount += list.size
            }
        }

        // Schedules
        if (root.has("schedules")) {
            val arr = root.getJSONArray("schedules")
            val list = mutableListOf<ScheduleItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    ScheduleItem(
                        id = o.optLong("id", 0L),
                        scheduleTypeId = o.optLong("scheduleTypeId", 1L),
                        dayOfWeek = o.getInt("dayOfWeek"),
                        title = o.getString("title"),
                        category = o.getString("category"),
                        startTime = o.getString("startTime"),
                        endTime = o.getString("endTime"),
                        durationMinutes = o.getInt("durationMinutes"),
                        notes = o.optString("notes", ""),
                        sortOrder = o.optInt("sortOrder", 0),
                        isCustom = o.optBoolean("isCustom", false)
                    )
                )
            }
            if (list.isNotEmpty()) {
                db.scheduleDao().insertAll(list)
                db.scheduleDao().removeDuplicates()
                importedCount += list.size
            }
        }

        // Lectures
        if (root.has("lectures")) {
            val arr = root.getJSONArray("lectures")
            val list = mutableListOf<LectureTracker>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    LectureTracker(
                        subject = o.getString("subject"),
                        displayName = o.optString("displayName", o.getString("subject")),
                        totalLectures = o.getInt("totalLectures"),
                        completedLectures = o.getInt("completedLectures"),
                        dailyTarget = o.optInt("dailyTarget", 2),
                        lectureDurationMinutes = o.optInt("lectureDurationMinutes", 90),
                        lastUpdatedDate = o.optString("lastUpdatedDate", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                db.lectureDao().insertAll(list)
                importedCount += list.size
            }
        }

        // Completions
        if (root.has("completions")) {
            val arr = root.getJSONArray("completions")
            val list = mutableListOf<CompletionRecord>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    CompletionRecord(
                        id = o.optLong("id", 0L),
                        date = o.getString("date"),
                        scheduleItemId = o.getLong("scheduleItemId"),
                        isCompleted = o.getBoolean("isCompleted"),
                        completedAt = o.optLong("completedAt", System.currentTimeMillis())
                    )
                )
            }
            if (list.isNotEmpty()) {
                db.completionDao().insertCompletions(list)
                importedCount += list.size
            }
        }

        // Study Sessions
        if (root.has("studySessions")) {
            val arr = root.getJSONArray("studySessions")
            val list = mutableListOf<StudySession>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    StudySession(
                        id = o.optLong("id", 0L),
                        subject = o.getString("subject"),
                        category = o.getString("category"),
                        durationMinutes = o.getInt("durationMinutes"),
                        timestamp = o.getLong("timestamp"),
                        notes = o.optString("notes", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                db.studySessionDao().insertSessions(list)
                importedCount += list.size
            }
        }

        // Workouts
        if (root.has("workouts")) {
            val arr = root.getJSONArray("workouts")
            val list = mutableListOf<WorkoutEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    WorkoutEntry(
                        id = o.optLong("id", 0L),
                        date = o.getString("date"),
                        timestamp = o.getLong("timestamp"),
                        muscleGroup = o.getString("muscleGroup"),
                        exerciseName = o.getString("exerciseName"),
                        sets = o.optInt("sets", 3),
                        reps = o.optInt("reps", 10),
                        weightKg = o.optDouble("weightKg", 0.0).toFloat(),
                        notes = o.optString("notes", "")
                    )
                )
            }
            if (list.isNotEmpty()) {
                db.workoutDao().insertWorkouts(list)
                importedCount += list.size
            }
        }

        importedCount
    }
}
