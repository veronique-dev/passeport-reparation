package com.passeportreparation.diagnosis.config;

import com.passeportreparation.common.dto.ApiError;
import com.passeportreparation.diagnosis.media.MediaFetchException;
import com.passeportreparation.diagnosis.service.DiagnosisClaimConflictException;
import com.passeportreparation.diagnosis.service.DiagnosisNotFoundException;
import com.passeportreparation.diagnosis.service.InvalidDiagnosisRequestException;
import com.passeportreparation.diagnosis.vision.VisionProviderException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class DiagnosisExceptionHandler {

    @ExceptionHandler(DiagnosisNotFoundException.class)
    public ResponseEntity<ApiError> notFound(DiagnosisNotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MediaFetchException.class)
    public ResponseEntity<ApiError> mediaNotFound(MediaFetchException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidDiagnosisRequestException.class)
    public ResponseEntity<ApiError> invalid(InvalidDiagnosisRequestException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DiagnosisClaimConflictException.class)
    public ResponseEntity<ApiError> claimConflict(DiagnosisClaimConflictException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(VisionProviderException.class)
    public ResponseEntity<ApiError> vision(VisionProviderException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .orElse("Requête invalide");
        return error(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
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
