package com.example.tipidtrack.ui

import java.text.SimpleDateFormat
import java.util.*

object CycleManager {
    private val sdf = SimpleDateFormat("MM/dd/yy", Locale.getDefault())

    data class CycleRange(val start: Date, val end: Date)

    fun getCycleRange(startDateStr: String?): CycleRange {
        val calendar = Calendar.getInstance()
        val now = calendar.time

        if (startDateStr == null) {
            val start = calendar.time
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            val end = calendar.time
            return CycleRange(start, end)
        }

        val firstDate = sdf.parse(startDateStr) ?: Date()
        calendar.time = firstDate

        while (true) {
            val cycleStart = calendar.time
            val nextMonth = calendar.clone() as Calendar
            nextMonth.add(Calendar.MONTH, 1)
            nextMonth.add(Calendar.DAY_OF_MONTH, -1)
            val cycleEnd = nextMonth.time

            if (now.after(cycleStart) && now.before(cycleEnd) || isSameDay(now, cycleStart) || isSameDay(now, cycleEnd)) {
                return CycleRange(cycleStart, cycleEnd)
            }
            
            if (now.before(cycleStart)) {
                return CycleRange(cycleStart, cycleEnd)
            }

            calendar.add(Calendar.MONTH, 1)
        }
    }

    fun getAllCycles(startDateStr: String?): List<CycleRange> {
        if (startDateStr == null) return emptyList()
        val calendar = Calendar.getInstance()
        val now = calendar.time
        val firstDate = sdf.parse(startDateStr) ?: return emptyList()
        
        val cycles = mutableListOf<CycleRange>()
        calendar.time = firstDate

        while (true) {
            val cycleStart = calendar.time
            val nextMonth = calendar.clone() as Calendar
            nextMonth.add(Calendar.MONTH, 1)
            nextMonth.add(Calendar.DAY_OF_MONTH, -1)
            val cycleEnd = nextMonth.time

            cycles.add(CycleRange(cycleStart, cycleEnd))

            if (now.before(cycleEnd) || isSameDay(now, cycleEnd)) {
                break
            }
            calendar.add(Calendar.MONTH, 1)
        }
        return cycles.reversed() // Latest cycle first
    }

    fun formatCycle(range: CycleRange): String {
        return "${sdf.format(range.start)} - ${sdf.format(range.end)}"
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.time = date1
        val cal2 = Calendar.getInstance()
        cal2.time = date2
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    
    fun isDateInCycle(dateStr: String, range: CycleRange): Boolean {
        val date = try { sdf.parse(dateStr) } catch(e: Exception) { null } ?: return false
        return (date.after(range.start) && date.before(range.end)) || isSameDay(date, range.start) || isSameDay(date, range.end)
    }
}
