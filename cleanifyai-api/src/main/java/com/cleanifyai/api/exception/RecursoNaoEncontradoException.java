package com.cleanifyai.api.exception;

@Deprecated(forRemoval = false)
public class RecursoNaoEncontradoException extends ResourceNotFoundException {

    public RecursoNaoEncontradoException(String message) {
        super(message);
    }
}

