package com.xgimi.xgimiservice.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** adb-controlled scenario switch for emulator-only tests. */
public final class MockControlReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (XgimiServices.MockState.ACTION_RESET.equals(action)) {
            XgimiServices.MockState.reset(context);
            return;
        }
        if (XgimiServices.MockState.ACTION_SET_SCENARIO.equals(action)) {
            XgimiServices.MockState.setScenario(context, intent);
            return;
        }
        if (XgimiServices.MockState.ACTION_SET_ENV.equals(action)) {
            XgimiServices.MockState.setEnvironment(context, intent);
        }
    }
}
