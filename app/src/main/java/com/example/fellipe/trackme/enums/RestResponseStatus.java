package com.example.fellipe.trackme.enums;

/**
 * Created by Fellipe on 17/10/2016.
 */
public enum RestResponseStatus {

    OK(0);

    private int statusCode;

    RestResponseStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
