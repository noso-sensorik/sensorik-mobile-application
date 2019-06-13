package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.json.JSONException;
import org.json.JSONObject;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import lombok.Data;

@Data
@Entity
public class Event {
    //    private EventTrigger trigger;
    // 1 = APPROACHING_PATIENT_ZONE // 2 = DIRECTLY_IN_PATIENT_ZONE // 3 = LEAVING_PATIENT_ZONE
    // 4 = APPROACHING_STATIONARY_DISPENSER // 5 = LEAVING_STATIONARY_DISPENSER
    // 6 = USING_MOBILE_SCRUB_BOTTLE // 7 = USING_SEMI_STATIONARY_DISPENSER
    public static final int APPROACHING_PATIENT_ZONE            = 1;
    public static final int DIRECTLY_IN_PATIENT_ZONE            = 2;
    public static final int LEAVING_PATIENT_ZONE                = 3;
    public static final int APPROACHING_STATIONARY_DISPENSER    = 4;
    public static final int LEAVING_STATIONARY_DISPENSER        = 5;
    public static final int USING_MOBILE_SCRUB_BOTTLE           = 6;
    public static final int USING_SEMI_STATIONARY_DISPENSER     = 7;

    private @Id @GeneratedValue Long id;
    private LocalDate date;
    private LocalTime time;
    private String station;
    private String job;
    private int trigger;
    private int rssi;

    @ManyToOne(cascade = {CascadeType.ALL})
    private Beacon eventSource;
	private DCN collectedFrom;

    public Event(LocalDate date, LocalTime time, String station, String job, int trigger, int rssi, Beacon eventSource, DCN collectedFrom) {
        this.date = date;
        this.time = time;
        this.station = station;
        this.job = job;
        this.trigger = trigger;
        this.rssi = rssi;
        this.eventSource = eventSource;
        this.collectedFrom = collectedFrom;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject rp = new JSONObject();

        // Details for the Event
        rp.put("date", this.date);
        rp.put("time", this.time);
        rp.put("station", this.station);
        rp.put("job", this.job);
        rp.put("rssi", this.rssi);
        rp.put("trigger", this.trigger);

        // Details on the eventsource (= the beacon)
        JSONObject innerObj = new JSONObject();
        innerObj.put("type", this.eventSource.getType());
        innerObj.put("label", "" + this.eventSource.getLabel());
        innerObj.put("model", this.eventSource.getModel());
        innerObj.put("manufacturer", this.eventSource.getManufacturer());
        innerObj.put("location", this.eventSource.getLocation());
        innerObj.put("macadress", "" + this.eventSource.getMacadress());
        rp.put("eventSource", innerObj);


        // Details on the eventsource (= the beacon)
        JSONObject innerObj2 = new JSONObject();
        innerObj2.put("label", "" + this.collectedFrom.getLabel());
        innerObj2.put("manufacturer", this.collectedFrom.getManufacturer());
        innerObj2.put("model", this.collectedFrom.getModel());
        innerObj2.put("imei", this.collectedFrom.getIMEI());
        innerObj2.put("fingerprint", "" + this.collectedFrom.getFingerprint());
        rp.put("collectedFrom", innerObj2);

        return rp;
    }
}