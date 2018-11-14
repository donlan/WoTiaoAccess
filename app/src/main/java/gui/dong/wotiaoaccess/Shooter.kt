package gui.dong.wotiaoaccess

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.support.v4.app.NotificationCompat



/**
 * @author 梁桂栋
 * @date 2018/11/12  19:44.
 * e-mail 760625325@qq.com
 * GitHub: https://github.com/donlan
 * description: gui.dong.wotiaoaccess
 * @version 1.0
 */
object Shooter {
    fun shoot(context: Context, title: String, content: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(
                "1024"
            ) == null
        ) {
            val channel =
                NotificationChannel("1024", "WutiaoAccess", NotificationManager.IMPORTANCE_MIN)
            channel.description = "仅作为程序运行时各种服务使用情况的消息提醒，如共享位置服务，前台常驻提醒服务"
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, "1024")
            .setContentTitle(title)
            .setContentText(content)
            .setSound(getNotifyUri(context,R.raw.dudulu))
            .setDefaults(Notification.DEFAULT_ALL)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content).setBigContentTitle(title))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setShowWhen(true)
        builder.setAutoCancel(false)
        notificationManager.notify(title.hashCode(),builder.build())
    }

    private fun getNotifyUri(context: Context,rawId: Int): Uri {
        return Uri.parse("android.resource://" + context.packageName + "/" + rawId)
    }
}