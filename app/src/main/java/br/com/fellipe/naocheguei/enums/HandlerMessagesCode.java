package br.com.fellipe.naocheguei.enums;

/**
 * Created by Fellipe on 13/11/2016.
 */

public enum HandlerMessagesCode {

    TIME_FINISHED(1),
    ARRIVED_AT_DESTIONATION(2),
    X_MINUTES_LEFT(3);

    private int code;

    HandlerMessagesCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
