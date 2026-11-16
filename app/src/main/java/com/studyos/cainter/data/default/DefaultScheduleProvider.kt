package com.studyos.cainter.data.default

import com.studyos.cainter.data.local.entity.LectureTracker
import com.studyos.cainter.data.local.entity.ScheduleCategory
import com.studyos.cainter.data.local.entity.ScheduleItem

object DefaultScheduleProvider {

    fun getDefaultSchedule(): List<ScheduleItem> {
        val items = mutableListOf<ScheduleItem>()

        // Days 1 (Mon) to 7 (Sun)
        for (day in 1..7) {
            when (day) {
                // Monday (1), Wednesday (3), Friday (5): MWF with Tuition 6-7 PM
                1, 3, 5 -> items.addAll(createMwfSchedule(day))

                // Tuesday (2), Thursday (4), Saturday (6): TTS with TTSS Running
                2, 4, 6 -> items.addAll(createTtsSchedule(day))

                // Sunday (7): Audit-only intensive day with TTSS Running
                7 -> items.addAll(createSundaySchedule(day))
            }
        }
        return items
    }

    private fun createMwfSchedule(day: Int): List<ScheduleItem> {
        var order = 1
        return listOf(
            ScheduleItem(
                dayOfWeek = day,
                title = "Wake Up & Hydrate",
                category = ScheduleCategory.WAKE.name,
                startTime = "07:00",
                endTime = "07:15",
                durationMinutes = 15,
                notes = "Consistent wake-up time for circadian rhythm",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Morning Gym",
                category = ScheduleCategory.GYM.name,
                startTime = "07:15",
                endTime = "08:15",
                durationMinutes = 60,
                notes = "Strength & cardio for mental endurance",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Freshen Up & Breakfast",
                category = ScheduleCategory.BREAK.name,
                startTime = "08:15",
                endTime = "08:40",
                durationMinutes = 25,
                notes = "High-protein breakfast at home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Home to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "08:40",
                endTime = "09:00",
                durationMinutes = 20,
                notes = "20-minute transition to distraction-free study room",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Costing Lecture 1 (2h 18m)",
                category = ScheduleCategory.COSTING.name,
                startTime = "09:00",
                endTime = "11:18",
                durationMinutes = 138,
                notes = "CA Inter Paper 4 - Module lecture with formulas & solved problems",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Study Room Break",
                category = ScheduleCategory.BREAK.name,
                startTime = "11:18",
                endTime = "11:30",
                durationMinutes = 12,
                notes = "Stretch, hydrate, rest eyes",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Costing Lecture 2 (2h 18m)",
                category = ScheduleCategory.COSTING.name,
                startTime = "11:30",
                endTime = "13:48",
                durationMinutes = 138,
                notes = "CA Inter Paper 4 - Daily lecture 2",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Study Room to Home",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "13:48",
                endTime = "14:08",
                durationMinutes = 20,
                notes = "20-minute travel home for lunch",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Lunch at Home & Rest",
                category = ScheduleCategory.LUNCH.name,
                startTime = "14:08",
                endTime = "14:40",
                durationMinutes = 32,
                notes = "Home-cooked lunch & short relaxation",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Home to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "14:40",
                endTime = "15:00",
                durationMinutes = 20,
                notes = "20-minute travel to study room for afternoon shift",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Audit Lecture 1 (1h 30m)",
                category = ScheduleCategory.AUDIT.name,
                startTime = "15:00",
                endTime = "16:30",
                durationMinutes = 90,
                notes = "CA Inter Paper 5 - Auditing & Ethics Standards",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Tea Break & Active Recall",
                category = ScheduleCategory.BREAK.name,
                startTime = "16:30",
                endTime = "16:45",
                durationMinutes = 15,
                notes = "15-minute break with concept recall",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Audit Notes & Pre-Tuition Study",
                category = ScheduleCategory.STUDY_BLOCK.name,
                startTime = "16:45",
                endTime = "17:40",
                durationMinutes = 55,
                notes = "Consolidate lecture 1 concepts",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel to Tuition",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "17:40",
                endTime = "18:00",
                durationMinutes = 20,
                notes = "Commute to tuition class",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Tuition Class (MWF)",
                category = ScheduleCategory.TUITION.name,
                startTime = "18:00",
                endTime = "19:00",
                durationMinutes = 60,
                notes = "6:00 PM – 7:00 PM Mon/Wed/Fri tuition session",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel from Tuition to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "19:00",
                endTime = "19:15",
                durationMinutes = 15,
                notes = "Return to study desk",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Audit Lecture 2 (1h 30m)",
                category = ScheduleCategory.AUDIT.name,
                startTime = "19:15",
                endTime = "20:45",
                durationMinutes = 90,
                notes = "CA Inter Paper 5 - Audit lecture 2",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Dinner & Refreshment",
                category = ScheduleCategory.LUNCH.name,
                startTime = "20:45",
                endTime = "21:15",
                durationMinutes = 30,
                notes = "Light dinner",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "2-Hour Study Block (Practical Practice)",
                category = ScheduleCategory.STUDY_BLOCK.name,
                startTime = "21:15",
                endTime = "23:15",
                durationMinutes = 120,
                notes = "2-Hour deep focus block: Costing practical problems & Audit MCQs",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Day Wrap-up & Tomorrow Planning",
                category = ScheduleCategory.BREAK.name,
                startTime = "23:15",
                endTime = "23:30",
                durationMinutes = 15,
                notes = "Review completion dashboard, set priorities for tomorrow",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Sleep",
                category = ScheduleCategory.SLEEP.name,
                startTime = "23:30",
                endTime = "07:00",
                durationMinutes = 450,
                notes = "Sleep at 11:30 PM sharp for 7.5 hours recovery",
                sortOrder = order++
            )
        )
    }

