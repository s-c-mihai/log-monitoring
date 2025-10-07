package org.project.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobExecutionTest {

    @Test
    void when_jobExecutionCreated_expect_allFieldsAccessible() {
        LocalTime start = LocalTime.of(11, 35, 23);
        LocalTime end = LocalTime.of(11, 35, 56);
        LogEntry startEntry = new LogEntry(start, "scheduled task 032", JobEntryStatus.START, 37980);
        LogEntry endEntry = new LogEntry(end, "scheduled task 032", JobEntryStatus.END, 37980);
        JobExecution job = new JobExecution(startEntry, endEntry);

        assertThat(job.pid()).isEqualTo(37980);
        assertThat(job.jobDescription()).isEqualTo("scheduled task 032");
        assertThat(job.startTime()).isEqualTo(start);
        assertThat(job.endTime()).isEqualTo(end);
    }

    @Test
    void when_durationLessThanOneMinute_expect_correctCalculation() {
        LocalTime start = LocalTime.of(11, 35, 23);
        LocalTime end = LocalTime.of(11, 35, 56);
        LogEntry startEntry = new LogEntry(start, "test job", JobEntryStatus.START, 37980);
        LogEntry endEntry = new LogEntry(end, "test job", JobEntryStatus.END, 37980);
        JobExecution job = new JobExecution(startEntry, endEntry);

        assertThat(job.duration().toMinutes()).isZero();
        assertThat(job.duration().getSeconds()).isEqualTo(33);
    }

    @Test
    void when_durationSpansMultipleMinutes_expect_correctCalculation() {
        LocalTime start = LocalTime.of(11, 35, 23);
        LocalTime end = LocalTime.of(11, 40, 24);
        LogEntry startEntry = new LogEntry(start, "test job", JobEntryStatus.START, 10515);
        LogEntry endEntry = new LogEntry(end, "test job", JobEntryStatus.END, 10515);
        JobExecution job = new JobExecution(startEntry, endEntry);

        assertThat(job.duration().toMinutes()).isEqualTo(5);
        assertThat(job.duration().getSeconds()).isEqualTo(301); // 5 minutes 1 second
    }

    @Test
    void when_durationCrossesMidnight_expect_correctCalculation() {
        LocalTime start = LocalTime.of(23, 55, 0);
        LocalTime end = LocalTime.of(0, 5, 0);
        LogEntry startEntry = new LogEntry(start, "test job", JobEntryStatus.START, 123);
        LogEntry endEntry = new LogEntry(end, "test job", JobEntryStatus.END, 123);
        JobExecution job = new JobExecution(startEntry, endEntry);

        assertThat(job.duration().toMinutes()).isEqualTo(10);
        assertThat(job.duration().getSeconds()).isEqualTo(600);
    }


    @Test
    void when_startEntryHasEndStatus_expect_validationFailure() {
        LogEntry startEntry = new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.END, 123);
        LogEntry endEntry = new LogEntry(LocalTime.of(11, 5, 0), "test job", JobEntryStatus.END, 123);

        assertThatThrownBy(() -> new JobExecution(startEntry, endEntry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("startEntry must have START status");
    }

    @Test
    void when_endEntryHasStartStatus_expect_validationFailure() {
        LogEntry startEntry = new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123);
        LogEntry endEntry = new LogEntry(LocalTime.of(11, 5, 0), "test job", JobEntryStatus.START, 123);

        assertThatThrownBy(() -> new JobExecution(startEntry, endEntry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endEntry must have END status");
    }

    @Test
    void when_pidMismatch_expect_validationFailure() {
        LogEntry startEntry = new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123);
        LogEntry endEntry = new LogEntry(LocalTime.of(11, 5, 0), "test job", JobEntryStatus.END, 456);

        assertThatThrownBy(() -> new JobExecution(startEntry, endEntry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("PID mismatch")
            .hasMessageContaining("123")
            .hasMessageContaining("456");
    }

    @Test
    void when_jobDescriptionMismatch_expect_validationFailure() {
        LogEntry startEntry = new LogEntry(LocalTime.of(11, 0, 0), "job A", JobEntryStatus.START, 123);
        LogEntry endEntry = new LogEntry(LocalTime.of(11, 5, 0), "job B", JobEntryStatus.END, 123);

        assertThatThrownBy(() -> new JobExecution(startEntry, endEntry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Job description mismatch")
            .hasMessageContaining("job A")
            .hasMessageContaining("job B");
    }

    @Test
    void when_startEntryIsNull_expect_validationFailure() {
        LogEntry endEntry = new LogEntry(LocalTime.of(11, 5, 0), "test job", JobEntryStatus.END, 123);

        assertThatThrownBy(() -> new JobExecution(null, endEntry))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("startEntry cannot be null");
    }

    @Test
    void when_endEntryIsNull_expect_validationFailure() {
        LogEntry startEntry = new LogEntry(LocalTime.of(11, 0, 0), "test job", JobEntryStatus.START, 123);

        assertThatThrownBy(() -> new JobExecution(startEntry, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("endEntry cannot be null");
    }
}

