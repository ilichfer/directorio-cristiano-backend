package com.directoriocristiano.exception;

import lombok.Getter;

/**
 * El estado actual no permite la acción (409). {@code current} lleva, si aplica, la versión
 * actualizada del recurso para que el cliente la vuelva a revisar.
 */
@Getter
public class ConflictException extends RuntimeException {

    private final Object current;

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, Object current) {
        super(message);
        this.current = current;
    }
}
