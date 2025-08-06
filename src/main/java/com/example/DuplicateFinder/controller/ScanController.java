package com.example.DuplicateFinder.controller;

import com.example.DuplicateFinder.dto.DeleteRequest;
import com.example.DuplicateFinder.service.CategorizationService;
import com.example.DuplicateFinder.service.FileHashInfo;
import com.example.DuplicateFinder.service.FileHashingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class ScanController {

    @Autowired
    private FileHashingService fileHashingService;

    @Autowired
    private CategorizationService categorizationService;

    @PostMapping("/scan")
    public ResponseEntity<?> scanDirectory(@RequestBody Map<String, String> payload) {
        String path = payload.get("path");
        if (path == null || !new File(path).isDirectory()) {
            return ResponseEntity.badRequest().body("Invalid directory path provided.");
        }

        try {
            // Call 1: Use the new efficient method to find duplicates.
            Map<String, List<String>> duplicates = fileHashingService.findDuplicates(path);

            // Call 2: Get the list of ALL files for the categorization feature.
            List<FileHashInfo> allFiles = fileHashingService.scanAndHashFiles(path);

            Map<String, Object> response = new HashMap<>();
            // The frontend likely expects an array of arrays of file paths.
            response.put("duplicates", duplicates.values());
            response.put("categorizedApps", categorizationService.categorize(allFiles));

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            // Log the full exception for better debugging on the backend.
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to scan directory: " + e.getMessage());
        }
    }

//    @PostMapping("/delete-files")
//    public ResponseEntity<?> deleteFiles(@RequestBody List<String> filePaths) {
//        if (filePaths == null || filePaths.isEmpty()) {
//            return ResponseEntity.badRequest().body("No file paths provided for deletion.");
//        }
//
//        List<String> deletedFiles = new ArrayList<>();
//        List<String> failedFiles = new ArrayList<>();
//
//        for (String pathString : filePaths) {
//            try {
//                Path pathToDelete = Paths.get(pathString);
//                if (Files.exists(pathToDelete)) {
//                    Files.delete(pathToDelete);
//                    deletedFiles.add(pathString);
//                } else {
//                    failedFiles.add(pathString + " (File not found)");
//                }
//            } catch (IOException e) {
//                failedFiles.add(pathString + " (Error: " + e.getMessage() + ")");
//                System.err.println("Failed to delete file '" + pathString + "'. Reason: " + e.getMessage());
//            }
//        }
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("deleted", deletedFiles);
//        response.put("failed", failedFiles);
//
//        if (!failedFiles.isEmpty()) {
//            response.put("message", "Some files could not be deleted.");
//        } else {
//            response.put("message", "Selected files deleted successfully.");
//        }
//
//        return ResponseEntity.ok(response);
//    }
@PostMapping("/delete-files")
public ResponseEntity<?> deleteFiles(@RequestBody DeleteRequest request) {
    if (request.getBasePath() == null || request.getFilesToDelete() == null || request.getFilesToDelete().isEmpty()) {
        return ResponseEntity.badRequest().body("Invalid request. 'basePath' and 'filesToDelete' are required.");
    }

    List<String> deletedFiles = new ArrayList<>();
    List<String> failedFiles = new ArrayList<>();

    // 1. Create a safe, absolute path for the allowed directory
    Path safeBasePath;
    try {
        safeBasePath = Paths.get(request.getBasePath()).toAbsolutePath().normalize();
    } catch (Exception e) {
        return ResponseEntity.badRequest().body("Invalid base path provided.");
    }

    for (String pathString : request.getFilesToDelete()) {
        try {
            // 2. Create an absolute path for the file to be deleted
            Path pathToDelete = Paths.get(pathString).toAbsolutePath().normalize();

            // 3. **SECURITY CHECK**: Ensure the file is inside the safe directory
            if (!pathToDelete.startsWith(safeBasePath)) {
                failedFiles.add(pathString + " (Security error: Path is outside the allowed directory)");
                continue; // Skip to the next file
            }

            // 4. Proceed with deletion
            if (Files.exists(pathToDelete) && Files.isRegularFile(pathToDelete)) {
                Files.delete(pathToDelete);
                deletedFiles.add(pathString);
            } else {
                failedFiles.add(pathString + " (File not found or is a directory)");
            }
        } catch (IOException e) {
            failedFiles.add(pathString + " (Error: " + e.getMessage() + ")");
            System.err.println("Failed to delete file '" + pathString + "'. Reason: " + e.getMessage());
        } catch (Exception e) {
            failedFiles.add(pathString + " (Invalid path format)");
        }
    }

    Map<String, Object> response = new HashMap<>();
    response.put("deleted", deletedFiles);
    response.put("failed", failedFiles);

    if (failedFiles.isEmpty()) {
        response.put("message", "Selected files deleted successfully.");
    } else {
        response.put("message", "Some files could not be deleted.");
    }

    return ResponseEntity.ok(response);
}
}