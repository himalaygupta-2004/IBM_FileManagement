package com.example.DuplicateFinder.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategorizedApp {
    private String path;
    private String category;
}
