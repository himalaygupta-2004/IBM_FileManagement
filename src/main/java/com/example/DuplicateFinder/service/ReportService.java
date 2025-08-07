package com.example.DuplicateFinder.service;

import com.example.DuplicateFinder.dto.ReportEntry;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ReportService {
    private final List<ReportEntry> reportEntries = new ArrayList<>();

    public void addReportEntry(String action, String details) {
        ReportEntry entry = new ReportEntry(action, details);
        reportEntries.add(entry);
    }

    public List<ReportEntry> getRecentEntries(int count) {
        int fromIndex = Math.max(0, reportEntries.size() - count);
        return Collections.unmodifiableList(reportEntries.subList(fromIndex, reportEntries.size()));
    }
}

// A simple data class for a report entry
