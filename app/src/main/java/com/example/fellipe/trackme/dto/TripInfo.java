package com.example.fellipe.trackme.dto;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Fellipe on 17/10/2016.
 */

public class TripInfo {

    private String tripId;
    private LatLng latLng;
    private int estimatedTime;

    public TripInfo(String tripId, LatLng latLng, int estimatedTime) {
        this.tripId = tripId;
        this.latLng = latLng;
        this.estimatedTime = estimatedTime;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public void setLatLng(LatLng latLng) {
        this.latLng = latLng;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }
}
