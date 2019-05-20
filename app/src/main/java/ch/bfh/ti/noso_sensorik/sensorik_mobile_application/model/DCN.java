package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Data;

@Data
@Entity
/**
 * Represents a ble beacon, which can trigger an event
 *
 * @author engeld
 *
 */
public class DCN {
    //    private BeaconType type;
    private @Id @GeneratedValue Long id;
    private String label;
    private String IMEI;
    private String manufacturer;
    private String model;
    private String fingerprint;

//    @OneToMany
//    private Collection<Event> eventList = new ArrayList<Event>();

    public DCN(String label, String IMEI, String manufacturer, String model, String fingerprint) {
        this.label = label;
        this.IMEI = IMEI;
        this.manufacturer = manufacturer;
        this.model = model;
        this.fingerprint = fingerprint;
    }
}