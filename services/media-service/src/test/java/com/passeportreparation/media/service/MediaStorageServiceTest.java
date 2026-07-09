package com.passeportreparation.media.service;

import com.passeportreparation.common.dto.MediaUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaStorageServiceTest {

    @TempDir
    Path tempDir;

    private MediaStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new MediaStorageService(tempDir.toString(), "http://localhost:8090");
    }

    @Test
    void us01_storesPngAndReturnsMediaId() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "appareil.png",
                MediaType.IMAGE_PNG_VALUE,
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
        );

        MediaUploadResponse response = service.store(file);

        assertThat(response.getMediaId()).isNotBlank();
        assertThat(response.getContentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
        assertThat(response.getSizeBytes()).isEqualTo(4);
        assertThat(response.getUrl()).isEqualTo("http://localhost:8090/api/media/" + response.getMediaId());

        Resource resource = service.loadAsResource(response.getMediaId());
        assertThat(resource.exists()).isTrue();
    }

    @Test
    void us01_rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(MediaValidationException.class)
                .hasMessageContaining("vide");
    }

    @Test
    void us01_rejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(MediaValidationException.class)
                .hasMessageContaining("Type non supporté");
    }

    @Test
    void us01_throwsWhenMediaNotFound() {
        assertThatThrownBy(() -> service.loadAsResource("missing-id"))
                .isInstanceOf(MediaNotFoundException.class);
    }

    @Test
    void us01_rejectsPathTraversalMediaId() {
        assertThatThrownBy(() -> service.loadAsResource("../secret"))
                .isInstanceOf(MediaNotFoundException.class);
    }
}
