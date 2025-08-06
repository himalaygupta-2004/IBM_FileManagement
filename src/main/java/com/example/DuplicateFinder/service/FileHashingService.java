package com.example.DuplicateFinder.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public List<FileHashInfo> scanAndGetFileContent(String directoryPath) throws IOException {
        List<FileHashInfo> fileInfos = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            // For this method, the "hash" is the file content itself.
                            // WARNING: This can be memory-intensive for very large files.
                            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                            long size = content.length();
                            String fileName = path.getFileName().toString();
                            // We don't need hashSize for this method.
                            fileInfos.add(new FileHashInfo(path.toString(), fileName, content, size, 0));
                        } catch (IOException e) {
                            System.err.println("Could not read file content: " + path + " - " + e.getMessage());
                        }
                    });
        }
        return fileInfos;
    }

    public List<List<FileHashInfo>> findSimilarFilesByLevenshtein(List<FileHashInfo> allFiles, int distanceThreshold) {
        List<List<FileHashInfo>> similarGroups = new ArrayList<>();
        boolean[] alreadyGrouped = new boolean[allFiles.size()];

        for (int i = 0; i < allFiles.size(); i++) {
            if (alreadyGrouped[i]) {
                continue;
            }

            List<FileHashInfo> currentGroup = new ArrayList<>();
            currentGroup.add(allFiles.get(i));

            for (int j = i + 1; j < allFiles.size(); j++) {
                if (alreadyGrouped[j]) {
                    continue;
                }

                int distance = calculateLevenshteinDistance(allFiles.get(i).getHash(), allFiles.get(j).getHash());

                // ENHANCED LOGGING: See the comparison results in your backend console.
                boolean isSimilar = distance <= distanceThreshold;
                System.out.println(
                        "DEBUG: Comparing '" + allFiles.get(i).getFileName() +
                                "' and '" + allFiles.get(j).getFileName() +
                                "'. Distance: " + distance + " (Threshold: " + distanceThreshold + ") -> Similar: " + isSimilar
                );

                if (isSimilar) {
                    currentGroup.add(allFiles.get(j));
                    alreadyGrouped[j] = true;
                }
            }

            if (currentGroup.size() > 1) {
                similarGroups.add(currentGroup);
                alreadyGrouped[i] = true;
            }
        }
        return similarGroups;
    }

    // Standard implementation of the Levenshtein Distance algorithm.
    private int calculateLevenshteinDistance(String x, String y) {
        int[][] dp = new int[x.length() + 1][y.length() + 1];

        for (int i = 0; i <= x.length(); i++) {
            for (int j = 0; j <= y.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + (x.charAt(i - 1) == y.charAt(j - 1) ? 0 : 1),
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }
        return dp[x.length()][y.length()];
    }
    // NEW: Method to find empty folders
    public List<String> findEmptyFolders(String directoryPath) throws IOException {
        List<String> emptyFolders = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            paths.filter(Files::isDirectory)
                    .filter(path -> {
                        try (Stream<Path> entries = Files.list(path)) {
                            return entries.findFirst().isEmpty();
                        } catch (IOException e) {
                            System.err.println("Could not check directory: " + path);
                            return false;
                        }
                    })
                    .forEach(path -> emptyFolders.add(path.toString()));
        }
        return emptyFolders;
    }

}
