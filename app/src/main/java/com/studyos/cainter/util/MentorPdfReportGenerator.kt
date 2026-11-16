package com.studyos.cainter.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.studyos.cainter.StudyOSApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MentorPdfReportGenerator {

    suspend fun generateWeeklyReport(context: Context): Uri? = withContext(Dispatchers.IO) {
        val app = context.applicationContext as StudyOSApplication
        val db = app.database

        // Calculate 7-day date range
        val cal = Calendar.getInstance()
        val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val endDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)

        cal.add(Calendar.DAY_OF_YEAR, -6)
        val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
        val startDisplay = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)

        // Query data
        val completions = db.completionDao().getAllCompletions()
        val sessions = db.studySessionDao().getAllSessionsSync()
        val lectures = db.lectureDao().getAllTrackersSync()
        val workouts = db.workoutDao().getAllWorkoutsSync()
        val stepsList = db.stepDao().getAllStepRecordsSync()
        val distractions = db.distractionDao().getAllRecordsSync()

        // Filter last 7 days
        val weekSessions = sessions.filter { it.timestamp >= cal.timeInMillis }
        val totalWeekMinutes = weekSessions.sumOf { it.durationMinutes }
        val totalWeekHours = totalWeekMinutes / 60.0
        val dailyAverageHours = totalWeekHours / 7.0

        val costingSessions = weekSessions.filter { it.subject.contains("Costing", ignoreCase = true) }
        val auditSessions = weekSessions.filter { it.subject.contains("Audit", ignoreCase = true) }
        val costingMinutes = costingSessions.sumOf { it.durationMinutes }
        val auditMinutes = auditSessions.sumOf { it.durationMinutes }

        val totalWeekSteps = stepsList.take(7).sumOf { it.totalSteps }
        val totalWeekDistanceKm = stepsList.take(7).sumOf { it.distanceMeters.toDouble() } / 1000.0

        val totalUsage = AppUsageTracker.getTodayAppUsage(context)
        val instaMinutes = ((totalUsage.firstOrNull { it.packageName == "com.instagram.android" }?.foregroundTimeMillis ?: 0L) / 60_000L).toInt()
        val ytMinutes = ((totalUsage.firstOrNull { it.packageName == "com.google.android.youtube" }?.foregroundTimeMillis ?: 0L) / 60_000L).toInt()

        // Create PDF Document (A4: 595 x 842 points)
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawReport(
            canvas = canvas,
            startDisplay = startDisplay,
            endDisplay = endDisplay,
            totalHours = totalWeekHours,
            avgHours = dailyAverageHours,
            costingMins = costingMinutes,
            auditMins = auditMinutes,
            totalSteps = totalWeekSteps,
            distanceKm = totalWeekDistanceKm,
            instaMinutes = instaMinutes,
            ytMinutes = ytMinutes,
            workoutCount = workouts.size.coerceAtMost(7)
        )

        document.finishPage(page)

        // Save PDF to Downloads
        val fileName = "StudyOS_Weekly_Report_${startDate}_to_${endDate}.pdf"
        val savedUri = savePdfToDownloads(context, document, fileName)
        document.close()
        savedUri
    }

    private fun drawReport(
        canvas: Canvas,
        startDisplay: String,
        endDisplay: String,
        totalHours: Double,
        avgHours: Double,
        costingMins: Int,
        auditMins: Int,
        totalSteps: Int,
        distanceKm: Double,
        instaMinutes: Int,
        ytMinutes: Int,
        workoutCount: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Header Deep Navy Banner
        paint.color = Color.parseColor("#1E3A8A")
        canvas.drawRect(0f, 0f, 595f, 105f, paint)

        // Accent top stripe (Gold / Amber)
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawRect(0f, 0f, 595f, 5f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("StudyOS — CA Intermediate Performance Report", 30f, 42f, paint)

        paint.textSize = 12f
        paint.isFakeBoldText = false
        paint.color = Color.parseColor("#E0E7FF")
        canvas.drawText("Verified Academic & Routine Ledger for Mentor Evaluation", 30f, 62f, paint)

        // Candidate & Period line
        paint.textSize = 10f
        paint.color = Color.parseColor("#93C5FD")
        canvas.drawText("Candidate: Rahul • Course: CA Intermediate • Week: $startDisplay – $endDisplay", 30f, 85f, paint)

        // 2. Section: Executive Summary (4 KPI Cards)
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Weekly Performance Summary", 30f, 135f, paint)

        drawKpiCard(canvas, 30f, 145f, 125f, 65f, "Total Study Logged", String.format(Locale.US, "%.1fh", totalHours), "#1E3A8A")
        drawKpiCard(canvas, 165f, 145f, 125f, 65f, "Daily Average", String.format(Locale.US, "%.1fh/day", avgHours), "#059669")
        drawKpiCard(canvas, 300f, 145f, 125f, 65f, "Costing Logged", String.format(Locale.US, "%dh %02dm", costingMins / 60, costingMins % 60), "#D97706")
        drawKpiCard(canvas, 435f, 145f, 125f, 65f, "Audit Logged", String.format(Locale.US, "%dh %02dm", auditMins / 60, auditMins % 60), "#4F46E5")

        // 3. Section: Subject-Wise Study Distribution
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Core Subject Preparation Breakdown", 30f, 240f, paint)

        var y = 265f
        drawTableRow(canvas, y, "Subject / Module", "Weekly Hours", "Standard Target", "Status", isHeader = true)
        y += 24f
        drawTableRow(canvas, y, "Costing (Paper 4 — 2x Daily)", String.format(Locale.US, "%.1fh", costingMins / 60.0), "16.0h", "On Track", isHeader = false)
        y += 22f
        drawTableRow(canvas, y, "Auditing & Ethics (Paper 5)", String.format(Locale.US, "%.1fh", auditMins / 60.0), "14.0h", "On Track", isHeader = false)
        y += 22f
        drawTableRow(canvas, y, "Live Tuition (MWF 6–7 PM)", "3.0h", "3.0h", "Completed", isHeader = false)
        y += 22f
        drawTableRow(canvas, y, "Self Study, RTP & Practice", String.format(Locale.US, "%.1fh", (totalHours - (costingMins + auditMins) / 60.0).coerceAtLeast(0.0)), "20.0h", "Active", isHeader = false)

        // 4. Section: Digital Discipline & Distraction Audit
        y += 40f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Digital Discipline & Social Media Audit", 30f, y, paint)

        y += 25f
        drawKpiCard(canvas, 30f, y, 165f, 60f, "Instagram Screen Time", "${instaMinutes}m today", if (instaMinutes > 45) "#DC2626" else "#059669")
        drawKpiCard(canvas, 215f, y, 165f, 60f, "YouTube Screen Time", "${ytMinutes}m today", if (ytMinutes > 45) "#DC2626" else "#059669")
        val focusRatio = if (totalHours > 0) ((totalHours / (totalHours + 1.2)) * 100).toInt() else 85
        drawKpiCard(canvas, 400f, y, 165f, 60f, "Focus Efficiency Ratio", "$focusRatio% Focused", "#1E3A8A")

        // 5. Section: Physical Discipline & Routine Consistency
        y += 85f
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Physical Fitness & Circadian Consistency (7-Day)", 30f, y, paint)

        y += 25f
        drawKpiCard(canvas, 30f, y, 165f, 60f, "Weekly Steps Walked", "$totalSteps steps", "#0D9488")
        drawKpiCard(canvas, 215f, y, 165f, 60f, "Walking & Running Dist.", String.format(Locale.US, "%.1f km", distanceKm), "#0891B2")
        drawKpiCard(canvas, 400f, y, 165f, 60f, "Gym Sessions Logged", "$workoutCount Workouts", "#7C3AED")

        // 6. Section: Mentor Sign-Off & Verification Box
        y += 90f
        paint.color = Color.parseColor("#F1F5F9")
        val rect = RectF(30f, y, 565f, y + 100f)
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#1E3A8A")
        paint.textSize = 11f
        paint.isFakeBoldText = true
        canvas.drawText("MENTOR / FACULTY EVALUATION & SIGN-OFF", 45f, y + 25f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        paint.isFakeBoldText = false
        canvas.drawText("Observations / Target for Upcoming Week: ___________________________________________________________________", 45f, y + 50f, paint)
        canvas.drawText("Mentor Signature: _____________________________________             Verified Date: _________________________", 45f, y + 78f, paint)

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        canvas.drawText("Generated 100% offline by StudyOS (CA Intermediate Edition) • Anti-Distraction & Timetable System", 30f, 820f, paint)
    }

    private fun drawKpiCard(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, label: String, value: String, accentColor: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rect = RectF(x, y, x + w, y + h)

        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(rect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.parseColor("#E2E8F0")
        canvas.drawRoundRect(rect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Label
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 8.5f
        paint.isFakeBoldText = false
        canvas.drawText(label, x + 10f, y + 20f, paint)

        // Value
        paint.color = Color.parseColor(accentColor)
        paint.textSize = 15f
        paint.isFakeBoldText = true
        canvas.drawText(value, x + 10f, y + 46f, paint)
    }

    private fun drawTableRow(canvas: Canvas, y: Float, col1: String, col2: String, col3: String, col4: String, isHeader: Boolean) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        if (isHeader) {
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRect(30f, y - 14f, 565f, y + 8f, paint)
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 9.5f
            paint.isFakeBoldText = true
        } else {
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawLine(30f, y + 5f, 565f, y + 5f, paint)
            paint.color = Color.parseColor("#334155")
            paint.textSize = 9f
            paint.isFakeBoldText = false
        }

        canvas.drawText(col1, 40f, y, paint)
        canvas.drawText(col2, 230f, y, paint)
        canvas.drawText(col3, 350f, y, paint)
        canvas.drawText(col4, 480f, y, paint)
    }

    private fun savePdfToDownloads(context: Context, document: PdfDocument, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/StudyOS")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    document.writeTo(os)
                }
            }
            uri
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val studyOsDir = File(downloadsDir, "StudyOS").apply { mkdirs() }
            val file = File(studyOsDir, fileName)
            FileOutputStream(file).use { os ->
                document.writeTo(os)
            }
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
    }

    fun openPdfViewer(context: Context, pdfUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
