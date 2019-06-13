package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.SecureProfileListener;
import com.kontakt.sdk.android.ble.spec.Acceleration;
import com.kontakt.sdk.android.ble.spec.KontaktTelemetry;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.ISecureProfile;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Beacon;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.DCN;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Event;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Scrubbottle;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.util.RestClientUsage;
import cz.msebera.android.httpclient.entity.StringEntity;

public class TrackingActivity extends AppCompatActivity implements View.OnClickListener {
    protected static final String TAG = "TrackingActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 100;

    private Intent serviceIntent;
    private Intent mServiceIntent;
    private TrackingService trackingService;
    private Context ctx;
    private boolean isServiceStarted = false;

    protected PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: initialize tracking...");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // initialize receiver
//        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
//        filter.addAction(Intent.ACTION_SCREEN_OFF);
//        BroadcastReceiver mReceiver = new TrackingRestarterBroadcastReceiver();
//        registerReceiver(mReceiver, filter);

        /* This code together with the one in onDestroy()
         * will make the screen be always on until this Activity gets destroyed. */
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "noso_sensorik:sensorik_mobile");
        this.mWakeLock.acquire();

        initChannels(this);
        setupButtons();
        ctx = this;

//        trackingService = new TrackingService(this.ctx);
        mServiceIntent = new Intent(this.ctx, TrackingService.class);
        if (!isMyServiceRunning(TrackingService.class)) {
            mServiceIntent.putExtra("DEPARTMENT", getIntent().getStringExtra(MainActivity.DEPARTMENT));
            mServiceIntent.putExtra("JOB", getIntent().getStringExtra(MainActivity.JOB));
            mServiceIntent.putExtra("SCRUBBOTTLE", getIntent().getStringExtra(MainActivity.SCRUBBOTTLE));
            mServiceIntent.putExtra("IMEI", getIMEI());
            Log.d(TAG, "onCreate(): IMEI is " + getIMEI());
            startService(mServiceIntent);
        }

        // start service
//        serviceIntent = new Intent(this, TrackingService.class);
//        serviceIntent.putExtra("DEPARTMENT", getIntent().getStringExtra(MainActivity.DEPARTMENT));
//        serviceIntent.putExtra("JOB", getIntent().getStringExtra(MainActivity.JOB));
//        serviceIntent.putExtra("SCRUBBOTTLE", getIntent().getStringExtra(MainActivity.SCRUBBOTTLE));
//        serviceIntent.putExtra("IMEI", getIMEI());

//        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE);
//        if(!isServiceStarted){
//            startForegroundService(serviceIntent);
//            isServiceStarted = true;
//        }

        Log.d(TAG, "onCreate: tracking initialized");
    }

    public void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
//        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
//        NotificationChannel channel = new NotificationChannel("default","Hygiene Events Channel", NotificationManager.IMPORTANCE_HIGH);
//        channel.setDescription("Notification Channel for Hygiene Events");
//        notificationManager.createNotificationChannel(channel);
    }

    private String getIMEI(){
        String serviceName = Context.TELEPHONY_SERVICE;
        TelephonyManager m_telephonyManager = (TelephonyManager) getSystemService(serviceName);

        int checkSelfPermissionResultPhone = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResultPhone) {
            //Permission not granted so we ask for it. Results are handled in onRequestPermissionsResult() callback.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_CODE_PERMISSIONS);
        }
        return m_telephonyManager.getDeviceId();
    }

    /**
     * Prepares the start/stop buttons
     */
    private void setupButtons() {
        Button startScanButton = (Button) findViewById(R.id.button_pause);
        Button stopScanButton = (Button) findViewById(R.id.button_terminate);
        startScanButton.setOnClickListener(this);
        stopScanButton.setOnClickListener(this);
    }

    @Override
    protected void onStart(){
        Log.d(TAG, "onStart: start tracking...");
        super.onStart();

//        if(!isServiceStarted){
//            startForegroundService(serviceIntent);
//            isServiceStarted = true;
//        }

        Log.d(TAG, "onStart: tracking started");
    }

    @Override
    protected void onResume(){
        Log.d(TAG, "onResume: resume tracking...");
        super.onResume();

//        if(!isServiceStarted){
//            startForegroundService(serviceIntent);
//            isServiceStarted = true;
//        }
        // only when screen turns on
//        if (!TrackingRestarterBroadcastReceiver.wasScreenOn) {
//            // this is when onResume() is called due to a screen state change
//            System.out.println("SCREEN TURNED ON");
//        } else {
//            // this is when onResume() is called when the screen state has not changed
//        }
        super.onResume();

        Log.d(TAG, "onResume: tracking resumed");
    }

    @Override
    protected void onPause(){
        Log.d(TAG, "onPause: pause tracking...");
        super.onPause();

        // when the screen is about to turn off
//        if (TrackingRestarterBroadcastReceiver.wasScreenOn) {
//            // this is the case when onPause() is called by the system due to a screen state change
//            System.out.println("SCREEN TURNED OFF");
//            this.startService(new Intent(this, TrackingService.class));;
//
//        } else {
//            // this is when onPause() is called when the screen state has not changed
//        }

        Log.d(TAG, "onPause: tracking paused");
    }

    @Override
    protected void onStop(){
        Log.d(TAG, "onStop: stopping tracking...");
        super.onStop();

        Log.d(TAG, "onStop: tracking stopped");
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: destroy tracking...");
        super.onDestroy();

        this.mWakeLock.release();
        stopService(mServiceIntent);

        Log.d(TAG, "onDestroy: tracking destroyed");
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i ("isMyServiceRunning?", true+"");
                return true;
            }
        }
        Log.i ("isMyServiceRunning?", false+"");
        return false;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param view The view that was clicked.
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_pause:
                if(isServiceStarted){
//                    mService.startScanning();
                } else {
//                    mService.stopScanning();
                }
                break;
            case R.id.button_terminate:
                stopService(serviceIntent);
                // redirect to evaluation screen
                Intent intent = new Intent(this, EvaluationActivity.class);
                //HINT: do I need to pass all the collected events to the evaluation class?
//                intent.putExtra(DEPARTMENT, department);
                startActivity(intent);
                break;
        }
    }
}
