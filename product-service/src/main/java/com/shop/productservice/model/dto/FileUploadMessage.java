package com.shop.productservice.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadMessage {
    private String fileName;
    private String contentType;
    private byte[] fileData;
}