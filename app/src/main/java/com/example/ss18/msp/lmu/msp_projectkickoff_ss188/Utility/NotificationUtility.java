package com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Utility;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.ss18.msp.lmu.msp_projectkickoff_ss188.R;

import static com.example.ss18.msp.lmu.msp_projectkickoff_ss188.Connection.ConnectionManager.getAppLogicActivity;

public class NotificationUtility {

    private static final String TAG = "Notification";
    private static final String CHANNEL_ID = "CHANNEL_ID_42";

    /**
     * Displays a notification message.
     * See @see <a>https://developer.android.com/training/notify-user/build-notification>this</a>
     * for more information
     *
     * @param title   The title of the not
     * @param message The message we want to display
     */
    public static void displayNotification(final String title, final String message, final int priority) {
        Log.i(TAG, "NOTIFICATION: " + message);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getAppLogicActivity(), CHANNEL_ID)
                .setSmallIcon(R.drawable.file_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setDefaults(Notification.DEFAULT_ALL)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setPriority(priority);
        mBuilder.build();
        NotificationManager mNotificationManager = (NotificationManager) getAppLogicActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        // notificationID allows you to update the notification later on.
        mNotificationManager.notify(42, mBuilder.build());
    }

    /**
     * Displays a notification message for the chat.
     * See @see <a>https://developer.android.com/training/notify-user/build-notification>this</a>
     * for more information
     *
     * @param title   The title of the not
     * @param message The message we want to display
     */
    public static void displayNotificationChat(final String title, final String message, final int priority) {
        Log.i(TAG, "NOTIFICATION: " + message);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getAppLogicActivity(), CHANNEL_ID)
                .setSmallIcon(R.drawable.chat_icon)
                .setContentTitle(title)
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setPriority(priority);
        mBuilder.build();
        NotificationManager mNotificationManager = (NotificationManager) getAppLogicActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        // notificationID allows you to update the notification later on.
        mNotificationManager.notify(42, mBuilder.build());
    }

    /**
     * Start a vibration until endVibration() is called
     */
    public static void startVibration(){
        Log.i(TAG,"startVibration()");
        Vibrator v = (Vibrator) getAppLogicActivity().getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        final int time = 99999;
        if (v != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(time, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(time);
            }
        }else{
            Log.i(TAG,"VIBRATOR IS NULL!");
        }
    }

    /**
     * Ends any vibration on the phone
     */
    public static void endVibration(){
        Log.i(TAG,"endVibration()");
        Vibrator v = (Vibrator) getAppLogicActivity().getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.cancel();
        }else{
            Log.i(TAG,"VIBRATOR IS NULL!");
        }
    }

}