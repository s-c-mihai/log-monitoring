package org.project.parser;

import lombok.extern.slf4j.Slf4j;
import org.project.model.JobEntryStatus;
import org.project.model.LogEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parser for CSV log files.
 * Reads log entries in the format: HH:MM:SS,job description,STATUS,PID
 */
@Slf4j
public class CsvLogParser {
    private final String csvDelimiter;
    private final DateTimeFormatter timeFormatter;


    public CsvLogParser() {
        this(",", DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public CsvLogParser(String csvDelimiter, DateTimeFormatter timeFormatter) {
        this.csvDelimiter = Objects.requireNonNull(csvDelimiter);
        this.timeFormatter = Objects.requireNonNull(timeFormatter);
    }

    /**
     * Parses a log file and returns a list of LogEntry objects.
     *
     * @param logFilePath Path to the log file
     * @return List of parsed log entries
     * @throws ParseException if there's an error while reading the file
     */
    public List<LogEntry> parse(Path logFilePath) throws ParseException {
        if (!logFilePath.toFile().exists()) {
            throw new IllegalArgumentException("File does not exist");
        }
        log.info("Starting to parse log file: {}", logFilePath);
        List<LogEntry> entries = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(logFilePath)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    log.debug("Skipping empty line at line number {}", lineNumber);
                    continue;
                }
                final LogEntry entry;
                try {
                    entry = parseLine(line);
                } catch (ParseException e) {
                    log.warn("Failed to parse line {}: {} - {}", lineNumber, line, e.getMessage());
                    continue;
                }
                entries.add(entry);
                log.debug("Successfully parsed line {}: {}", lineNumber, entry);
            }
        } catch (IOException e) {
            throw new ParseException(e.getMessage(), e);
        }

        log.info("Successfully parsed {} log entries from {}", entries.size(), logFilePath);
        return entries;
    }

    /**
     * @throws ParseException if the line cannot be parsed
     */
    LogEntry parseLine(String line) throws ParseException {
        if (line == null || line.isBlank()) {
            throw new ParseException("Line is null or empty");
        }

        String[] parts = line.split(this.csvDelimiter);

        if (parts.length != 4) {
            throw new ParseException(
                String.format("Expected 4 fields but found %d. Line: %s", parts.length, line));
        }

        LocalTime timestamp;
        try {
            timestamp = LocalTime.parse(parts[0].trim(), this.timeFormatter);
        } catch (DateTimeParseException e) {
            throw new ParseException(
                String.format("Invalid timestamp format: %s", parts[0]), e);
        }

        String statusStr = parts[2].trim().toUpperCase();
        JobEntryStatus status;
        try {
            status = JobEntryStatus.valueOf(statusStr);
        } catch (IllegalArgumentException e) {
            throw new ParseException(
                String.format("Invalid status '%s'. Expected START or END", statusStr));
        }

        int pid;
        try {
            pid = Integer.parseInt(parts[3].trim());
        } catch (NumberFormatException e) {
            throw new ParseException(
                String.format("Invalid PID format: %s", parts[3]), e);
        }

        String jobDescription = parts[1].trim();
        return new LogEntry(timestamp, jobDescription, status, pid);
    }
}

