package com.cowtrack.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a client sends more requests than the configured window allows.
 * A caller that repeatedly guesses credentials for a locked account or hammers
 * the login endpoint from one address receives this instead of an infinite
 * stream of 401s.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}