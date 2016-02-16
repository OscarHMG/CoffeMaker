package com.example.user.coffeemaker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by user on 12/02/2016.
 */
public class AlarmReceiver extends BroadcastReceiver {



    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("Alarm","Yay!");
        Intent ringtone = new Intent(context, RingTonePlayService.class);
        context.startService(ringtone);
    }
}
