package com.openbas.intelligence.api.exception;

public class ResourceAlreadyExistsException extends RuntimeException {
    public ResourceAlreadyExistsException(String message) { super(message); }
    public ResourceAlreadyExistsException(String resourceType, String id) {
        super(resourceType + " already exists with id: " + id);
    }
}
