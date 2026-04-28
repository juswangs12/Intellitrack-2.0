package com.intellitrack.service;

import com.intellitrack.dto.RawSubmissionSummaryData;
import com.intellitrack.dto.SubmissionSummaryDto;
import com.intellitrack.dto.SummaryInsightDto;
import org.springframework.stereotype.Service;

@Service
public class SummaryFormatterService {

    public SubmissionSummaryDto format(RawSubmissionSummaryData rawData, SummaryInsightDto insight) {
        return new SubmissionSummaryDto(
                rawData.groupId(),
                rawData.groupCode(),
                rawData.groupTitle(),
                insight.headline(),
                insight.detail(),
                rawData.deliverables(),
                rawData.timeline());
    }
}