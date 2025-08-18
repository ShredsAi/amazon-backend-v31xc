package ai.shreds.adapter.exceptions;

import ai.shreds.application.exceptions.ApplicationOrderCreationException;
import ai.shreds.application.exceptions.ApplicationOrderValidationException;
import ai.shreds.shared.dtos.SharedErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class AdapterOrderExceptionHandler {

    @ExceptionHandler(ApplicationOrderValidationException.class)
    public ResponseEntity<SharedErrorResponse> handleValidationException(
            ApplicationOrderValidationException ex,
            WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new SharedErrorResponse(
                        ex.getMessage(),
                        "ORDER_VALIDATION_ERROR",
                        request.getDescription(false)));
    }

    @ExceptionHandler(ApplicationOrderCreationException.class)
    public ResponseEntity<SharedErrorResponse> handleCreationException(
            ApplicationOrderCreationException ex,
            WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new SharedErrorResponse(
                        ex.getMessage(),
                        "ORDER_CREATION_ERROR",
                        request.getDescription(false)));
    }

    @ExceptionHandler(AdapterOrderCreationException.class)
    public ResponseEntity<SharedErrorResponse> handleAdapterCreationException(
            AdapterOrderCreationException ex,
            WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SharedErrorResponse(
                        ex.getMessage(),
                        ex.getErrorCode(),
                        request.getDescription(false)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<SharedErrorResponse> handleException(
            Exception ex,
            WebRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new SharedErrorResponse(
                        "An unexpected error occurred: " + ex.getMessage(),
                        "INTERNAL_SERVER_ERROR",
                        request.getDescription(false)));
    }
}