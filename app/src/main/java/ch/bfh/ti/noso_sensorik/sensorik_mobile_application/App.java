package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.app.Application;
import com.kontakt.sdk.android.common.KontaktSDK;

import net.danlew.android.joda.JodaTimeAndroid;

public class App extends Application {

    private static final String API_KEY = "tGFfZXTAgFxvzejApmcNoHgLyEHFpzYJ";

    @Override
    public void onCreate() {
        super.onCreate();
        JodaTimeAndroid.init(this);
        initializeDependencies();
    }

    //Initializing Kontakt SDK. Insert your API key to allow all samples to work correctly
    private void initializeDependencies() {
        KontaktSDK.initialize(API_KEY);
    }
}