    private fun createTtsSchedule(day: Int): List<ScheduleItem> {
        var order = 1
        return listOf(
            ScheduleItem(
                dayOfWeek = day,
                title = "Wake Up & Hydrate",
                category = ScheduleCategory.WAKE.name,
                startTime = "07:00",
                endTime = "07:15",
                durationMinutes = 15,
                notes = "Consistent wake-up time for circadian rhythm",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Morning Gym",
                category = ScheduleCategory.GYM.name,
                startTime = "07:15",
                endTime = "08:15",
                durationMinutes = 60,
                notes = "Strength & core conditioning",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Freshen Up & Breakfast",
                category = ScheduleCategory.BREAK.name,
                startTime = "08:15",
                endTime = "08:40",
                durationMinutes = 25,
                notes = "Breakfast at home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Home to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "08:40",
                endTime = "09:00",
                durationMinutes = 20,
                notes = "20-minute transition to study room",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Costing Lecture 1 (2h 18m)",
                category = ScheduleCategory.COSTING.name,
                startTime = "09:00",
                endTime = "11:18",
                durationMinutes = 138,
                notes = "CA Inter Paper 4 - Costing lecture 1",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Study Room Break",
                category = ScheduleCategory.BREAK.name,
                startTime = "11:18",
                endTime = "11:30",
                durationMinutes = 12,
                notes = "Hydrate and stretch",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Costing Lecture 2 (2h 18m)",
                category = ScheduleCategory.COSTING.name,
                startTime = "11:30",
                endTime = "13:48",
                durationMinutes = 138,
                notes = "CA Inter Paper 4 - Costing lecture 2",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Study Room to Home",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "13:48",
                endTime = "14:08",
                durationMinutes = 20,
                notes = "20-minute travel home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Lunch at Home & Rest",
                category = ScheduleCategory.LUNCH.name,
                startTime = "14:08",
                endTime = "14:40",
                durationMinutes = 32,
                notes = "Nutritious lunch at home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Home to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "14:40",
                endTime = "15:00",
                durationMinutes = 20,
                notes = "20-minute travel to study room",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Audit Lecture 1 (1h 30m)",
                category = ScheduleCategory.AUDIT.name,
                startTime = "15:00",
                endTime = "16:30",
                durationMinutes = 90,
                notes = "CA Inter Paper 5 - Auditing & Ethics",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Tea Break",
                category = ScheduleCategory.BREAK.name,
                startTime = "16:30",
                endTime = "16:45",
                durationMinutes = 15,
                notes = "15-minute break",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Audit Lecture 2 (1h 30m)",
                category = ScheduleCategory.AUDIT.name,
                startTime = "16:45",
                endTime = "18:15",
                durationMinutes = 90,
                notes = "CA Inter Paper 5 - Audit lecture 2",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel Home for Running",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "18:15",
                endTime = "18:30",
                durationMinutes = 15,
                notes = "Change into running gear",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "TTSS Running Session",
                category = ScheduleCategory.RUNNING.name,
                startTime = "18:30",
                endTime = "19:15",
                durationMinutes = 45,
                notes = "Outdoor endurance run & cool-down (Tue/Thu/Sat/Sun routine)",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Freshen Up & Dinner at Home",
                category = ScheduleCategory.LUNCH.name,
                startTime = "19:15",
                endTime = "20:00",
                durationMinutes = 45,
                notes = "Dinner and hydration",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "20:00",
                endTime = "20:20",
                durationMinutes = 20,
                notes = "20-minute travel for night study",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "2-Hour Study Block (Costing/Audit Practice)",
                category = ScheduleCategory.STUDY_BLOCK.name,
                startTime = "20:20",
                endTime = "22:20",
                durationMinutes = 120,
                notes = "2-hour uninterrupted problem-solving block",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Quick Break",
                category = ScheduleCategory.BREAK.name,
                startTime = "22:20",
                endTime = "22:30",
                durationMinutes = 10,
                notes = "Short breath",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Revision & Day Wrap-up",
                category = ScheduleCategory.STUDY_BLOCK.name,
                startTime = "22:30",
                endTime = "23:30",
                durationMinutes = 60,
                notes = "Review concepts and plan next day",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Sleep",
                category = ScheduleCategory.SLEEP.name,
                startTime = "23:30",
                endTime = "07:00",
                durationMinutes = 450,
                notes = "Sleep at 11:30 PM",
                sortOrder = order++
            )
        )
    }

