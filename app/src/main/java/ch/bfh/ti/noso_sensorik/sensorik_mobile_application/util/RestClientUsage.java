package ch.bfh.ti.noso_sensorik.sensorik_mobile_application.util;

import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class RestClientUsage {
    protected static final String TAG = "RestClientUsage";

    public void getEvents() throws JSONException {
        RestClient.get("events", null, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                // If the response is JSONObject instead of expected JSONArray
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray events) {
                // Pull out the first event on the public timeline
                JSONObject firstEvent = null;
                String triggerText = null;
                try {
                    firstEvent = (JSONObject) events.get(0);
                    triggerText = firstEvent.getString("trigger");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                // Do something with the response
                System.out.println("AAAA" + triggerText);
            }
        });
    }

    public void postEvent(StringEntity params){
        RestClient.post("events", params, new JsonHttpResponseHandler(){
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse){
                Log.w(TAG, "Posting failed, received status code: " + statusCode);
                if( (headers != null) && (headers.length > 0)){
                    for (Header header :headers) {
                        Log.w(TAG, "received Header: '" + header.getName() + "' with value '" + header.getValue() +"'");
                    }
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
            }
        });
    }
}
