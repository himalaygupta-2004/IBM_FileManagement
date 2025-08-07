package com.example.DuplicateFinder.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// You might want a simple DTO for results, but the map is often sufficient.
// If needed, you can re-introduce the FileHashInfo class.
// Static DTO class for file information

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileHashInfo {
    private String path;
    private String fileName;
    private String hash;
    private long size;
    private int hashSize; // <-- ADDED THIS FIELD

    public Object getFilePath() {
        return this.path=path;
    }
}