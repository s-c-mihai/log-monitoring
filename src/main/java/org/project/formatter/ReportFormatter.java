package org.project.formatter;

import org.project.model.JobAnalysis;
import org.project.model.JobAnalysisState;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Formats analyzed jobs into a human-readable report.
 */
public class ReportFormatter {

    /**
     * @param analyzedJobs The list of analyzed jobs to format
     * @return A formatted report string
     */
    public String format(List<JobAnalysis> analyzedJobs) {
        Map<JobAnalysisState, List<JobAnalysis>> stateToJobs = analyzedJobs.stream()
                .filter(JobAnalysis::hasJobExecution)
                .collect(Collectors.groupingBy(JobAnalysis::state));

        List<JobAnalysis> danglingEntries = analyzedJobs.stream()
                .filter(JobAnalysis::hasDanglingEntry)
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append('\n').append("=".repeat(128)).append('\n');
        sb.append("LOG MONITORING REPORT\n");
        sb.append("=".repeat(128)).append("\n\n");

        sb.append("Jobs summary\n");
        for (JobAnalysisState state : JobAnalysisState.values()) {
            sb.append("   + ").append(stateToJobs.getOrDefault(state, List.of()).size())
                    .append(' ').append(state).append('\n');
        }
        sb.append("A total of ").append(analyzedJobs.size() - danglingEntries.size()).append(" jobs completed").append('\n');
        sb.append('\n');

        if (!danglingEntries.isEmpty()) {
            sb.append("Dangling entries:\n");
            for (JobAnalysis analysis : danglingEntries) {
                sb.append(" - ").append(analysis.reason()).append('\n');
            }
            sb.append('\n');
        }

        for (JobAnalysisState state : JobAnalysisState.values()) {
            List<JobAnalysis> jobs = stateToJobs.getOrDefault(state, List.of())
                    .stream()
                    .toList();
            if (!jobs.isEmpty()) {
                sb.append("-".repeat(128)).append('\n');
                sb.append(state).append(" jobs:\n");
                sb.append("-".repeat(128)).append('\n');
                for (JobAnalysis jobAnalysis : jobs) {
                    sb.append(jobAnalysis.jobExecution()).append('\n');
                }
                sb.append('\n');
            }
        }

        sb.append("=".repeat(128)).append('\n');
        return sb.toString();
    }

}

