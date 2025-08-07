package com.example.DuplicateFinder.service;

import com.example.DuplicateFinder.controller.CategorizationRule;
import com.example.DuplicateFinder.dto.CategorizedApp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

//@Service
//public class CategorizationService {
//
//    private final List<CategorizationRule> rules;
//
//    public CategorizationService() throws IOException {
//        this.rules = loadRules();
//    }
//
//    private List<CategorizationRule> loadRules() throws IOException {
//        ObjectMapper mapper = new ObjectMapper();
//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("categorization-rules.json");
//        if (inputStream == null) {
//            throw new IOException("Cannot find categorization-rules.json");
//        }
//        return mapper.readValue(inputStream, new TypeReference<List<CategorizationRule>>() {});
//    }
//
//    public List<CategorizedApp> categorize(List<FileHashInfo> files) {
//        return files.stream()
//                .map(file -> new CategorizedApp(file.getPath(), applyRules(file.getPath())))
//                .collect(Collectors.toList());
//    }
//
//    private String applyRules(String path) {
//        String lowerCasePath = path.toLowerCase();
//        for (CategorizationRule rule : rules) {
//            for (String keyword : rule.getKeywords()) {
//                if (lowerCasePath.contains(keyword)) {
//                    return rule.getCategory();
//                }
//            }
//        }
//        return "Uncategorized"; // Default category
//    }
//}
@Service
public class CategorizationService {

    private List<CategorizationRule> rules;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File rulesFile;

    public CategorizationService() throws IOException {
        rulesFile = new ClassPathResource("categorization-rules.json").getFile();
        this.rules = loadRules();
    }

    public List<CategorizationRule> loadRules() throws IOException {
        if (!rulesFile.exists() || rulesFile.length() == 0) {
            return Collections.emptyList();
        }
        this.rules = objectMapper.readValue(rulesFile, new TypeReference<List<CategorizationRule>>() {});
        return this.rules;
    }

    public void saveRules(List<CategorizationRule> newRules) throws IOException {
        objectMapper.writeValue(rulesFile, newRules);
        this.rules = newRules;
    }

    public Map<String, List<String>> categorize(List<FileHashInfo> allFiles) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> categorizedApps = new HashMap<>();
        for (FileHashInfo file : allFiles) {
            String fileName = file.getFileName().toLowerCase();
            for (CategorizationRule rule : rules) {
                if (rule.getKeywords().stream().anyMatch(fileName::contains)) {
                    categorizedApps.computeIfAbsent(rule.getCategory(), k -> new ArrayList<>()).add(file.getPath());
                    break;
                }
            }
        }
        return categorizedApps;
    }

    public List<CategorizationRule> getRules() {
        return rules;
    }
}
