package com.example.fellipe.trackme.runnable;

import android.content.Context;
import android.media.tv.TvView;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fellipe.trackme.enums.HandlerMessagesCode;
import com.example.fellipe.trackme.service.MapService;
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

    private static final int DELAY_TO_MARK_LOCATION = 120;

    private int secondsCounter = 1;

    private SimpleLocation location;
    private MapService mapService;
    private TextView timeText;
    private Handler handler;
    private Context context;


    public LocationTracker(SimpleLocation location, Handler handler, Context context, MapService mapService, TextView timeText) {
        this.location = location;
        this.handler = handler;
        this.context = context;
        this.mapService = mapService;
        this.timeText = timeText;
    }

    @Override
    public void run() {
        try {
            secondsCounter-=1;
            if(secondsCounter == 0) {
                location.beginUpdates();
                sendCurrentLocation(location.getLatitude(), location.getLongitude());
                secondsCounter = DELAY_TO_MARK_LOCATION;
            }

            mapService.decreaseActualEstimatedTime(1);
            timeText.setText(mapService.getActualEstimatedTimeText());

            if(mapService.getActualEstimatedTime() < 1){
                Message message = new Message();
                message.what = HandlerMessagesCode.TIME_FINISHED.getCode();
                handler.sendMessage(message);
            }else {
                handler.postDelayed(this, 1000);
            }
        } catch (Exception e) {
            Log.e("Handler", e.getLocalizedMessage(), e);
        }
    }

    private void sendCurrentLocation(double latitude, double longitude) {
        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getTrip().getTripId());
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
