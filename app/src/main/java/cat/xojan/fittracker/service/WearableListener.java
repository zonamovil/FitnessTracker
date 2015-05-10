package cat.xojan.fittracker.service;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import cat.xojan.fittracker.main.MainActivity;

public class WearableListener extends WearableListenerService {

    private static final String TAG = WearableListener.class.getSimpleName();
    private static final String LAUNCH_HANDHELD_APP = "/launch_handheld_app";



    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.v(TAG, "onMessageReceived: " + messageEvent);

        if (LAUNCH_HANDHELD_APP.equals(messageEvent.getPath())) {
            Intent i = new Intent();
            i.setClass(this, MainActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }
    }
}
