package com.recipevault.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.recipevault.R;
import com.recipevault.activity.RecipeDetailActivity;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "recipe_reminder_channel";
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String recipeTitle = intent.getStringExtra("recipeTitle");
            String recipeId = intent.getStringExtra("recipeId");

            // Create notification channel for Android O+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Recipe Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                );
                NotificationManager manager = context.getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }

            Intent detailIntent = new Intent(context, RecipeDetailActivity.class);
            detailIntent.putExtra("recipe_id", recipeId);
            detailIntent.putExtra("recipe_title", recipeTitle);
            detailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                recipeId != null ? recipeId.hashCode() : 0,
                detailIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_time)
                .setContentTitle("Recipe Reminder")
                .setContentText("It's time to check: " + recipeTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(recipeId != null ? recipeId.hashCode() : 0, builder.build());
            }
        } catch (Exception e) {
            android.util.Log.e("ReminderReceiver", "Error in onReceive", e);
        }
    }
}
