package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model;

import com.kontakt.sdk.android.ble.spec.Acceleration;

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
public class Scrubbottle {
    private @Id @GeneratedValue Long id;
    private LocalDate date;
    private LocalTime time;
    private Acceleration acceleration;
    private int x, y, z;
    private int rssi;
    private String label;
    private String station;
    private String job;

    @ManyToOne(cascade = {CascadeType.ALL})
	private DCN collectedFrom;

    public Scrubbottle(LocalDate date, LocalTime time, int x, int y, int z, int rssi, String label, String station, String job, DCN collectedFrom) {
        this.date = date;
        this.time = time;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rssi = rssi;
        this.label = label;
        this.station = station;
        this.job = job;
        this.collectedFrom = collectedFrom;
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject rp = new JSONObject();

        // Details for the Event
        rp.put("date", this.date);
        rp.put("time", this.time);
        rp.put("x", this.x);
        rp.put("y", this.y);
        rp.put("z", this.z);
        rp.put("rssi", this.rssi);
        rp.put("trigger", this.label);
        rp.put("station", this.station);
        rp.put("job", this.job);

        // Details on the dcn (= the the mobile)
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