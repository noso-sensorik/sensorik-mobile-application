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


import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Beacon;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.BeaconType;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Event;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.EventTrigger;
import cz.msebera.android.httpclient.entity.StringEntity;

public class TrackingActivity extends AppCompatActivity implements View.OnClickListener {
    protected static final String TAG = "TrackingActivity";

    private ArrayList<IBeaconDevice> foundBeaconList;
    private ProximityManager proximityManager;

    private RestClientUsage restClient;

    private String department;
    private String job;

    private TrackingAdapter mTrackadapder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Get the Intent that started this activity and extract the strings
        Intent intent = getIntent();
        department = intent.getStringExtra(MainActivity.DEPARTMENT);
        job = intent.getStringExtra(MainActivity.JOB);

        Log.d(TAG, "test +" + department);

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

        restClient = new RestClientUsage();
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
                .deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(1));    //OnDeviceUpdate callback will be received with 5 seconds interval

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
                try {
                    restClient.getEvents();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
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

                // TODO: Replace with BeaconType-ENUM
                if(iBeacon.getMajor() != 3){
                    mTrackadapder.add(iBeacon);
                }

                // FIRE NEW EVENT
                Event beaconFound = new Event(LocalDate.now(), LocalTime.now(), department, job, EventTrigger.APPROACHING_STATIONARY_BED,
                        new Beacon(BeaconType.STATIONARY_BED, "TESSTTT FROM MOBILE APP", "Smart Beacon SB18-3", "Kontakt.IO", "Labor", iBeacon.getAddress()));

                JSONObject rp = new JSONObject();
                try {
                    // Details for the Event
                    rp.put("date", LocalDate.now().toString());
                    rp.put("time", LocalTime.now().toString());
                    rp.put("station", department);
                    rp.put("job", job);
                    rp.put("trigger", EventTrigger.APPROACHING_STATIONARY_BED.toString());

                    // Details on the eventsource (= the beacon)
                    JSONObject innerObj = new JSONObject();
                    innerObj.put("type", BeaconType.STATIONARY_BED );
                    innerObj.put("label", "TEEEEST");
                    innerObj.put("model", "Smart Beacon SB18-3");
                    innerObj.put("manufacturer", "Kontakt.IO");
                    innerObj.put("loc", "Labor");
                    innerObj.put("mac", iBeacon.getAddress());

                    rp.put("eventSource", innerObj);

                    restClient.postEvent(new StringEntity(rp.toString()));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
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

                    // TODO: Replace with BeaconType-ENUM
                    if(iBeacon.getMajor() != 3){
                        mTrackadapder.remove(iBeacon);
                        mTrackadapder.add(iBeacon);
                    }

                    // FIRE NEW EVENT
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

    private JSONObject generateEventJSON() {
        JSONObject jsonParams = new JSONObject();
        try{
            jsonParams.put("notes", "Test api support");
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return jsonParams;
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
