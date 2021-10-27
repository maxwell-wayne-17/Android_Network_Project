package com.example.proj3_max_wayne;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityCheck {
    private Context context;

    ConnectivityCheck(Context context){ this.context = context; }

    public boolean isNetworkReachable(){
        NetworkInfo current = getNetworkInfo();
        return (current == null) ? false : (current.getState() == NetworkInfo.State.CONNECTED);
    }

    public boolean isWiFiReachable(){
        NetworkInfo current = getNetworkInfo();
        return (current == null) ? false : (current.getType() == ConnectivityManager.TYPE_WIFI);
    }


    private NetworkInfo getNetworkInfo(){
        ConnectivityManager mManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return mManager.getActiveNetworkInfo();
    }
}
