package com.xgimi.xgimiservice.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

/** Emulator-only binder stub used to render the app's connected UI state. */
public final class XgimiServices extends Service {
    private static final String ROOT_DESCRIPTOR = "com.xgimi.aidl.IXgimiService";
    private static final String COMMON_DESCRIPTOR = "com.xgimi.aidl.IGimiCommon";

    private final IBinder common = new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            data.enforceInterface(COMMON_DESCRIPTOR);
            reply.writeNoException();
            if (code == 6) {
                data.readString();
                reply.writeString("0");
            }
            return true;
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
            reply.writeNoException();
            reply.writeStrongBinder(common);
            return true;
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return root;
    }
}
