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
import com.kontakt.sdk.android.ble.discovery.ibeacon.IBeaconDeviceEvent;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilter;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilters;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.SecureProfileListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleSecureProfileListener;
import com.kontakt.sdk.android.ble.spec.Acceleration;
import com.kontakt.sdk.android.ble.spec.KontaktTelemetry;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.ISecureProfile;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Beacon;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.BeaconType;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Event;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.EventTrigger;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.util.BeaconListener;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.util.RestClientUsage;
import cz.msebera.android.httpclient.entity.StringEntity;

public class TrackingActivity extends AppCompatActivity implements View.OnClickListener {
    protected static final String TAG = "TrackingActivity";

    private ProximityManager proximityManager;
    private boolean isScanning = false;

    private RestClientUsage restClient;

    private String department;
    private String job;

//    private TrackingAdapter mTrackadapder;
    private TrackingEventAdapter mTrackadapder;
    private ArrayList<Event> eventList;
    private ArrayList<IBeaconDevice> foundBeaconList;

    private ArrayList<IBeaconDevice> arrayOfBeacons;
    private ArrayList<IBeaconDevice> currentlyKnownBeacons;
    private ArrayList<Integer> currentlyKnownMinors;

    private String AssetTagName = "ntZ2co";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: initialize tracking...");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        // Get the Intent that started this activity and extract the strings
        Intent intent = getIntent();
        department = intent.getStringExtra(MainActivity.DEPARTMENT);
        job = intent.getStringExtra(MainActivity.JOB);

        foundBeaconList = new ArrayList<IBeaconDevice>();
        currentlyKnownBeacons = new ArrayList<IBeaconDevice>();

        setupButtons();
        setupProximityManager();
        startScanning();

        // Construct the data source
        arrayOfBeacons = new ArrayList<IBeaconDevice>();
        eventList = new ArrayList<Event>();

        // Create the adapter to convert the array to views
//        mTrackadapder = new TrackingAdapter(this, arrayOfBeacons);
        mTrackadapder = new TrackingEventAdapter(this, eventList);


        // Attach the adapter to a ListView
        ListView listView = (ListView) findViewById(R.id.listView_beaconstatus);
        listView.setAdapter(mTrackadapder);

        restClient = new RestClientUsage();
        Log.d(TAG, "onCreate: tracking initialized");
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

    /**
     * Prepares the proximity manager
     */
    private void setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(this);

        //Configure proximity manager basic options
        proximityManager.configuration()
                .scanPeriod(ScanPeriod.RANGING)                                         //Using ranging for continuous scanning or MONITORING for scanning with intervals
                .scanMode(ScanMode.LOW_LATENCY)                                          //Using BALANCED for best performance/battery ratio
                .deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(1));    //OnDeviceUpdate callback will be received with 1 seconds interval

        //Setting up iBeacon listeners
        //proximityManager.setIBeaconListener(new BeaconListener());
        proximityManager.setIBeaconListener(createIBeaconListener());
        proximityManager.setSecureProfileListener(createSecureProfileListener());
        proximityManager.filters().iBeaconFilter(IBeaconFilters.newMinorFilter(2010));

