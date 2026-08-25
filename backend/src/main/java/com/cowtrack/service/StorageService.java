package com.cowtrack.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface StorageService {

    /** Stores an image and returns its metadata, including a retrieval URL. */
    Map<String, Object> storeImage(MultipartFile file);

    /**
     * Parses an uploaded CSV of cattle and creates the rows it contains.
     * Returns a per-row summary rather than failing the whole upload on one bad line.
     */
    Map<String, Object> importCowsCsv(MultipartFile file);
}
