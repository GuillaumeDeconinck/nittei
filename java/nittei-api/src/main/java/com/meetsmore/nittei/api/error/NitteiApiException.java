package com.meetsmore.nittei.api.error;

public class NitteiApiException extends RuntimeException {

    private final NitteiErrorCode code;

    public NitteiApiException(NitteiErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public NitteiErrorCode getCode() {
        return code;
    }
}
