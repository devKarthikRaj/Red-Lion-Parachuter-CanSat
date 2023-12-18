package com.example.canapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener, View.OnClickListener{

    BottomNavigationView bottomNavView;
    FloatingActionButton recordButton;
    boolean isRecord = false;

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
        if(item.getItemId() == R.id.fragment_telemetry)
            selectedFragment = new TelemetryFragment();
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
            //not recording data or video... Just reading and displaying on screen
        } else {
            recordButton.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.pause_icon));
            //recording data and video... and also reading and displaying on screen
        }
        isRecord = !isRecord;
    }
}