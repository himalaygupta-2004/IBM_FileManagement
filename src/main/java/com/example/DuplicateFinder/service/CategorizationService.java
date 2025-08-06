package com.example.DuplicateFinder.service;

import com.example.DuplicateFinder.controller.CategorizationRule;
import com.example.DuplicateFinder.dto.CategorizedApp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategorizationService {

    private final List<CategorizationRule> rules;

    public CategorizationService() throws IOException {
        this.rules = loadRules();
    }

    private List<CategorizationRule> loadRules() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("categorization-rules.json");
        if (inputStream == null) {
            throw new IOException("Cannot find categorization-rules.json");
        }
        return mapper.readValue(inputStream, new TypeReference<List<CategorizationRule>>() {});
    }

    public List<CategorizedApp> categorize(List<FileHashInfo> files) {
        return files.stream()
                .map(file -> new CategorizedApp(file.getPath(), applyRules(file.getPath())))
                .collect(Collectors.toList());
    }

    private String applyRules(String path) {
        String lowerCasePath = path.toLowerCase();
        for (CategorizationRule rule : rules) {
            for (String keyword : rule.getKeywords()) {
                if (lowerCasePath.contains(keyword)) {
                    return rule.getCategory();
                }
            }
        }
        return "Uncategorized"; // Default category
    }
}