//        proximityManager.filters().iBeaconFilter(IBeaconFilters.newMajorFilter(3));
    }

    /**
     * Connect to scanning service and start scanning when ready.
     * Also show a UI text hint.
     */
    private void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                //Check if proximity manager is already scanning
                if (proximityManager.isScanning()) {
                    Toast.makeText(TrackingActivity.this, "Bereits am scannen", Toast.LENGTH_SHORT).show();
                    return;
                }
                proximityManager.startScanning();
                isScanning = true;

                Button pauseButton = (Button) findViewById(R.id.button_pause);
                pauseButton.setText("Scannen unterbrechen");
                Toast.makeText(TrackingActivity.this, "Scanning gestartet", Toast.LENGTH_SHORT).show();

                // FIXME: currently, if we continue a previously started scan any new results just get appended instead of existing results being updated
            }
        });

    }

    /**
     * Stop scanning if scanning is in progress
     */
    private void stopScanning() {
        if (proximityManager.isScanning()) {
            proximityManager.stopScanning();
            isScanning = false;
            Button pauseButton = (Button) findViewById(R.id.button_pause);
            pauseButton.setText("Scannen weiterführen");
            Toast.makeText(this, "Scannen gestopt", Toast.LENGTH_SHORT).show();
        }
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

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_pause:
                if(isScanning){
                    stopScanning();
                } else {
                    startScanning();
                }
//                    try {
//                        restClient.getEvents();
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                break;
            case R.id.button_terminate:
                stopScanning();
                // redirect to evaluation screen
                Intent intent = new Intent(this, EvaluationActivity.class);
                //HINT: do I need to pass all the collected events to the evaluation class?
//                intent.putExtra(DEPARTMENT, department);
                startActivity(intent);
                break;
        }
    }

    // ----------------------

    private SecureProfileListener createSecureProfileListener(){
        return new SecureProfileListener() {
            @Override
            public void onProfileDiscovered(ISecureProfile profile) {

                if((profile.getUniqueId() != null) && !(profile.getUniqueId().isEmpty())){
                    Log.i(TAG, "onProfileDiscovered: Name: " + profile.getName() + ", Address: " +profile.getMacAddress() + ", Unique ID: '" + profile.getUniqueId() +"'");
                    if((profile.getUniqueId().equals("ttOIcr"))){
                        // Extract telemetry
                        KontaktTelemetry telemetry = profile.getTelemetry();
                        if (telemetry != null) {
                            // Get acceleration, temperature, light level and device time
                            Acceleration acceleration = telemetry.getAcceleration();
                            int temperature = telemetry.getTemperature();
                            int lightLevel = telemetry.getLightSensor();
                            int deviceTime = telemetry.getTimestamp();
                            if(deviceTime != 0){
                                Log.i(TAG, "onProfileDiscovered: last threshold - "+telemetry.getLastThreshold() + " timestamp: '" + telemetry.getTimestamp()+"'");
                            }
//                            if(acceleration != null){
//                                Log.i(TAG, "onProfileDiscovered: acceleration -  X: " + acceleration.getX() + " Y: " + acceleration.getY() + " Z: " + acceleration.getZ());
//                            }
                        }
                    }
                }
            }

            @Override
            public void onProfilesUpdated(List<ISecureProfile> profiles) {
                for (ISecureProfile profile : profiles) {
//                    Log.i(TAG, "onProfilesUpdated(): Name: " + profile.getName() + ", Address: " +profile.getMacAddress() + ", Unique ID: '" + profile.getUniqueId() +"'");
                    if((profile.getUniqueId() != null) && !(profile.getUniqueId().isEmpty())){
                        if((profile.getUniqueId().equals("ttOIcr"))){
                            Log.i(TAG, "onProfilesUpdated: Name: " + profile.getName() + ", Address: " +profile.getMacAddress() + ", Unique ID: '" + profile.getUniqueId() +"'");

                            // Extract telemetry
                            KontaktTelemetry telemetry = profile.getTelemetry();
                            if (telemetry != null) {
                                // Get acceleration, temperature, light level and device time
                                Acceleration acceleration = telemetry.getAcceleration();
                                int temperature = telemetry.getTemperature();
                                int lightLevel = telemetry.getLightSensor();
                                int deviceTime = telemetry.getTimestamp();
                                if(deviceTime != 0){
                                    Log.i(TAG, "onProfilesUpdated: last threshold - "+telemetry.getLastThreshold() + " timestamp: '" + telemetry.getTimestamp()+"'");
                                }
//                                if(acceleration != null){
//                                    Log.i(TAG, "onProfilesUpdated: acceleration -  X: " + acceleration.getX() + " Y: " + acceleration.getY() + " Z: " + acceleration.getZ());
//                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onProfileLost(ISecureProfile profile) {

            }
        };

    }


    // ------------------------------

    private IBeaconListener createIBeaconListener() {
        return new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.i(TAG, "onIBeaconDiscovered: new Beacon found");

                switch (iBeacon.getMajor()){
                    case 1:
                        handleDiscoveredPatZone(iBeacon);
                        break;
                    case 2:
                        handleDiscoveredStatDisp(iBeacon);
                        break;
                    case 3:
                        handleDiscoveredMobileScrubBottle(iBeacon);
                        break;
                    case 4:
                        handlesDiscoveredSemiStatDisp(iBeacon);
                        break;
                    default:
                        // none of the above matched...
                        break;
                }
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {
                Log.i(TAG, "onIBeaconsUpdated(): " + iBeacons.size());
                for (IBeaconDevice iBeacon: iBeacons) {

                    switch (iBeacon.getMajor()){
                        case 1:
                            handleUpdatedPatZone(iBeacon);
                            break;
                        case 2:
                            handleUpdatedStatDisp(iBeacon);
                            break;
                        case 3:
                            //handleUpdatedMobileScrubBottle(iBeacon);
                            break;
                        case 4:
                            //handleUpdatedSemiStatDisp(iBeacon);
                            break;
                        default:
                            // none of the above matched...
                            break;
                    }
                }
            }

            @Override
            public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.e(TAG, "onIBeaconLost: " + iBeacon.toString());
//                mTrackadapder.remove(iBeacon);

                switch (iBeacon.getMajor()){
                    case 1:
//                        handleLostPatZone(iBeacon);
                        break;
                    case 2:
                        handleLostStatDisp(iBeacon);
                        break;
                    case 3:
                        //handleLostMobileScrubBottle(iBeacon);
                        break;
                    case 4:
                        //handleLostSemiStatDisp(iBeacon);
                        break;
                    default:
                        // none of the above matched...
                        break;
                }
            }
        };
    }

    // ------------------

    private void handleDiscoveredPatZone(IBeaconDevice iBeacon) {
        Log.v(TAG, "handleDiscoveredPatZone(): enter for " + iBeacon.toString());

        Event newEvent;
        try {
            if(iBeacon.getProximity().equals(Proximity.NEAR)){ // we discovered a beacon nearby, fire event
                newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.APPROACHING_PATIENT_ZONE-1),
                        new Beacon((iBeacon.getMajor()-1), "(1) Test Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress())
                );

                Log.d(TAG, "handleDiscoveredPatZone(): logged event: " +newEvent.toString() );
                restClient.postEvent(new StringEntity(newEvent.toJSON().toString()));
//                eventList.add(newEvent);
                mTrackadapder.insert(newEvent,0);
            } else if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){

                newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.APPROACHING_PATIENT_ZONE-1),
                        new Beacon((iBeacon.getMajor()-1), "(2.1) Test Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress())
                );

                Log.d(TAG, "handleDiscoveredPatZone(): logged event: " +newEvent.toString() );
                restClient.postEvent(new StringEntity(newEvent.toJSON().toString()));
//                eventList.add(newEvent);
                mTrackadapder.insert(newEvent,0);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        Log.v(TAG, "handleDiscoveredPatZone(): leave");

    }

    private void handleUpdatedPatZone(IBeaconDevice iBeacon) {
        Log.v(TAG, "handleUpdatedPatZone(): enter for "+iBeacon.toString());

        if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){ // we discovered a beacon in immediate distance, fire event
            Event newEvent = new Event(
                    LocalDate.now(), LocalTime.now(), department, job, (Event.DIRECTLY_IN_PATIENT_ZONE-1),
                    new Beacon((iBeacon.getMajor()-1), "(2.2) Test Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress())
            );

            try {
                Log.d(TAG, "handleUpdatedPatZone(): logged event: " +newEvent.toString() );
                restClient.postEvent(new StringEntity(newEvent.toJSON().toString()));
//                eventList.add(newEvent);
                mTrackadapder.insert(newEvent,0);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.v(TAG, "handleUpdatedPatZone(): enter");

    }

    private void handleDiscoveredStatDisp(IBeaconDevice iBeacon) {
        Log.v(TAG, "handleDiscoveredStatDisp(): enter for " + iBeacon.toString());

        if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){ // we discovered a beacon in immediate distance, fire event
            Event newEvent = new Event(
                    LocalDate.now(), LocalTime.now(), department, job, (Event.APPROACHING_STATIONARY_DISPENSER-1),
                    new Beacon((iBeacon.getMajor()-1), "(4) Test Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress())
            );

            try {
                Log.d(TAG, "handleDiscoveredStatDisp(): logged event: " +newEvent.toString() );
                Log.d(TAG, "handleDiscoveredStatDisp(): logged event: " +newEvent.toJSON() );
                restClient.postEvent(new StringEntity(newEvent.toJSON().toString()));
//                eventList.add(newEvent);
                mTrackadapder.insert(newEvent, 0);
                currentlyKnownBeacons.add(iBeacon);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Log.v(TAG, "handleDiscoveredStatDisp(): leave");
    }

    private void handleUpdatedStatDisp(IBeaconDevice iBeacon) {
        Log.v(TAG, "handleUpdatedStatDisp(): enter for " + iBeacon.toString());

        if(! (currentlyKnownBeacons.contains(iBeacon)) ) {  // we don't already known the beacon
            if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){ // we discovered a beacon in immediate distance, fire event
                Event newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.APPROACHING_STATIONARY_DISPENSER-1),
                        new Beacon((iBeacon.getMajor()-1), "(4) Test Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress())
                );

                try {
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toString() );
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toJSON() );
                    restClient.postEvent(new StringEntity(newEvent.toJSON().toString()));
//                eventList.add(newEvent);
                    mTrackadapder.insert(newEvent, 0);
                    currentlyKnownBeacons.add(iBeacon);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else { // beacon already known and because we only add this kind of beacon when IMMEDIATE, it should be IMMEDIATE
            // if the proximity is NEAR instead of IMMEDIATE, then we are leaving the range and should "forget" the beacon
            if(iBeacon.getProximity().equals(Proximity.NEAR)) {
                Event newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.LEAVING_STATIONARY_DISPENSER-1),
                        new Beacon((iBeacon.getMajor()-1), "(5) Test Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress())
                );

                try {
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toString() );
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toJSON() );
                    restClient.postEvent(new StringEntity(newEvent.toJSON().toString()));
//                eventList.add(newEvent);
                    mTrackadapder.insert(newEvent, 0);
                    currentlyKnownBeacons.remove(iBeacon);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        Log.v(TAG, "handleUpdatedStatDisp(): leave");
    }

    private void handleLostStatDisp(IBeaconDevice iBeacon){
        Log.v(TAG, "handleLostStatDisp(): enter for " + iBeacon.toString());



        Log.v(TAG, "handleLostStatDisp(): leave");

    }

    private void handleDiscoveredMobileScrubBottle(IBeaconDevice iBeacon) {

    }

    private void handlesDiscoveredSemiStatDisp(IBeaconDevice iBeacon) {

    }


}
