package com.example.fellipe.trackme.dto;

import com.example.fellipe.trackme.enums.TransportType;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Fellipe on 17/10/2016.
 */

public class TripInfo {

    private String tripId;
    private LatLng latLng;
    private int estimatedTime;
    private TransportType transportType;

    public TripInfo(String tripId, LatLng latLng, int estimatedTime, TransportType transportType) {
        this.tripId = tripId;
        this.latLng = latLng;
        this.estimatedTime = estimatedTime;
        this.transportType = transportType;
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

    public TransportType getTransportType() {
        return transportType;
    }

    public void setTransportType(TransportType transportType) {
        this.transportType = transportType;
    }
}
