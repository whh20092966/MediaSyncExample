package org.mediasyncexample;

import android.util.Log;

public final class MLog {


    static void d(String tag, String msg){
        Log.d("MediaCodePlayer - " + tag, msg);
    }
}
