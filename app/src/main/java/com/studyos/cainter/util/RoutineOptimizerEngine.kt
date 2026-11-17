package com.studyos.cainter.util

import com.studyos.cainter.data.local.entity.ScheduleCategory
import com.studyos.cainter.data.local.entity.ScheduleItem
import java.util.Locale

object RoutineOptimizerEngine {

    enum class GymShift(val displayName: String) {
        MORNING("Morning Gym (7:15 AM)"),
        EVENING("Evening Gym (6:00 PM)"),
        NIGHT("Night Workout (8:30 PM)"),
        NONE("Rest Day / Skip Gym")
    }

    enum class TuitionShift(val displayName: String, val minutes: Int) {
        EVENING_6PM("Evening 6:00–7:00 PM (MWF)", 60),
        MORNING_8AM("Morning 8:00–9:00 AM", 60),
        MORNING_10AM("Morning 10:00–11:00 AM", 60),
        AFTERNOON_3PM("Afternoon 3:00–4:00 PM", 60),
        NONE("No Tuition Today", 0)
    }

    data class OptimizationParams(
        val dayOfWeek: Int,
        val scheduleTypeId: Long = 1L,
        val wakeTimeStr: String = "07:00",
        val bedTimeStr: String = "23:30",
        val gymShift: GymShift = GymShift.MORNING,
        val tuitionShift: TuitionShift = TuitionShift.EVENING_6PM,
        val targetStudyHours: Double = 11.5
    )

    data class OptimizationResult(
        val items: List<ScheduleItem>,
        val totalStudyMinutes: Int,
        val costingMinutes: Int,
        val auditMinutes: Int,
        val tuitionMinutes: Int,
        val practiceMinutes: Int,
        val totalStudyHoursDisplay: String,
        val sleepHoursDisplay: String,
        val summaryNotes: String
    )

    fun optimize(params: OptimizationParams): OptimizationResult {
        val targetStudyMinutes = (params.targetStudyHours * 60).toInt()
        val wakeMins = timeToMinutes(params.wakeTimeStr)
        val bedMins = timeToMinutes(params.bedTimeStr)
        val wakingMins = (if (bedMins <= wakeMins) bedMins + 1440 else bedMins) - wakeMins

        val items = mutableListOf<ScheduleItem>()
        var currentMins = wakeMins
        var order = 1

        fun addBlock(
            title: String,
            category: ScheduleCategory,
            duration: Int,
            notes: String
        ) {
            val startStr = minutesToTime(currentMins)
            val endMins = currentMins + duration
            val endStr = minutesToTime(endMins)

            items.add(
                ScheduleItem(
                    scheduleTypeId = params.scheduleTypeId,
                    dayOfWeek = params.dayOfWeek,
                    title = title,
                    category = category.name,
                    startTime = startStr,
                    endTime = endStr,
                    durationMinutes = duration,
                    notes = notes,
                    sortOrder = order++
                )
            )
            currentMins = endMins
        }

        // 1. Wake up
        addBlock("Wake Up & Hydrate", ScheduleCategory.WAKE, 15, "Circadian rhythm start & hydration")

        // 2. Morning Gym (if selected)
        if (params.gymShift == GymShift.MORNING) {
            addBlock("Morning Gym / Workout", ScheduleCategory.GYM, 60, "Cardio & strength for mental endurance")
            addBlock("Freshen Up & Breakfast", ScheduleCategory.BREAK, 25, "High-protein breakfast & shower")
        } else {
            addBlock("Freshen Up & Breakfast", ScheduleCategory.BREAK, 30, "Nutritious breakfast at home")
        }

        // 3. Morning Tuition (if 8:00 AM)
        if (params.tuitionShift == TuitionShift.MORNING_8AM) {
            addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, 15, "Morning commute")
            addBlock("Tuition Class (Morning Shift)", ScheduleCategory.TUITION, 60, "8:00 AM – 9:00 AM morning tuition session")
            addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Transition to distraction-free study room")
        } else {
            addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Transition to distraction-free study desk")
        }

        // 4. Costing Lecture 1 (Core Non-negotiable: 2h 18m = 138m)
        addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")

        // 5. Short Recharge Break
        addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")

        // 6. Morning Tuition (if 10:00 AM)
        if (params.tuitionShift == TuitionShift.MORNING_10AM) {
            addBlock("Tuition Class (Morning Shift)", ScheduleCategory.TUITION, 60, "10:00 AM – 11:00 AM tuition session")
            addBlock("Study Break & Resume", ScheduleCategory.BREAK, 10, "Transition back to Costing")
        }

        // 7. Costing Lecture 2 (Core Non-negotiable: 2h 18m = 138m)
        addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")

        // 8. Lunch & Midday Recharge
        addBlock("Travel: Study Room to Home", ScheduleCategory.TRAVEL, 15, "Commute home for lunch")
        addBlock("Lunch at Home & Relaxation", ScheduleCategory.LUNCH, 35, "Home-cooked lunch & mental reset")
        addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk for afternoon shift")

        // 9. Afternoon Tuition (if 3:00 PM)
        if (params.tuitionShift == TuitionShift.AFTERNOON_3PM) {
            addBlock("Tuition Class (Afternoon)", ScheduleCategory.TUITION, 60, "3:00 PM – 4:00 PM afternoon tuition session")
            addBlock("Short Transition Break", ScheduleCategory.BREAK, 10, "Transition to Auditing")
        }

        // 10. Audit Lecture 1 (Core Non-negotiable: 1h 30m = 90m)
        addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")

