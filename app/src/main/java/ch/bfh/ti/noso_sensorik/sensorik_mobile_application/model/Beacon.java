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
    //    private BeaconType type;
    // 1 = STATIONARY_BED // 2 = STATIONARY_DISPENSER // 3 = MOBILE_SCRUB_BOTTLE // 4 = SEMI_STATIONARY_DISPENSER
    public static final int STATIONARY_BED              = 1;
    public static final int STATIONARY_DISPENSER        = 2;
    public static final int MOBILE_SCRUB_BOTTLE         = 3;
    public static final int SEMI_STATIONARY_DISPENSER   = 4;

    private @Id @GeneratedValue Long id;
    private int type;
    private String label;
    private String model;
    private String manufacturer;
    private String location;
    private String macadress;

//    @OneToMany
//    private Collection<Event> eventList = new ArrayList<Event>();

    public Beacon(int type, String label, String model, String manufacturer, String location, String macadress ) {
        this.type = type;
        this.label = label;
        this.model = model;
        this.manufacturer = manufacturer;
        this.location = location;
        this.macadress = macadress;
    }

    public static String getNameForMinor(int minor){
        switch (minor) {
            case 4001:
                return "tag01-ttOIcr";
            case 4002:
                return "tag02-ttZCRU";
            case 4003:
                return "tag03-ttcYPV";
            case 4004:
                return "tag04-tthXUc";
            case 4005:
                return "tag05-ttj3di";
            case 4006:
                return "tag06-tt6N6y";
            case 4007:
                return "tag07-ttv1x0";
            case 4008:
                return "tag08-ttwKPG";
            case 4009:
                return "tag09-ntCOx5";
            case 4010:
                return "tag10-ntl3eY";
        }
        return "";
    }
}