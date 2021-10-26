package com.example.proj3_max_wayne;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.proj3_max_wayne.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    // TODO make all of these private
    private final String TAG = "asdf Main Debug";
    // Persists across config changes
    DataVM myVM;
    ImageView iv;
    private Spinner spinner;
    private String result;

    // Preference, default to cnu site
    String jsonLink;
    SharedPreferences myPreference;
    SharedPreferences.OnSharedPreferenceChangeListener listener = null;


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iv = findViewById(R.id.imageView1);

        // Set up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Don't display title
        getSupportActionBar().setDisplayShowTitleEnabled(false);

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
                    //setupSpinner();
                }
            };
        }
        myPreference.registerOnSharedPreferenceChangeListener(listener);
        myVM.getPrefValues(myPreference);
        // Always set preference on create
        //myVM.getPrefValues(myPreference);
        //Log.d(TAG, "On create spinner set up call");
        //setupSpinner();

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
                //validPets = myVM.setImgLinks(result);
                // Save results in this class

                MainActivity.this.result = result;
                //setupSpinner(result);
                handleResults(result);
            }
        };
        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        myVM.getResult().observe(this,resultObserver);
        myVM.getJSON();
    }

    // Set up spinner
    // TODO currently getting called multiple times.  Needs to be set up first in oncreate.  Should track preference changes
    private void setupSpinner(List<String> petNames){
        // Make sure view model link is up to date
//        myVM.getPrefValues(myPreference);
//        // Get json into results
//        myVM.getJSON();
        // Check if valid json received, if not, leave spinner empty
        Log.d(TAG, "Spinner set up first");
        // Get array from view model
//        List<String> petNames = myVM.setImgLinks(result);
//        // If array is empty, then there was not a valid json passed in, do not set up spinner
//        if (petNames.isEmpty()){
//            spinner = null;
//            Log.d(TAG, "list is empty");
//            return;
//        }
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
            Bitmap failedNetwork = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_scared_cat);
            iv.setImageResource(R.mipmap.ic_portrait_cat);
            // TODO add textveiw
        }
        else{
            Log.d(TAG, "Handle results not empty array");
            setupSpinner(petNames);
        }
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