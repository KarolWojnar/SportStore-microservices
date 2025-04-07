package com.shop.files.controller;

import com.shop.files.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    @GetMapping("/{filename:.+}")
    public Mono<ResponseEntity<Resource>> getFile(@PathVariable String filename) {
        return fileService.getFile(filename)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<String> uploadFile(@RequestPart("file") FilePart filePart) {
        String filename = filePart.filename();
        Path filePath = Paths.get("./uploads/" + filename);

        return filePart.transferTo(filePath)
                .then(Mono.just("http://file-storage-service/images/" + filename));
    }
}
