package org.project.model;

import java.util.Objects;

/**
 * Represents the analysis result of a job execution or dangling log entry.
 * Contains either a complete JobExecution or a dangling LogEntry with a reason.
 */
public record JobAnalysis(
    JobExecution jobExecution,
    LogEntry danglingEntry,
    JobAnalysisState state,
    String reason
) {
    public JobAnalysis {
        Objects.requireNonNull(state, "state cannot be null");
        if ((jobExecution == null && danglingEntry == null) ||
            (jobExecution != null && danglingEntry != null)) {
            throw new IllegalArgumentException(
                "One of jobExecution or danglingEntry must be non-null");
        }
    }

    public static JobAnalysis completed(JobExecution jobExecution) {
        return new JobAnalysis(jobExecution, null, JobAnalysisState.OK, null);
    }

    public static JobAnalysis warning(JobExecution jobExecution, String reason) {
        Objects.requireNonNull(reason, "reason cannot be null for warning");
        return new JobAnalysis(jobExecution, null, JobAnalysisState.WARNING, reason);
    }

    public static JobAnalysis faulty(JobExecution jobExecution, String reason) {
        Objects.requireNonNull(reason, "reason cannot be null for faulty job");
        return new JobAnalysis(jobExecution, null, JobAnalysisState.FAULTY, reason);
    }

    public static JobAnalysis dangling(LogEntry logEntry, String reason) {
        Objects.requireNonNull(logEntry, "logEntry cannot be null");
        Objects.requireNonNull(reason, "reason cannot be null for dangling entry");
        return new JobAnalysis(null, logEntry, JobAnalysisState.FAULTY, reason);
    }

    public boolean hasJobExecution() {
        return jobExecution != null;
    }

    public boolean hasDanglingEntry() {
        return danglingEntry != null;
    }

    public boolean hasReason() {
        return reason != null && !reason.isBlank();
    }
}

