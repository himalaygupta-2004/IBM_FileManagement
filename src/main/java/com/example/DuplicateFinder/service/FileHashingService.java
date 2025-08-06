package com.example.DuplicateFinder.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileHashingService {

    /**
     * Finds duplicate files in a directory using an efficient two-pass strategy.
     * It first groups files by size, then calculates hashes only for potential duplicates.
     *
     * @param fileInfos The path to the directory to scan.
     * @return A map where the key is the content hash and the value is a list of paths for the duplicate files.
     * @throws IOException if an I/O error occurs during file scanning.
     */
    public Map<String, List<FileHashInfo>> findDuplicates(List<FileHashInfo> fileInfos) {
        // 1. Group the list of all files by their content hash.
        return fileInfos.stream()
                .collect(Collectors.groupingBy(FileHashInfo::getHash))
                // 2. Convert the stream of map entries back into a stream.
                .entrySet().stream()
                // 3. Keep only the groups that have more than one file (i.e., the duplicates).
                .filter(entry -> entry.getValue().size() > 1)
                // 4. Collect the results into a new map of [Hash -> List of Duplicate Files].
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    /**
     * Scans a directory and returns a list of all files with their info.
     * This is needed for features like categorization.
     *
     * @param directoryPath The path to the directory to scan.
     * @return A list of FileHashInfo objects for all files found.
     * @throws IOException if an I/O error occurs.
     */
    public List<FileHashInfo> scanAndHashFiles(String directoryPath) throws IOException {
        List<FileHashInfo> fileInfos = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            String hash = calculateSha256(path.toFile().toPath());
                            long size = Files.size(path);
                            int hashSize = hash.length();
                            String fileName = path.getFileName().toString();// <-- CALCULATE HASH LENGTH
                            // v-- ADDED HASH SIZE TO CONSTRUCTOR
                            fileInfos.add(new FileHashInfo(path.toString(), fileName,hash, size, hashSize));
                        } catch (IOException e) {
                            System.err.println("Could not process file: " + path + " - " + e.getMessage());
                        }
                    });
        }
        return fileInfos;
    }


    private String calculateSha256(Path path) {
        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            return DigestUtils.sha256Hex(fis);
        } catch (IOException e) {
            System.err.println("Could not hash file: " + path + " - " + e.getMessage());
            return "error-hash-" + path.toString();
        }
    }

    private long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            System.err.println("Could not get size for file: " + path + " - " + e.getMessage());
            return -1L;
        }
    }


}