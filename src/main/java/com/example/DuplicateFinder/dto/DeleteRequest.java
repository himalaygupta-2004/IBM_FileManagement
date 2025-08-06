package com.example.DuplicateFinder.dto;

import lombok.Data;

import java.util.List;

@Data
public class DeleteRequest {
    private String basePath;
    private List<String> filesToDelete;
}
