package com.directoriocristiano.exception;

/** La acción no está permitida para este usuario (403). */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
