package com.example.fellipe.trackme.service;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.fellipe.trackme.R;
import com.example.fellipe.trackme.dto.TripInfo;
import com.example.fellipe.trackme.enums.RestResponseStatus;
import com.example.fellipe.trackme.util.Session;
import com.example.fellipe.trackme.util.rest.CustomRequest;
import com.example.fellipe.trackme.util.rest.MySingleton;
import com.example.fellipe.trackme.enums.TransportType;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import im.delight.android.location.SimpleLocation;

/**
 * Created by Fellipe on 16/10/2016.
 */

public class MapService {

    private static final String GOOGLE_DISTANCE_MATRIX_API_URL = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%s,%s&destinations=%s,%s&mode=%s&language=pt-BR&key=%s";
    private static final String TAG = "MAPSERVICE";

    private String distanceMatrixKey;
    private Context context;
    private SimpleLocation location;

    private Map<String,Integer> estimatedTimes = new HashMap<>();
    private int actualEstimatedTime;
    private LatLng currentDestination;

    public MapService(String distanceMatrixKey, Context context, SimpleLocation location){
        this.distanceMatrixKey = distanceMatrixKey;
        this.context = context;
        this.location = location;

        estimatedTimes.put(TransportType.BICYCLING.getName(),0);
        estimatedTimes.put(TransportType.DRIVING.getName(),0);
        estimatedTimes.put(TransportType.WALKING.getName(),0);
        estimatedTimes.put(TransportType.PUBLIC_TRANSPORT.getName(),0);
    }

    public void updateEstimatedTime(Place place) {

        for (final String transportType: estimatedTimes.keySet()) {

            String url = String.format(GOOGLE_DISTANCE_MATRIX_API_URL,location.getLatitude(),location.getLongitude(),place.getLatLng().latitude,place.getLatLng().longitude,transportType,distanceMatrixKey);
            CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONObject elements = response.getJSONArray("rows").getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                                if(elements.getString("status").equalsIgnoreCase("ok")) {
                                    estimatedTimes.put(transportType,elements.getJSONObject("duration").getInt("value"));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d(TAG, error.getMessage());
                        }
                    }){
            };

            MySingleton.getInstance(context).addToRequestQueue(jsObjRequest);
        }
    }

    public void changeTransportType(TransportType transportType){
        actualEstimatedTime = estimatedTimes.get(transportType.getName());
    }

    public int getActualEstimatedTime() {
        return actualEstimatedTime;
    }

    public void setActualEstimatedTime(int actualEstimatedTime) {
        this.actualEstimatedTime = actualEstimatedTime;
    }

    public void decreaseActualEstimatedTime(int i) {
        actualEstimatedTime -= i;
    }

    public void addActualEstimatedTime(int i) {
        actualEstimatedTime += i;
    }

    public String getActualEstimatedTimeText() {
        String text;
        if(actualEstimatedTime > (60*60*24)-1) { //23h 59m 59s
            text = actualEstimatedTime/86400+"d "+(actualEstimatedTime%86400)/3600+"h "+((actualEstimatedTime%86400)%3600)/60+"m "+((actualEstimatedTime%86400)%3600)%60+"s";
        }else if(actualEstimatedTime > (60*60)-1){ //59m 59s
            text = actualEstimatedTime/3600+"h "+(actualEstimatedTime%3600)/60+"m "+(actualEstimatedTime%3600)%60+"s";
        }else if(actualEstimatedTime > 59){
            text = actualEstimatedTime/60+"m "+actualEstimatedTime%60+"s";
        }else if(actualEstimatedTime > 0){
            text = actualEstimatedTime+"s";
        }else{
            text = "-";
        }
        return text;
    }

    public void clear() {
        actualEstimatedTime = 0;
        currentDestination = null;
        estimatedTimes.put(TransportType.BICYCLING.getName(),0);
        estimatedTimes.put(TransportType.DRIVING.getName(),0);
        estimatedTimes.put(TransportType.WALKING.getName(),0);
        estimatedTimes.put(TransportType.PUBLIC_TRANSPORT.getName(),0);
    }

    public void setCurrentDestination(LatLng currentDestination) {
        this.currentDestination = currentDestination;
    }

    public LatLng getCurrentDestination() {
        return currentDestination;
    }
}
