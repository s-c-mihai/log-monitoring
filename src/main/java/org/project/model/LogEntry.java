package org.project.model;

import java.time.temporal.Temporal;
import java.util.StringJoiner;

/**
 * Represents a single log entry from the CSV log file
 */
public record LogEntry(
    Temporal timestamp,
    String jobDescription,
    JobEntryStatus status,
    int pid
) {
    public boolean isStart() {
        return status == JobEntryStatus.START;
    }

    public boolean isEnd() {
        return status == JobEntryStatus.END;
    }

    @Override
    public String toString() {
        return new StringJoiner(",")
            .add(timestamp.toString())
            .add(jobDescription)
            .add(status.toString())
            .add(String.valueOf(pid))
            .toString();
    }
}

