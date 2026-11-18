package com.studyos.cainter.util

import com.studyos.cainter.data.local.entity.ScheduleCategory
import com.studyos.cainter.data.local.entity.ScheduleItem
import java.util.Locale

object RoutineOptimizerEngine {

    enum class GymShift(val displayName: String) {
        POST_AUDIT("Post-Audit Gym (10:30 AM)"),
        MORNING("Early Morning Gym (07:00 AM)"),
        EVENING("Evening Gym (06:00 PM)"),
        NIGHT("Night Workout (08:30 PM)"),
        NONE("Rest Day / Skip Gym")
    }

    enum class TuitionShift(val displayName: String, val minutes: Int) {
        EVENING_6PM("Evening 6:00–7:00 PM (MWF)", 60),
        EVENING_615PM("Evening 6:15–7:15 PM", 60),
        MORNING_11AM("Mid-Day 11:00 AM–12:00 PM", 60),
        MORNING_8AM("Early Morning 7:30–8:30 AM", 60),
        AFTERNOON_3PM("Afternoon 3:00–4:00 PM", 60),
        NONE("No Tuition Today", 0)
    }

    data class CustomOptimizationParams(
        val dayOfWeek: Int,
        val scheduleTypeId: Long = 1L,
        val wakeTimeStr: String = "06:45",
        val hasGym: Boolean = true,
        val gymStartTimeStr: String = "10:30",
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

        // 2. Morning Period before Live Audit (Live Audit is locked at 07:45 AM = 465 mins)
        val auditLiveStart = 465 // 07:45 AM
        val auditDuration = 150  // 2h 30m (07:45 to 10:15 AM)

        // Check if user specifically requested Early Morning Gym (before Audit, < 7:45 AM)
        if (hasGym && gymStartMins < auditLiveStart) {
            if (gymStartMins > currentMins) {
                val prepDuration = gymStartMins - currentMins
                addBlock("Freshen Up & Pre-Workout Snack", ScheduleCategory.BREAK, prepDuration, "Light snack, pre-workout hydration")
                currentMins = gymStartMins
            }
            addBlock("Morning Gym / Workout", ScheduleCategory.GYM, gymDur, "Strength & cardio conditioning")
            val availableBeforeAudit = (auditLiveStart - 10) - currentMins
            if (availableBeforeAudit > 0) {
                addBlock("Post-Workout Shower & Breakfast", ScheduleCategory.BREAK, availableBeforeAudit, "Shower & high-protein breakfast")
            }
            addBlock("Travel / Settle at Study Desk", ScheduleCategory.TRAVEL, 10, "Transition & prepare notes for live session")
            currentMins = auditLiveStart
        } else {
            // Standard: Breakfast before Live Audit
            val deskSetupTime = 10
            val availableForBreakfast = (auditLiveStart - deskSetupTime) - currentMins
            if (availableForBreakfast > 0) {
                addBlock("Freshen Up & Breakfast", ScheduleCategory.BREAK, availableForBreakfast, "High-protein breakfast at home before Live Audit")
            }
            addBlock("Travel / Settle at Study Desk", ScheduleCategory.TRAVEL, deskSetupTime, "Transition & prepare notes for live session")
            currentMins = auditLiveStart
        }

        // 3. Live Audit Lecture (07:45 AM – 10:15 AM)
        addBlock(
            "Live Audit Lecture (2h 30m)",
            ScheduleCategory.AUDIT,
            auditDuration,
            "CA Inter Paper 5 - Auditing & Ethics LIVE Batch (07:45–10:15 AM)"
        )
        currentMins = 615 // 10:15 AM

        // Check if user has morning/mid-day tuition (e.g. 11:00 AM, tuitionStartMins between 10:15 AM and 1:00 PM)
        val hasMidDayTuition = hasTuition && tuitionStartMins in 615..780

        if (hasMidDayTuition) {
            // Mid-day tuition shift (e.g. 11:00 AM)
            if (tuitionStartMins > currentMins) {
                val gap = tuitionStartMins - currentMins
                if (gap > 15) {
                    addBlock("Post-Audit Refresh & Hydrate", ScheduleCategory.BREAK, gap - 15, "Rest eyes & hydrate after live audit")
                    addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, 15, "Commute to tuition class")
                } else {
                    addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, gap, "Commute to tuition class")
                }
                currentMins = tuitionStartMins
            }
            addBlock("Tuition Class", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} tuition session")
            currentMins += tuitionDur

