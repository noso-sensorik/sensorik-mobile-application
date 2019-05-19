package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;

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

    @ManyToOne(cascade = {CascadeType.ALL})
    private Beacon eventSource;
//	private DataCollectionNode collectedFrom;

    public Event(LocalDate date, LocalTime time, String station, String job, int trigger, Beacon eventSource) {
        this.date = date;
        this.time = time;
        this.station = station;
        this.job = job;
        this.trigger = trigger;
        this.eventSource = eventSource;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject rp = new JSONObject();

        // Details for the Event
        rp.put("date", this.date);
        rp.put("time", this.time);
        rp.put("station", this.station);
        rp.put("job", this.job);
        rp.put("trigger", this.trigger);

        // Details on the eventsource (= the beacon)
        JSONObject innerObj = new JSONObject();
        innerObj.put("type", this.eventSource.getType() );
        innerObj.put("label", "" + this.eventSource.getLabel());
        innerObj.put("model", this.eventSource.getModel());
        innerObj.put("manufacturer", this.eventSource.getManufacturer());
        innerObj.put("loc", this.eventSource.getLocation());
        innerObj.put("mac", "" + this.eventSource.getMacadress());
        rp.put("eventSource", innerObj);

        return rp;
    }
}