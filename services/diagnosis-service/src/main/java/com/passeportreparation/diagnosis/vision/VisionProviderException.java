package com.passeportreparation.diagnosis.vision;

public class VisionProviderException extends RuntimeException {
    public VisionProviderException(String message) {
        super(message);
    }

    public VisionProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
