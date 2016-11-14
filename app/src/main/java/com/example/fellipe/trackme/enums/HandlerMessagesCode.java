package com.example.fellipe.trackme.enums;

/**
 * Created by Fellipe on 13/11/2016.
 */

public enum HandlerMessagesCode {

    TIME_FINISHED(1);

    private int code;

    HandlerMessagesCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
