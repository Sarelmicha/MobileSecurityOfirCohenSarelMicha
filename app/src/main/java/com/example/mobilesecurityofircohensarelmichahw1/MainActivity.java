package com.example.mobilesecurityofircohensarelmichahw1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.security.Permission;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity_Base {

    // Finals
    private final float MAX_STEPS = 5;
    private final float MAX_NOISE_VALUE = 28000;
    private final int REQUEST_CODE = 123;
    private final int MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE = 124;

    // Variables for step counter sensor.
    private float stepCounter = 0;
    private float initialStepCounter = 0;
    private boolean firstStep = true;
    private boolean isMicrophoneListening = false;
    private Sensor stepCounterSensor;
    private SensorManager sensorManager;
    private SensorEventListener sensorEventListener;


    private boolean isNoisy = false;
    private Timer timer;


    //Views
    private MaterialButton main_BTN_login;
    private EditText main_EDT_num_of_contacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
        initSensors();
        initListeners();
        tryLogin();
    }

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    }

    private void findViews() {
        main_BTN_login = findViewById(R.id.main_BTN_login);
        main_EDT_num_of_contacts = findViewById(R.id.main_EDT_num_of_contacts);
    }

    private void initListeners() {

        initSensorsListeners();
        initClickListeners();
    }

    private void initSensorsListeners() {

        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {

                if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                    if (firstStep) {
                        initialStepCounter = event.values[0];
                        firstStep = false;
                    }
                    stepCounter = event.values[0] - initialStepCounter;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(sensorEventListener, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL);

    }

    private void initClickListeners() {

        main_BTN_login.setOnClickListener(new View.OnClickListener() {
                                              @Override
                                              public void onClick(View v) {
                                                  tryLogin();
                                              }
                                          }
        );
    }

    private boolean checkPermission() {

        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
        ) {
            return true;
        }
        return false;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.READ_CONTACTS, Manifest.permission.RECORD_AUDIO
        }, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults.length == 0 ||
                            grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionWithRationaleCheck();
                        return;
                    }
                }

                isMicrophoneListening = true;
                tryLogin();


            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE) {
            isMicrophoneListening = true;
            tryLogin();
        }
    }

    private void requestPermissionWithRationaleCheck() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
            // Show user description for what we need the permission
           requestPermission();
        } else {
            openPermissionSettingDialog();
        }
    }

    private void openPermissionSettingDialog() {
        String message = "The Application needs some permission, please enable all permission required.";
        AlertDialog alertDialog =
                new AlertDialog.Builder(this)
                        .setMessage(message)
                        .setPositiveButton(getString(android.R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE);
                                        dialog.cancel();
                                    }
                                }).show();
        alertDialog.setCanceledOnTouchOutside(true);
    }


    private void tryLogin() {

        boolean isGranted = checkPermission();

        if (!isGranted) {
            requestPermission();
            return;
        }
        if(!isMicrophoneListening){
            isMicrophoneListening = true;
            getAmplitudeFromMicrophone(this);
        }

        if (main_EDT_num_of_contacts.getText().toString().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please Enter Number Of Contacts", Toast.LENGTH_SHORT).show();
            return;
        }

        if (getContactCount() < Integer.parseInt(main_EDT_num_of_contacts.getText().toString()) ||
                stepCounter < MAX_STEPS ||
                !isNoisy) {
            Toast.makeText(MainActivity.this, "Access Denied", Toast.LENGTH_SHORT).show();
            return;
        }

        navigateToVictoryActivity();
    }


    private void getAmplitudeFromMicrophone(Activity activity) {

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                isNoisy();
            }
        }, 0, 1000); //  5000 milliseconds before the first run, the interval - 1000 milliseconds.

    }

    public int getContactCount() {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        return cursor.getCount();
    }

    public void isNoisy() {
        boolean recorder = true;
        int noiseValue;

        int minSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        AudioRecord ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, minSize);


        short[] buffer = new short[minSize];

        ar.startRecording();
        while (recorder) {

            ar.read(buffer, 0, minSize);
            for (short s : buffer) {
                if (Math.abs(s) > MAX_NOISE_VALUE)   //DETECT VOLUME (IF I BLOW IN THE MIC)
                {
                    noiseValue = Math.abs(s);
                    ar.stop();
                    recorder = false;
                    isNoisy = true;
                }

            }
        }
    }

    private void navigateToVictoryActivity() {
        Intent intent = new Intent(MainActivity.this, VictoryActivity.class);
        startActivity(intent);
    }


}