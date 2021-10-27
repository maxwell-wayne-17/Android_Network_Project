package com.example.proj3_max_wayne;

import static android.provider.Settings.System.getString;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.Edits;
import android.util.Log;
import android.widget.Toast;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DataVM extends ViewModel {

    // TODO - make variables private and use getters

    // Consts for accessing json object
    private final String NAME = "name";
    private final String FILE = "file";
    // Will hold pet names as keys and their image file name as values
    // This will be utilized in the getImg call
    private HashMap<String,String> petsAndImgs; //= new HashMap<>();
    // Used for displaying error status code
    private int vMStatusCode;

    // Must get from settings
    String link;
    private final String URL_PREF_KEY = "url_preference";
    private final String DEFAULT_URL = "https://www.pcs.cnu.edu/~kperkins/pets/";
    private final String TAG = "DataVM Debug";

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
        link = settings.getString(URL_PREF_KEY,DEFAULT_URL);
        Log.d(TAG, link);
    }

    public void getJSON(){
        String jsonLink = link + "pets.json";
        txtThread = new GetTextThread(jsonLink);
        txtThread.start();
        Log.d(TAG, "getJSON result = " + getResult().toString());
    }

    public List<String> setImgLinks(String jsonStr){
        //boolean check = true;
        List<String> petNames = new ArrayList<>();
        petsAndImgs = new HashMap<String, String>();
        try {
            JSONObject jsonObj = new JSONObject(jsonStr);
            JSONArray jsonArray = jsonObj.getJSONArray("pets");
            // Iterate through json array
            // Put pet name in petNames
            // Put pet name as key and img file name as value in petsAndImgs
            for (int i = 0; i < jsonArray.length(); i++){
                JSONObject pet = jsonArray.getJSONObject(i);
                String name = (String) pet.get(NAME);
                String file = (String) pet.get(FILE);
                petNames.add(name);
                petsAndImgs.put(name, file);

                Log.d(TAG, "setImgLinks : " + name + " " + file);
            }
        }catch (Exception e) {
            Log.d(TAG, "setImgLinks : " + e.toString());;
        }
        return petNames;
    }

    public String getPetImg(String petName){
        if (petsAndImgs.containsKey(petName)){
            return petsAndImgs.get(petName);
        }
        else{
            return "ERROR - Pet name " + petName + " not in hashmap";
        }
    }

    public void getImage(String file){
        String imgUrl = link + file;
        Log.d(TAG, "getimg link = " + imgUrl);
        imgThread = new GetImageThread(imgUrl);
        imgThread.start();
    }

    private void setVmStatusCode(int statusCode){ vMStatusCode = statusCode;  }
    public int getVmStatusCode(){ return vMStatusCode; }

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
                    setVmStatusCode(statusCode);
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
                setVmStatusCode(statusCode);

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
                    //result.postValue(url);
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
