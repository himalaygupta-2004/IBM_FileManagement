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
     * @param directoryPath The path to the directory to scan.
     * @return A map where the key is the content hash and the value is a list of paths for the duplicate files.
     * @throws IOException if an I/O error occurs during file scanning.
     */
    public Map<String, List<String>> findDuplicates(String directoryPath) throws IOException {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .collect(Collectors.groupingBy(this::getFileSize)) // Pass 1: Group by size
                    .values().stream()
                    .filter(list -> list.size() > 1) // Filter for potential duplicates
                    .flatMap(List::stream)
                    .collect(Collectors.groupingBy(this::calculateSha256)) // Pass 2: Group by hash
                    .entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1) // Filter for actual duplicates
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().stream().map(Path::toString).collect(Collectors.toList())
                    ));
        }
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
                        // We create a FileHashInfo object for every file.
                        // Hash and size are calculated using the existing helper methods.
                        String hash = calculateSha256(path);
                        long size = getFileSize(path);
                        fileInfos.add(new FileHashInfo(path.toString(), hash, size));
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