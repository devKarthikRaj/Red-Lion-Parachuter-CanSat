package com.example.canapp;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.video.MediaStoreOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import com.google.common.util.concurrent.ListenableFuture;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, BaseGPSListenerInterface, SurfaceHolder.Callback{
    //Telemetry Vars
    Button playStopButton;
    String latitude, longitude, altitude;
    private static final int PERMISSION_LOCATION = 1000;
    TextView textViewLatitude;
    TextView textViewLongitude;
    TextView textViewAltitude;
    boolean isRecordTelemetry = true;
    private static final int REQUEST_PERMISSION_CODE = 100;

    //Video Vars
    ExecutorService service;
    Recording recording = null;
    VideoCapture<Recorder> videoCapture = null;
    ImageButton capture, toggleFlash, flipCamera;
    PreviewView previewView;
    int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private final ActivityResultLauncher<String> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera(cameraFacing);
        }
    });

    //Comms Vars
    Button armDisarmButton;
    boolean isArm = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewLatitude = findViewById(R.id.textview_latitude);
        textViewLongitude = findViewById(R.id.textview_longitude);
        textViewAltitude = findViewById(R.id.textview_altitude);

        textViewLatitude.setText("Waiting For Data");
        textViewLongitude.setText("Waiting For Data");
        textViewAltitude.setText("Waiting For Data");

        playStopButton = findViewById(R.id.play_stop_button);
        playStopButton.setText("RECORD TELEMETRY");
        playStopButton.setOnClickListener(this);

        previewView = findViewById(R.id.viewFinder);
        capture = findViewById(R.id.capture);
        toggleFlash = findViewById(R.id.toggleFlash);
        flipCamera = findViewById(R.id.flipCamera);
        capture.setOnClickListener(view -> {
            requestPermissions();
            captureVideo();
        });
        startCamera(cameraFacing);

        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                    cameraFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    cameraFacing = CameraSelector.LENS_FACING_BACK;
                }
                startCamera(cameraFacing);
            }
        });

        service = Executors.newSingleThreadExecutor();

        armDisarmButton = findViewById(R.id.arm_disarm_button);
        armDisarmButton.setText("ARM");
        armDisarmButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.play_stop_button) {
            if (!isRecordTelemetry) {
                playStopButton.setText("RECORD TELEMETRY");
                //not recording data or video
                textViewLatitude.setText("Waiting For Data");
                textViewLongitude.setText("Waiting For Data");
                textViewAltitude.setText("Waiting For Data");
            } else {
                playStopButton.setText("STOP TELEMETRY");
                //recording data and video... and also displaying all data on screen
                //Check for location permission
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_LOCATION);
                } else {
                    showLocation();
                }
            }
            isRecordTelemetry = !isRecordTelemetry;
        } else if(view.getId() == R.id.arm_disarm_button) {
            if(!isArm) {
                //arm and start recording lat long alt to csv and sending via wifi to esp32
                armDisarmButton.setText("DISARM");
            } else {
                //disarm and stop recording lat long alt to csv and sending via wifi to esp32
                armDisarmButton.setText("ARM");
            }
            isArm = !isArm;
        }
    }

    @SuppressLint("MissingPermission")
    private void showLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //check if gps enabled
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
        } else {
            Toast.makeText(getApplicationContext(), "Enable GPS", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(!isRecordTelemetry) {
            //update location
            latitude = Double.toString(location.getLatitude());
            textViewLatitude.setText(latitude);
            longitude = Double.toString(location.getLongitude());
            textViewLongitude.setText(longitude);
            altitude = Double.toString(location.getAltitude());
            textViewAltitude.setText(altitude);
            //Toast.makeText(getApplicationContext(),latitude+" "+longitude+" "+altitude,Toast.LENGTH_SHORT).show();
        } else {
            textViewLatitude.setText("Waiting For Data");
            textViewLongitude.setText("Waiting For Data");
            textViewAltitude.setText("Waiting For Data");
        }
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

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        //empty
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        //empty
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
        //empty
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.MANAGE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
        } else {
            Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
                // Start video recording or perform the required action
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void captureVideo() {
        capture.setImageResource(R.drawable.round_stop_circle_24);
        Recording recording1 = recording;
        if (recording1 != null) {
            recording1.stop();
            recording = null;
            return;
        }
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video");

        MediaStoreOutputOptions options = new MediaStoreOutputOptions.Builder(getContentResolver(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recording = videoCapture.getOutput().prepareRecording(MainActivity.this, options).withAudioEnabled().start(ContextCompat.getMainExecutor(MainActivity.this), videoRecordEvent -> {
            if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                capture.setEnabled(true);
            } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                if (!((VideoRecordEvent.Finalize) videoRecordEvent).hasError()) {
                    String msg = "Video capture succeeded: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getOutputResults().getOutputUri();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                } else {
                    recording.close();
                    recording = null;
                    String msg = "Error: " + ((VideoRecordEvent.Finalize) videoRecordEvent).getError();
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
                capture.setImageResource(R.drawable.round_fiber_manual_record_24);
            }
        });
    }

    public void startCamera(int cameraFacing) {
        ListenableFuture<ProcessCameraProvider> processCameraProvider = ProcessCameraProvider.getInstance(MainActivity.this);

        processCameraProvider.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = processCameraProvider.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Recorder recorder = new Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);

                cameraProvider.unbindAll();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(cameraFacing).build();

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture);

                toggleFlash.setOnClickListener(view -> toggleFlash(camera));
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(MainActivity.this));
    }

    private void toggleFlash(Camera camera) {
        if (camera.getCameraInfo().hasFlashUnit()) {
            if (camera.getCameraInfo().getTorchState().getValue() == 0) {
                camera.getCameraControl().enableTorch(true);
                toggleFlash.setImageResource(R.drawable.round_flash_off_24);
            } else {
                camera.getCameraControl().enableTorch(false);
                toggleFlash.setImageResource(R.drawable.round_flash_on_24);
            }
        } else {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Flash is not available currently", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        service.shutdown();
    }
}