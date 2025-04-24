package com.shop.files.service;

import com.shop.files.model.dto.FileUploadMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class FileService {

    public  Mono<ResponseEntity<Resource>> getFile(String filename) {
        Path uploadsPath = Paths.get("./uploads/" + filename);
        Mono<ResponseEntity<Resource>> CONTENT_TYPE = getResponseEntityMono(uploadsPath);
        if (CONTENT_TYPE != null) return CONTENT_TYPE;

        Path staticPath = Paths.get("./src/main/resources/static/images/" + filename);
        Mono<ResponseEntity<Resource>> CONTENT_TYPE1 = getResponseEntityMono(staticPath);
        if (CONTENT_TYPE1 != null) return CONTENT_TYPE1;
        return Mono.empty();
    }

    private static Mono<ResponseEntity<Resource>> getResponseEntityMono(Path uploadsPath) {
        if (Files.exists(uploadsPath)) {
            try {
                Resource resource = new FileSystemResource(uploadsPath);
                String contentType = Files.probeContentType(uploadsPath);
                return Mono.just(
                        ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_TYPE, contentType)
                                .body(resource)
                );
            } catch (IOException e) {
                return Mono.error(e);
            }
        }
        return null;
    }

    @KafkaListener(topics = "file-uploads", groupId = "file-storage-service",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeFileUpload(ConsumerRecord<String, FileUploadMessage> record) {
        FileUploadMessage fileMessage = record.value();

        try {
            Path uploadDir = Paths.get("./uploads");

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(fileMessage.getFileName());
            Files.write(filePath, fileMessage.getFileData());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
