package com.james.imeetpolycc;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.onesignal.Continue;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

import java.util.concurrent.TimeUnit;

public class ApplicationClass extends Application implements Configuration.Provider {

    private static final String ONESIGNAL_APP_ID = BuildConfig.ONESIGNAL_APP_ID;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize OneSignal
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        // requestPermission will show the native Android notification permission prompt.
        OneSignal.getNotifications().requestPermission(true, Continue.none());

        // Schedule the first work request
        scheduleMeetingStatusCheck();
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().build();
    }

    private void scheduleMeetingStatusCheck() {
        OneTimeWorkRequest checkMeetingStatusWork = new OneTimeWorkRequest.Builder(MeetingNotificationWorker.class)
                .setInitialDelay(1, TimeUnit.MINUTES) // Adjust the delay as needed
                .build();
        WorkManager.getInstance(this).enqueue(checkMeetingStatusWork);
    }
}