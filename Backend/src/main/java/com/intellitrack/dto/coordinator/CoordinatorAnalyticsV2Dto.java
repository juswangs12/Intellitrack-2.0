package com.intellitrack.dto.coordinator;

import java.util.List;

public record CoordinatorAnalyticsV2Dto(
    SummaryCardsDto summaryCards,
    List<DeliverableMetaDto> deliverables,
    List<StudentAnalyticsRowDto> students,
    List<SubjectMetaDto> subjects, // for filtering
    List<SectionMetaDto> sections, // for filtering
    List<GroupMetaDto> groups, // for filtering
    List<AdviserMetaDto> advisers // for filtering
) {}
