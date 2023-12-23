package com.example.canapp;

import static androidx.core.location.LocationManagerCompat.requestLocationUpdates;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;


public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener, View.OnClickListener, BaseGPSListenerInterface{

    BottomNavigationView bottomNavView;
    FloatingActionButton recordButton;
    boolean isRecord = false;
    String latitude, longitude, altitude;

    private static final int PERMISSION_LOCATION = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavView = findViewById(R.id.bottom_navigation_view);
        bottomNavView.setOnItemSelectedListener(this);

        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new TelemetryFragment()).commit(); //Initially display fragment 1

        recordButton = (FloatingActionButton) findViewById(R.id.play_stop_button);
        recordButton.setBackgroundResource(R.drawable.play_icon);
        recordButton.setOnClickListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;
        if(item.getItemId() == R.id.fragment_telemetry) {
            selectedFragment = new TelemetryFragment();
            String[] locationData = new String[] {"No Data Yet", "No Data Yet", "No Data Yet"};
            Bundle locationDataBundle = new Bundle();
            locationDataBundle.putStringArray("location_data", locationData);
            selectedFragment.setArguments(locationDataBundle);
        }

        if(item.getItemId() == R.id.fragment_video)
            selectedFragment = new VideoFragment();

        //Display the clicked fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();

        return true;
    }

    @Override
    public void onClick(View view) {
        if(!isRecord) {
            recordButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.play_icon));
            //not recording data or video
        } else {
            recordButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.pause_icon));
            //recording data and video... and also displaying all data on screen
            //Check for location permission
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
            } else {
                showLocation();
            }
        }
        isRecord = !isRecord;

        getCameraPermission();
        recordVideo();
    }

    @SuppressLint("MissingPermission")
    private void showLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //check if gps enabled
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } else {
            Toast.makeText(getApplicationContext(), "Enable GPS", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //update location
        latitude = Double.toString(location.getLatitude());
        longitude = Double.toString(location.getLongitude());
        altitude = Double.toString(location.getAltitude());
        //Toast.makeText(getApplicationContext(),latitude+" "+longitude+" "+altitude,Toast.LENGTH_SHORT).show();

        TelemetryFragment telemetryFragment= new TelemetryFragment();
        String[] locationData = new String[] {latitude, longitude, altitude};
        Bundle locationDataBundle = new Bundle();
        locationDataBundle.putStringArray("location_data", locationData);
        //Fragment telemetryFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_telemetry);
        telemetryFragment.setArguments(locationDataBundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, telemetryFragment);
        fragmentTransaction.commit();
    }

    @Override
    public void onProviderDisabled(String provider) {
        //empty
    }

    @Override
    public void onProviderEnabled(String provider) {
        //empty
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        //empty
    }

    @Override
    public void onGpsStatusChanged(int event) {
        //empty
    }

    private void getCameraPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 100);
        }
    }

    private void recordVideo() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE;
        startActivityForResult(intent, 101);
    }
}