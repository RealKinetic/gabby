package com.realkinetic.app.gabby.model.error;

/**
 * Thrown when an acknowledge id is sent to the server that is in incorrect
 * format.
 */
public class InvalidAckIdException extends Exception {
    public InvalidAckIdException(String message, Throwable cause) {
        super(message, cause);
    }
}
