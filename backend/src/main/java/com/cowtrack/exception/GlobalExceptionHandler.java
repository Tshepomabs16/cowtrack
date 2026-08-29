package com.cowtrack.exception;

import com.cowtrack.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {

        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * An unmapped URL. Without this the catch-all {@code Exception} handler below
     * would report a 500 for what is really a 404 - visible once the application
     * began serving the bundled web client, since unmatched paths reach the static
     * resource resolver rather than a controller.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(
            NoResourceFoundException ex, WebRequest request) {

        log.warn("No handler for path: {}", getRequestPath(request));

        ErrorResponse errorResponse = new ErrorResponse(
                "No endpoint found for the requested path",
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * A role check refused the call. Spring Security raises this from
     * {@code @PreAuthorize}, and without an explicit handler it reached the
     * catch-all below and was reported as a 500 - so a client could not tell
     * "you are not allowed" from "the server broke", and every denial was
     * logged as a fault.
     *
     * <p>Deliberately terse: the message says the action was refused and not
     * which role would have been required, since that discloses the
     * authorisation model to a caller who already failed it.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {

        log.warn("Access denied to {}: {}", getRequestPath(request), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                "You do not have permission to perform this action",
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }

    /**
     * Framework-level request faults: wrong verb, missing or unparseable
     * parameter, malformed body. All of these are the caller's mistake, but
     * without explicit handlers they fell through to the catch-all below and
     * were reported as 500s, which sends anyone debugging them looking for a
     * server fault that does not exist.
     */
    @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequest(Exception ex, WebRequest request) {
        HttpStatus status = ex instanceof HttpRequestMethodNotSupportedException
                ? HttpStatus.METHOD_NOT_ALLOWED
                : HttpStatus.BAD_REQUEST;

        log.warn("Rejected request to {}: {}", getRequestPath(request), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                status.value(),
                status.getReasonPhrase(),
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {

        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Failed",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {

        log.warn("Unauthorized access: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequestsException(
            TooManyRequestsException ex, WebRequest request) {

        log.warn("Rate limited: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too Many Requests",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.warn("Business rule violation: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT.value(),
                "Business Rule Violation",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(
            DatabaseException ex, WebRequest request) {

        log.error("Database error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "An internal database error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Database Error",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {

        log.error("Data integrity violation: {}", ex.getMessage(), ex);

        String message = "Data integrity violation";
        if (ex.getMessage() != null && ex.getMessage().contains("Duplicate entry")) {
            message = "Duplicate entry found. Please check your input.";
        }

        ErrorResponse errorResponse = new ErrorResponse(
                message,
                HttpStatus.CONFLICT.value(),
                "Data Integrity Violation",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex, WebRequest request) {

        log.error("Data access error: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "An error occurred while accessing the database",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Data Access Error",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            Object rejectedValue = ((FieldError) error).getRejectedValue();

            validationErrors.add(new ErrorResponse.ValidationError(
                    fieldName, errorMessage, rejectedValue
            ));
        });

        log.warn("Validation failed for {} fields", validationErrors.size());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage("Validation failed");
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Bad Request");
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setValidationErrors(validationErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        List<ErrorResponse.ValidationError> validationErrors = new ArrayList<>();

        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            Object rejectedValue = violation.getInvalidValue();

            validationErrors.add(new ErrorResponse.ValidationError(
                    fieldName, errorMessage, rejectedValue
            ));
        });

        log.warn("Constraint violation for {} fields", validationErrors.size());

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setSuccess(false);
        errorResponse.setMessage("Constraint violation");
        errorResponse.setStatus(HttpStatus.BAD_REQUEST.value());
        errorResponse.setError("Bad Request");
        errorResponse.setTimestamp(LocalDateTime.now());
        errorResponse.setPath(getRequestPath(request));
        errorResponse.setValidationErrors(validationErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                "Invalid Argument",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                "An unexpected error occurred. Please try again later.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                getRequestPath(request)
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String getRequestPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            ServletWebRequest servletWebRequest = (ServletWebRequest) request;
            HttpServletRequest httpServletRequest = servletWebRequest.getRequest();
            return httpServletRequest.getRequestURI();
        }
        return "Unknown";
    }
}