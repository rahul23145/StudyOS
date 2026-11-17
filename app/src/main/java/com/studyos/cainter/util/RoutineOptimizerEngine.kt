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

    data class CustomOptimizationParams(
        val dayOfWeek: Int,
        val scheduleTypeId: Long = 1L,
        val wakeTimeStr: String = "07:00",
        val hasGym: Boolean = true,
        val gymStartTimeStr: String = "07:15",
        val gymDurationMinutes: Int = 60,
        val hasTuition: Boolean = true,
        val tuitionStartTimeStr: String = "18:00",
        val tuitionDurationMinutes: Int = 60,
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
        val summaryNotes: String,
        val timelinePreview: String
    )

    fun optimize(params: CustomOptimizationParams): OptimizationResult {
        val targetStudyMinutes = (params.targetStudyHours * 60).toInt().coerceIn(600, 780)
        val wakeMins = timeToMinutes(params.wakeTimeStr)
        val hasGym = params.hasGym
        val gymStartMins = timeToMinutes(params.gymStartTimeStr)
        val gymDur = params.gymDurationMinutes.coerceIn(30, 120)
        val hasTuition = params.hasTuition
        val tuitionStartMins = timeToMinutes(params.tuitionStartTimeStr)
        val tuitionDur = params.tuitionDurationMinutes.coerceIn(30, 150)

        val items = mutableListOf<ScheduleItem>()
        var currentMins = wakeMins
        var order = 1

        fun addBlock(
            title: String,
            category: ScheduleCategory,
            duration: Int,
            notes: String
        ) {
            if (duration <= 0) return
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

        // 2. Morning Gym (if Gym is before 12:00 PM)
        if (hasGym && gymStartMins < 720) {
            if (gymStartMins > currentMins) {
                val prepDuration = gymStartMins - currentMins
                addBlock("Freshen Up & Pre-Workout Snack", ScheduleCategory.BREAK, prepDuration, "Light snack, pre-workout hydration")
            }
            currentMins = gymStartMins
            addBlock("Morning Gym / Workout", ScheduleCategory.GYM, gymDur, "Cardio & strength for mental endurance")
            addBlock("Post-Workout Shower & Breakfast", ScheduleCategory.BREAK, 25, "High-protein breakfast & shower")
            addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Transition to distraction-free study desk")
        } else {
            addBlock("Freshen Up & Breakfast", ScheduleCategory.BREAK, 30, "Nutritious breakfast at home")
            addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Transition to distraction-free study desk")
        }

        // 3. Morning Tuition check (< 12:00 PM)
        if (hasTuition && tuitionStartMins < 720) {
            if (tuitionStartMins <= currentMins + 40) {
                if (tuitionStartMins > currentMins) {
                    addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, tuitionStartMins - currentMins, "Commute to tuition class")
                }
                currentMins = tuitionStartMins
                addBlock("Tuition Class (Morning Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} morning tuition session")
                addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Transition back to study room")
                addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")
                addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
                addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")
            } else {
                addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")
                addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
                if (tuitionStartMins > currentMins) {
                    val gap = tuitionStartMins - currentMins
                    if (gap >= 20) {
                        addBlock("Pre-Tuition Concept Revision", ScheduleCategory.STUDY_BLOCK, gap, "Rapid review of concepts")
                    }
                }
                currentMins = tuitionStartMins
                addBlock("Tuition Class (Morning Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} tuition session")
                addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Transition back to study desk")
                addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")
            }
        } else {
            addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")
            addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
            addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")
        }

        // 4. Midday Lunch
        addBlock("Travel: Study Room to Home", ScheduleCategory.TRAVEL, 15, "Commute home for lunch")
        addBlock("Lunch at Home & Relaxation", ScheduleCategory.LUNCH, 35, "Home-cooked lunch & mental reset")
        addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk for afternoon shift")

        // 5. Afternoon Tuition (12:00 PM to 5:00 PM) or Audit Lecture 1
        if (hasTuition && tuitionStartMins in 720..1020) {
            if (tuitionStartMins >= currentMins + 90) {
                addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")
                if (tuitionStartMins > currentMins) {
                    addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, (tuitionStartMins - currentMins).coerceIn(10, 30), "Commute to tuition")
                }
                currentMins = tuitionStartMins
                addBlock("Tuition Class (Afternoon Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} afternoon tuition session")
                addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk")
            } else {
                if (tuitionStartMins > currentMins) {
                    addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, tuitionStartMins - currentMins, "Commute to tuition")
                }
                currentMins = tuitionStartMins
                addBlock("Tuition Class (Afternoon Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} afternoon tuition session")
                addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk")
                addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")
            }
        } else {
            addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")
            addBlock("Tea Break & Active Recall", ScheduleCategory.BREAK, 15, "15-minute concept recall over tea")
        }

        // 6. Evening Slot: Tuition vs Gym vs Notes
        if (hasTuition && tuitionStartMins > 1020) {
            val travelTime = 15
            val targetPreTuition = tuitionStartMins - travelTime
            if (targetPreTuition > currentMins) {
                val studyGap = targetPreTuition - currentMins
                if (studyGap >= 15) {
                    addBlock("Audit Notes & Pre-Tuition Study", ScheduleCategory.STUDY_BLOCK, studyGap, "Consolidate lecture 1 concepts & revise formula sheet")
                }
            }
            addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, 15, "Commute to tuition class")
            currentMins = tuitionStartMins
            addBlock("Tuition Class (Evening Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} evening tuition session")
            addBlock("Travel from Tuition to Study Desk", ScheduleCategory.TRAVEL, 15, "Return to study desk")

            if (hasGym && gymStartMins in 1140..1260) {
                addBlock("Evening Gym & Running", ScheduleCategory.GYM, gymDur, "Strength training, running & cardio endurance")
                addBlock("Post-Workout Shower & Shake", ScheduleCategory.BREAK, 20, "Shower & refresh")
            }
            addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")
        } else if (hasGym && gymStartMins in 960..1230) {
            val targetPreGym = gymStartMins - 10
            if (targetPreGym > currentMins) {
                val studyGap = targetPreGym - currentMins
                if (studyGap >= 15) {
                    addBlock("Pre-Workout Concept Revision", ScheduleCategory.STUDY_BLOCK, studyGap, "Rapid revision of audit standards before workout")
                }
            }
            currentMins = gymStartMins
            addBlock("Evening Gym & Running", ScheduleCategory.GYM, gymDur, "Strength training, running & cardio endurance")
            addBlock("Post-Workout Shower & Shake", ScheduleCategory.BREAK, 25, "Shower, protein shake & refresh")
            addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")
        } else {
            addBlock("Self-Study & Audit Practice", ScheduleCategory.STUDY_BLOCK, 45, "Auditing Standards summary & MCQ practice")
            addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")
        }

        // 7. Night Workout or Dinner
        if (hasGym && gymStartMins > 1230) {
            addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 30, "Light dinner & relaxation")
            currentMins = gymStartMins
            addBlock("Night Workout & Stretch", ScheduleCategory.GYM, gymDur, "Night workout routine")
            addBlock("Shower & Refresh", ScheduleCategory.BREAK, 15, "Cool down & freshen up")
        } else {
            addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 30, "Light dinner & relaxation")
        }

        // 8. Dynamic Practical Problem Solving Block (Guarantees targetStudyMinutes: 11h - 12h)
        val currentStudyMins = items.filter { isStudyCategory(it.category) }.sumOf { it.durationMinutes }
        val neededPracticeMins = (targetStudyMinutes - currentStudyMins).coerceIn(45, 210)
        addBlock(
            "2-Hour Deep Focus Block (Practical Problems)",
            ScheduleCategory.STUDY_BLOCK,
            neededPracticeMins,
            "Costing practical problem solving, ICAI RTP & Audit case study questions"
        )

        // 9. Day Wrap-up & Sleep
        addBlock("Day Wrap-up & Tomorrow Planning", ScheduleCategory.BREAK, 15, "Review completion dashboard & set tomorrow priorities")

        val sleepStartMins = currentMins
        val sleepDurationRaw = ((wakeMins - (sleepStartMins % 1440) + 1440) % 1440)
        val sleepDuration = if (sleepDurationRaw <= 0) 450 else sleepDurationRaw
        addBlock(
            "Sleep",
            ScheduleCategory.SLEEP,
            sleepDuration,
            "Deep restorative sleep for memory consolidation"
        )

        // Metrics & Totals
        val finalStudyMins = items.filter { isStudyCategory(it.category) }.sumOf { it.durationMinutes }
        val costingMins = items.filter { it.category == ScheduleCategory.COSTING.name }.sumOf { it.durationMinutes }
        val auditMins = items.filter { it.category == ScheduleCategory.AUDIT.name }.sumOf { it.durationMinutes }
        val tuitionMins = items.filter { it.category == ScheduleCategory.TUITION.name }.sumOf { it.durationMinutes }
        val practiceMins = items.filter { it.category == ScheduleCategory.STUDY_BLOCK.name }.sumOf { it.durationMinutes }

        val studyHoursDisp = String.format(Locale.US, "%dh %02dm", finalStudyMins / 60, finalStudyMins % 60)
        val sleepHours = sleepDuration / 60.0
        val sleepHoursDisp = String.format(Locale.US, "%.1fh (%s – %s)", sleepHours, formatTo12Hour(minutesToTime(sleepStartMins)), formatTo12Hour(params.wakeTimeStr))

        val shiftNotes = buildString {
            append("Waking: ").append(formatTo12Hour(params.wakeTimeStr))
            if (hasGym) {
                append(" • Gym: ").append(formatTo12Hour(params.gymStartTimeStr)).append(" (").append(gymDur).append("m)")
            } else {
                append(" • Gym: Rest Day")
            }
            if (hasTuition) {
                append(" • Tuition: ").append(formatTo12Hour(params.tuitionStartTimeStr)).append(" (").append(tuitionDur).append("m)")
            } else {
                append(" • Tuition: None")
            }
            append(" • Study: ").append(studyHoursDisp)
        }

        val timelinePreview = buildString {
            append("Wake (").append(formatTo12Hour(params.wakeTimeStr)).append(")")
            if (hasGym && gymStartMins < 720) {
                append(" ➔ Gym (").append(formatTo12Hour(params.gymStartTimeStr)).append(")")
            }
            append(" ➔ Costing 1 ➔ Costing 2 ➔ Lunch ➔ Audit 1")
            if (hasTuition && tuitionStartMins in 720..1020) {
                append(" ➔ Tuition (").append(formatTo12Hour(params.tuitionStartTimeStr)).append(")")
            } else if (hasTuition && tuitionStartMins > 1020) {
                append(" ➔ Tuition (").append(formatTo12Hour(params.tuitionStartTimeStr)).append(")")
            } else if (hasGym && gymStartMins in 960..1230) {
                append(" ➔ Gym (").append(formatTo12Hour(params.gymStartTimeStr)).append(")")
            }
            append(" ➔ Audit 2 ➔ Practice ➔ Sleep (").append(formatTo12Hour(minutesToTime(sleepStartMins))).append(")")
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
            summaryNotes = shiftNotes,
            timelinePreview = timelinePreview
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

    fun formatTo12Hour(timeStr: String): String {
        val totalMins = timeToMinutes(timeStr)
        val h = totalMins / 60
        val m = totalMins % 60
        val amPm = if (h < 12) "AM" else "PM"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%02d:%02d %s", h12, m, amPm)
    }
}