        // 11. Tea Break & Active Recall
        addBlock("Tea Break & Active Recall", ScheduleCategory.BREAK, 15, "15-minute concept recall over tea")

        // 12. Late Afternoon / Evening Slot: Tuition vs Gym vs Pre-Tuition Study
        if (params.tuitionShift == TuitionShift.EVENING_6PM) {
            addBlock("Audit Notes & Pre-Tuition Study", ScheduleCategory.STUDY_BLOCK, 50, "Consolidate lecture 1 concepts & revise formula sheet")
            addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, 15, "Commute to tuition class")
            addBlock("Tuition Class (MWF)", ScheduleCategory.TUITION, 60, "6:00 PM – 7:00 PM evening tuition session")
            addBlock("Travel from Tuition to Study Desk", ScheduleCategory.TRAVEL, 15, "Return to study desk")
        } else if (params.gymShift == GymShift.EVENING) {
            addBlock("Pre-Workout Concept Revision", ScheduleCategory.STUDY_BLOCK, 45, "Rapid revision of audit standards before workout")
            addBlock("Evening Gym & Running", ScheduleCategory.GYM, 60, "Strength training, running & cardio endurance")
            addBlock("Post-Workout Shower & Shake", ScheduleCategory.BREAK, 25, "Shower, protein shake & refresh")
        } else {
            addBlock("Self-Study & Audit Practice", ScheduleCategory.STUDY_BLOCK, 60, "Auditing Standards summary & MCQ practice")
        }

        // 13. Audit Lecture 2 (Core Non-negotiable: 1h 30m = 90m)
        addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")

        // 14. Night Workout (if chosen)
        if (params.gymShift == GymShift.NIGHT) {
            addBlock("Night Workout & Stretch", ScheduleCategory.GYM, 45, "Night workout routine")
            addBlock("Shower & Refresh", ScheduleCategory.BREAK, 15, "Cool down & freshen up")
        }

        // 15. Dinner
        addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 30, "Light dinner & relaxation")

        // 16. Dynamic Practical Practice Block (Calibrated to hit exact 11.0h - 12.0h target!)
        val currentStudyMins = items.filter { isStudyCategory(it.category) }.sumOf { it.durationMinutes }
        val neededPracticeMins = (targetStudyMinutes - currentStudyMins).coerceIn(60, 180)
        addBlock(
            "2-Hour Deep Focus Block (Practical Problems)",
            ScheduleCategory.STUDY_BLOCK,
            neededPracticeMins,
            "Costing practical problem solving, RTP & Audit case study questions"
        )

        // 17. Day Wrap-up
        addBlock("Day Wrap-up & Tomorrow Planning", ScheduleCategory.BREAK, 15, "Review completion dashboard & set tomorrow priorities")

        // 18. Sleep
        val sleepStartMins = currentMins
        val sleepDurationRaw = ((wakeMins - (sleepStartMins % 1440) + 1440) % 1440)
        val sleepDuration = if (sleepDurationRaw <= 0) 450 else sleepDurationRaw
        addBlock(
            "Sleep",
            ScheduleCategory.SLEEP,
            sleepDuration,
            "Deep restorative sleep for memory consolidation"
        )

        // Totals & Metrics
        val finalStudyMins = items.filter { isStudyCategory(it.category) }.sumOf { it.durationMinutes }
        val costingMins = items.filter { it.category == ScheduleCategory.COSTING.name }.sumOf { it.durationMinutes }
        val auditMins = items.filter { it.category == ScheduleCategory.AUDIT.name }.sumOf { it.durationMinutes }
        val tuitionMins = items.filter { it.category == ScheduleCategory.TUITION.name }.sumOf { it.durationMinutes }
        val practiceMins = items.filter { it.category == ScheduleCategory.STUDY_BLOCK.name }.sumOf { it.durationMinutes }

        val studyHoursDisp = String.format(Locale.US, "%dh %02dm", finalStudyMins / 60, finalStudyMins % 60)
        val sleepHours = sleepDuration / 60.0
        val sleepHoursDisp = String.format(Locale.US, "%.1fh (%s – %s)", sleepHours, minutesToTime(sleepStartMins), params.wakeTimeStr)

        val shiftNotes = buildString {
            append("Waking: ").append(params.wakeTimeStr)
            append(" • Gym: ").append(params.gymShift.displayName)
            append(" • Tuition: ").append(params.tuitionShift.displayName)
            append(" • Guaranteed Study: ").append(studyHoursDisp)
        }

        return OptimizationResult(
            items = items,
            totalStudyMinutes = finalStudyMins,
            costingMinutes = costingMins,
            auditMinutes = auditMins,
            tuitionMinutes = tuitionMins,
            practiceMinutes = practiceMins,
            totalStudyHoursDisplay = studyHoursDisp,
            sleepHoursDisplay = sleepHoursDisp,
            summaryNotes = shiftNotes
        )
    }

    private fun isStudyCategory(categoryName: String): Boolean {
        return categoryName == ScheduleCategory.COSTING.name ||
                categoryName == ScheduleCategory.AUDIT.name ||
                categoryName == ScheduleCategory.TUITION.name ||
                categoryName == ScheduleCategory.STUDY_BLOCK.name
    }

    fun timeToMinutes(timeStr: String): Int {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = if (parts.size > 1) parts[1].toInt() else 0
            (h * 60 + m) % 1440
        } catch (_: Exception) {
            420 // 07:00 fallback
        }
    }

    fun minutesToTime(minutes: Int): String {
        val normalized = ((minutes % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }
}
