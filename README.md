# Log Monitoring Application

## Prerequisites

To build and run on local device, ensure you have the following installed:

- **Java 21** or higher
  - Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://openjdk.org/)
  - Verify installation: `java -version`

- **Apache Maven 3.6+**
  - Download from [Maven website](https://maven.apache.org/download.cgi)
  - Verify installation: `mvn -version`

## Log File Format

The application expects CSV log files with the following format:

```
HH:MM:SS,job description,STATUS,PID
```

Where:
- `HH:MM:SS`: Timestamp in 24-hour format
- `job description`: Description of the job (e.g., "scheduled task 032", "background job wmy")
- `STATUS`: Either "START" or "END" (case-insensitive)
- `PID`: Process ID (integer)

Example:
```
11:35:23,scheduled task 032, START,37980
11:35:56,scheduled task 032, END,37980
11:36:11,scheduled task 796, START,57672
11:36:18,scheduled task 796, END,57672
```

**Note:** Lines with invalid format are skipped with a warning logged.
