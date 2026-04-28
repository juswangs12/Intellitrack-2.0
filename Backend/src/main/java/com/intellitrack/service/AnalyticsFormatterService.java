package com.intellitrack.service;

import com.intellitrack.dto.ChartPointDto;
import com.intellitrack.dto.MetricCardDto;
import com.intellitrack.dto.SubmissionTrackingDashboardDto;
import com.intellitrack.dto.TrackingSnapshotDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnalyticsFormatterService {

    public SubmissionTrackingDashboardDto formatTracking(TrackingSnapshotDto snapshot) {
        return new SubmissionTrackingDashboardDto(
                List.of(
                        new MetricCardDto("Total Deliverables", snapshot.totalDeliverables(), "neutral"),
                        new MetricCardDto("Submitted", snapshot.submitted(), "success"),
                        new MetricCardDto("Pending", snapshot.pending(), "warning"),
                        new MetricCardDto("Late", snapshot.late(), "danger")),
                List.of(
                        new ChartPointDto("Submitted", snapshot.submitted()),
                        new ChartPointDto("Pending", snapshot.pending()),
                        new ChartPointDto("Late", snapshot.late())),
                snapshot.activityFeed(),
                snapshot.groupProgress());
    }
}