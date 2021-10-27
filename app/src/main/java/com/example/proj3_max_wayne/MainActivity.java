package com.example.proj3_max_wayne;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // TODO make all of these private
    private final String TAG = "asdf Main Debug";
    // Persists across config changes
    private DataVM myVM;

    private ImageView iv;
    private TextView tvStatusCode;
    private TextView tvStatusMsg;
    private Spinner spinner;
    private String result;

    private ConnectivityCheck myCheck;

    SharedPreferences myPreference;
    SharedPreferences.OnSharedPreferenceChangeListener listener = null;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = findViewById(R.id.imageView1);
        tvStatusCode = findViewById(R.id.statusCode);
        tvStatusMsg = findViewById(R.id.statusMsg);

        // Set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Don't display title
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Check connectivity
        myCheck = new ConnectivityCheck(this);

        // Create ViewModel
        myVM = new ViewModelProvider(this).get(DataVM.class);

        // Set up preferences
        if (myPreference == null){
            myPreference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        }
        if (listener == null){
            listener = new SharedPreferences.OnSharedPreferenceChangeListener(){
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    Log.d(TAG, "on create preference");
                    myVM.getPrefValues(myPreference);
                    myVM.getJSON();
                }
            };
        }
        myPreference.registerOnSharedPreferenceChangeListener(listener);
        myVM.getPrefValues(myPreference);

        // Create observer to update UI image
        final Observer<Bitmap> bmpObserver = new Observer<Bitmap>() {
            @Override
            public void onChanged(@Nullable final Bitmap bitmap) {
                // Update UI
                iv.setImageBitmap(bitmap);
            }
        };
        // Observe the LiveData
        myVM.getbmp().observe(this, bmpObserver);

        // Create the observer which updates the UI with a toast
        final Observer<String> resultObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String result) {
                // Update the UI, in this case, a TextView.
                Toast.makeText(MainActivity.this,result,Toast.LENGTH_SHORT).show();
                Log.d(TAG, "onChanged listener = " + result);
                handleResults(result);
            }
        };
        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        myVM.getResult().observe(this,resultObserver);
        myVM.getJSON();
    }

    // Set up spinner
    private void setupSpinner(List<String> petNames){
        // Check if valid json received, if not, leave spinner empty
        Log.d(TAG, "Spinner set up first");
        Log.d(TAG, "Spinner set up " + petNames.toString());
        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.spinner_item, petNames);
        // Get reference to the spinner
        spinner = (Spinner) findViewById(R.id.spinner);
        // Bind adapter
        spinner.setAdapter(adapter);
        // On click listener
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public static final int SELECTED_ITEM = 0;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String name = (String) parent.getItemAtPosition(position);
                String file = myVM.getPetImg(name);
                Log.d(TAG, "Spinner clicked " + name + " " + file);
                myVM.getImage(file);
                if (parent.getChildAt(SELECTED_ITEM) != null){
                    ( (TextView) parent.getChildAt(SELECTED_ITEM) ).setTextColor(Color.WHITE);
                    Toast.makeText(MainActivity.this,(String) parent.getItemAtPosition(position),Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void handleResults(String result){
        // Test is json is valid through setImg links
        // if invalid, clear spinner, set scared cat background, set text
        // if valid, set up spinner
        List<String> petNames = myVM.setImgLinks(result);
        if (petNames.isEmpty()){
            if (spinner != null) {
                // Clear Spinner
                spinner.setAdapter(null);
            }
            Log.d(TAG, "Handle results empty array");
            // Reset background
            setErrorConnectionGUI(result);
        }
        else{
            Log.d(TAG, "Handle results not empty array");
            setTvsVisibility(false);
            setupSpinner(petNames);
        }
    }

    private boolean checkConnection(){
        return myCheck.isNetworkReachable() || myCheck.isWiFiReachable();
    }

    private void setErrorConnectionGUI(String result){
        Bitmap failedNetwork = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_scared_cat);
        iv.setImageResource(R.mipmap.ic_portrait_cat);
        if (!checkConnection()){
            tvStatusCode.setText(R.string.no_connection_code);
            tvStatusMsg.setText(R.string.no_connection_msg);
            setTvsVisibility(true);
            setTvsText(getString(R.string.no_connection_code), getString(R.string.no_connection_msg));

        }
        else{
            // Need to extract status code and exception message or link
            // Post status code
            // Post link (try and get full link with json or img file)
            Log.d(TAG, "Result error string : " + result);

        }
    }

    private void setTvsVisibility(Boolean visibile){
        if (visibile){
            tvStatusCode.setVisibility(View.VISIBLE);
            tvStatusMsg.setVisibility(View.VISIBLE);
        }
        else {
            tvStatusCode.setVisibility(View.INVISIBLE);
            tvStatusMsg.setVisibility(View.INVISIBLE);
        }
    }
    private void setTvsText(String statusCode, String statusMsg){
        tvStatusCode.setText(statusCode);
        tvStatusMsg.setText(statusMsg);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent myIntent = new Intent(this, SettingsActivity.class);
            startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}