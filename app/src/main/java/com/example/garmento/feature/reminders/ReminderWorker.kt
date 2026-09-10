package com.example.garmento.feature.reminders

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.garmento.GarmentoApp
import com.example.garmento.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val reminderRepository: ReminderRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val expiring = reminderRepository.getExpiringSoon(7)
            val context = applicationContext

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            expiring.forEachIndexed { index, subscription ->
                val daysLeft = subscription.daysRemaining
                val clothesLeft = subscription.remainingClothes

                val notification = NotificationCompat.Builder(context, GarmentoApp.CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_alert)
                    .setContentTitle("Subscription Expiring")
                    .setContentText(
                        "Subscription ${subscription.id.toString().take(8)} " +
                        "expires in $daysLeft days — $clothesLeft clothes remaining"
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(index + 1000, notification)
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
