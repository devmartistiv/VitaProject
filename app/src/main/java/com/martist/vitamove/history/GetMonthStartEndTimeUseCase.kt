package com.martist.vitamove.history

import java.time.YearMonth
import java.time.ZoneId

class GetMonthStartEndTimeUseCase {
    operator fun invoke(year: Int, month: Int): Pair<Long, Long> {
        val yearMonth = YearMonth.of(year, month)
        val monthStart =
            yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val monthEnd =
            yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        return Pair(monthStart, monthEnd)
    }
}