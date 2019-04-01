package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;

import org.altbeacon.beacon.Beacon;

import java.util.ArrayList;

public class TrackingAdapter extends ArrayAdapter<IBeaconDevice> {
    protected static final String TAG = "TrackingAdapter";

    public TrackingAdapter(Context context, ArrayList<IBeaconDevice> iBeaconDevices) {
        super(context, 0, iBeaconDevices);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        IBeaconDevice iBeacon = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_tracking_listresults, parent, false);

        }

        // Lookup view for data population
        ImageView imgView = (ImageView) convertView.findViewById(R.id.device_image);
        TextView beaconName = (TextView) convertView.findViewById(R.id.beacon_name);
        TextView beaconUuid = (TextView) convertView.findViewById(R.id.beacon_uuid);
        TextView beaconMajorMinor = (TextView) convertView.findViewById(R.id.beacon_major_minor);
//        TextView beaconLastSeen = (TextView) convertView.findViewById(R.id.beacon_lastseen);
        TextView distance = (TextView) convertView.findViewById(R.id.distance_interpreted);

        // Populate the data into the template view using the data object
        if(iBeacon.getName() != null && !iBeacon.getName().isEmpty()){
            beaconName.setText("" + iBeacon.getName());
        } else {
            beaconName.setText("" + iBeacon.getAddress());
        }
        beaconUuid.setText("" + iBeacon.getProximityUUID());
        beaconMajorMinor.setText("Major: " + iBeacon.getMajor() + ", Minor: " + iBeacon.getMinor());
//        beaconLastSeen.setText("" + iBeacon.getTimestamp());
        distance.setText("" + iBeacon.getProximity());

        // set the corresponding image depending on Major
        // 1 = Bed / 2 = Dispenser / 3 = Scrub bottle
        if(iBeacon.getMajor() == 1){
            Log.d(TAG, "Beacon is allocated to a Bed");
            imgView.setImageResource(R.drawable.ic_bett);
        } else if(iBeacon.getMajor() == 2){
            Log.d(TAG, "Beacon is allocated to a Dispenser");
            imgView.setImageResource(R.drawable.ic_dispenser);
        } else if(iBeacon.getMajor() == 3){
            Log.d(TAG, "Beacon is allocated to a Scrub bottle");
            imgView.setImageResource(R.drawable.ic_doctor);
        }

        // interpret the distance according to
        if(iBeacon.getProximity() == Proximity.IMMEDIATE){
            distance.setText("Unmittelbar");
            distance.setBackgroundColor(Color.parseColor("#1b995c"));
        } else if(iBeacon.getProximity() == Proximity.NEAR){
            distance.setText("In der Nähe");
            distance.setBackgroundColor(Color.parseColor("#f4d469"));
        } else{
            distance.setText("Entfernt");
            distance.setBackgroundColor(Color.parseColor("#f75445"));
        }

        // Return the completed view to render on screen
        return convertView;
    }

}