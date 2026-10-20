package com.mraphaelpy.terriflow.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mraphaelpy.terriflow.MainActivity
import com.mraphaelpy.terriflow.domain.service.SystemNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) : SystemNotifier {

    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val nextId = AtomicInteger(FIRST_NOTIFICATION_ID)

    init {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notificações do TerriFlow"
            }
        )
    }

    override fun show(title: String, body: String, territoryId: String?) {
        val id = nextId.getAndIncrement()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            territoryId?.let { putExtra(EXTRA_TERRITORY_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(id, notification)
    }

    companion object {
        /** Extra do Intent com o território a abrir quando a notificação é tocada. */
        const val EXTRA_TERRITORY_ID = "territoryId"

        private const val CHANNEL_ID = "terriflow_main"
        private const val CHANNEL_NAME = "TerriFlow"
        private const val FIRST_NOTIFICATION_ID = 1000
    }
}
