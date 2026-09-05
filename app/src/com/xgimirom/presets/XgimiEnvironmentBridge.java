package com.xgimirom.presets;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

final class XgimiEnvironmentBridge implements ServiceConnection {
    interface Listener {
        void onConnectionChanged(boolean connected, String message);
    }

    private static final String SERVICE_PACKAGE = "com.xgimi.xgimiservice";
    private static final String SERVICE_CLASS =
            "com.xgimi.xgimiservice.services.XgimiServices";
    private static final String XGIMI_SERVICE_DESCRIPTOR =
            "com.xgimi.aidl.IXgimiService";
    private static final String COMMON_DESCRIPTOR = "com.xgimi.aidl.IGimiCommon";
    private static final int TRANSACTION_GET_COMMON = 1;
    private static final int TRANSACTION_SET_CMD_TO_NATIVE = 5;
    private static final int TRANSACTION_GET_ENVIRONMENT = 6;
    private static final int TRANSACTION_SET_ENVIRONMENT = 7;
    private static final String[] KEYSTONE_COMMANDS = {
        "offset_tl_x",
        "offset_tl_y",
        "offset_tr_x",
        "offset_tr_y",
        "offset_bl_x",
        "offset_bl_y",
        "offset_br_x",
        "offset_br_y"
    };
    private final Context context;
    private final Listener listener;
    private IBinder commonBinder;
    private boolean bound;

    XgimiEnvironmentBridge(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    void connect() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(SERVICE_PACKAGE, SERVICE_CLASS));
        try {
            bound = context.bindService(intent, this, Context.BIND_AUTO_CREATE);
            if (!bound) {
                listener.onConnectionChanged(false, "无法绑定极米系统服务");
            }
        } catch (RuntimeException error) {
            listener.onConnectionChanged(false, "绑定失败: " + error.getMessage());
        }
    }

    void disconnect() {
        commonBinder = null;
        if (bound) {
            context.unbindService(this);
            bound = false;
        }
    }

    boolean isConnected() {
        return commonBinder != null && commonBinder.isBinderAlive();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        try {
            commonBinder = getCommonBinder(service);
            boolean connected = isConnected();
            listener.onConnectionChanged(
                    connected,
                    connected ? "已连接极米系统服务" : "系统服务未返回 Common 接口");
        } catch (RemoteException error) {
            commonBinder = null;
            listener.onConnectionChanged(false, "读取系统服务失败: " + error.getMessage());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        commonBinder = null;
        listener.onConnectionChanged(false, "极米系统服务已断开");
    }

    String getEnvironment(String key) throws RemoteException {
        requireConnected();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(COMMON_DESCRIPTOR);
            data.writeString(key);
            commonBinder.transact(TRANSACTION_GET_ENVIRONMENT, data, reply, 0);
            reply.readException();
            return reply.readString();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    void setEnvironment(String key, String value) throws RemoteException {
        requireConnected();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(COMMON_DESCRIPTOR);
            data.writeString(key);
            data.writeString(value);
            commonBinder.transact(TRANSACTION_SET_ENVIRONMENT, data, reply, 0);
            reply.readException();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    void applyKeystoneOffsets(String kstOfs) throws RemoteException {
        int[] stored = parseKeystoneOffsets(kstOfs);

        // kst_ofs is stored by the MStar trapezoid server in this order:
        // LB.x, LB.y, RB.x, RB.y, RT.x, RT.y, LT.x, LT.y.
        // The legacy display-chip API expects TL, TR, BL, BR.
        int[] commandValues = {
            stored[6], stored[7],
            stored[4], stored[5],
            stored[0], stored[1],
            stored[2], stored[3]
        };
        for (int i = 0; i < KEYSTONE_COMMANDS.length; i++) {
            setNativeCommand(KEYSTONE_COMMANDS[i], String.valueOf(commandValues[i]));
        }
    }

    private void setNativeCommand(String command, String value) throws RemoteException {
        requireConnected();
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(COMMON_DESCRIPTOR);
            data.writeString(command);
            data.writeString(value);
            boolean handled = commonBinder.transact(
                    TRANSACTION_SET_CMD_TO_NATIVE, data, reply, 0);
            if (!handled) {
                throw new RemoteException(command + " 未被系统服务处理");
            }
            reply.readException();
            // XH25L's MstarCommon implementation returns false even after it has
            // successfully forwarded the command. Consume but do not trust it.
            reply.readInt();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private int[] parseKeystoneOffsets(String value) {
        if (value == null || value.length() == 0 || "0".equals(value)) {
            throw new IllegalArgumentException("预设 kst_ofs 为空");
        }
        String[] raw = value.split(",", -1);
        int[] parsed = new int[9];
        int count = 0;
        for (String item : raw) {
            String trimmed = item.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            if (count >= parsed.length) {
                break;
            }
            try {
                parsed[count] = Integer.parseInt(trimmed);
            } catch (NumberFormatException error) {
                throw new IllegalArgumentException("kst_ofs 含有无效整数: " + trimmed);
            }
            count++;
        }
        if (count < 9) {
            throw new IllegalArgumentException("kst_ofs 不是校验值加 8 个角点");
        }
        int[] offsets = new int[8];
        System.arraycopy(parsed, 1, offsets, 0, offsets.length);
        return offsets;
    }

    private IBinder getCommonBinder(IBinder service) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeInterfaceToken(XGIMI_SERVICE_DESCRIPTOR);
            service.transact(TRANSACTION_GET_COMMON, data, reply, 0);
            reply.readException();
            return reply.readStrongBinder();
        } finally {
            reply.recycle();
            data.recycle();
        }
    }

    private void requireConnected() throws RemoteException {
        if (!isConnected()) {
            throw new RemoteException("XGIMI service is not connected");
        }
    }
}
