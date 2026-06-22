package com.directoriocristiano.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, String field, Object value) {
        super("%s no encontrado con %s: '%s'".formatted(resource, field, value));
    }
}
