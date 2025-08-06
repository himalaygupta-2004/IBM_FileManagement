package com.example.DuplicateFinder.controller;

import lombok.Data;

import java.util.List;

@Data
public class CategorizationRule {
    private String category;
    private List<String> keywords;
}