    private fun createSundaySchedule(day: Int): List<ScheduleItem> {
        var order = 1
        return listOf(
            ScheduleItem(
                dayOfWeek = day,
                title = "Wake Up & Hydrate",
                category = ScheduleCategory.WAKE.name,
                startTime = "07:00",
                endTime = "07:15",
                durationMinutes = 15,
                notes = "Wake at 7 AM sharp",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "TTSS Morning Running / Mobility",
                category = ScheduleCategory.RUNNING.name,
                startTime = "07:15",
                endTime = "08:15",
                durationMinutes = 60,
                notes = "Sunday TTSS endurance run & mobility",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Breakfast & Freshen Up",
                category = ScheduleCategory.BREAK.name,
                startTime = "08:15",
                endTime = "08:40",
                durationMinutes = 25,
                notes = "Hearty breakfast at home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Home to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "08:40",
                endTime = "09:00",
                durationMinutes = 20,
                notes = "20-minute travel to study room",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "2-Hour Study Block 1: Audit SA 200/500 Series",
                category = ScheduleCategory.STUDY_BLOCK.name,
                startTime = "09:00",
                endTime = "11:00",
                durationMinutes = 120,
                notes = "Sunday Audit-Only: Standards on Auditing deep dive & memory codes",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Break with Active Recall",
                category = ScheduleCategory.BREAK.name,
                startTime = "11:00",
                endTime = "11:15",
                durationMinutes = 15,
                notes = "15-minute break",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "2-Hour Study Block 2: Company Audit & CARO",
                category = ScheduleCategory.STUDY_BLOCK.name,
                startTime = "11:15",
                endTime = "13:15",
                durationMinutes = 120,
                notes = "Sunday Audit-Only: Sec 139-148 & CARO 2020 reporting clauses",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Study Room to Home",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "13:15",
                endTime = "13:35",
                durationMinutes = 20,
                notes = "20-minute travel home for Sunday lunch",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Sunday Lunch at Home & Rest",
                category = ScheduleCategory.LUNCH.name,
                startTime = "13:35",
                endTime = "14:40",
                durationMinutes = 65,
                notes = "Relaxation and lunch at home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Home to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "14:40",
                endTime = "15:00",
                durationMinutes = 20,
                notes = "20-minute travel back to study room",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Audit Lecture 1 (1h 30m)",
                category = ScheduleCategory.AUDIT.name,
                startTime = "15:00",
                endTime = "16:30",
                durationMinutes = 90,
                notes = "Sunday Audit Lecture session 1",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Tea Break",
                category = ScheduleCategory.BREAK.name,
                startTime = "16:30",
                endTime = "16:45",
                durationMinutes = 15,
                notes = "15-minute break",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Audit Lecture 2 (1h 30m)",
                category = ScheduleCategory.AUDIT.name,
                startTime = "16:45",
                endTime = "18:15",
                durationMinutes = 90,
                notes = "Sunday Audit Lecture session 2",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel Home",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "18:15",
                endTime = "18:35",
                durationMinutes = 20,
                notes = "20-minute travel home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Sunday Evening Walk / TTSS Stretch",
                category = ScheduleCategory.RUNNING.name,
                startTime = "18:35",
                endTime = "19:15",
                durationMinutes = 40,
                notes = "Active recovery walk and stretching",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Dinner & Family Time",
                category = ScheduleCategory.LUNCH.name,
                startTime = "19:15",
                endTime = "20:15",
                durationMinutes = 60,
                notes = "Dinner at home",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Travel: Home to Study Room",
                category = ScheduleCategory.TRAVEL.name,
                startTime = "20:15",
                endTime = "20:35",
                durationMinutes = 20,
                notes = "20-minute travel to study room",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "2-Hour Study Block 3: Weekly Audit Mastery Quiz",
                category = ScheduleCategory.STUDY_BLOCK.name,
                startTime = "20:35",
                endTime = "22:35",
                durationMinutes = 120,
                notes = "Solve mock questions, case scenarios, and review weekly mistakes",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Weekly Review & Schedule Audit",
                category = ScheduleCategory.BREAK.name,
                startTime = "22:35",
                endTime = "23:30",
                durationMinutes = 55,
                notes = "Audit weekly study logs, calculate total lecture progress, plan coming week",
                sortOrder = order++
            ),
            ScheduleItem(
                dayOfWeek = day,
                title = "Sleep",
                category = ScheduleCategory.SLEEP.name,
                startTime = "23:30",
                endTime = "07:00",
                durationMinutes = 450,
                notes = "Sleep at 11:30 PM",
                sortOrder = order++
            )
        )
    }

    fun getDefaultTrackers(): List<LectureTracker> {
        return listOf(
            LectureTracker(
                subject = "COSTING",
                displayName = "Cost & Management Accounting (Paper 4)",
                totalLectures = 85,
                completedLectures = 0,
                dailyTarget = 2,
                lectureDurationMinutes = 138 // 2h 18m
            ),
            LectureTracker(
                subject = "AUDIT",
                displayName = "Auditing and Ethics (Paper 5)",
                totalLectures = 65,
                completedLectures = 0,
                dailyTarget = 2, // Monday to Saturday (Sunday Audit-only)
                lectureDurationMinutes = 90 // 1h 30m
            )
        )
    }
}
