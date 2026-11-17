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
        MORNING_11AM("Morning 11:00 AM–12:00 PM", 60),
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
        val gymDur = params.gymDurationMinutes.coerceIn(30, 90)
        val hasTuition = params.hasTuition
        val tuitionStartMins = timeToMinutes(params.tuitionStartTimeStr)
        val tuitionDur = params.tuitionDurationMinutes.coerceIn(30, 120)
        val isTtssDay = params.dayOfWeek in listOf(2, 4, 6, 7) // Tue, Thu, Sat, Sun

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

        // 2. Morning Gym (before 12:00 PM)
        if (hasGym && gymStartMins < 720) {
            if (gymStartMins > currentMins) {
                val prepDuration = gymStartMins - currentMins
                addBlock("Freshen Up & Pre-Workout Snack", ScheduleCategory.BREAK, prepDuration, "Light snack, pre-workout hydration")
                currentMins = gymStartMins
            }
            addBlock("Morning Gym / Workout", ScheduleCategory.GYM, gymDur, "Strength & cardio conditioning")
            addBlock("Post-Workout Shower & Breakfast", ScheduleCategory.BREAK, 25, "High-protein breakfast & shower")
            addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Transition to distraction-free study desk")
        } else {
            addBlock("Freshen Up & Breakfast", ScheduleCategory.BREAK, 30, "Nutritious breakfast at home")
            addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Transition to distraction-free study desk")
        }

        // 3. Morning Schedule Construction
        var auditLecture1Completed = false

        if (hasTuition && tuitionStartMins < 720) { // Morning Tuition (< 12:00 PM)
            val availableMinsBeforeTuition = (tuitionStartMins - 15) - currentMins

            if (availableMinsBeforeTuition >= 138) {
                // Costing 1 (138m) fits before tuition!
                addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")
                addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
                if (tuitionStartMins - 15 > currentMins) {
                    val gap = (tuitionStartMins - 15) - currentMins
                    if (gap >= 15) {
                        addBlock("Formula & Notes Revision", ScheduleCategory.STUDY_BLOCK, gap, "Quick formula revision")
                    }
                }
                addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, 15, "Commute to tuition class")
                currentMins = tuitionStartMins
                addBlock("Tuition Class (Morning Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} tuition session")
                addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk")
                addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")
            } else if (availableMinsBeforeTuition >= 90) {
                // Audit 1 (90m) fits before tuition! (e.g. Tuition at 11:00 AM)
                addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")
                auditLecture1Completed = true
                if (tuitionStartMins - 15 > currentMins) {
                    val gap = (tuitionStartMins - 15) - currentMins
                    if (gap >= 15) {
                        addBlock("Audit Concept Recall & Revision", ScheduleCategory.STUDY_BLOCK, gap, "Active concept recall before tuition")
                    }
                }
                addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, 15, "Commute to tuition class")
                currentMins = tuitionStartMins
                addBlock("Tuition Class (Morning Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} tuition session")
                addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk")
                addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")
            } else {
                // Early Morning Tuition (e.g. 8:00 AM)
                if (tuitionStartMins - 15 > currentMins) {
                    addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, (tuitionStartMins - currentMins).coerceIn(10, 20), "Commute to tuition")
                } else if (tuitionStartMins > currentMins) {
                    addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, tuitionStartMins - currentMins, "Commute to tuition")
                }
                currentMins = tuitionStartMins
                addBlock("Tuition Class (Morning Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} tuition session")
                addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Transition back to study room")
                addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")
                addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
                addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")
            }
        } else {
            // Standard Morning Lectures (Costing 1 + Costing 2)
            addBlock("Costing Lecture 1 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Module lecture with formulas & solved problems")
            addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
            addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")
        }

        // 4. Midday Lunch
        addBlock("Travel: Study Room to Home", ScheduleCategory.TRAVEL, 15, "Commute home for lunch")
        addBlock("Lunch at Home & Relaxation", ScheduleCategory.LUNCH, 35, "Home-cooked lunch & mental reset")
        addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk for afternoon shift")

        // 5. Afternoon Shift: Costing 2 (if deferred due to mid-morning tuition) or Audit 1
        if (items.none { it.title.contains("Costing Lecture 2") }) {
            addBlock("Costing Lecture 2 (2h 18m)", ScheduleCategory.COSTING, 138, "CA Inter Paper 4 - Daily lecture 2 & module illustrations")
            addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
        }

        if (!auditLecture1Completed) {
            if (hasTuition && tuitionStartMins in 720..1020) { // Afternoon tuition (12:00 PM – 5:00 PM)
                if (tuitionStartMins >= currentMins + 90) {
                    addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")
                    auditLecture1Completed = true
                    if (tuitionStartMins > currentMins) {
                        addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, (tuitionStartMins - currentMins).coerceIn(10, 30), "Commute to tuition")
                        currentMins = tuitionStartMins
                    }
                    addBlock("Tuition Class (Afternoon Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} afternoon tuition session")
                    addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk")
                } else {
                    if (tuitionStartMins > currentMins) {
                        addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, tuitionStartMins - currentMins, "Commute to tuition")
                        currentMins = tuitionStartMins
                    }
                    addBlock("Tuition Class (Afternoon Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} afternoon tuition session")
                    addBlock("Travel: Tuition to Study Room", ScheduleCategory.TRAVEL, 15, "Return to study desk")
                    addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")
                    auditLecture1Completed = true
                }
            } else {
                addBlock("Audit Lecture 1 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Auditing & Ethics Standards deep dive")
                auditLecture1Completed = true
                addBlock("Tea Break & Active Recall", ScheduleCategory.BREAK, 15, "15-minute concept recall over tea")
            }
        }

        // 6. Evening Shift: Tuition vs Running (TTSS) vs Evening Gym
        if (hasTuition && tuitionStartMins > 1020) { // Evening Tuition (e.g. 6:00 PM MWF)
            val travelTime = 15
            val targetPreTuition = tuitionStartMins - travelTime
            if (targetPreTuition > currentMins) {
                val studyGap = targetPreTuition - currentMins
                if (studyGap >= 15) {
                    addBlock("Audit Notes & Pre-Tuition Study", ScheduleCategory.STUDY_BLOCK, studyGap, "Consolidate lecture 1 concepts & revise formula sheet")
                }
            }
            if (tuitionStartMins > currentMins) {
                addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, 15, "Commute to tuition class")
                currentMins = tuitionStartMins
            }
            addBlock("Tuition Class (Evening Shift)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} evening tuition session")
            addBlock("Travel from Tuition to Study Desk", ScheduleCategory.TRAVEL, 15, "Return to study desk")

            addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")
            addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 30, "Light dinner & relaxation")
        } else if (isTtssDay) {
            // TTSS Routine: Running 6:30–7:15 PM + Audit 2
            val targetRunningStart = 1110 // 18:30 (6:30 PM)
            if (targetRunningStart > currentMins) {
                val gap = targetRunningStart - currentMins
                if (gap >= 20) {
                    addBlock("Audit Notes & Formula Sheet Revision", ScheduleCategory.STUDY_BLOCK, gap, "Active recall & formula revision")
                }
                currentMins = targetRunningStart
            }
            addBlock("TTSS Running Session", ScheduleCategory.RUNNING, 45, "Outdoor endurance run & cool-down (Tue/Thu/Sat/Sun routine)")
            addBlock("Freshen Up & Dinner at Home", ScheduleCategory.LUNCH, 45, "Post-run shower, hydration & nutritious dinner")
            addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")
        } else if (hasGym && gymStartMins in 960..1230) {
            // Evening Gym on non-running days
            addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")
            if (gymStartMins > currentMins) {
                val gap = gymStartMins - currentMins
                if (gap >= 15) {
                    addBlock("Self-Study & Formula Revision", ScheduleCategory.STUDY_BLOCK, gap, "Quick formula revision")
                }
                currentMins = gymStartMins
            }
            addBlock("Evening Gym & Running", ScheduleCategory.GYM, gymDur, "Strength training, running & cardio endurance")
            addBlock("Post-Workout Shower & Shake", ScheduleCategory.BREAK, 25, "Shower, protein shake & refresh")
            addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 30, "Light dinner & relaxation")
        } else {
            // Standard self-study evening
            addBlock("Self-Study & Audit Practice", ScheduleCategory.STUDY_BLOCK, 45, "Auditing Standards summary & MCQ practice")
            addBlock("Audit Lecture 2 (1h 30m)", ScheduleCategory.AUDIT, 90, "CA Inter Paper 5 - Audit lecture 2 & case studies")
            addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 30, "Light dinner & relaxation")
        }

        // 7. Dynamic Deep Focus Practical Problem Solving Block (guarantees ~11h to 12h study)
        val currentStudyMins = items.filter { isStudyCategory(it.category) }.sumOf { it.durationMinutes }
        val neededPracticeMins = (targetStudyMinutes - currentStudyMins).coerceIn(45, 180)

        val hours = neededPracticeMins / 60
        val mins = neededPracticeMins % 60
        val durationLabel = if (hours > 0 && mins > 0) "${hours}h ${mins}m" else if (hours > 0) "${hours}-Hour" else "${mins}m"
        val practiceTitle = "$durationLabel Deep Focus Block (Practical Problems & RTP)"

        addBlock(
            practiceTitle,
            ScheduleCategory.STUDY_BLOCK,
            neededPracticeMins,
            "Costing practical problem solving, ICAI RTP & Audit case study questions"
        )

        // 8. Day Wrap-up & Tomorrow Planning
        addBlock("Day Wrap-up & Tomorrow Planning", ScheduleCategory.BREAK, 15, "Review completion dashboard & set tomorrow priorities")

        // 9. Sleep (7.5 to 8 hours recovery)
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
            if (hasTuition && tuitionStartMins < 720) {
                append(" ➔ Tuition (").append(formatTo12Hour(params.tuitionStartTimeStr)).append(")")
            }
            append(" ➔ Costing 1 ➔ Costing 2 ➔ Lunch ➔ Audit 1")
            if (hasTuition && tuitionStartMins in 720..1020) {
                append(" ➔ Tuition (").append(formatTo12Hour(params.tuitionStartTimeStr)).append(")")
            } else if (hasTuition && tuitionStartMins > 1020) {
                append(" ➔ Tuition (").append(formatTo12Hour(params.tuitionStartTimeStr)).append(")")
            }
            if (isTtssDay) {
                append(" ➔ Running (06:30 PM)")
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
