package com.example.DuplicateFinder.service;

import com.example.DuplicateFinder.controller.CategorizationRule;
import com.example.DuplicateFinder.dto.CategorizedApp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;

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

    private static final Logger logger = LoggerFactory.getLogger(CategorizationService.class);
    private final ObjectMapper objectMapper;
    private final File rulesFile;

    @Getter
    private List<CategorizationRule> rules;

    public CategorizationService(ObjectMapper objectMapper) throws IOException {
        this.objectMapper = objectMapper;
        this.rulesFile = new ClassPathResource("categorization-rules.json").getFile();
        this.rules = loadRules();
        logger.info("Successfully loaded {} categorization rules.", rules.size());
    }

    public List<CategorizationRule> loadRules() throws IOException {
        if (!rulesFile.exists() || rulesFile.length() == 0) {
            logger.warn("Categorization rules file not found or is empty. Returning an empty list.");
            return Collections.emptyList();
        }
        try {
            this.rules = objectMapper.readValue(rulesFile, new TypeReference<List<CategorizationRule>>() {});
            return this.rules;
        } catch (IOException e) {
            logger.error("Failed to load categorization rules from file.", e);
            throw e;
        }
    }

    public void saveRules(List<CategorizationRule> newRules) throws IOException {
        try {
            objectMapper.writeValue(rulesFile, newRules);
            this.rules = newRules;
            logger.info("Successfully saved {} categorization rules.", newRules.size());
        } catch (IOException e) {
            logger.error("Failed to save categorization rules to file.", e);
            throw e;
        }
    }

    public Map<String, List<String>> categorize(List<FileHashInfo> allFiles) {
        if (rules == null || rules.isEmpty() || allFiles == null) {
            logger.warn("Rules or file list is empty. Categorization skipped.");
            return Collections.emptyMap();
        }

        Map<String, List<String>> categorizedApps = new HashMap<>();
        for (FileHashInfo file : allFiles) {
            if (file == null || file.getFileName() == null) {
                continue;
            }
            String fileName = file.getFileName().toLowerCase();
            logger.debug("Checking file: {}", fileName);

            for (CategorizationRule rule : rules) {
                boolean isMatch = rule.getKeywords().stream()
                        .anyMatch(keyword -> {
                            boolean found = fileName.contains(keyword.toLowerCase());
                            logger.debug(" - against rule '{}' with keyword '{}': {}", rule.getCategory(), keyword, found);
                            return found;
                        });

                if (isMatch) {
                    categorizedApps.computeIfAbsent(rule.getCategory(), k -> new ArrayList<>()).add(file.getPath());
                    logger.info("File '{}' categorized as '{}'.", fileName, rule.getCategory());
                    break;
                }
            }
        }
        logger.info("Completed file categorization. Found {} categories.", categorizedApps.size());
        return categorizedApps;
    }



    //    private List<CategorizationRule> rules;
//    private final ObjectMapper objectMapper = new ObjectMapper();
//    private final File rulesFile;
//
//    public CategorizationService() throws IOException {
//        rulesFile = new ClassPathResource("categorization-rules.json").getFile();
//        this.rules = loadRules();
//    }
//
//    public List<CategorizationRule> loadRules() throws IOException {
//        if (!rulesFile.exists() || rulesFile.length() == 0) {
//            return Collections.emptyList();
//        }
//        this.rules = objectMapper.readValue(rulesFile, new TypeReference<List<CategorizationRule>>() {});
//        return this.rules;
//    }
//
//    public void saveRules(List<CategorizationRule> newRules) throws IOException {
//        objectMapper.writeValue(rulesFile, newRules);
//        this.rules = newRules;
//    }
//
//    public Map<String, List<String>> categorize(List<FileHashInfo> allFiles) {
//        if (rules == null || rules.isEmpty()) {
//            return Collections.emptyMap();
//        }
//
//        Map<String, List<String>> categorizedApps = new HashMap<>();
//        for (FileHashInfo file : allFiles) {
//            String fileName = file.getFileName().toLowerCase();
//            for (CategorizationRule rule : rules) {
//                if (rule.getKeywords().stream().anyMatch(fileName::contains)) {
//                    categorizedApps.computeIfAbsent(rule.getCategory(), k -> new ArrayList<>()).add(file.getPath());
//                    break;
//                }
//            }
//        }
//        return categorizedApps;
//    }
//
    public List<CategorizationRule> getRules() {
        return rules;
    }
}

