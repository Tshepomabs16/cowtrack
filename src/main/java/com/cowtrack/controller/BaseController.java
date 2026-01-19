package com.cowtrack.controller;

import com.cowtrack.dto.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "*", maxAge = 3600)  // Allow all origins for now
public class BaseController {

    protected <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> success(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    protected <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resource created successfully", data));
    }

    protected ResponseEntity<ApiResponse<Void>> noContent() {
        return ResponseEntity.noContent().build();
    }

    protected ResponseEntity<ApiResponse<Void>> accepted() {
        return ResponseEntity.accepted()
                .body(ApiResponse.success("Request accepted", null));
    }
}