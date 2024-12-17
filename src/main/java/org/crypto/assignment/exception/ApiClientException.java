package org.crypto.assignment.exception;

public class ApiClientException extends RuntimeException{
    public ApiClientException(String message) {
        super(message);
    }

    public ApiClientException(String message, Exception e) {
        super(message);
    }
}

