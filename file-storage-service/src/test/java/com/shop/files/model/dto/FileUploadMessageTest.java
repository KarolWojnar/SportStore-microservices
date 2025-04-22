package com.shop.files.model.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileUploadMessageTest {

    @Test
    void fileUploadMessage_shouldHaveCorrectFields() {
        String filename = "test.txt";
        String contentType = "text/plain";
        byte[] data = "test content".getBytes();

        FileUploadMessage message = new FileUploadMessage(filename, contentType, data);

        assertEquals(filename, message.getFileName());
        assertEquals(contentType, message.getContentType());
        assertArrayEquals(data, message.getFileData());
    }

    @Test
    void fileUploadMessage_shouldHaveNoArgsConstructor() {
        FileUploadMessage message = new FileUploadMessage();

        assertNotNull(message);
    }
}