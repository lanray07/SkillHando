package com.skillhando.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.time.Duration
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Reminders {
    const val ID = "practice-reminder"
    private const val SNOOZE = "practice-reminder-snooze"
    fun schedule(context: Context, hour: Int) {
        val now = LocalTime.now()
        val target = LocalTime.of(hour, 0)
        val wait = (Duration.between(now, target).seconds + 86_400) % 86_400
        val work = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(wait, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(ID, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, work)
    }

    fun snooze(context: Context, minutes: Long) {
        val request = OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(minutes, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniqueWork(SNOOZE, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(ID)
        WorkManager.getInstance(context).cancelUniqueWork(SNOOZE)
        context.getSystemService(NotificationManager::class.java).cancel(72)
    }
}

class SnoozeReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.getSystemService(NotificationManager::class.java).cancel(72)
        Reminders.snooze(context, 60)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val context = Localisation.configuredContext(applicationContext)
        val hour = LocalTime.now().hour
        if (hour < 8 || hour >= 21) {
            Reminders.snooze(context, if (hour < 8) (8 - hour) * 60L else (32 - hour) * 60L)
            return Result.success()
        }
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return Result.success()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(Reminders.ID, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT))
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val launch = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), flags)
        val snooze = PendingIntent.getBroadcast(context, 1, Intent(context, SnoozeReminder::class.java), flags)
        manager.notify(72, NotificationCompat.Builder(context, Reminders.ID).setSmallIcon(R.drawable.ic_hando)
            .setContentTitle(context.getString(R.string.reminder_title)).setContentText(context.getString(R.string.reminder_body))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setContentIntent(launch)
            .addAction(0, context.getString(R.string.snooze), snooze).setAutoCancel(true).build())
        return Result.success()
    }
}
