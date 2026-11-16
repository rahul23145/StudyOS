package com.studyos.cainter.data.local.entity

enum class ScheduleCategory(val displayName: String, val colorHex: String) {
    COSTING("Costing Lecture", "#0284C7"),
    AUDIT("Audit Lecture", "#8B5CF6"),
    STUDY_BLOCK("2h Study Block", "#10B981"),
    TUITION("Tuition (MWF)", "#F59E0B"),
    GYM("Morning Gym", "#EC4899"),
    RUNNING("TTSS Running", "#F43F5E"),
    TRAVEL("Travel (20m)", "#64748B"),
    LUNCH("Lunch at Home", "#14B8A6"),
    BREAK("Break", "#94A3B8"),
    WAKE("Wake Up", "#EAB308"),
    SLEEP("Sleep", "#334155"),
    CUSTOM("Custom", "#6366F1");

    companion object {
        fun fromString(value: String): ScheduleCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOM
        }
    }
}
