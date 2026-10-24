package com.intellitrack.service;

import com.intellitrack.controller.GoogleWorkspaceController;
import com.intellitrack.dto.TrackingRow;
import com.intellitrack.entity.User;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportExportScopeTest {
    @Test void exportSummaryUsesOnlyAuthorizedFilteredRows() {
        var google=mock(GoogleWorkspaceService.class);
        var tracking=mock(TrackingService.class);
        var access=mock(AccessService.class);
        var summaries=mock(TrackingSummaryService.class);
        var user=new User("Test","Coordinator","test@example.test","hash","coordinator");user.setId(7L);
        var included=mock(TrackingRow.class);var excluded=mock(TrackingRow.class);
        var all=List.of(included,excluded);var selected=List.of(included);
        var filters=Map.of("subject","1");var counts=Map.<String,Object>of("expected",1);
        var explanation=Map.<String,Object>of("source","Computed summary","text","One assignment.","notice","AI unavailable.");
        when(access.current()).thenReturn(user);
        when(tracking.rows(user)).thenReturn(all);
        when(tracking.filter(all,filters)).thenReturn(selected);
        when(tracking.summary(selected)).thenReturn(counts);
        when(summaries.summarize(7L,"coordinator",selected)).thenReturn(explanation);
        when(google.export(user,selected,filters,counts,explanation)).thenReturn(Map.of("url","report"));
        assertEquals("report",new GoogleWorkspaceController(google,tracking,access,summaries).export(filters).get("url"));
        verify(summaries).summarize(7L,"coordinator",selected);
        verifyNoMoreInteractions(summaries);
    }
}
