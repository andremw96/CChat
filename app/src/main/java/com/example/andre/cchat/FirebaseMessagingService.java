package com.example.andre.cchat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by Wijaya_PC on 25-Jan-18.
 */

// kelas ini berguna untuk menerima notifikasi walaupun aplikasi sedang dibuka
    // sblm ada kelals ini, notifikasi hanya muncul ketika aplikasi ditutup
public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService
{
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // ngambil dari node.js
       // String notification_title = remoteMessage.getNotification().getTitle();
      //  String notification_body = remoteMessage.getNotification().getBody();

        // String click_action = remoteMessage.getNotification().getClickAction();
        String from_sender_id = remoteMessage.getData().get("from_user_id");
        String from_user_name = remoteMessage.getData().get("from_user_name");
        String click_action = remoteMessage.getData().get("click_action");
        String notification_title = remoteMessage.getData().get("title");
        String notification_body = remoteMessage.getData().get("body");


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.icon)
                    .setContentTitle(notification_title)
                    .setContentText(notification_body);

        Intent resultIntent = new Intent(click_action);
        resultIntent.putExtra("visit_user_id", from_sender_id);
        resultIntent.putExtra("user_name", from_user_name);
        Log.d("visit_user_id", from_sender_id);
        Log.d("user_name", from_user_name);

        // Because clicking the notification opens a new ("special") activity, there's
        // no need to create an artificial back stack.
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);



        // Sets an ID for the notification
        int mNotificationId = (int) System.currentTimeMillis(); // random notification ID
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }
}
