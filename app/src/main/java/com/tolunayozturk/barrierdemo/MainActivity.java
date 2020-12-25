package com.tolunayozturk.barrierdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.hms.kit.awareness.Awareness;
import com.huawei.hms.kit.awareness.barrier.AwarenessBarrier;
import com.huawei.hms.kit.awareness.barrier.BarrierStatus;
import com.huawei.hms.kit.awareness.barrier.BarrierUpdateRequest;
import com.huawei.hms.kit.awareness.barrier.LocationBarrier;


import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String ENTER_BARRIER_LABEL = "ENTER_BARRIER_LABEL";
    private static final String EXIT_BARRIER_LABEL = "EXIT_BARRIER_LABEL";
    private static final int PERMISSION_REQUEST_CODE = 820;

    private final String[] mPermissionsOnHigherVersion = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION};
    private final String[] mPermissionsOnLowerVersion = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION};


    private PendingIntent mPendingIntent;
    private LocationBarrierReceiver mBarrierReceiver;

    double latitude = 41.02456;
    double longitude = 28.85843;
    double radius = 300;

    Chronometer mChronometer;
    TextView tv_log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_log = findViewById(R.id.tv_log);
        mChronometer = findViewById(R.id.chronometer);

        final String barrierReceiverAction = getApplication().getPackageName() + "LOCATION_BARRIER_RECEIVER_ACTION";
        Intent intent = new Intent(barrierReceiverAction);
        mPendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        mBarrierReceiver = new LocationBarrierReceiver();
        registerReceiver(mBarrierReceiver, new IntentFilter(barrierReceiverAction));

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        List<String> deniedPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (String permission : mPermissionsOnHigherVersion) {
                if (ActivityCompat.checkSelfPermission(this, permission) !=
                        PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permission);
                }
            }
        } else {
            for (String permission : mPermissionsOnLowerVersion) {
                if (ActivityCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permission);
                }
            }
        }

        if (deniedPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this,
                    deniedPermissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            AwarenessBarrier enterBarrier = LocationBarrier.enter(latitude, longitude, radius);
            addBarrier(this, ENTER_BARRIER_LABEL, enterBarrier, mPendingIntent);

            AwarenessBarrier exitBarrier = LocationBarrier.enter(latitude, longitude, radius);
            addBarrier(this, EXIT_BARRIER_LABEL, exitBarrier, mPendingIntent);

            Awareness.getCaptureClient(this).getCurrentLocation()
                    .addOnSuccessListener(locationResponse -> {
                        Location origin = new Location("");
                        origin.setLatitude(latitude);
                        origin.setLongitude(longitude);

                        double dist = locationResponse.getLocation().distanceTo(origin);
                        printLog("Distance: " + dist);
                    }).addOnFailureListener(e -> Log.e(TAG, e.getMessage(), e));
        }
    }

    private void addBarrier(Context context, final String label,
                            AwarenessBarrier barrier, PendingIntent pendingIntent) {
        BarrierUpdateRequest.Builder builder = new BarrierUpdateRequest.Builder();
        BarrierUpdateRequest request = builder.addBarrier(label, barrier, pendingIntent).build();
        Awareness.getBarrierClient(context).updateBarriers(request)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "add barrier " + label + " success",
                            Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "add barrier success");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "add barrier " + label + " success",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "add barrier failed", e);
                });
    }

    private void printLog(String msg) {
        DateFormat formatter = SimpleDateFormat.getDateTimeInstance();
        String time = formatter.format(new Date(System.currentTimeMillis()));
        tv_log.append("[" + time + "] " + msg + "\n");
    }

    @Override
    protected void onDestroy() {
        if (mBarrierReceiver != null) {
            unregisterReceiver(mBarrierReceiver);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean isPermissionDenied = false;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_DENIED) {
                    isPermissionDenied = true;
                }
            }

            if (isPermissionDenied) {
                Toast.makeText(this, "PERMISSION DENIED", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "PERMISSION GRANTED", Toast.LENGTH_LONG).show();

                @SuppressLint("MissingPermission") AwarenessBarrier enterBarrier = LocationBarrier.enter(latitude, longitude, radius);
                addBarrier(this, ENTER_BARRIER_LABEL, enterBarrier, mPendingIntent);

                @SuppressLint("MissingPermission") AwarenessBarrier exitBarrier = LocationBarrier.enter(latitude, longitude, radius);
                addBarrier(this, EXIT_BARRIER_LABEL, exitBarrier, mPendingIntent);
            }
        }
    }

    final class LocationBarrierReceiver extends BroadcastReceiver {
        private final String TAG = LocationBarrierReceiver.class.getSimpleName();

        @Override
        public void onReceive(Context context, Intent intent) {
            BarrierStatus barrierStatus = BarrierStatus.extract(intent);
            String label = barrierStatus.getBarrierLabel();
            switch (barrierStatus.getPresentStatus()) {
                case BarrierStatus.TRUE:
                    Log.i(TAG, label + " status:true" + barrierStatus.getLastBarrierUpdateTime());
                    printLog(label + " status:true");

                    if (label.equals("ENTER_BARRIER_LABEL")) {
                        mChronometer.setBase(SystemClock.elapsedRealtime());
                        mChronometer.start();
                    } else if (label.equals("EXIT_BARRIER_LABEL")) {
                        mChronometer.stop();
                        double elapsedMillis = SystemClock.elapsedRealtime() - mChronometer.getBase();
                        printLog("Length of stay: " + elapsedMillis / 1000 + " seconds");
                    }
                    break;
                case BarrierStatus.FALSE:
                    Log.i(TAG, label + " status:false");
                    printLog(label + " status:false");
                    break;
                case BarrierStatus.UNKNOWN:
                    Log.i(TAG, label + " status:unknown");
                    printLog(label + " status:unknown");
                    break;
            }
        }
    }
}