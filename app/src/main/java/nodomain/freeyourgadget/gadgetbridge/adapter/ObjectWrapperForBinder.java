package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.os.Binder;
import android.os.IBinder;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ObjectWrapperForBinder extends Binder {

    private final GBDevice mData;

    public ObjectWrapperForBinder(GBDevice data) {
        mData = data;
    }

    public GBDevice getData() {
        return mData;
    }
}