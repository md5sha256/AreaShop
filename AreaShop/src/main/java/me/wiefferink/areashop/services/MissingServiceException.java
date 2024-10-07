package me.wiefferink.areashop.services;

public class MissingServiceException extends RuntimeException {

    public MissingServiceException() {
    }

    public MissingServiceException(String message) {
        super(message);
    }

    public MissingServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingServiceException(Throwable cause) {
        super(cause);
    }

    public MissingServiceException(String message,
                                   Throwable cause,
                                   boolean enableSuppression,
                                   boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
