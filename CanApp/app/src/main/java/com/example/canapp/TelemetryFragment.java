package com.example.canapp;

import static androidx.core.location.LocationManagerCompat.getCurrentLocation;

import android.location.LocationManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Map;

public class TelemetryFragment extends Fragment {
    String latitude, longitude;
    TextView textViewLatitude;
    TextView textViewLongitude;
    TextView textViewAltitude;

    public TelemetryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_telemetry, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textViewLatitude = (TextView) getView().findViewById(R.id.textview_latitude);
        textViewLongitude = (TextView) getView().findViewById(R.id.textview_longitude);
        textViewAltitude = (TextView) getView().findViewById(R.id.textview_altitude);

        Bundle locationDataBundle = this.getArguments();
        if(locationDataBundle != null) {
            String[] locationData = locationDataBundle.getStringArray("location_data");
            if(locationData != null) {
                textViewLatitude.setText(locationData[0]);
                textViewLongitude.setText(locationData[1]);
                textViewAltitude.setText(locationData[2]);
            }
        }
    }
}