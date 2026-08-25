package com.cowtrack.service.impl;

import com.cowtrack.dto.request.CowRequest;
import com.cowtrack.exception.BusinessException;
import com.cowtrack.exception.ValidationException;
import com.cowtrack.service.CowService;
import com.cowtrack.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/gif", "image/webp");

    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

    private final CowService cowService;

    @Value("${cowtrack.storage.upload-dir}")
    private String uploadDir;

    @Override
    public Map<String, Object> storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("No file supplied");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new ValidationException("Image must be 5MB or smaller");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(String.valueOf(file.getContentType()))) {
            throw new ValidationException(
                    "Unsupported image type: " + file.getContentType());
        }

        try {
            Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(directory);

            // Generate the name rather than trusting the client's, which would
            // otherwise allow path traversal via something like "../../app.jar".
            String extension = extensionFor(file.getContentType());
            String storedName = UUID.randomUUID() + extension;
            Path target = directory.resolve(storedName);

            file.transferTo(target);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("filename", storedName);
            result.put("originalName", file.getOriginalFilename());
            result.put("size", file.getSize());
            result.put("contentType", file.getContentType());
            result.put("url", "/api/upload/files/" + storedName);
            return result;

        } catch (IOException e) {
            log.error("Failed to store uploaded image", e);
            throw new BusinessException("Could not store the uploaded image");
        }
    }

    @Override
    public Map<String, Object> importCowsCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("No file supplied");
        }

        List<Map<String, Object>> failures = new ArrayList<>();
        int imported = 0;
        int lineNumber = 1;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new ValidationException("CSV file is empty");
            }

            List<String> columns = Arrays.stream(header.split(","))
                    .map(column -> column.trim().toLowerCase())
                    .toList();
            requireColumns(columns, "tagid", "name");

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    cowService.createCow(toCowRequest(columns, line));
                    imported++;
                } catch (Exception e) {
                    // One bad row should not discard the rest of the file.
                    failures.add(Map.of("line", lineNumber, "error", e.getMessage()));
                }
            }
        } catch (IOException e) {
            log.error("Failed to read uploaded CSV", e);
            throw new BusinessException("Could not read the uploaded CSV");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("imported", imported);
        summary.put("failed", failures.size());
        summary.put("errors", failures);
        return summary;
    }

    private void requireColumns(List<String> columns, String... required) {
        for (String column : required) {
            if (!columns.contains(column)) {
                throw new ValidationException(
                        "CSV must contain a '" + column + "' column; found " + columns);
            }
        }
    }

    private CowRequest toCowRequest(List<String> columns, String line) {
        String[] values = line.split(",", -1);
        CowRequest request = new CowRequest();

        for (int i = 0; i < columns.size() && i < values.length; i++) {
            String value = values[i].trim();
            if (value.isEmpty()) {
                continue;
            }
            switch (columns.get(i)) {
                case "tagid" -> request.setTagId(value);
                case "name" -> request.setName(value);
                case "breed" -> request.setBreed(value);
                case "dateofbirth" -> request.setDateOfBirth(parseDate(value));
                case "caretakerid" -> request.setCaretakerId(Long.valueOf(value));
                default -> { /* Unknown columns are ignored. */ }
            }
        }
        return request;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date '" + value + "', expected yyyy-MM-dd");
        }
    }

    private String extensionFor(String contentType) {
        return switch (String.valueOf(contentType)) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
