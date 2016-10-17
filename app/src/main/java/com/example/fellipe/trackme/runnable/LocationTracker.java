package com.example.fellipe.trackme.runnable;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fellipe.trackme.util.rest.MySingleton;
import com.example.fellipe.trackme.R;
import com.example.fellipe.trackme.util.Session;
import com.example.fellipe.trackme.util.rest.CustomRequest;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import im.delight.android.location.SimpleLocation;

/**
 * Created by Fellipe on 16/10/2016.
 */

public class LocationTracker implements Runnable{

    private static final long DELAY = 1000*60*2; //2min

    private SimpleLocation location;
    private Handler handler;
    private Context context;


    public LocationTracker(SimpleLocation location, Handler handler, Context context) {
        this.location = location;
        this.handler = handler;
        this.context = context;
    }

    @Override
    public void run() {
        try {
            location.beginUpdates();
            sendCurrentLocation(location.getLatitude(), location.getLongitude());
            handler.postDelayed(this, DELAY);
        } catch (Exception e) {
            Log.e("Handler", e.getLocalizedMessage(), e);
        }
    }

    private void sendCurrentLocation(double latitude, double longitude) {
        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getTripId());
        params.put("lat", String.valueOf(latitude));
        params.put("lng", String.valueOf(longitude));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, context.getString(R.string.map_rest_url)+"/position", params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Trip","Enviado posição");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Trip","Fallha ao enviar posição");
                    }
                }){
        };
        jsObjRequest.setRetryPolicy(new DefaultRetryPolicy(
                0,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }
}
