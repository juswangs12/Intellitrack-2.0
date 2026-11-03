package com.intellitrack.service;
import com.intellitrack.entity.*;
import com.intellitrack.dto.TrackingRow;
import com.intellitrack.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
class WorkspaceHistoryServiceTest {
 @Test void historyIsOwnerAndRoleScopedAndRechecksGroupAccess() throws Exception {
  var repo=mock(AuditLogRepository.class);var service=new WorkspaceHistoryService(mock(AuditService.class),repo,new ObjectMapper());
  var user=new User();user.setId(7L);user.setRole("adviser");
  var row=mock(TrackingRow.class);when(row.enrollmentId()).thenReturn(1L);when(row.deliverableId()).thenReturn(2L);when(row.groupId()).thenReturn(3L);
  var log=new AuditLog("AI_SUMMARY","workspace:7","WORKSPACE",new ObjectMapper().writeValueAsString(Map.of("role","adviser","keys",List.of("1:2:3"),"scope",Map.of(),"result",Map.of("text","Saved progress"))),null);log.setId(9L);
  when(repo.findTop50ByActionAndPerformedByOrderByTimestampDesc("AI_SUMMARY","workspace:7")).thenReturn(List.of(log));
  assertEquals(1,service.list(user,"AI_SUMMARY",List.of(row)).size());
  assertTrue(service.list(user,"AI_SUMMARY",List.of()).isEmpty());
  when(row.groupId()).thenReturn(4L);assertTrue(service.list(user,"AI_SUMMARY",List.of(row)).isEmpty());
  user.setRole("student");assertTrue(service.list(user,"AI_SUMMARY",List.of(row)).isEmpty());
  verify(repo,times(4)).findTop50ByActionAndPerformedByOrderByTimestampDesc("AI_SUMMARY","workspace:7");
 }
 @Test void savesSnapshotAndImportsUseCurrentAccountEmail() throws Exception {
  var repo=mock(AuditLogRepository.class);var audit=mock(AuditService.class);var service=new WorkspaceHistoryService(audit,repo,new ObjectMapper());var user=new User();user.setId(5L);user.setRole("coordinator");user.setEmail("owner@example.test");
  service.save(user,"REPORT_EXPORT",Map.of("term","2026-2027 / 1"),List.of(),Map.of("url","https://docs.google.com/spreadsheets/d/example"));
  verify(audit).log(eq("REPORT_EXPORT"),eq("workspace:5"),eq("WORKSPACE"),contains("2026-2027 / 1"),isNull());
  service.imports(user);verify(repo).findTop50ByActionAndPerformedByOrderByTimestampDesc("CLASSLIST_IMPORT","owner@example.test");
 }
}
