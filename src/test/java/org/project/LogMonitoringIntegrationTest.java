package org.project;

import org.project.model.JobAnalysis;
import org.project.model.JobAnalysisState;
import org.project.model.LogEntry;
import org.project.monitor.LogMonitor;
import org.project.parser.CsvLogParser;
import org.project.parser.ParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogMonitoringIntegrationTest {

    private final CsvLogParser parser = new CsvLogParser();
    private final LogMonitor monitor = new LogMonitor();

    @Test
    void when_completeWorkflow_expect_correctProcessing(@TempDir Path tempDir) throws ParseException, IOException {
        // Create a test log file with various scenarios
        Path logFile = tempDir.resolve("test.log");
        String content = """
            11:35:23,scheduled task 032, START,37980
            11:35:56,scheduled task 032, END,37980
            11:36:11,scheduled task 796, START,57672
            11:36:18,scheduled task 796, END,57672
            11:36:58,background job wmy, START,81258
            11:37:14,scheduled task 515, START,45135
            11:49:37,scheduled task 515, END,45135
            11:51:44,background job wmy, END,81258
            """;
        Files.writeString(logFile, content);

        // Parse the log file
        List<LogEntry> entries = parser.parse(logFile);
        assertThat(entries).hasSize(8);

        // Process the logs
        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        // Verify results
        assertThat(analyzedJobs.stream().filter(JobAnalysis::hasJobExecution).count()).isEqualTo(4);

        // scheduled task 032: 33 seconds - Ok
        // scheduled task 796: 7 seconds - Ok
        // scheduled task 515: 12 minutes 23 seconds - faulty
        // background job wmy: 14 minutes 46 seconds - faulty

        assertThat(analyzedJobs.stream().filter(a -> a.hasJobExecution() && a.state() == JobAnalysisState.OK).count()).isEqualTo(2L);
        assertThat(analyzedJobs.stream().filter(a -> a.hasJobExecution() && a.state() == JobAnalysisState.WARNING).count()).isEqualTo(0L);
        assertThat(analyzedJobs.stream().filter(a -> a.state() == JobAnalysisState.FAULTY && a.hasJobExecution()).count()).isEqualTo(2L);
        assertThat(analyzedJobs.stream().anyMatch(a -> a.state() == JobAnalysisState.FAULTY)).isTrue();
    }

    @Test
    void when_workflowWithWarnings_expect_correctCategorization(@TempDir Path tempDir) throws ParseException, IOException {
        Path logFile = tempDir.resolve("test.log");
        String content = """
            11:00:00,job 1, START,100
            11:03:00,job 1, END,100
            11:10:00,job 2, START,200
            11:17:00,job 2, END,200
            11:20:00,job 3, START,300
            11:35:00,job 3, END,300
            """;
        Files.writeString(logFile, content);

        List<LogEntry> entries = parser.parse(logFile);
        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(analyzedJobs.stream().filter(JobAnalysis::hasJobExecution).count()).isEqualTo(3);
        assertThat(analyzedJobs.stream().filter(a -> a.hasJobExecution() && a.state() == JobAnalysisState.OK).count()).isEqualTo(1L);  // job 1: 3 minutes
        assertThat(analyzedJobs.stream().filter(a -> a.hasJobExecution() && a.state() == JobAnalysisState.WARNING).count()).isEqualTo(1L); // job 2: 7 minutes
        assertThat(analyzedJobs.stream().filter(a -> a.state() == JobAnalysisState.FAULTY && a.hasJobExecution()).count()).isEqualTo(1L);   // job 3: 15 minutes
    }

    @Test
    void when_workflowWithIssues_expect_issuesReported(@TempDir Path tempDir) throws ParseException, IOException {
        Path logFile = tempDir.resolve("test.log");
        String content = """
            11:00:00,job 1, START,100
            11:01:00,job 2, START,200
            11:02:00,job 2, END,200
            11:03:00,job 3, END,300
            """;
        Files.writeString(logFile, content);

        List<LogEntry> entries = parser.parse(logFile);
        List<JobAnalysis> analyzedJobs = monitor.process(entries);

        assertThat(analyzedJobs.stream().filter(JobAnalysis::hasJobExecution).count()).isEqualTo(1L); // Only job 2 completed
        assertThat(analyzedJobs.stream().anyMatch(JobAnalysis::hasDanglingEntry)).isTrue();
        assertThat(analyzedJobs.stream().filter(JobAnalysis::hasDanglingEntry).count()).isEqualTo(2L); // job 1 no END, job 3 no START
    }


}
