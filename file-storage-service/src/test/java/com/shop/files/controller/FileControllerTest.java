package com.shop.files.controller;

import com.shop.files.service.FileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock
    private FileService fileService;

    @Mock
    private FilePart filePart;

    @InjectMocks
    private FileController fileController;

    @Test
    void getFile_shouldReturnFileWhenExists() {
        Resource mockResource = mock(Resource.class);
        ResponseEntity<Resource> mockResponse = ResponseEntity.ok().body(mockResource);
        when(fileService.getFile(anyString())).thenReturn(Mono.just(mockResponse));

        Mono<ResponseEntity<Resource>> result = fileController.getFile("test.txt");

        StepVerifier.create(result)
                .expectNext(mockResponse)
                .verifyComplete();
    }

    @Test
    void getFile_shouldReturnNotFoundWhenFileNotExists() {
        when(fileService.getFile(anyString())).thenReturn(Mono.empty());

        Mono<ResponseEntity<Resource>> result = fileController.getFile("nonexistent.txt");

        StepVerifier.create(result)
                .assertNext(response -> assertEquals(404, response.getStatusCodeValue()))
                .verifyComplete();
    }

    @Test
    void uploadFile_shouldTransferFileAndReturnUrl() {
        String filename = "test.txt";
        Path filePath = Paths.get("./uploads/" + filename);
        when(filePart.filename()).thenReturn(filename);
        when(filePart.transferTo(filePath)).thenReturn(Mono.empty());

        Mono<String> result = fileController.uploadFile(filePart);

        StepVerifier.create(result)
                .expectNext("http://file-storage-service/images/test.txt")
                .verifyComplete();
    }
}