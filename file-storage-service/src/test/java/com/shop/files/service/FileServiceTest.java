package com.shop.files.service;

import com.shop.files.model.dto.FileUploadMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@EmbeddedKafka
class FileServiceTest {

    @InjectMocks
    private FileService fileService;

    private final String testFilename = "test.txt";

    @BeforeEach
    void setUp() throws IOException {
        Path uploadsDir = Paths.get("./uploads");
        if (!Files.exists(uploadsDir)) {
            Files.createDirectories(uploadsDir);
        }
        String testContent = "Test file content";
        Files.write(uploadsDir.resolve(testFilename), testContent.getBytes());

        Path staticDir = Paths.get("./src/test/resources/static/images");
        if (!Files.exists(staticDir)) {
            Files.createDirectories(staticDir);
        }
        Files.write(staticDir.resolve(testFilename), testContent.getBytes());
    }

    @Test
    void getFile_shouldReturnFileFromUploads() {
        Mono<ResponseEntity<Resource>> result = fileService.getFile(testFilename);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertEquals(200, response.getStatusCodeValue());
                    assertInstanceOf(FileSystemResource.class, response.getBody());
                    assertNotNull(response.getHeaders().get(HttpHeaders.CONTENT_TYPE));
                })
                .verifyComplete();
    }

    @Test
    void getFile_shouldReturnEmptyWhenFileNotFound() {
        Mono<ResponseEntity<Resource>> result = fileService.getFile("nonexistent.txt");

        StepVerifier.create(result)
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void consumeFileUpload_shouldSaveFileToUploads() throws IOException {
        FileUploadMessage message = new FileUploadMessage();
        message.setFileName("kafka-test.txt");
        message.setFileData("Kafka test content".getBytes());
        ConsumerRecord<String, FileUploadMessage> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", message);

        fileService.consumeFileUpload(consumerRecord);

        Path filePath = Paths.get("./uploads/kafka-test.txt");
        assertTrue(Files.exists(filePath));
        assertEquals("Kafka test content", new String(Files.readAllBytes(filePath)));

        Files.deleteIfExists(filePath);
    }
}