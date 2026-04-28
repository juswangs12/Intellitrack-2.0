package com.intellitrack.service;

import com.intellitrack.dto.ChartPointDto;
import com.intellitrack.dto.RealTimeInsightDto;
import com.intellitrack.entity.Deadline;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SubmissionMetricsEngine {

    private final ProjectGroupRepository projectGroupRepository;
    private final DeliverableRepository deliverableRepository;
    private final SubmissionRepository submissionRepository;
    private final DeadlineRepository deadlineRepository;
    private final StatusEvaluationService statusEvaluationService;

    public SubmissionMetricsEngine(
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            SubmissionRepository submissionRepository,
            DeadlineRepository deadlineRepository,
            StatusEvaluationService statusEvaluationService) {
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.submissionRepository = submissionRepository;
        this.deadlineRepository = deadlineRepository;
        this.statusEvaluationService = statusEvaluationService;
    }

    public RealTimeInsightDto computeInsights(String stage, Long adviserId) {
        List<ProjectGroup> groups = adviserId == null
                ? projectGroupRepository.findAll()
                : projectGroupRepository.findByAdviserId(adviserId);

        List<Deliverable> deliverables = deliverableRepository.findAll().stream()
                .filter(deliverable -> stage == null || stage.isBlank()
                        || deliverable.getStage().equalsIgnoreCase(stage))
                .toList();

        Map<String, double[]> stageBuckets = new LinkedHashMap<>();
        long onTime = 0;
        long late = 0;
        long pending = 0;

        for (Deliverable deliverable : deliverables) {
            double[] counts = stageBuckets.computeIfAbsent(deliverable.getStage(), key -> new double[] { 0.0, 0.0 });

            for (ProjectGroup group : groups) {
                Submission submission = submissionRepository
                        .findByGroupIdAndDeliverableId(group.getId(), deliverable.getId()).orElse(null);
                Deadline deadline = deadlineRepository.findByDeliverableId(deliverable.getId()).orElse(null);
                SubmissionStatus statusValue = statusEvaluationService.computeStatus(submission, deadline);

                counts[1] += 1.0;

                if (statusValue == SubmissionStatus.SUBMITTED || statusValue == SubmissionStatus.UPDATED) {
                    onTime++;
                    counts[0] += 1.0;
                } else if (statusValue == SubmissionStatus.LATE) {
                    late++;
                } else {
                    pending++;
                }
            }
        }

        double total = onTime + late + pending;
        List<ChartPointDto> trendSeries = new ArrayList<>();
        stageBuckets.forEach((key, value) -> {
            double completionRate = value[1] == 0.0 ? 0.0 : (value[0] / value[1]) * 100.0;
            trendSeries.add(new ChartPointDto(key, roundToOneDecimal(completionRate)));
        });

        return new RealTimeInsightDto(
                percentage(onTime, total),
                percentage(late, total),
                percentage(pending, total),
                trendSeries,
                List.of(
                        new ChartPointDto("On Time", onTime),
                        new ChartPointDto("Late", late),
                        new ChartPointDto("Pending", pending)));
    }

    private double percentage(double part, double total) {
        if (total == 0.0) {
            return 0.0;
        }
        return roundToOneDecimal((part / total) * 100.0);
    }

    private double roundToOneDecimal(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.1f", value));
    }
}