            // Lunch & travel back to study room
            addBlock("Travel & Lunch at Home", ScheduleCategory.LUNCH, 45, "Nutritious lunch & recovery")
            addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "Transition to distraction-free study room")
            currentMins = currentMins.coerceAtLeast(780) // 13:00 (1:00 PM)
        } else {
            // 4. Post-Audit Mid-Day Shift (10:15 AM – 01:00 PM): Gym + Lunch
            if (hasGym && gymStartMins in 600..750) {
                if (gymStartMins > currentMins) {
                    val gap = gymStartMins - currentMins
                    addBlock("Post-Audit Hydrate & Gym Gear", ScheduleCategory.BREAK, gap, "Hydrate and transition to gym workout")
                    currentMins = gymStartMins
                }
                addBlock("Morning Gym", ScheduleCategory.GYM, gymDur, "Strength & cardio endurance session")
                addBlock("Post-Workout Shower & Refresh", ScheduleCategory.BREAK, 30, "Shower & protein refreshment")
                addBlock("Lunch at Home & Relaxation", ScheduleCategory.LUNCH, 45, "Home-cooked lunch & mental reset")
                addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "15-minute transition to distraction-free study room")
                currentMins = 780 // 13:00 (1:00 PM)
            } else {
                // No gym or gym scheduled for evening/night
                addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 15, "Hydrate, rest eyes & stretch")
                addBlock("Audit Notes & Concept Consolidation", ScheduleCategory.STUDY_BLOCK, 75, "Consolidate live lecture concepts and memory codes")
                addBlock("Lunch at Home & Relaxation", ScheduleCategory.LUNCH, 45, "Home-cooked lunch & short relaxation")
                addBlock("Travel: Home to Study Room", ScheduleCategory.TRAVEL, 15, "15-minute transition to distraction-free study room")
                currentMins = 780 // 13:00 (1:00 PM)
            }
        }

        // 5. Afternoon Recorded Costing Lectures (1:00 PM onwards)
        addBlock(
            "Costing Lecture 1 (2h 18m)",
            ScheduleCategory.COSTING,
            138,
            "CA Inter Paper 4 - Module lecture with formulas & solved problems (recorded)"
        )
        addBlock("Study Room Recharge Break", ScheduleCategory.BREAK, 12, "Stretch, hydrate, rest eyes")
        addBlock(
            "Costing Lecture 2 (2h 18m)",
            ScheduleCategory.COSTING,
            138,
            "CA Inter Paper 4 - Daily lecture 2 & module illustrations (recorded)"
        )
        // currentMins is now 780 + 138 + 12 + 138 = 1068 (05:48 PM)

        // 6. Evening Shift: Tuition vs Running (TTSS) vs Evening Gym
        if (hasTuition && tuitionStartMins >= 1020) { // Evening Tuition (e.g. 6:00 PM MWF)
            if (tuitionStartMins > currentMins) {
                val gap = tuitionStartMins - currentMins
                addBlock("Travel to Tuition", ScheduleCategory.TRAVEL, gap, "Commute to tuition class")
                currentMins = tuitionStartMins
            }
            addBlock("Tuition Class (MWF)", ScheduleCategory.TUITION, tuitionDur, "${formatTo12Hour(params.tuitionStartTimeStr)} tuition session")
            addBlock("Travel from Tuition to Home", ScheduleCategory.TRAVEL, 15, "Return home for dinner")
            addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 30, "Nutritious dinner & mental reset")
            currentMins = 1185 // 19:45 (7:45 PM)
        } else if (isTtssDay && !hasTuition) {
            // TTSS Routine: Running 6:15–7:00 PM + Dinner
            val targetRunningStart = 1095 // 18:15 (6:15 PM)
            if (targetRunningStart > currentMins) {
                val gap = targetRunningStart - currentMins
                addBlock("Travel Home for Running", ScheduleCategory.TRAVEL, gap, "Change into running gear")
                currentMins = targetRunningStart
            }
            addBlock("TTSS Running Session", ScheduleCategory.RUNNING, 45, "Outdoor endurance run & cool-down (Tue/Thu/Sat/Sun routine)")
            addBlock("Freshen Up & Dinner at Home", ScheduleCategory.LUNCH, 45, "Dinner, hydration & post-run shower")
            currentMins = 1185 // 19:45 (7:45 PM)
        } else if (hasGym && gymStartMins >= 1020) {
            // Evening Gym
            if (gymStartMins > currentMins) {
                val gap = gymStartMins - currentMins
                addBlock("Travel to Gym", ScheduleCategory.TRAVEL, gap, "Transition to workout")
                currentMins = gymStartMins
            }
            addBlock("Evening Gym & Workout", ScheduleCategory.GYM, gymDur, "Strength training & cardio endurance")
            addBlock("Post-Workout Shower & Refresh", ScheduleCategory.BREAK, 25, "Shower, protein shake & refresh")
            addBlock("Dinner & Refreshment", ScheduleCategory.LUNCH, 35, "Nutritious dinner & relaxation")
            currentMins = 1185 // 19:45 (7:45 PM)
        } else {
            // Standard dinner / evening break
            addBlock("Evening Walk & Refreshment", ScheduleCategory.BREAK, 35, "Evening walk and relaxation")
            addBlock("Dinner at Home", ScheduleCategory.LUNCH, 40, "Nutritious dinner & relaxation")
            currentMins = 1185 // 19:45 (7:45 PM)
        }

        // 7. Dynamic Deep Focus Practical Problem Solving Block (guarantees ~11h to 12h study)
        val currentStudyMins = items.filter { isStudyCategory(it.category) }.sumOf { it.durationMinutes }
        val neededPracticeMins = (targetStudyMinutes - currentStudyMins).coerceIn(150, 240)

        val hours = neededPracticeMins / 60
        val mins = neededPracticeMins % 60
        val durationLabel = if (hours > 0 && mins > 0) "${hours}h ${mins}m" else if (hours > 0) "${hours}-Hour" else "${mins}m"
        val practiceTitle = "$durationLabel Deep Focus Block (Practical Problems & Audit Review)"

        addBlock(
            practiceTitle,
            ScheduleCategory.STUDY_BLOCK,
            neededPracticeMins,
            "Costing practical problem solving, ICAI RTP/MTP & Audit concept revision"
        )

        // 8. Day Wrap-up & Tomorrow Planning
        addBlock("Day Wrap-up & Tomorrow Planning", ScheduleCategory.BREAK, 15, "Review completion dashboard & set tomorrow priorities")

        // 9. Sleep (7 to 7.5 hours recovery)
        val sleepStartMins = currentMins
        val sleepDurationRaw = ((wakeMins - (sleepStartMins % 1440) + 1440) % 1440)
        val sleepDuration = if (sleepDurationRaw <= 0) 435 else sleepDurationRaw
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
            append(" • Live Audit: 7:45–10:15 AM")
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
            append(" ➔ Breakfast ➔ Live Audit (07:45 AM)")
            if (hasGym && gymStartMins in 600..750) {
                append(" ➔ Gym (").append(formatTo12Hour(params.gymStartTimeStr)).append(")")
            }
            append(" ➔ Lunch ➔ Costing 1 (1:00 PM) ➔ Costing 2 (3:30 PM)")
            if (hasTuition) {
                append(" ➔ Tuition (").append(formatTo12Hour(params.tuitionStartTimeStr)).append(")")
            } else if (isTtssDay) {
                append(" ➔ Running (06:15 PM)")
            }
            append(" ➔ Deep Focus Practice ➔ Sleep (").append(formatTo12Hour(minutesToTime(sleepStartMins))).append(")")
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
            450 // 07:30 fallback
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
