package com.directoriocristiano.exception;

import lombok.Getter;

import java.util.List;

/** El borrador no tiene los datos mínimos para enviarse a revisión (400, FR-008). */
@Getter
public class IncompleteBusinessException extends RuntimeException {

    private final List<String> missing;

    public IncompleteBusinessException(List<String> missing) {
        super("Faltan datos para enviar a revisión.");
        this.missing = missing;
    }
}
