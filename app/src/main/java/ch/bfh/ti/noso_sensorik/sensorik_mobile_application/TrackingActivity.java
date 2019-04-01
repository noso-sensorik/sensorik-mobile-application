package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackingActivity extends AppCompatActivity implements View.OnClickListener {
    protected static final String TAG = "TrackingActivity";

    private ArrayList<IBeaconDevice> foundBeaconList;
    private ProximityManager proximityManager;

    private TrackingAdapter mTrackadapder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        String dep = intent.getStringExtra(MainActivity.DEPARTMENT);
        String job = intent.getStringExtra(MainActivity.JOB);

        Log.d(TAG, "test +" + dep);

        foundBeaconList = new ArrayList<IBeaconDevice>();

        setupButtons();
        setupProximityManager();
        startScanning();


        // Construct the data source
        ArrayList<IBeaconDevice> arrayOfBeacons = new ArrayList<IBeaconDevice>();

        // Create the adapter to convert the array to views
        mTrackadapder = new TrackingAdapter(this, arrayOfBeacons);

        // Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.listView_beaconstatus);
        listView.setAdapter(mTrackadapder);
    }

    private void setupButtons() {
        Button startScanButton = (Button) findViewById(R.id.button_pause);
        Button stopScanButton = (Button) findViewById(R.id.button_terminate);
        startScanButton.setOnClickListener(this);
        stopScanButton.setOnClickListener(this);
    }

    private void setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(this);

        //Configure proximity manager basic options
        proximityManager.configuration()
                .scanPeriod(ScanPeriod.RANGING)                                         //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanMode(ScanMode.BALANCED)                                            //Using BALANCED for best performance/battery ratio
                .deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(5));    //OnDeviceUpdate callback will be received with 5 seconds interval

        //Setting up iBeacon listeners
        proximityManager.setIBeaconListener(createIBeaconListener());
    }

    private void startScanning() {
        //Connect to scanning service and start scanning when ready
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                //Check if proximity manager is already scanning
                if (proximityManager.isScanning()) {
                    Toast.makeText(TrackingActivity.this, "Already scanning", Toast.LENGTH_SHORT).show();
                    return;
                }
                proximityManager.startScanning();
            }
        });
    }

    private void stopScanning() {
        //Stop scanning if scanning is in progress
        if (proximityManager.isScanning()) {
            proximityManager.stopScanning();
            Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_pause:
                //startScanning();
                //pauseScanning();
                break;
            case R.id.button_terminate:
                stopScanning();
                break;
        }
    }

    private IBeaconListener createIBeaconListener() {
        return new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.i(TAG, "onIBeaconDiscovered: " + iBeacon.toString());
                Log.i(TAG, "Name: " + iBeacon.getName() + ", Proximity: " +iBeacon.getProximity());
                foundBeaconList.add(iBeacon);
                mTrackadapder.add(iBeacon);
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                Log.i(TAG, "onIBeaconsUpdated: " + iBeacons.size());
                for (IBeaconDevice iBeacon: iBeacons) {
                    Log.i(TAG, "Name: " + iBeacon.getAddress() + ", Major: " + iBeacon.getMajor() + ", Proximity: " +iBeacon.getProximity());

                    if(foundBeaconList.contains(iBeacon)){
                        foundBeaconList.remove(iBeacon);
                        foundBeaconList.add(iBeacon);
                    } else {
                        foundBeaconList.add(iBeacon);
                    }

                    //FIXME: adapter does not update correctly, may need to delete the objs manually
                    mTrackadapder.remove(iBeacon);
                    mTrackadapder.add(iBeacon);
                }
            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.e(TAG, "onIBeaconLost: " + iBeacon.toString());
                foundBeaconList.remove(iBeacon);
                mTrackadapder.remove(iBeacon);
            }
        };
    }

    @Override
    protected void onStop() {
        //Stop scanning when leaving screen.
        stopScanning();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        //Remember to disconnect when finished.
        proximityManager.disconnect();
        super.onDestroy();
    }


}
