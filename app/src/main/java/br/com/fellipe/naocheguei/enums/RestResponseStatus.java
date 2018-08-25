package br.com.fellipe.naocheguei.enums;

/**
 * Created by Fellipe on 17/10/2016.
 */
public enum RestResponseStatus {

    TRIP_FOUND(0),
    CONTACT_DELETED(1),
    CONTACT_ALREADY_EXISTS(2),
    CONTACT_ADD(3);

    private int statusCode;

    RestResponseStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
