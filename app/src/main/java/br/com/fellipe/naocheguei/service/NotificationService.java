package br.com.fellipe.naocheguei.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import br.com.fellipe.naocheguei.MainActivity;
import br.com.fellipe.naocheguei.R;

import br.com.fellipe.naocheguei.MainActivity;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

/**
 * Created by Fellipe on 15/11/2016.
 */

public class NotificationService {

    private Context context;

    public NotificationService(Context context) {
        this.context = context;
    }

    public NotificationCompat.Builder createDefaultTextNotification(String title, String description){

        Intent resultIntent = new Intent(context, MainActivity.class);
        resultIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        context,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(br.com.fellipe.naocheguei.R.drawable.logo)
                        .setContentTitle(title)
                        .setContentText(description)
                        .setContentIntent(resultPendingIntent)
                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setAutoCancel(true)
                        .setVisibility(VISIBILITY_PUBLIC);

        return mBuilder;
    }


}
