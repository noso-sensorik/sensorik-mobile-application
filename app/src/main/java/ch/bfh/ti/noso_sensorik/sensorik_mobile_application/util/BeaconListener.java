package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.util;

import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;

import java.util.List;

public class BeaconListener implements IBeaconListener {
    /**
     * Called when iBeacon is discovered for the first time. This will be called only once per scan or after beacon is reported lost.
     *
     * @param iBeacon {@link IBeaconDevice} instance.
     * @param region  {@link IBeaconRegion} instance.
     */
    @Override
    public void onIBeaconDiscovered(IBeaconDevice iBeacon, IBeaconRegion region) {

    }

    /**
     * Called when iBeacons are updated.
     *
     * @param iBeacons List of updated {@link IBeaconDevice} devices.
     * @param region   {@link IBeaconRegion} instance.
     */
    @Override
    public void onIBeaconsUpdated(List<IBeaconDevice> iBeacons, IBeaconRegion region) {

    }

    /**
     * Called when iBeacon is reported lost (out of range). onIBeaconDiscovered will be called when beacon is back in range.
     *
     * @param iBeacon Lost {@link IBeaconDevice} instance.
     * @param region  {@link IBeaconRegion} instance.
     */
    @Override
    public void onIBeaconLost(IBeaconDevice iBeacon, IBeaconRegion region) {

    }
}
