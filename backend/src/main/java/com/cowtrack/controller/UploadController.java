package com.cowtrack.controller;

import com.cowtrack.exception.ResourceNotFoundException;
import com.cowtrack.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController extends BaseController {

    private final StorageService storageService;

    @Value("${cowtrack.storage.upload-dir}")
    private String uploadDir;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        return created(storageService.storeImage(file));
    }

    @PostMapping(value = "/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCsv(@RequestParam("file") MultipartFile file) {
        return success("CSV processed", storageService.importCowsCsv(file));
    }

    /** Serves a previously uploaded image. */
    @GetMapping("/files/{filename}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        // Resolve then verify containment, so a crafted filename cannot escape the
        // upload directory even though names are generated on our side.
        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path target = directory.resolve(filename).normalize();

        if (!target.startsWith(directory) || !Files.exists(target)) {
            throw new ResourceNotFoundException("File not found: " + filename);
        }

        try {
            Resource resource = new UrlResource(target.toUri());
            String contentType = Files.probeContentType(target);

            return ResponseEntity.ok()
                    .contentType(contentType == null
                            ? MediaType.APPLICATION_OCTET_STREAM
                            : MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File not found: " + filename);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Could not read file: " + filename);
        }
    }
}
