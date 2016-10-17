package com.example.fellipe.trackme.util;

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
    private String tripId;


    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
