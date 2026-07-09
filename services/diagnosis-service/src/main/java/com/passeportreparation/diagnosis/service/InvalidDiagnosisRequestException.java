package com.passeportreparation.diagnosis.service;

public class InvalidDiagnosisRequestException extends RuntimeException {
    public InvalidDiagnosisRequestException(String message) {
        super(message);
    }
}
