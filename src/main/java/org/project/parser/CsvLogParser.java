package org.project.parser;

import lombok.extern.slf4j.Slf4j;
import org.project.model.LogEntry;

import java.nio.file.Path;
import java.util.List;

@Slf4j
public class CsvLogParser {

    public List<LogEntry> parse(Path logFilePath) throws ParseException {
        return List.of();
    }

    /**
     * @throws ParseException if the line cannot be parsed
     */
    public LogEntry parseLine(String line) throws ParseException {
        return null;
    }
}

