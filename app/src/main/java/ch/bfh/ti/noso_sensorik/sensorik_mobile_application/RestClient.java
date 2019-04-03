package ch.bfh.ti.noso_sensorik.sensorik_mobile_application;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.protocol.HTTP;

public class RestClient {
    protected static final String TAG = "RestClient";

    // FIXME: DEV-URL, REPLACE WITH DEPLOYED BACKEND URL
    //private static final String BASE_URL = "http://localhost:8080/";
    private static final String BASE_URL = "http://147.87.116.62:8080/sensorik-backend/";

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        Log.i(TAG, "attempting to connect to: '" + getAbsoluteUrl(url) + "'");
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }


    public static void post(String url, StringEntity params, JsonHttpResponseHandler responseHandler) {
        Log.i(TAG, "attempting to post to: '" + getAbsoluteUrl(url) + "'");
        params.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
        client.post(null, getAbsoluteUrl(url), params, "application/json", responseHandler);
    }

    public static void getByUrl(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    public static void postByUrl(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }


    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}