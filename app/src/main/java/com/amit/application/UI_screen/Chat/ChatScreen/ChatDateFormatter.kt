package com.amit.application.UI_screen.Chat.ChatScreen

import java.text.SimpleDateFormat
import java.util.*

object ChatDateFormatter {
    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateDivider(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)

        calendar.timeInMillis = timestamp
        val msgDay = calendar.get(Calendar.DAY_OF_YEAR)
        val msgYear = calendar.get(Calendar.YEAR)

        return when {
            year == msgYear && today == msgDay -> "Today"
            year == msgYear && today - msgDay == 1 -> "Yesterday"
            else -> SimpleDateFormat("dd:MM:yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}