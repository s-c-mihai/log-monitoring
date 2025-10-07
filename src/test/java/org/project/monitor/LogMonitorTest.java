package org.project.monitor;

import org.project.model.JobAnalysis;
import org.project.model.JobAnalysisState;
import org.project.model.JobEntryStatus;
import org.project.model.JobExecution;
import org.project.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogMonitorTest {

    private final LogMonitor monitor = new LogMonitor();

    @Test
    void when_jobWithinThreshold_expect_jobOk() {
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(LocalTime.of(11, 35, 23), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 35, 56), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isEqualTo(1);
        assertThat(getOkJobs(analyzedJobs)).hasSize(1);
        assertThat(getWarningJobs(analyzedJobs)).isEmpty();
        assertThat(getFaultyJobs(analyzedJobs)).isEmpty();
        assertThat(hasWarnings(analyzedJobs)).isFalse();
        assertThat(hasFaults(analyzedJobs)).isFalse();
    }

    @Test
    void when_jobExceedsWarningThreshold_expect_warningJob() {
        List<LogEntry> entries = new ArrayList<>();
        // Job that takes 6 minutes (default warning threshold is 5 minutes)
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 6, 0), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isEqualTo(1);
        assertThat(getOkJobs(analyzedJobs)).isEmpty();
        assertThat(getWarningJobs(analyzedJobs)).hasSize(1);
        assertThat(getFaultyJobs(analyzedJobs)).isEmpty();
        assertThat(hasWarnings(analyzedJobs)).isTrue();
        assertThat(hasFaults(analyzedJobs)).isFalse();
    }

    @Test
    void when_jobExceedsFaultThreshold_expect_markedFaulty() {
        List<LogEntry> entries = new ArrayList<>();
        // Job that takes 15 minutes (default fault threshold is 10 minutes)
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 15, 0), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isEqualTo(1);
        assertThat(getOkJobs(analyzedJobs)).isEmpty();
        assertThat(getWarningJobs(analyzedJobs)).isEmpty();
        assertThat(getFaultyJobs(analyzedJobs)).hasSize(1);
        assertThat(hasWarnings(analyzedJobs)).isFalse();
        assertThat(hasFaults(analyzedJobs)).isTrue();
    }

    @Test
    void when_multipleJobsWithDifferentDurations_expect_correctCategorization() {
        List<LogEntry> entries = new ArrayList<>();
        // Normal job (33 seconds)
        entries.add(new LogEntry(LocalTime.of(11, 35, 23), "job 1", JobEntryStatus.START, 100));
        entries.add(new LogEntry(LocalTime.of(11, 35, 56), "job 1", JobEntryStatus.END, 100));

        // Warning job (6 minutes)
        entries.add(new LogEntry(LocalTime.of(11, 40, 0), "job 2", JobEntryStatus.START, 200));
        entries.add(new LogEntry(LocalTime.of(11, 46, 0), "job 2", JobEntryStatus.END, 200));

        // Faulty job (15 minutes)
        entries.add(new LogEntry(LocalTime.of(11, 50, 0), "job 3", JobEntryStatus.START, 300));
        entries.add(new LogEntry(LocalTime.of(12, 5, 0), "job 3", JobEntryStatus.END, 300));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isEqualTo(3);
        assertThat(getOkJobs(analyzedJobs)).hasSize(1);
        assertThat(getWarningJobs(analyzedJobs)).hasSize(1);
        assertThat(getFaultyJobs(analyzedJobs)).hasSize(1);
        assertThat(hasWarnings(analyzedJobs)).isTrue();
        assertThat(hasFaults(analyzedJobs)).isTrue();
    }

    @Test
    void when_startWithoutEnd_expect_issue() {
        List<LogEntry> entries = List.of(new LogEntry(LocalTime.of(11, 35, 23), "test job", JobEntryStatus.START, 123));
        // No END event

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isZero();
        assertThat(hasIssues(analyzedJobs)).isTrue();
        assertThat(getIssues(analyzedJobs)).hasSize(1);
        assertThat(getIssues(analyzedJobs).getFirst()).contains("START event without matching END");
    }

    @Test
    void when_endWithoutStart_expect_issue() {
        List<LogEntry> entries = List.of(new LogEntry(LocalTime.of(11, 35, 56), "test job", JobEntryStatus.END, 123));
        // No START event

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isZero();
        assertThat(hasIssues(analyzedJobs)).isTrue();
        assertThat(getIssues(analyzedJobs)).hasSize(1);
        assertThat(getIssues(analyzedJobs).getFirst()).contains("END event without matching START");
    }

    @Test
    void when_duplicateStart_expect_issue() {
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(LocalTime.of(11, 35, 23), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 36, 0), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 36, 30), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isEqualTo(1);
        assertThat(hasIssues(analyzedJobs)).isTrue();
        assertThat(getIssues(analyzedJobs).getFirst()).contains("Duplicate START event");
    }

    @Test
    void when_interleavedJobs_expect_bothProcessed() {
        List<LogEntry> entries = new ArrayList<>();
        // Job 1 starts
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "job 1", JobEntryStatus.START, 100));
        // Job 2 starts
        entries.add(new LogEntry(LocalTime.of(11, 1, 0), "job 2", JobEntryStatus.START, 200));
        // Job 1 ends
        entries.add(new LogEntry(LocalTime.of(11, 2, 0), "job 1", JobEntryStatus.END, 100));
        // Job 2 ends
        entries.add(new LogEntry(LocalTime.of(11, 3, 0), "job 2", JobEntryStatus.END, 200));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isEqualTo(2);
        assertThat(getOkJobs(analyzedJobs)).hasSize(2);
        assertThat(hasIssues(analyzedJobs)).isFalse();
    }

    @Test
    void when_emptyLogs_expect_emptyReport() {
        List<JobAnalysis> analyzedJobs = monitor.process(List.of());

        assertThat(getCompletedJobs(analyzedJobs)).isZero();
        assertThat(hasWarnings(analyzedJobs)).isFalse();
        assertThat(hasFaults(analyzedJobs)).isFalse();
        assertThat(hasIssues(analyzedJobs)).isFalse();
    }

    @Test
    void when_jobsAtExactThresholds_expect_correctCategorization() {
        List<LogEntry> entries = new ArrayList<>();

        // Exactly 5 minutes
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "job 1", JobEntryStatus.START, 100));
        entries.add(new LogEntry(LocalTime.of(11, 5, 0), "job 1", JobEntryStatus.END, 100));

        // Exactly 10 minutes
        entries.add(new LogEntry(LocalTime.of(11, 10, 0), "job 2", JobEntryStatus.START, 200));
        entries.add(new LogEntry(LocalTime.of(11, 20, 0), "job 2", JobEntryStatus.END, 200));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getCompletedJobs(analyzedJobs)).isEqualTo(2);
        assertThat(getOkJobs(analyzedJobs)).hasSize(1);
        assertThat(getWarningJobs(analyzedJobs)).hasSize(1);
        assertThat(getFaultyJobs(analyzedJobs)).isEmpty();
    }

    @Test
    void when_jobDurationAtWarningThreshold_expect_categorizedAsOk() {
        List<LogEntry> entries = new ArrayList<>();
        // Exactly 5 minutes - should be ok (not exceeding threshold)
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 5, 0), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getOkJobs(analyzedJobs)).hasSize(1);
        assertThat(getWarningJobs(analyzedJobs)).isEmpty();
        assertThat(getFaultyJobs(analyzedJobs)).isEmpty();
    }

    @Test
    void when_jobDurationExceedsWarningThreshold_expect_categorizedAsWarning() {
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 6, 0), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getOkJobs(analyzedJobs)).isEmpty();
        assertThat(getWarningJobs(analyzedJobs)).hasSize(1);
        assertThat(getFaultyJobs(analyzedJobs)).isEmpty();
    }

    @Test
    void when_jobDurationAtFaultThreshold_expect_categorizedAsWarning() {
        List<LogEntry> entries = new ArrayList<>();
        // Exactly 10 minutes - should be warning (not exceeding fault threshold)
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 10, 0), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getOkJobs(analyzedJobs)).isEmpty();
        assertThat(getWarningJobs(analyzedJobs)).hasSize(1);
        assertThat(getFaultyJobs(analyzedJobs)).isEmpty();
    }

    @Test
    void when_jobDurationExceedsFaultThreshold_expect_categorizedAsFaulty() {
        List<LogEntry> entries = new ArrayList<>();
        entries.add(new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123));
        entries.add(new LogEntry(LocalTime.of(11, 11, 0), "test job", JobEntryStatus.END, 123));

        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(getOkJobs(analyzedJobs)).isEmpty();
        assertThat(getWarningJobs(analyzedJobs)).isEmpty();
        assertThat(getFaultyJobs(analyzedJobs)).hasSize(1);
    }

    private List<JobExecution> getOkJobs(List<JobAnalysis> analyzedJobs) {
        return analyzedJobs.stream()
            .filter(JobAnalysis::hasJobExecution)
            .filter(a -> a.state() == JobAnalysisState.OK)
            .map(JobAnalysis::jobExecution)
            .toList();
    }

    private List<JobExecution> getWarningJobs(List<JobAnalysis> analyzedJobs) {
        return analyzedJobs.stream()
            .filter(JobAnalysis::hasJobExecution)
            .filter(a -> a.state() == JobAnalysisState.WARNING)
            .map(JobAnalysis::jobExecution)
            .toList();
    }

    private List<JobExecution> getFaultyJobs(List<JobAnalysis> analyzedJobs) {
        return analyzedJobs.stream()
            .filter(a -> a.state() == JobAnalysisState.FAULTY)
            .filter(JobAnalysis::hasJobExecution)
            .map(JobAnalysis::jobExecution)
            .toList();
    }

    private List<String> getIssues(List<JobAnalysis> analyzedJobs) {
        return analyzedJobs.stream()
            .filter(JobAnalysis::hasDanglingEntry)
            .map(JobAnalysis::reason)
            .toList();
    }

    private int getCompletedJobs(List<JobAnalysis> analyzedJobs) {
        return (int) analyzedJobs.stream()
            .filter(JobAnalysis::hasJobExecution)
            .count();
    }

    private boolean hasWarnings(List<JobAnalysis> analyzedJobs) {
        return analyzedJobs.stream()
            .anyMatch(a -> a.state() == JobAnalysisState.WARNING);
    }

    private boolean hasFaults(List<JobAnalysis> analyzedJobs) {
        return analyzedJobs.stream()
            .anyMatch(a -> a.state() == JobAnalysisState.FAULTY);
    }

    private boolean hasIssues(List<JobAnalysis> analyzedJobs) {
        return analyzedJobs.stream()
            .anyMatch(JobAnalysis::hasDanglingEntry);
    }
}

