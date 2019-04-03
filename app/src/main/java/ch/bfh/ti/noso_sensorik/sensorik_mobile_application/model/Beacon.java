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
public class Beacon {
    private @Id @GeneratedValue Long id;
    private BeaconType type;
    private String label;
    private String model;
    private String manufacturer;
    private String location;
    private String macadress;

//    @OneToMany
//    private Collection<Event> eventList = new ArrayList<Event>();

    public Beacon(BeaconType type, String label, String model, String manufacturer, String loc, String mac ) {
        this.type = type;
        this.label = label;
        this.model = model;
        this.manufacturer = manufacturer;
        this.location = loc;
        this.macadress = mac;

    }
}