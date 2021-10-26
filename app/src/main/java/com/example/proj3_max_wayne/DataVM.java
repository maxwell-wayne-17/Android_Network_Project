package com.example.proj3_max_wayne;

import static android.provider.Settings.System.getString;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class DataVM extends ViewModel {

    // TODO - make variables private and use getters

    // Hardcoded for now, must be dynamically pulled
    String links[] = {  "https://www.pcs.cnu.edu/~kperkins/pets/p33.png",
            "https://www.pcs.cnu.edu/~kperkins/pets/p44.png",
            "https://www.pcs.cnu.edu/~kperkins/pets/p55.png",
            "https://www.pcs.cnu.edu/~kperkins/pets/p22.png"};

    // Must get from settings
    String jsonLink; //"https://www.pcs.cnu.edu/~kperkins/pets/pets.json";
    private final String URL_PREF_KEY = "url_preference";
    private final String DEFAULT_URL = "https://www.pcs.cnu.edu/~kperkins/pets/";
    private final String TAG = "DataVM Debug";

    int currentLink = 0;

    // Threads
    GetImageThread imgThread;
    GetTextThread txtThread;

    // Live data, bitmap we need
    private MutableLiveData<Bitmap> bmp;
    public MutableLiveData<Bitmap> getbmp(){
        if (bmp == null){
            bmp = new MutableLiveData<Bitmap>();
        }
        return bmp;
    }

    // Any communications from thread
    private MutableLiveData<String> result;
    public MutableLiveData<String> getResult(){
        if (result == null){
            result = new MutableLiveData<String>();
        }
        return result;
    }

    public void getPrefValues(SharedPreferences settings){
        jsonLink = settings.getString(URL_PREF_KEY,DEFAULT_URL) + "pets.json";
        //Toast.makeText(this,jsonLink,Toast.LENGTH_LONG).show();
        Log.d(TAG, jsonLink);
    }

    public void getJSON(){
        txtThread = new GetTextThread(jsonLink);
        txtThread.start();
        Log.d(TAG, getResult().toString());
    }

    public void getImage(String url){
        imgThread = new GetImageThread(url);
        imgThread.start();
    }

    public class GetTextThread extends Thread {
        private static final String TAG = "GetTextThread";
        private static final int    DEFAULT_BUFFER_SIZE = 8096;
        private static final int    TIME_OUT = 1000; // in milisec
        protected int               statusCode = 0;
        private String              url;

        public GetTextThread(String url){ this.url = url; }

        public void run() {
            try {
                Log.d(TAG, "url = " + url);
                URL url1 = new URL(url);

                HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
                // Consider moving get and others to final string constants
                connection.setRequestMethod("GET");
                connection.setReadTimeout(TIME_OUT);
                connection.setConnectTimeout(TIME_OUT);
                // Accept character data
                connection.setRequestProperty("Accept-Charset", "UTF-8");

                BufferedReader in =  null;
                try {
                    // Official connection
                    connection.connect();
                    statusCode = connection.getResponseCode();
                    if (statusCode / 100 != 2) {
                        // Failed
                        result.postValue("Failed, Status code = " + Integer.toString(statusCode));
                        return;
                    }

                    in = new BufferedReader(new InputStreamReader(connection.getInputStream()), DEFAULT_BUFFER_SIZE);

                    String myData;
                    StringBuffer sb = new StringBuffer();

                    while ( (myData = in.readLine()) != null ){
                        sb.append(myData);
                    }

                    result.postValue(sb.toString());

                } finally {
                    // Close resource
                    if (in != null){ in.close(); }
                    connection.disconnect();
                }

            } catch (Exception e){
                Log.d(TAG, e.toString());
                result.postValue(e.toString());
            }
        }
    }

    public class GetImageThread extends Thread{
        private static final String TAG = "GetImageThread";
        private static final int    DEFAULT_BUFFER_SIZE = 50;
        private static final int    NO_DATA = -1;
        private static final int    TIME_OUT = 1000; // in milisec
        private int                 statusCode = 0;
        private String              url;

        public GetImageThread(String url){ this.url = url; }

        public void run(){

            try {
                URL url1 = new URL(url);

                HttpURLConnection connection = (HttpURLConnection) url1.openConnection();

                 connection.setRequestMethod("GET");
                 connection.setReadTimeout(TIME_OUT);
                 connection.setConnectTimeout(TIME_OUT);

                 connection.connect();

                int statusCode = connection.getResponseCode();

                if (statusCode / 100 != 2) {
                    result.postValue("Failed! Statuscode returned is " + Integer.toString(statusCode));
                    return;
                }

                // Get streams
                InputStream is = connection.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);

                ByteArrayOutputStream baf = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);
                int current = 0;

                // Ensure stream bis gets closed
                try{
                    while( (current = bis.read()) != NO_DATA){
                        baf.write( (byte) current );
                    }
                    // Convert to bitmap
                    byte[] imageData = baf.toByteArray();
                    // Can only postValue from background thread, not setValue
                    bmp.postValue(BitmapFactory.decodeByteArray(imageData, 0, imageData.length));
                    result.postValue(url);
                } finally {
                    // Close resources
                    if (bis != null){ bis.close(); }
                }

            } catch (Exception e) {
                Log.d(TAG, e.toString());
                result.postValue(e.toString());
            }
        }
    }

}
