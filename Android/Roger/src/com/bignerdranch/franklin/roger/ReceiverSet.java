package com.bignerdranch.franklin.roger;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public abstract class ReceiverSet {
    public static final int CREATE = 0;
    public static final int START = 1;
    public static final int RESUME = 2;

    public abstract int getSpan();
    public abstract IntentFilter getFilter();
    public abstract BroadcastReceiver createReceiver();

    BroadcastReceiver receiver;
    public BroadcastReceiver getReceiver() {
        if (receiver == null) {
            receiver = createReceiver();
        }

        return receiver;
    }
}
