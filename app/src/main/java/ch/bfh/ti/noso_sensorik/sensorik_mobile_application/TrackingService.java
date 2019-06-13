package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.util.Log;
import android.view.LayoutInflater;
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
import com.kontakt.sdk.android.ble.manager.listeners.SecureProfileListener;
import com.kontakt.sdk.android.ble.spec.Acceleration;
import com.kontakt.sdk.android.ble.spec.KontaktTelemetry;
import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.ISecureProfile;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Beacon;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.DCN;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Event;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Scrubbottle;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.util.RestClientUsage;
import cz.msebera.android.httpclient.entity.StringEntity;

public class TrackingService extends Service {
    private static final String TAG = "TrackingService";
    private static final int ONGOING_NOTIFICATION_ID = 50;
    private NotificationManager mNM;

    private int NOTIFICATION = ONGOING_NOTIFICATION_ID;

    private static final int THRESHOLD_ALLOWANCE = 4;

    private ProximityManager proximityManager;
    private boolean isScanning = false;

    private RestClientUsage restClient;

    private ArrayList<IBeaconDevice> currentlyKnownBeacons;
    private ArrayList<String> currentlyKnownSemiStatDisps;

    private View layout;
    private String department, job, scrubbottle, imei;

    //    private TrackingAdapter mTrackadapder;
    private TrackingEventAdapter mTrackadapder;
    private ArrayList<Event> eventList;

