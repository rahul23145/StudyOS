package com.studyos.cainter.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

data class DayStudyData(
    val dayLabel: String,
    val hours: Float,
    val targetHours: Float = 8.0f,
    val isToday: Boolean = false
)

class StudyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var chartData: List<DayStudyData> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val todayBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0284C7") // Sapphire Blue
    }

    private val targetLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#94A3B8")
        strokeWidth = 3f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#475569")
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }

    private val hourTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        textSize = 26f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    private val targetTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 24f
        textAlign = Paint.Align.RIGHT
    }

    private val barRect = RectF()

    fun setData(data: List<DayStudyData>) {
        this.chartData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (chartData.isEmpty()) return

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val paddingLeft = 40f
        val paddingRight = 40f
        val paddingTop = 50f
        val paddingBottom = 60f

        val availableWidth = viewWidth - paddingLeft - paddingRight
        val availableHeight = viewHeight - paddingTop - paddingBottom

        val maxHours = max(10f, (chartData.maxOfOrNull { it.hours } ?: 8f) * 1.2f)
        val targetHours = chartData.firstOrNull()?.targetHours ?: 8f

        // Draw Target Line
        val targetY = paddingTop + availableHeight * (1f - (targetHours / maxHours))
        canvas.drawLine(paddingLeft, targetY, viewWidth - paddingRight, targetY, targetLinePaint)
        canvas.drawText("Target ${targetHours.toInt()}h", viewWidth - paddingRight, targetY - 10f, targetTextPaint)

        val barCount = chartData.size
        val barWidth = (availableWidth / barCount) * 0.55f
        val slotWidth = availableWidth / barCount

        chartData.forEachIndexed { index, dayData ->
            val slotCenter = paddingLeft + (index * slotWidth) + (slotWidth / 2f)
            val barLeft = slotCenter - (barWidth / 2f)
            val barRight = slotCenter + (barWidth / 2f)

            val barHeight = (dayData.hours / maxHours) * availableHeight
            val barTop = paddingTop + availableHeight - barHeight
            val barBottom = paddingTop + availableHeight

            barRect.set(barLeft, barTop, barRight, barBottom)

            // Select color based on completion and today
            val paint = when {
                dayData.isToday -> todayBarPaint
                dayData.hours >= dayData.targetHours -> {
                    barPaint.color = Color.parseColor("#10B981") // Emerald Green
                    barPaint
                }
                dayData.hours > 0f -> {
                    barPaint.color = Color.parseColor("#38BDF8") // Sky Blue
                    barPaint
                }
                else -> {
                    barPaint.color = Color.parseColor("#E2E8F0") // Empty / Rest
                    barPaint
                }
            }

            // Draw rounded bar
            canvas.drawRoundRect(barRect, 14f, 14f, paint)

            // Draw hour text above bar
            val hourText = if (dayData.hours > 0f) String.format("%.1fh", dayData.hours) else "0h"
            canvas.drawText(hourText, slotCenter, barTop - 12f, hourTextPaint)

            // Draw Day label below bar
            textPaint.isFakeBoldText = dayData.isToday
            textPaint.color = if (dayData.isToday) Color.parseColor("#0284C7") else Color.parseColor("#64748B")
            canvas.drawText(dayData.dayLabel, slotCenter, viewHeight - 16f, textPaint)
        }
    }
}
