package com.example.fellipe.trackme.util;

import com.example.fellipe.trackme.dto.TripInfo;

/**
 * Created by Fellipe on 28/07/2016.
 */
public class Session {
    private static Session ourInstance = new Session();

    public static Session getInstance() {
        return ourInstance;
    }

    private Session() {
    }

    private String userId;
    private TripInfo trip;

    public TripInfo getTrip() {
        return trip;
    }

    public void setTrip(TripInfo trip) {
        this.trip = trip;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
