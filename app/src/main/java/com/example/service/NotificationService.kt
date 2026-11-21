package com.example.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.R

object NotificationService {
    private const val CHANNEL_ID = "hadirin_attendance_channel"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Presensi & Notifikasi HADIRIN",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Pengingat jadwal masuk, status presensi, dan persetujuan izin."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showLocalNotification(context: Context, title: String, body: String, notificationId: Int = (System.currentTimeMillis() % 100000).toInt()) {
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(notificationId, builder.build())
    }

    fun openWhatsAppMessage(context: Context, phoneRaw: String?, message: String) {
        val digits = phoneRaw.orEmpty().filter { it.isDigit() }.let {
            when {
                it.startsWith("0") -> "62" + it.drop(1)
                else -> it
            }
        }
        val encoded = Uri.encode(message)
        val url = if (digits.isNotBlank()) {
            "https://wa.me/$digits?text=$encoded"
        } else {
            "https://wa.me/?text=$encoded"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
