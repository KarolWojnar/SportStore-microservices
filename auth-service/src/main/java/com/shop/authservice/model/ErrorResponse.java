package com.shop.authservice.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    public ErrorResponse(String message) {
        this.message = message;
    }



    @Getter
    private String message;
    private Map<String, String> details;


    public ErrorResponse(String message, Map<String, String> details) {
        this.message = message;
        this.details = details;
    }
}
