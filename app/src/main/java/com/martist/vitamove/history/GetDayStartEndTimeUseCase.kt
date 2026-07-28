package com.martist.vitamove.history

import com.prolificinteractive.materialcalendarview.CalendarDay
import java.time.LocalDate
import java.time.ZoneId

class GetDayStartEndTimeUseCase {
    operator fun invoke(calendarDay: CalendarDay): Pair<Long, Long> {
        val date = LocalDate.of(calendarDay.year, calendarDay.month, calendarDay.day)
        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = date.atTime(23, 59, 59, 999_000_000)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        return dayStart to dayEnd
    }
}