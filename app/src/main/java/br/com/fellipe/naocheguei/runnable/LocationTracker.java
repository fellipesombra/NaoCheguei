package br.com.fellipe.naocheguei.runnable;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import br.com.fellipe.naocheguei.enums.HandlerMessagesCode;
import br.com.fellipe.naocheguei.service.MapService;
import br.com.fellipe.naocheguei.util.Session;
import br.com.fellipe.naocheguei.util.rest.CustomRequest;
import br.com.fellipe.naocheguei.util.rest.MySingleton;
import im.delight.android.location.SimpleLocation;

/**
 * Created by Fellipe on 16/10/2016.
 */

public class LocationTracker implements Runnable{

    private static final int DELAY_TO_MARK_LOCATION = 60;

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
                sendCurrentLocation(location.getLatitude(), location.getLongitude());
                secondsCounter = DELAY_TO_MARK_LOCATION;
            }

            mapService.decreaseActualEstimatedTime(1);
            timeText.setText(mapService.getActualEstimatedTimeText());

            if(mapService.getActualEstimatedTime() == 300){
                Message message1 = new Message();
                message1.what = HandlerMessagesCode.X_MINUTES_LEFT.getCode();
                handler.sendMessage(message1);
            }

            if((secondsCounter % 20 == 0) && isInDestinationRadius()){
                Message message2 = new Message();
                message2.what = HandlerMessagesCode.ARRIVED_AT_DESTIONATION.getCode();
                handler.sendMessage(message2);
            }else if(mapService.getActualEstimatedTime() < 1 ){
                Message message3 = new Message();
                message3.what = HandlerMessagesCode.TIME_FINISHED.getCode();
                handler.sendMessage(message3);
            }else {
                handler.postDelayed(this, 1000);
            }
        } catch (Exception e) {
            Log.e("Handler", e.getLocalizedMessage(), e);
        }
    }

    private boolean isInDestinationRadius() {

        boolean isInRadiusOfDestionation = false;

        double radius = 0.05; //50m

        double curLat = location.getLatitude();
        double curLng = location.getLongitude();

        double lat = mapService.getCurrentDestination().latitude;
        double lng = mapService.getCurrentDestination().longitude;

        double latDelta = radius/110.54;
        double longDelta = radius/(111.320*Math.cos(Math.toDegrees(lat)));

        double lat1 = lat + latDelta;
        double lng1 = lng + longDelta;
        double lat2 = lat - latDelta;
        double lng2 = lng - longDelta;

        if(curLat < lat1 && curLat > lat2 && curLng < lng1 && curLng > lng2){
            isInRadiusOfDestionation = true;
        }
        return isInRadiusOfDestionation;
    }

    private void sendCurrentLocation(double latitude, double longitude) {
        Map<String, String> params = new HashMap<>();
        params.put("id", Session.getInstance().getTrip().getTripId());
        params.put("lat", String.valueOf(latitude));
        params.put("lng", String.valueOf(longitude));

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, context.getString(br.com.fellipe.naocheguei.R.string.map_rest_url)+"/position", params,
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
                DefaultRetryPolicy.DEFAULT_TIMEOUT_MS*2,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
    }
}
