package com.passeportreparation.media.service;

public class MediaNotFoundException extends RuntimeException {
    public MediaNotFoundException(String mediaId) {
        super("Média introuvable: " + mediaId);
    }
}
