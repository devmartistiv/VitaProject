package com.martist.vitamove.history

class GetRussianMonthNameUseCase {
    operator fun invoke(month: Int): String {

        val monthNames = listOf(
            "январь", "февраль", "март", "апрель", "май", "июнь",
            "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь"
        )

        if (month in 1..12)
            return monthNames[month - 1]

        return "неизвестный месяц"

    }
}