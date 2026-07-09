package com.passeportreparation.diagnosis.service;

import java.util.UUID;

public class DiagnosisNotFoundException extends RuntimeException {
    public DiagnosisNotFoundException(UUID id) {
        super("Diagnostic introuvable: " + id);
    }
}
