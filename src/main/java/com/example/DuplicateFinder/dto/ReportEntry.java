package com.example.DuplicateFinder.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportEntry {
    public final LocalDateTime timestamp;
    public final String action;
    public final String details;

    public ReportEntry(String action, String details) {
        this.timestamp = LocalDateTime.now();
        this.action = action;
        this.details = details;
    }
}
