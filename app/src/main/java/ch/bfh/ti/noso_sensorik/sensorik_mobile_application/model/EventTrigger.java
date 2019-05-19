package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.model;

import java.util.HashMap;
import java.util.Map;

public enum EventTrigger {
    APPROACHING_PATIENT_ZONE(1),
    DIRECTLY_IN_PATIENT_ZONE(2),
    LEAVING_PATIENT_ZONE(3),
    APPROACHING_STATIONARY_DISPENSER(4),
    LEAVING_STATIONARY_DISPENSER(5),
    USING_MOBILE_SCRUB_BOTTLE(6),
    USING_SEMI_STATIONARY_DISPENSER(7);

    private final int trigger;
    private static Map map = new HashMap<>();

    EventTrigger(int type) {
        this.trigger = type;
    }

    static {
        for (EventTrigger eventTrigger : EventTrigger.values()) {
            map.put(eventTrigger.trigger, eventTrigger);
        }
    }

    public static EventTrigger valueOf(int trigger) {
        return (EventTrigger) map.get(trigger);
    }

    public int getValue() {
        return trigger;
    }

    public String toString() {
        return Integer.toString(trigger);
    }
}
