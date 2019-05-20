package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.kontakt.sdk.android.common.Proximity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;

import java.time.LocalDate;
import java.util.ArrayList;

import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Beacon;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.Event;
import ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model.EventTrigger;

public class TrackingEventAdapter extends ArrayAdapter<Event> {
    protected static final String TAG = "TrackingAdapter";

    public TrackingEventAdapter(Context context, ArrayList<Event> events) {
        super(context, 0, events);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Event currentEvent = getItem(position);
        Beacon beacon = currentEvent.getEventSource();

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_trackingevents_listresults, parent, false);
        }

        // Lookup view for data population
//        ImageView imgView = (ImageView) convertView.findViewById(R.id.device_image);
        TextView eventID = (TextView) convertView.findViewById(R.id.event_id);
        TextView eventDate = (TextView) convertView.findViewById(R.id.event_date);
        TextView eventTime = (TextView) convertView.findViewById(R.id.event_time);
        TextView eventJob = (TextView) convertView.findViewById(R.id.event_job);
        TextView eventStation = (TextView) convertView.findViewById(R.id.event_station);
        TextView eventTrigger = (TextView) convertView.findViewById(R.id.event_trigger);

        TextView beaconType = (TextView) convertView.findViewById(R.id.beacon_type);
        TextView beaconLabel = (TextView) convertView.findViewById(R.id.beacon_label);
        TextView beaconModel = (TextView) convertView.findViewById(R.id.beacon_model);
        TextView beaconManu = (TextView) convertView.findViewById(R.id.beacon_manufacturer);
        TextView beaconLoc = (TextView) convertView.findViewById(R.id.beacon_location);
        TextView beaconMac = (TextView) convertView.findViewById(R.id.beacon_macadress);

        // Populate the data into the template view using the data object
        if((currentEvent.getId() == null) || (currentEvent.getId().equals(""))){
            if(beacon.getLabel().contains("#"))
                eventID.setText("Beacon " + beacon.getLabel().split("#")[1]);
            else
                eventID.setText("Beacon " + beacon.getLabel() );
        } else {
            eventID.setText("" + currentEvent.getId());
        }
        eventDate.setText("Date: " + currentEvent.getDate().toString());
        eventTime.setText("Time: " + currentEvent.getTime().toString());
        eventJob.setText("Job: " + currentEvent.getJob());
        eventStation.setText("Station: " + currentEvent.getStation());
        eventTrigger.setText("Trigger " + currentEvent.getTrigger()+1);

        int tmpTrigger = currentEvent.getTrigger()+1;
        switch (tmpTrigger){
            case Event.APPROACHING_PATIENT_ZONE:
                eventTrigger.setText("Trigger: (" + tmpTrigger + ") APPROACHING_PATIENT_ZONE");
                break;
            case Event.DIRECTLY_IN_PATIENT_ZONE:
                eventTrigger.setText("Trigger: (" + tmpTrigger + ") DIRECTLY_IN_PATIENT_ZONE");
                break;
            case Event.LEAVING_PATIENT_ZONE:
                eventTrigger.setText("Trigger: (" + tmpTrigger + ") LEAVING_PATIENT_ZONE");
                break;
            case Event.APPROACHING_STATIONARY_DISPENSER:
                eventTrigger.setText("Trigger: (" + tmpTrigger + ") APPROACHING_STATIONARY_DISPENSER");
                break;
            case Event.LEAVING_STATIONARY_DISPENSER:
                eventTrigger.setText("Trigger: (" + tmpTrigger + ") LEAVING_STATIONARY_DISPENSER");
                break;
            case Event.USING_MOBILE_SCRUB_BOTTLE:
                eventTrigger.setText("Trigger: (" + tmpTrigger + ") USING_MOBILE_SCRUB_BOTTLE");
                break;
            case Event.USING_SEMI_STATIONARY_DISPENSER:
                eventTrigger.setText("Trigger: (" + tmpTrigger + ") USING_SEMI_STATIONARY_DISPENSER");
                break;
        }

        int tmpBeaconType = beacon.getType()+1;
        switch (tmpBeaconType){
            case Beacon.STATIONARY_BED:
                beaconType.setText("BeaconType: (" + tmpBeaconType + ") STATIONARY_BED");
                break;
            case Beacon.STATIONARY_DISPENSER:
                beaconType.setText("BeaconType: (" + tmpBeaconType + ") STATIONARY_DISPENSER");
                break;
            case Beacon.MOBILE_SCRUB_BOTTLE:
                beaconType.setText("BeaconType: (" + tmpBeaconType + ") MOBILE_SCRUB_BOTTLE");
                break;
            case Beacon.SEMI_STATIONARY_DISPENSER:
                beaconType.setText("BeaconType: (" + tmpBeaconType + ") SEMI_STATIONARY_DISPENSER");
                break;
        }
        beaconLabel.setText("Label: " + beacon.getLabel());
        beaconModel.setText("Model: " + beacon.getModel());
        beaconManu.setText("Manufacturer: " + beacon.getManufacturer());
        beaconLoc.setText("Location: " + beacon.getLocation());
        beaconMac.setText("MAC: " + beacon.getMacadress());

        // Return the completed view to render on screen
        return convertView;
    }

}