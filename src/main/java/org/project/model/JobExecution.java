package org.project.model;

import java.time.Duration;
import java.time.temporal.Temporal;

/**
 * Represents a complete job execution from START to END.
 * Contains the start and end log entries, and calculated duration.
 */
public record JobExecution(
    LogEntry startEntry,
    LogEntry endEntry,
    Duration duration
) {
    public JobExecution(LogEntry startEntry, LogEntry endEntry) {
        this(startEntry, endEntry, calculateDurationWithValidation(startEntry, endEntry));
    }

    /**
     * Validates the start and end entries, then calculates the duration.
     * This ensures validation happens before duration calculation.
     */
    private static Duration calculateDurationWithValidation(LogEntry startEntry, LogEntry endEntry) {
        if (startEntry == null) {
            throw new IllegalArgumentException("startEntry cannot be null");
        }
        if (endEntry == null) {
            throw new IllegalArgumentException("endEntry cannot be null");
        }
        if (!startEntry.isStart()) {
            throw new IllegalArgumentException(
                String.format("startEntry must have START status, but has %s", startEntry.status()));
        }
        if (!endEntry.isEnd()) {
            throw new IllegalArgumentException(
                String.format("endEntry must have END status, but has %s", endEntry.status()));
        }
        if (startEntry.pid() != endEntry.pid()) {
            throw new IllegalArgumentException(
                String.format("PID mismatch: startEntry has PID %d, endEntry has PID %d",
                    startEntry.pid(), endEntry.pid()));
        }
        if (!startEntry.jobDescription().equals(endEntry.jobDescription())) {
            throw new IllegalArgumentException(
                String.format("Job description mismatch: startEntry has '%s', endEntry has '%s'",
                    startEntry.jobDescription(), endEntry.jobDescription()));
        }
        return calculateDuration(startEntry, endEntry);
    }

    public int pid() {
        return startEntry.pid();
    }

    public String jobDescription() {
        return startEntry.jobDescription();
    }

    public Temporal startTime() {
        return startEntry.timestamp();
    }

    public Temporal endTime() {
        return endEntry.timestamp();
    }

    /**
     * Calculates the duration between start and end entries.
     * Handles the case where the end time is on the next day (crosses midnight) when date information is not available
     */
    private static Duration calculateDuration(LogEntry startEntry, LogEntry endEntry) {
        Duration duration = Duration.between(startEntry.timestamp(), endEntry.timestamp());
        if (duration.isNegative()) {
            return duration.plusHours(24);
        }
        return duration;
    }

    public String getFormattedDuration() {
        return String.format("%02d:%02d", duration.toMinutes(), duration.getSeconds() % 60);
    }
}

