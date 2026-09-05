package com.xgimi.xgimiservice.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/** Emulator-only binder stub for deterministic XGIMI private-service scenarios. */
public final class XgimiServices extends Service {
    private static final String ROOT_DESCRIPTOR = "com.xgimi.aidl.IXgimiService";
    private static final String COMMON_DESCRIPTOR = "com.xgimi.aidl.IGimiCommon";
    private static final String PREFS_NAME = "mock_xgimi_service";
    private static final String ENV_PREFIX = "env.";
    private static final String PREF_SCENARIO = "scenario";
    private static final String PREF_FAIL_KEY = "fail_key";
    private static final String PREF_DELAY_MS = "delay_ms";
    private static final String SCENARIO_SUCCESS = "success";
    private static final String SCENARIO_NO_COMMON = "no_common";
    private static final String SCENARIO_TIMEOUT = "timeout";
    private static final String SCENARIO_ENV_WRITE_FAILURE = "env_write_failure";
    private static final String SCENARIO_ENV_READBACK_MISMATCH = "env_readback_mismatch";
    private static final String DEFAULT_FAIL_KEY = "kst_ofs";
    private static final long DEFAULT_TIMEOUT_MS = 15000L;

    private final IBinder common = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            data.enforceInterface(COMMON_DESCRIPTOR);
            if (code == 5) {
                data.readString();
                data.readString();
                reply.writeNoException();
                reply.writeInt(1);
                return true;
            }
            if (code == 6) {
                String key = data.readString();
                reply.writeNoException();
                reply.writeString(readEnvironment(key));
                return true;
            }
            if (code == 7) {
                String key = data.readString();
                String value = data.readString();
                if (SCENARIO_ENV_WRITE_FAILURE.equals(currentScenario())
                        && failureKey().equals(key)) {
                    reply.writeException(new IllegalArgumentException(
                            "mock environment write failure for " + key));
                    return true;
                }
                writeEnvironment(key, value);
                reply.writeNoException();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    };

    private final IBinder root = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            if (code != 1) {
                return super.onTransact(code, data, reply, flags);
            }
            data.enforceInterface(ROOT_DESCRIPTOR);
            maybeDelayForTimeout();
            reply.writeNoException();
            reply.writeStrongBinder(SCENARIO_NO_COMMON.equals(currentScenario()) ? null : common);
            return true;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        MockState.seedDefaults(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return root;
    }

    private String currentScenario() {
        return MockState.preferences(this).getString(PREF_SCENARIO, SCENARIO_SUCCESS);
    }

    private String failureKey() {
        return MockState.preferences(this).getString(PREF_FAIL_KEY, DEFAULT_FAIL_KEY);
    }

    private String readEnvironment(String key) {
        if (SCENARIO_ENV_READBACK_MISMATCH.equals(currentScenario()) && failureKey().equals(key)) {
            return "mock-readback-mismatch";
        }
        return MockState.preferences(this).getString(ENV_PREFIX + key, MockState.defaultValue(key));
    }

    private void writeEnvironment(String key, String value) {
        MockState.preferences(this).edit().putString(ENV_PREFIX + key, value).apply();
    }

    private void maybeDelayForTimeout() {
        if (!SCENARIO_TIMEOUT.equals(currentScenario())) {
            return;
        }
        long delayMs = MockState.preferences(this).getLong(PREF_DELAY_MS, DEFAULT_TIMEOUT_MS);
        try {
            Thread.sleep(Math.max(1L, delayMs));
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }

    static final class MockState {
        static final String ACTION_RESET = "com.xgimi.xgimiservice.mock.RESET";
        static final String ACTION_SET_SCENARIO =
                "com.xgimi.xgimiservice.mock.SET_SCENARIO";
        static final String ACTION_SET_ENV = "com.xgimi.xgimiservice.mock.SET_ENV";
        static final String EXTRA_SCENARIO = "scenario";
        static final String EXTRA_KEY = "key";
        static final String EXTRA_VALUE = "value";
        static final String EXTRA_FAIL_KEY = "fail_key";
        static final String EXTRA_DELAY_MS = "delay_ms";

        private MockState() {
        }

        static SharedPreferences preferences(Context context) {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        static void reset(Context context) {
            SharedPreferences.Editor editor = preferences(context).edit().clear()
                    .putString(PREF_SCENARIO, SCENARIO_SUCCESS)
                    .putString(PREF_FAIL_KEY, DEFAULT_FAIL_KEY)
                    .putLong(PREF_DELAY_MS, DEFAULT_TIMEOUT_MS);
            putDefaults(editor);
            editor.apply();
        }

        static void seedDefaults(Context context) {
            SharedPreferences preferences = preferences(context);
            if (!preferences.contains(PREF_SCENARIO)) {
                reset(context);
            }
        }

        static void setScenario(Context context, Intent intent) {
            seedDefaults(context);
            SharedPreferences.Editor editor = preferences(context).edit();
            String scenario = intent.getStringExtra(EXTRA_SCENARIO);
            if (scenario != null && scenario.length() > 0) {
                editor.putString(PREF_SCENARIO, scenario);
            }
            String failKey = intent.getStringExtra(EXTRA_FAIL_KEY);
            if (failKey != null && failKey.length() > 0) {
                editor.putString(PREF_FAIL_KEY, failKey);
            }
            long delayMs = intent.getLongExtra(EXTRA_DELAY_MS, -1L);
            if (delayMs >= 0L) {
                editor.putLong(PREF_DELAY_MS, delayMs);
            }
            editor.apply();
        }

        static void setEnvironment(Context context, Intent intent) {
            seedDefaults(context);
            String key = intent.getStringExtra(EXTRA_KEY);
            String value = intent.getStringExtra(EXTRA_VALUE);
            if (key == null || key.length() == 0 || value == null) {
                return;
            }
            preferences(context).edit().putString(ENV_PREFIX + key, value).apply();
        }

        static String defaultValue(String key) {
            if ("shape_type".equals(key)) {
                return "1";
            }
            if ("zoom_factor".equals(key)) {
                return "100";
            }
            if ("kst_ofs".equals(key) || "kst_ofs_backup".equals(key)) {
                return "1,10,20,30,40,50,60,70,80";
            }
            if ("kst_ofs_limit".equals(key) || "kst_ofs_limit_backup".equals(key)) {
                return "0";
            }
            return "";
        }

        private static void putDefaults(SharedPreferences.Editor editor) {
            String[] keys = {
                "shape_type",
                "zoom_factor",
                "kst_ofs_backup",
                "kst_ofs_limit_backup",
                "kst_ofs_limit",
                "kst_ofs"
            };
            for (String key : keys) {
                editor.putString(ENV_PREFIX + key, defaultValue(key));
            }
        }
    }
}
