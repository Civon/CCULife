package org.zankio.cculife;

import android.util.Log;

public class Debug {
    public static final String TAG = "CCULife";
    public static boolean debug = false;
    public void info(String msg) {
      if(debug) {
        Log.i(TAG, msg);
      }
    }
    public void error(String msg) {
        if (debug) Log.e(TAG, msg);
    }
}
