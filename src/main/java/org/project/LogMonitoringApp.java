package org.project;

import lombok.extern.slf4j.Slf4j;
import org.project.formatter.ReportFormatter;
import org.project.model.JobAnalysis;
import org.project.model.LogEntry;
import org.project.monitor.LogMonitor;
import org.project.parser.CsvLogParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reads one or more CSV log files, processes job executions, and generates a report
 */
@Slf4j
public class LogMonitoringApp {

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                log.error("Aborting, at least one log file path must be provided as argument");
                System.exit(400);
            }
            List<Path> filePaths = Stream.of(args).distinct().map(Paths::get).toList();
            List<String> invalidFilePaths = filePaths.stream().filter(logFilePath -> !logFilePath.toFile().exists()).map(Path::toString).toList();
            if (!invalidFilePaths.isEmpty()) {
                log.error("Aborting, log file(s) not found: {}", invalidFilePaths);
                System.exit(400);
            }
            CsvLogParser parser = new CsvLogParser();
            List<LogEntry> logEntries = new ArrayList<>();
            for (Path logFilePath : filePaths) {
                log.info("Reading log file: {}", logFilePath);
                logEntries.addAll(parser.parse(logFilePath));
            }

            log.info("Parsed {} log entries", logEntries.size());

            List<JobAnalysis> analyzedLogEntries = new LogMonitor().process(logEntries);

            String report = new ReportFormatter().format(analyzedLogEntries);
            log.info(report);
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            System.exit(500);
        }
    }
}
