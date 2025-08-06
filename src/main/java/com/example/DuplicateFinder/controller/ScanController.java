package com.example.DuplicateFinder.controller;

import com.example.DuplicateFinder.dto.DeleteRequest;
import com.example.DuplicateFinder.service.CategorizationService;
import com.example.DuplicateFinder.service.FileHashInfo;
import com.example.DuplicateFinder.service.FileHashingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ScanController {

    @Autowired
    private FileHashingService fileHashingService;

    @Autowired
    private CategorizationService categorizationService;

//    @PostMapping("/scan")
//    public ResponseEntity<?> scanDirectory(@RequestBody Map<String, String> payload) {
//        String path = payload.get("path");
//        if (path == null || !new File(path).isDirectory()) {
//            return ResponseEntity.badRequest().body("Invalid directory path provided.");
//        }
//
//        try {
//            // Call 1: Use the new efficient method to find duplicates.
//            Map<String, List<String>> duplicates = fileHashingService.findDuplicates(path);
//
//            // Call 2: Get the list of ALL files for the categorization feature.
//            List<FileHashInfo> allFiles = fileHashingService.scanAndHashFiles(path);
//
//            Map<String, Object> response = new HashMap<>();
//            // The frontend likely expects an array of arrays of file paths.
//            response.put("duplicates", duplicates.values());
//            response.put("categorizedApps", categorizationService.categorize(allFiles));
//
//            return ResponseEntity.ok(response);
//        } catch (IOException e) {
//            // Log the full exception for better debugging on the backend.
//            e.printStackTrace();
//            return ResponseEntity.status(500).body("Failed to scan directory: " + e.getMessage());
//        }
//    }

//
    // CORRECTED AND EFFICIENT VERSION
//    @PostMapping("/scan")
//    public ResponseEntity<?> scanDirectory(@RequestBody Map<String, String> payload) {
//        String path = payload.get("path");
//        if (path == null || path.trim().isEmpty() || !new File(path).isDirectory()) {
//            return ResponseEntity.badRequest().body("Invalid or non-existent directory path provided.");
//        }
//
//        try {
//            // 1. Scan the directory ONCE to get all file information.
//            List<FileHashInfo> allFiles = fileHashingService.scanAndHashFiles(path);
//
//            // 2. Process the results from the single scan to find duplicates.
//            Map<String, List<FileHashInfo>> duplicates = fileHashingService.findDuplicates(allFiles);
//
//            // 3. Construct the response object.
//            Map<String, Object> response = new HashMap<>();
//            response.put("duplicates", duplicates.values());
//            response.put("categorizedApps", categorizationService.categorize(allFiles));
//
//            return ResponseEntity.ok(response);
//
//        } catch (IOException e) {
//            e.printStackTrace(); // Log the full exception for better backend debugging
//            return ResponseEntity.status(500).body("Failed to scan directory: " + e.getMessage());
//        }
//    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanDirectory(@RequestBody Map<String, String> payload) {
        String path = payload.get("path");
        String scanType = payload.getOrDefault("scanType", "EXACT");

        if (path == null || path.trim().isEmpty() || !new File(path).isDirectory()) {
            return ResponseEntity.badRequest().body("Invalid or non-existent directory path provided.");
        }

        try {

            Map<String, Object> response = new HashMap<>();

            if ("FUZZY".equalsIgnoreCase(scanType)) {
                List<FileHashInfo> allFiles = fileHashingService.scanAndGetFileContent(path);
                // INCREASED THRESHOLD: More lenient for finding similarities.
                int threshold = 50;
                List<List<FileHashInfo>> similarFiles = fileHashingService.findSimilarFilesByLevenshtein(allFiles, threshold);

                response.put("duplicates", similarFiles);
                response.put("categorizedApps", categorizationService.categorize(allFiles));

            } else { // Default to "EXACT"
                List<FileHashInfo> allFiles = fileHashingService.scanAndHashFiles(path);
                Map<String, List<FileHashInfo>> duplicates = fileHashingService.findDuplicates(allFiles);

                response.put("duplicates", duplicates.values());
                response.put("categorizedApps", categorizationService.categorize(allFiles));
                response.put("emptyFolders", new ArrayList<>());
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
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

    // NEW: Endpoint for file previews
    @GetMapping("/preview")
    public ResponseEntity<?> getFilePreview(@RequestParam String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("File path is required.");
        }

        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return ResponseEntity.notFound().build();
            }

            // Read up to 500 lines to prevent memory issues with huge files
            String content = Files.lines(path).limit(500).collect(Collectors.joining("\n"));

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(content);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Could not read file preview: " + e.getMessage());
        }
    }
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