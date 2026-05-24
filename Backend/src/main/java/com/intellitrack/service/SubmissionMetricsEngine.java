package com.intellitrack.service;

import com.intellitrack.dto.ChartPointDto;
import com.intellitrack.dto.RealTimeInsightDto;
import com.intellitrack.entity.Deliverable;
import com.intellitrack.entity.ProjectGroup;
import com.intellitrack.entity.Submission;
import com.intellitrack.entity.SubmissionStatus;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.ProjectGroupRepository;
import com.intellitrack.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final StatusEvaluationService statusEvaluationService;

    public SubmissionMetricsEngine(
            ProjectGroupRepository projectGroupRepository,
            DeliverableRepository deliverableRepository,
            SubmissionRepository submissionRepository,
            StatusEvaluationService statusEvaluationService) {
        this.projectGroupRepository = projectGroupRepository;
        this.deliverableRepository = deliverableRepository;
        this.submissionRepository = submissionRepository;
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

        List<Long> groupIds = groups.stream().map(ProjectGroup::getId).toList();
        List<Long> deliverableIds = deliverables.stream().map(Deliverable::getId).toList();

        // Bulk fetch all relevant data
        List<Submission> allSubmissions = submissionRepository.findByGroupIdInAndDeliverableIdIn(groupIds,
                deliverableIds);

        // Build in-memory lookup map for submissions
        Map<Long, Map<Long, Submission>> submissionsByGroupAndDeliverable = new HashMap<>();
        for (Submission submission : allSubmissions) {
            Long groupId = submission.getGroup().getId();
            Long deliverableId = submission.getDeliverable().getId();
            submissionsByGroupAndDeliverable
                    .computeIfAbsent(groupId, k -> new HashMap<>())
                    .put(deliverableId, submission);
        }

        Map<String, double[]> stageBuckets = new LinkedHashMap<>();
        long onTime = 0;
        long late = 0;
        long notSubmitted = 0;

        for (Deliverable deliverable : deliverables) {
            double[] counts = stageBuckets.computeIfAbsent(deliverable.getStage(), key -> new double[] { 0.0, 0.0 });

            for (ProjectGroup group : groups) {
                Submission submission = submissionsByGroupAndDeliverable
                        .getOrDefault(group.getId(), new HashMap<>())
                        .get(deliverable.getId());
                SubmissionStatus statusValue = statusEvaluationService.computeStatus(submission,
                        deliverable.getDueAt());

                counts[1] += 1.0;

                if (statusValue == SubmissionStatus.SUBMITTED_ON_TIME) {
                    onTime++;
                    counts[0] += 1.0;
                } else if (statusValue == SubmissionStatus.SUBMITTED_LATE) {
                    late++;
                } else if (statusValue == SubmissionStatus.NOT_SUBMITTED) {
                    notSubmitted++;
                }
            }
        }

        double total = onTime + late + notSubmitted;
        List<ChartPointDto> trendSeries = new ArrayList<>();
        stageBuckets.forEach((key, value) -> {
            double completionRate = value[1] == 0.0 ? 0.0 : (value[0] / value[1]) * 100.0;
            trendSeries.add(new ChartPointDto(key, roundToOneDecimal(completionRate)));
        });

        return new RealTimeInsightDto(
                percentage(onTime, total),
                percentage(late, total),
                percentage(notSubmitted, total),
                trendSeries,
                List.of(
                        new ChartPointDto("On Time", onTime),
                        new ChartPointDto("Late", late),
                        new ChartPointDto("Not Submitted", notSubmitted)));
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