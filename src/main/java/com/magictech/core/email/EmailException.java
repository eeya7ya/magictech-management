package com.magictech.core.email;

/**
 * Exception thrown when email operations fail.
 */
public class EmailException extends Exception {

    public EmailException(String message) {
        super(message);
    }

    public EmailException(String message, Throwable cause) {
        super(message, cause);
    }
}
