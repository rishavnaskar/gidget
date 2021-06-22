package com.dscvit.gidget.common

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dscvit.gidget.widget.GidgetWidget
import java.util.Calendar

class AppWidgetAlarm(private val context: Context) {
    private val alarmID = 0
    private val intervalMillis: Long = 1200000

    fun startGidgetRefresh() {
        val calendar: Calendar = Calendar.getInstance()
        calendar.add(Calendar.MILLISECOND, intervalMillis.toInt())

        val alarmIntent = Intent(context, GidgetWidget::class.java).let { intent ->
            intent.action = Utils.getOnRefreshButtonClicked()
            PendingIntent.getBroadcast(context, 0, intent, 0)
        }
        with(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager) {
            setRepeating(AlarmManager.RTC, calendar.timeInMillis, intervalMillis, alarmIntent)
        }
    }

    fun stopGidgetRefresh() {
        val alarmIntent = Intent(Utils.getUpdateWidgetAction())
        val pendingIntent = PendingIntent.getBroadcast(context, alarmID, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent)
    }
}