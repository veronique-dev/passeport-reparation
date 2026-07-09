package com.passeportreparation.media.config;

import com.passeportreparation.common.dto.ApiError;
import com.passeportreparation.media.service.MediaNotFoundException;
import com.passeportreparation.media.service.MediaStorageException;
import com.passeportreparation.media.service.MediaValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class MediaExceptionHandler {

    @ExceptionHandler(MediaValidationException.class)
    public ResponseEntity<ApiError> validation(MediaValidationException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MediaNotFoundException.class)
    public ResponseEntity<ApiError> notFound(MediaNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MediaStorageException.class)
    public ResponseEntity<ApiError> storage(MediaStorageException ex, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request.getRequestURI());
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build());
    }
}