    private String filename = "tracking.txt";
    private FileOutputStream outputStream;
    private File file;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: initialize tracking service...");
        HandlerThread thread = new HandlerThread("ServiceStartArguments", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        currentlyKnownBeacons = new ArrayList<IBeaconDevice>();
        currentlyKnownSemiStatDisps = new ArrayList<String>();
        restClient = new RestClientUsage();

        // Construct the data source and create the adapter to convert the array to views
        eventList = new ArrayList<Event>();
        mTrackadapder = new TrackingEventAdapter(this, eventList);

        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        layout = inflater.inflate(R.layout.activity_tracking, null);

        // Attach the adapter to a ListView
        ListView listView = (ListView) layout.findViewById(R.id.listView_beaconstatus);
        listView.setAdapter(mTrackadapder);

        setupProximityManager();
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

        Toast.makeText(this, "Service was Created", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onCreate: tracking service initialized");
    }

    @Override
    public int onStartCommand (Intent intent, int flags, int startId){
        Log.d(TAG, "onStartCommand: starting tracking service...");

        if( (department==null) || (job==null) || (imei==null)){
            department = intent.getStringExtra("DEPARTMENT");
            job = intent.getStringExtra("JOB");
            scrubbottle = intent.getStringExtra("SCRUBBOTTLE");
            imei = intent.getStringExtra("IMEI");
        }


        Log.d(TAG, "onStartCommand(): IMEI is " + imei);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        Notification notification = new Notification.Builder(this.getApplicationContext())
                        .setContentTitle("Service Started")
                        .setContentText("Service successfully started")
                        .setSmallIcon(R.drawable.ic_dispenser)
                        .setContentIntent(pendingIntent)
                        .setTicker("TICKER TEXT")
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
        startScanning();

        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onStartCommand: tracking service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy(){
        Log.d(TAG, "onDestroy(): destroying service...");
        proximityManager.disconnect();
        super.onDestroy();
        stopSelf();
        mNM.cancel(NOTIFICATION);

//        Intent broadcastIntent = new Intent(this, TrackingRestarterBroadcastReceiver.class);

//        sendBroadcast(broadcastIntent);
//        stoptimertask();

        Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
        Log.d(TAG, "onDestroy(): service destroyed");
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
        proximityManager.setIBeaconListener(createIBeaconListener());
        proximityManager.setSecureProfileListener(createSecureProfileListener());
//        proximityManager.filters().iBeaconFilter(IBeaconFilters.newMinorFilter(1005));
//        proximityManager.filters().iBeaconFilter(IBeaconFilters.newMajorFilter(4));
    }

    /**
     * Connect to scanning service and start scanning when ready.
     * Also show a UI text hint.
     */
    public void startScanning() {
        proximityManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                if (proximityManager.isScanning()) {
//                    Toast.makeText(this, "Bereits am scannen", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "startScanning: Bereits am scannen");
                    return;
                }
                proximityManager.startScanning();
                isScanning = true;

                Button pauseButton = (Button) layout.findViewById(R.id.button_pause);
                pauseButton.setText("Scannen unterbrechen");
//                Toast.makeText(TrackingActivity.this, "Scanning gestartet", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "startScanning: Scanning gestartet");
            }
        });

    }

    /**
     * Stop scanning if scanning is in progress
     */
    public void stopScanning() {
        if (proximityManager.isScanning()) {
            proximityManager.stopScanning();
            isScanning = false;
            Button pauseButton = (Button) layout.findViewById(R.id.button_pause);
            pauseButton.setText("Scannen weiterführen");
            Toast.makeText(this, "Scannen gestopt", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------------------- SECUREPROFILELISTENER

    private SecureProfileListener createSecureProfileListener(){
        return new SecureProfileListener() {
            @Override
            public void onProfileDiscovered(ISecureProfile profile) {

                if((profile.getUniqueId() != null) && !(profile.getUniqueId().isEmpty())){
                    Log.i(TAG, "onProfileDiscovered: Name: " + profile.getName() + ", Address: " +profile.getMacAddress() + ", Unique ID: '" + profile.getUniqueId() +"'");
                    // Extract telemetry
                    KontaktTelemetry telemetry = profile.getTelemetry();
                    if (telemetry != null) {
                        // Get acceleration, temperature, light level and device time
                        Acceleration acceleration = telemetry.getAcceleration();
                        int deviceTime = telemetry.getTimestamp();
                        if(deviceTime != 0){
                            Log.i(TAG, "onProfileDiscovered: last threshold - "+telemetry.getLastThreshold() + " timestamp: '" + telemetry.getTimestamp()+"'");
                        }
                    }
                }
            }

            @Override
            public void onProfilesUpdated(List<ISecureProfile> profiles) {
                for (ISecureProfile profile : profiles) {
//                   Log.i(TAG, "onProfilesUpdated(): Name: " + profile.getName() + ", Address: " +profile.getMacAddress() + ", Unique ID: '" + profile.getUniqueId() +"'");
                    if((profile.getUniqueId() != null) && !(profile.getUniqueId().isEmpty())){
                        if(profile.getUniqueId().equals(scrubbottle)){
                            // get acceleration data
                            KontaktTelemetry telemetry = profile.getTelemetry();
                            if (telemetry != null) {
                                if( (telemetry.getAcceleration() != null)){
                                    Log.i(TAG, "onProfilesUpdated: scrubbottle logging - Name: " + profile.getName() + ", Address: " +profile.getMacAddress() + ", Unique ID: '" + profile.getUniqueId() +"'");
                                    logScrubbottle(telemetry.getAcceleration(), profile);
                                }
                            }
                        } else {
                            Log.i(TAG, "onProfilesUpdated: Name: " + profile.getName() + ", Address: " +profile.getMacAddress() + ", Unique ID: '" + profile.getUniqueId() +"'");

                            // Extract telemetry
                            KontaktTelemetry telemetry = profile.getTelemetry();
                            if (telemetry != null) {
                                // Get acceleration, temperature, light level and device time
                                Acceleration acceleration = telemetry.getAcceleration();
                                if(telemetry.getTimestamp() != 0){
                                    Log.i(TAG, "onProfilesUpdated: last threshold - "+telemetry.getLastThreshold() + " timestamp: '" + telemetry.getTimestamp()+"'");
                                    if(telemetry.getLastThreshold() < THRESHOLD_ALLOWANCE){
                                        Log.i(TAG,"onProfilesUpdated: call event function"  );
                                        usedSemistationaryDispenser(profile);
                                    }
                                }
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

    // ------------------------------ IBEACONLISTENER

    private IBeaconListener createIBeaconListener() {
        return new IBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {
                Log.i(TAG, "onIBeaconDiscovered: new Beacon found " + iBeacon.toString());

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
                    Log.i(TAG, "onIBeaconsUpdated: Name: " + iBeacon.getMinor() + ", Address: " +iBeacon.getAddress() + ", Unique ID: '" + iBeacon.getProximity() +"'");

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
                            handleUpdatedSemiStatDisp(iBeacon);
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
                        LocalDate.now(), LocalTime.now(), department, job, (Event.APPROACHING_PATIENT_ZONE-1), iBeacon.getRssi(),
                        new Beacon((iBeacon.getMajor()-1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                        new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT ));

                Log.d(TAG, "handleDiscoveredPatZone(): logged event: " +newEvent.toString() );
                restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                outputStream = openFileOutput(filename, Context.MODE_APPEND);
                outputStream.write('\n');
                outputStream.write((newEvent.toJSON().toString().getBytes()));
                outputStream.write(',');
                outputStream.close();

                mTrackadapder.insert(newEvent,0);
                currentlyKnownBeacons.add(iBeacon);
            } else if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){ // we discover a beacon and are directly IMMEDIATE (should never happen...)
                newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.DIRECTLY_IN_PATIENT_ZONE-1), iBeacon.getRssi(),
                        new Beacon((iBeacon.getMajor()-1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                        new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                );
                Log.d(TAG, "handleDiscoveredPatZone(): logged event: " +newEvent.toString() );
                restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                outputStream = openFileOutput(filename, Context.MODE_APPEND);
                outputStream.write('\n');
                outputStream.write((newEvent.toJSON().toString().getBytes()));
                outputStream.write(',');
                outputStream.close();
                mTrackadapder.insert(newEvent,0);
                currentlyKnownBeacons.add(iBeacon);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Log.v(TAG, "handleDiscoveredPatZone(): leave");

    }

    private void handleUpdatedPatZone(IBeaconDevice iBeacon) {
        Log.v(TAG, "handleUpdatedPatZone(): enter for "+iBeacon.toString());

        if(! (currentlyKnownBeacons.contains(iBeacon)) ) {  // we don't already known the beacon
            if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)) { // we discovered a beacon in immediate distance, fire event
                Event newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.DIRECTLY_IN_PATIENT_ZONE - 1), iBeacon.getRssi(),
                        new Beacon((iBeacon.getMajor() - 1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                        new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                );

                try {
                    Log.d(TAG, "handleUpdatedPatZone(): logged event: " + newEvent.toString());
                    restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                    outputStream = openFileOutput(filename, Context.MODE_APPEND);
                    outputStream.write('\n');
                    outputStream.write((newEvent.toJSON().toString().getBytes()));
                    outputStream.write(',');
                    outputStream.close();
                    mTrackadapder.insert(newEvent, 0);
                    currentlyKnownBeacons.add(iBeacon);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else { // we already know the beacon

            // but maybe the proximity has changed? if the proximity is NEAR instead of IMMEDIATE or vice-versa then we need to fire another event
            IBeaconDevice tmpBeacon = currentlyKnownBeacons.get(currentlyKnownBeacons.indexOf(iBeacon));
            if(tmpBeacon.getProximity() != iBeacon.getProximity()){ // proximity changed
                if(iBeacon.getProximity().equals(Proximity.FAR)){
                    Log.d(TAG, "handleUpdatedPatZone(): proximity changed from " +tmpBeacon.getProximity() + " to " + iBeacon.getProximity());
                    Event newEvent = new Event(
                            LocalDate.now(), LocalTime.now(), department, job, (Event.LEAVING_PATIENT_ZONE - 1), iBeacon.getRssi(),
                            new Beacon((iBeacon.getMajor() - 1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                            new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                    );

                    try {
                        Log.d(TAG, "handleUpdatedPatZone(): logged event: " + newEvent.toString());
                        restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                        outputStream = openFileOutput(filename, Context.MODE_APPEND);
                        outputStream.write('\n');
                        outputStream.write((newEvent.toJSON().toString().getBytes()));
                        outputStream.write(',');
                        outputStream.close();
                        mTrackadapder.insert(newEvent, 0);
                        currentlyKnownBeacons.remove(tmpBeacon);
                        currentlyKnownBeacons.add(iBeacon);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (iBeacon.getProximity().equals(Proximity.IMMEDIATE)){
                    Log.d(TAG, "handleUpdatedPatZone(): proximity changed from " +tmpBeacon.getProximity() + " to " + iBeacon.getProximity());
                    Event newEvent = new Event(
                            LocalDate.now(), LocalTime.now(), department, job, (Event.DIRECTLY_IN_PATIENT_ZONE - 1), iBeacon.getRssi(),
                            new Beacon((iBeacon.getMajor() - 1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                            new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                    );

                    try {
                        Log.d(TAG, "handleUpdatedPatZone(): logged event: " + newEvent.toString());
                        restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                        outputStream = openFileOutput(filename, Context.MODE_APPEND);
                        outputStream.write('\n');
                        outputStream.write((newEvent.toJSON().toString().getBytes()));
                        outputStream.write(',');
                        outputStream.close();
                        mTrackadapder.insert(newEvent, 0);
                        currentlyKnownBeacons.remove(tmpBeacon);
                        currentlyKnownBeacons.add(iBeacon);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.d(TAG, "handleUpdatedPatZone(): proximity has NOT changed from " +tmpBeacon.getProximity() + " to " + iBeacon.getProximity());
                if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){
                    Event newEvent = new Event(
                            LocalDate.now(), LocalTime.now(), department, job, (Event.DIRECTLY_IN_PATIENT_ZONE - 1), iBeacon.getRssi(),
                            new Beacon((iBeacon.getMajor() - 1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                            new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                    );

                    try {
                        Log.d(TAG, "handleUpdatedPatZone(): logged event: " + newEvent.toString());
                        restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                        outputStream = openFileOutput(filename, Context.MODE_APPEND);
                        outputStream.write('\n');
                        outputStream.write((newEvent.toJSON().toString().getBytes()));
                        outputStream.write(',');
                        outputStream.close();
                        mTrackadapder.insert(newEvent, 0);
                        currentlyKnownBeacons.remove(tmpBeacon);
                        currentlyKnownBeacons.add(iBeacon);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Log.v(TAG, "handleUpdatedPatZone(): enter");

    }

    private void handleDiscoveredStatDisp(IBeaconDevice iBeacon) {
        Log.v(TAG, "handleDiscoveredStatDisp(): enter for " + iBeacon.toString());

        if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){ // we discovered a beacon in immediate distance, fire event
            Event newEvent = new Event(
                    LocalDate.now(), LocalTime.now(), department, job, (Event.APPROACHING_STATIONARY_DISPENSER-1), iBeacon.getRssi(),
                    new Beacon((iBeacon.getMajor()-1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                    new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
            );

            try {
                Log.d(TAG, "handleDiscoveredStatDisp(): logged event: " +newEvent.toString() );
                Log.d(TAG, "handleDiscoveredStatDisp(): logged event: " +newEvent.toJSON() );
                restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                outputStream = openFileOutput(filename, Context.MODE_APPEND);
                outputStream.write('\n');
                outputStream.write((newEvent.toJSON().toString().getBytes()));
                outputStream.write(',');
                outputStream.close();
                mTrackadapder.insert(newEvent, 0);
                currentlyKnownBeacons.add(iBeacon);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
                        LocalDate.now(), LocalTime.now(), department, job, (Event.APPROACHING_STATIONARY_DISPENSER-1), iBeacon.getRssi(),
                        new Beacon((iBeacon.getMajor()-1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                        new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                );

                try {
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toString() );
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toJSON() );
                    restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                    outputStream = openFileOutput(filename, Context.MODE_APPEND);
                    outputStream.write('\n');
                    outputStream.write((newEvent.toJSON().toString().getBytes()));
                    outputStream.write(',');
                    outputStream.close();
                    mTrackadapder.insert(newEvent, 0);
                    currentlyKnownBeacons.add(iBeacon);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else { // beacon already known and because we only add this kind of beacon when IMMEDIATE, it should be IMMEDIATE
            // if the proximity is NEAR instead of IMMEDIATE, then we are leaving the range and should "forget" the beacon
            if(iBeacon.getProximity().equals(Proximity.NEAR)) {
                Event newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.LEAVING_STATIONARY_DISPENSER-1), iBeacon.getRssi(),
                        new Beacon((iBeacon.getMajor()-1), "Beacon #" + iBeacon.getMinor(), "Smart Beacon SB18-3", "Kontakt.io", "Labor", iBeacon.getAddress()),
                        new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                );

                try {
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toString() );
                    Log.d(TAG, "handleUpdatedStatDisp(): logged event: " +newEvent.toJSON() );
                    restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                    outputStream = openFileOutput(filename, Context.MODE_APPEND);
                    outputStream.write('\n');
                    outputStream.write((newEvent.toJSON().toString().getBytes()));
                    outputStream.write(',');
                    outputStream.close();
                    mTrackadapder.insert(newEvent, 0);
                    currentlyKnownBeacons.remove(iBeacon);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
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
        Log.v(TAG, "handlesDiscoveredSemiStatDisp(): enter for " + iBeacon.toString());

        if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){
            currentlyKnownBeacons.add(iBeacon);
            currentlyKnownSemiStatDisps.add(Beacon.getNameForMinor(iBeacon.getMinor()));
            Log.v(TAG, "handlesDiscoveredSemiStatDisp(): beacon in IMMEDIATE PROXIMITY, added to known list " + iBeacon.toString());
        }

        Log.v(TAG, "handlesDiscoveredSemiStatDisp(): leave");
    }

    private void handleUpdatedSemiStatDisp(IBeaconDevice iBeacon) {
        Log.v(TAG, "handleUpdatedSemiStatDisp(): enter for " + iBeacon.toString());

        if(!(currentlyKnownBeacons.contains(iBeacon))){
            if(iBeacon.getProximity().equals(Proximity.IMMEDIATE)){
                currentlyKnownBeacons.add(iBeacon);
                currentlyKnownSemiStatDisps.add(Beacon.getNameForMinor(iBeacon.getMinor()));
                Log.v(TAG, "handleUpdatedSemiStatDisp(): beacon in IMMEDIATE PROXIMITY, added to known list " + iBeacon.toString());
            }
        }

        Log.v(TAG, "handleUpdatedSemiStatDisp(): leave");
    }


    private void usedSemistationaryDispenser(ISecureProfile profile){
        Log.v(TAG, "usedSemistationaryDispenser(): enter for " + profile.toString());
        Log.i(TAG, "usedSemistationaryDispenser(): size of list of known disps " + currentlyKnownSemiStatDisps.size());

        String tmpProfileName = profile.getName();
        Iterator itr = currentlyKnownSemiStatDisps.iterator();
        while(itr.hasNext()) {
            String tmpBeaconName = (String) itr.next();
            Log.i(TAG,"usedSemistationaryDispenser(): comparing name of " + tmpProfileName + " with " + tmpBeaconName );
            if(tmpBeaconName.equals(tmpProfileName)){
                // the disp used is one in immediate proximity, send event
                Event newEvent = new Event(
                        LocalDate.now(), LocalTime.now(), department, job, (Event.USING_SEMI_STATIONARY_DISPENSER-1), profile.getRssi(),
                        new Beacon((4-1), "Beacon (" + tmpBeaconName +")", "Smart Beacon SB18-3", "Kontakt.io", "Labor", profile.getMacAddress()),
                        new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
                );

                try {
                    Log.d(TAG, "usedSemistationaryDispenser(): logged event: " +newEvent.toString() );
                    Log.d(TAG, "usedSemistationaryDispenser(): logged event: " +newEvent.toJSON() );
                    restClient.postEvent(new StringEntity(newEvent.toJSON().toString(), "UTF-8"));
                    outputStream = openFileOutput(filename, Context.MODE_APPEND);
                    outputStream.write('\n');
                    outputStream.write((newEvent.toJSON().toString().getBytes()));
                    outputStream.write(',');
                    outputStream.close();
                    mTrackadapder.insert(newEvent, 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        Log.v(TAG, "usedSemistationaryDispenser(): leave");

    }

    private void logScrubbottle(Acceleration acceleration, ISecureProfile profile) {
        Log.v(TAG, "logScrubbottle(): enter");

        Scrubbottle tmpScrubbottle = new Scrubbottle(
                LocalDate.now(), LocalTime.now(), acceleration.getX(), acceleration.getY(), acceleration.getY(), profile.getRssi(), profile.getUniqueId(), department, job,
                new DCN("DCN Label XY", this.imei, Build.MANUFACTURER, Build.MODEL, Build.FINGERPRINT )
        );

        try {
            Log.d(TAG, "logScrubbottle(): logged event: " +tmpScrubbottle.toString() );
            restClient.postScrubbottle(new StringEntity(tmpScrubbottle.toJSON().toString(), "UTF-8"));
            outputStream = openFileOutput(filename, Context.MODE_APPEND);
            outputStream.write('\n');
            outputStream.write((tmpScrubbottle.toJSON().toString().getBytes()));
            outputStream.write(',');
            outputStream.close();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.v(TAG, "logScrubbottle(): leave");

    }

}
