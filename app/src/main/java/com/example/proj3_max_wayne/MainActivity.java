package com.example.proj3_max_wayne;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "HapyHaloweenDr.Perkins!";
    // Persists across config changes
    private DataVM myVM;

    private ImageView iv;
    private TextView tvStatusCode;
    private TextView tvStatusMsg;
    private Spinner spinner;

    private ConnectivityCheck myCheck;

    private SharedPreferences myPreference;
    private SharedPreferences.OnSharedPreferenceChangeListener listener = null;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Set reference to widgets
        iv = findViewById(R.id.imageView1);
        tvStatusCode = findViewById(R.id.statusCode);
        tvStatusMsg = findViewById(R.id.statusMsg);
        spinner = findViewById(R.id.spinner);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Don't display title
        if (getSupportActionBar() != null)
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
            listener = (sharedPreferences, key) -> {
                Log.d(TAG, "on create preference");
                myVM.getPrefValues(myPreference);
                myVM.getJSON();
            };
        }
        myPreference.registerOnSharedPreferenceChangeListener(listener);
        myVM.getPrefValues(myPreference);

        // Create observer to update UI image
        final Observer<Bitmap> bmpObserver = bitmap -> {
            // Update UI Image with animation
            imageViewAnimatedChange(getApplicationContext(), iv, bitmap);
        };
        // Observe the LiveData
        myVM.getbmp().observe(this, bmpObserver);

        // Create the observer which updates the UI
        final Observer<String> resultObserver = result -> {
            // Update the UI
            Log.d(TAG, "onChanged listener = " + result);
            handleResults(result);
        };
        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        myVM.getResult().observe(this,resultObserver);
        myVM.getJSON();
    }

    // Add animation to changing image
    private void imageViewAnimatedChange(Context c, final ImageView iv, final Bitmap new_image){
        final Animation anim_out = AnimationUtils.loadAnimation(c, android.R.anim.fade_out);
        final Animation anim_in = AnimationUtils.loadAnimation(c, android.R.anim.fade_in);
        anim_out.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                iv.setImageBitmap(new_image);
                anim_in.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {}
                    @Override
                    public void onAnimationEnd(Animation animation) {}
                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                iv.startAnimation(anim_in);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        iv.startAnimation(anim_out);
    }

    // Set up spinner
    private void setupSpinner(List<String> petNames){
        // Create adapter
        ArrayAdapter<String> adapter = new ArrayAdapter(this, R.layout.spinner_item, petNames);
        spinner.setVisibility(View.VISIBLE);
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
        // Set up new image view, no animation... this cat should jump at you
        iv.setImageResource(R.drawable.funny_cat2);
        iv.setScaleType(ImageView.ScaleType.FIT_XY);

        setTvsVisibility(true);
        spinner.setVisibility(View.INVISIBLE);
        if (!checkConnection()){
            setTvsText(getString(R.string.no_connection_code), getString(R.string.no_connection_msg));
        }
        else{
            // Need to extract status code and exception message or link
            // Post status code
            // Post link (try and get full link with json or img file)
            Log.d(TAG, "Result error string : " + result);
            setTvsText(Integer.toString(myVM.getVmStatusCode()), result);
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