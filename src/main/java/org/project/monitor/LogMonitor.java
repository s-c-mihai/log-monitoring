package org.project.monitor;

import org.project.model.JobAnalysis;
import org.project.model.JobEntryStatus;
import org.project.model.JobExecution;
import org.project.model.LogEntry;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Processes log entries to track job executions and generate reports.
 * Matches START and END events for each job (identified by PID),
 * calculates durations, and categorizes jobs based on execution time.
 * <p>
 * Thresholds can be configured via system properties:
 * - log.monitor.warning.threshold.minutes (default: 5)
 * - log.monitor.fault.threshold.minutes (default: 10)
 */
public class LogMonitor {
    private final Duration warningThreshold;
    private final Duration faultThreshold;

    public LogMonitor() {
        warningThreshold = Duration.ofMinutes(Optional.ofNullable(System.getProperty("log.monitor.warning.threshold.minutes")).map(Long::parseLong).orElse(5L));
        faultThreshold = Duration.ofMinutes(Optional.ofNullable(System.getProperty("log.monitor.fault.threshold.minutes")).map(Long::parseLong).orElse(10L));
    }

    /**
     * Processes a list of log entries and generates an analysis result for each job
     *
     * @param logEntries List of log entries to process
     * @return A list of JobAnalysis entries containing all analyzed jobs and dangling entries
     */
    public List<JobAnalysis> process(List<LogEntry> logEntries) {
        if (logEntries == null || logEntries.isEmpty()) {
            return List.of();
        }

        Map<JobEntryStatus, List<LogEntry>> statusToEntries = logEntries.stream().collect(Collectors.groupingBy(LogEntry::status));
        List<JobAnalysis> analyzedJobs = new ArrayList<>();

        Map<Integer, LogEntry> pidToStartEntry = new HashMap<>();
        for (LogEntry entry : statusToEntries.getOrDefault(JobEntryStatus.START, List.of())) {
            LogEntry duplicateStart = pidToStartEntry.get(entry.pid());
            if (duplicateStart != null) {
                String issue = String.format(
                        "Duplicate START event for PID %d (%s) at %s. Previous START was at %s.",
                        entry.pid(),
                        entry.jobDescription(),
                        entry.timestamp(),
                        duplicateStart.timestamp());
                analyzedJobs.add(JobAnalysis.dangling(entry, issue));
            }
            pidToStartEntry.put(entry.pid(), entry);
        }

        for (LogEntry entry : statusToEntries.getOrDefault(JobEntryStatus.END, List.of())) {
            LogEntry startEvent = pidToStartEntry.get(entry.pid());
            if (startEvent == null) {
                String issue = String.format(
                        "END event without matching START for PID %d (%s) at %s",
                        entry.pid(),
                        entry.jobDescription(),
                        entry.timestamp());
                analyzedJobs.add(JobAnalysis.dangling(entry, issue));
            } else {
                analyzedJobs.add(categorizeJob(new JobExecution(startEvent, entry)));
                pidToStartEntry.remove(entry.pid());
            }
        }

        /*
         * Check for unmatched START events (jobs that never finished)
         * These could also be events that did not complete yet
         */
        for (LogEntry unmatchedEntry : pidToStartEntry.values()) {
            String issue = String.format(
                    "START event without matching END for PID %d (%s) at %s",
                    unmatchedEntry.pid(),
                    unmatchedEntry.jobDescription(),
                    unmatchedEntry.timestamp());
            analyzedJobs.add(JobAnalysis.dangling(unmatchedEntry, issue));
        }
        return analyzedJobs;
    }

    /**
     * Categorizes a job execution based on its duration.
     *
     * @param job The job execution to categorize
     * @return A JobAnalysis with the appropriate state and reason
     */
    private JobAnalysis categorizeJob(JobExecution job) {
        if (isFaulty(job)) {
            String reason = String.format("Exceeded fault threshold (%d min) with duration %s",
                    faultThreshold.toMinutes(), job.getFormattedDuration());
            return JobAnalysis.faulty(job, reason);
        }
        if (isWarning(job)) {
            String reason = String.format("Exceeded warning threshold (%d min) with duration %s",
                    warningThreshold.toMinutes(), job.getFormattedDuration());
            return JobAnalysis.warning(job, reason);
        }
        return JobAnalysis.completed(job);
    }

    /**
     * @param job The job execution to check
     * @return true if duration exceeds warning threshold but not fault threshold
     */
    private boolean isWarning(JobExecution job) {
        return job.duration().compareTo(warningThreshold) > 0
                && job.duration().compareTo(faultThreshold) <= 0;
    }

    /**
     * @param job The job execution to check
     * @return true if duration exceeds fault threshold
     */
    private boolean isFaulty(JobExecution job) {
        return job.duration().compareTo(faultThreshold) > 0;
    }

    public Duration getWarningThreshold() {
        return warningThreshold;
    }

    public Duration getFaultThreshold() {
        return faultThreshold;
    }
}

