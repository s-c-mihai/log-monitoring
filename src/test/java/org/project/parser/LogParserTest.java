package org.project.parser;

import org.project.model.JobEntryStatus;
import org.project.model.LogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogParserTest {

    private final CsvLogParser parser = new CsvLogParser();

    @Test
    void when_validStartLine_expect_parsedCorrectly() throws ParseException {
        String line = "11:35:23,scheduled task 032, START,37980";
        LogEntry entry = parser.parseLine(line);

        assertThat(entry.timestamp()).isEqualTo(LocalTime.of(11, 35, 23));
        assertThat(entry.jobDescription()).isEqualTo("scheduled task 032");
        assertThat(entry.status()).isEqualTo(JobEntryStatus.START);
        assertThat(entry.pid()).isEqualTo(37980);
    }

    @Test
    void when_validEndLine_expect_parsedCorrectly() throws ParseException {
        String line = "11:35:56,scheduled task 032, END,37980";
        LogEntry entry = parser.parseLine(line);

        assertThat(entry.timestamp()).isEqualTo(LocalTime.of(11, 35, 56));
        assertThat(entry.jobDescription()).isEqualTo("scheduled task 032");
        assertThat(entry.status()).isEqualTo(JobEntryStatus.END);
        assertThat(entry.pid()).isEqualTo(37980);
    }

    @Test
    void when_backgroundJobLine_expect_parsedCorrectly() throws ParseException {
        String line = "11:36:58,background job wmy, START,81258";
        LogEntry entry = parser.parseLine(line);

        assertThat(entry.timestamp()).isEqualTo(LocalTime.of(11, 36, 58));
        assertThat(entry.jobDescription()).isEqualTo("background job wmy");
        assertThat(entry.status()).isEqualTo(JobEntryStatus.START);
        assertThat(entry.pid()).isEqualTo(81258);
    }

    @Test
    void when_invalidTimestamp_expect_parseException() {
        String line = "25:35:23,scheduled task 032, START,37980";

        assertThatThrownBy(() -> parser.parseLine(line))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void when_invalidStatus_expect_parseException() {
        String line = "11:35:23,scheduled task 032, RUNNING,37980";

        assertThatThrownBy(() -> parser.parseLine(line))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void when_invalidPid_expect_parseException() {
        String line = "11:35:23,scheduled task 032, START,abc";

        assertThatThrownBy(() -> parser.parseLine(line))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void when_tooFewFields_expect_parseException() {
        String line = "11:35:23,scheduled task 032, START";

        assertThatThrownBy(() -> parser.parseLine(line))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void when_tooManyFields_expect_parseException() {
        String line = "11:35:23,scheduled task 032, START,37980,extra";

        assertThatThrownBy(() -> parser.parseLine(line))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void when_emptyLine_expect_parseException() {
        assertThatThrownBy(() -> parser.parseLine(""))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void when_nullLine_expect_parseException() {
        assertThatThrownBy(() -> parser.parseLine(null))
            .isInstanceOf(ParseException.class);
    }

    @Test
    void when_logFileWithMultipleEntries_expect_allParsed(@TempDir Path tempDir) throws IOException, ParseException {
        Path logFile = tempDir.resolve("test.log");
        String content = """
            11:35:23,scheduled task 032, START,37980
            11:35:56,scheduled task 032, END,37980
            11:36:11,scheduled task 796, START,57672
            11:36:18,scheduled task 796, END,57672
            """;
        Files.writeString(logFile, content);

        List<LogEntry> entries = parser.parse(logFile);

        assertThat(entries).hasSize(4);
        assertThat(entries.get(0).pid()).isEqualTo(37980);
        assertThat(entries.get(1).pid()).isEqualTo(37980);
        assertThat(entries.get(2).pid()).isEqualTo(57672);
        assertThat(entries.get(3).pid()).isEqualTo(57672);
    }

    @Test
    void when_logFileWithEmptyLines_expect_emptyLinesSkipped(@TempDir Path tempDir) throws IOException, ParseException {
        Path logFile = tempDir.resolve("test.log");
        String content = """
            11:35:23,scheduled task 032, START,37980
            
            11:35:56,scheduled task 032, END,37980
            
            """;
        Files.writeString(logFile, content);

        List<LogEntry> entries = parser.parse(logFile);

        assertThat(entries).hasSize(2);
    }

    @Test
    void when_logFileWithInvalidLines_expect_invalidLinesSkipped(@TempDir Path tempDir) throws IOException, ParseException {
        Path logFile = tempDir.resolve("test.log");
        String content = """
            11:35:23,scheduled task 032, START,37980
            invalid line
            11:35:56,scheduled task 032, END,37980
            """;
        Files.writeString(logFile, content);

        // Should parse valid lines and skip invalid ones
        assertThat(parser.parse(logFile)).hasSize(2);
    }

    @Test
    void when_logFileNotFound_expect_validationFailure() {
        Path nonExistentFile = Path.of("non_existent_file.log");
        assertThatThrownBy(() -> parser.parse(nonExistentFile))
            .isInstanceOf(IllegalArgumentException.class);
    }
}

