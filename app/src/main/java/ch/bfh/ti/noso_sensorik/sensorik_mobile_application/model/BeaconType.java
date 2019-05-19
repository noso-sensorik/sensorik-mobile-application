package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model;

import java.util.HashMap;
import java.util.Map;

public enum BeaconType {
    STATIONARY_BED(1),
    STATIONARY_DISPENSER(2),
    MOBILE_SCRUB_BOTTLE(3),
    SEMI_STATIONARY_DISPENSER(4);

    private final int type;
    private static Map map = new HashMap<>();

    BeaconType(int type) {
        this.type = type;
    }

    static {
        for (BeaconType beaconType : BeaconType.values()) {
            map.put(beaconType.type, beaconType);
        }
    }

    public static BeaconType valueOf(int beaconType) {
        return (BeaconType) map.get(beaconType);
    }

    public int getValue() {
        return type;
    }

    public String toString() {
        return Integer.toString(type);
    }

}
