package com.passeportreparation.media.controller;

import com.passeportreparation.common.dto.MediaUploadResponse;
import com.passeportreparation.media.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

    private final MediaStorageService mediaStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MediaUploadResponse upload(@RequestPart("file") MultipartFile file) {
        return mediaStorageService.store(file);
    }

    @GetMapping("/{mediaId}")
    public ResponseEntity<Resource> download(@PathVariable String mediaId) {
        Resource resource = mediaStorageService.loadAsResource(mediaId);
        String contentType = mediaStorageService.detectContentType(mediaId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
}
