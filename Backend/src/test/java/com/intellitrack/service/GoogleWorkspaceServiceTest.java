package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.repository.GoogleConnectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.access.AccessDeniedException;
import java.net.http.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GoogleWorkspaceServiceTest {
    GoogleWorkspaceService service;GoogleConnectionRepository repo;TokenCipher cipher;HttpClient http;
    Map<String,GoogleConnection> records;
    @BeforeEach void setup()throws Exception {
        repo=mock(GoogleConnectionRepository.class);records=new HashMap<>();
        when(repo.findById(anyString())).thenAnswer(i->Optional.ofNullable(records.get(i.getArgument(0))));
        when(repo.save(any())).thenAnswer(i->{GoogleConnection c=i.getArgument(0);records.put(c.getId(),c);return c;});
        when(repo.findByStateHash(anyString())).thenAnswer(i->records.values().stream().filter(c->Objects.equals(c.getStateHash(),i.getArgument(0))).findFirst());
        cipher=new TokenCipher(Base64.getEncoder().encodeToString(new byte[32]));
        service=new GoogleWorkspaceService(repo,cipher,new ObjectMapper());
        ReflectionTestUtils.setField(service,"clientId","test-client");ReflectionTestUtils.setField(service,"clientSecret","test-secret");
        ReflectionTestUtils.setField(service,"redirectUri","https://example.test/api/google/callback");ReflectionTestUtils.setField(service,"reportOwner","");
        http=mock(HttpClient.class);ReflectionTestUtils.setField(service,"http",http);
    }
    User user(String role){var u=new User("Test","User","user@example.test","hash",role);u.setId(7L);return u;}
    @Test void connectUsesStateAndPkceAndCannotCrossRoles(){
        var result=service.start(user("student"),"calendar");
        assertTrue(result.get("url").contains("code_challenge_method=S256"));
        assertTrue(result.get("url").contains("calendar.app.created"));
        assertNotEquals(result.get("state"),records.get("7:calendar").getStateHash());
        assertThrows(AccessDeniedException.class,()->service.start(user("student"),"reports"));
        assertTrue(service.start(user("coordinator"),"calendar").get("url").contains("calendar.app.created"));
        assertThrows(AccessDeniedException.class,()->service.start(user("adviser"),"calendar"));
    }
    @Test void wrongUserAndExpiredStateAreRejectedBeforeNetwork(){
        var result=service.start(user("student"),"calendar");
        assertThrows(AccessDeniedException.class,()->service.finish(8L,result.get("state"),"code"));
        records.get("7:calendar").setStateExpiresAt(Instant.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class,()->service.finish(7L,result.get("state"),"code"));
        verifyNoInteractions(http);
    }
    @SuppressWarnings("unchecked")
    @Test void successEncryptsRefreshTokenAndConsumesState()throws Exception {
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(i->{
            HttpRequest req=i.getArgument(0);HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(200);
            when(response.body()).thenReturn(req.uri().getPath().equals("/token")?
                "{\"access_token\":\"access\",\"refresh_token\":\"refresh\",\"scope\":\"https://www.googleapis.com/auth/calendar.app.created\"}":
                "{\"email\":\"user@gmail.com\",\"email_verified\":true}");return response;
        });
        var result=service.start(user("student"),"calendar");
        assertEquals("calendar",service.finish(7L,result.get("state"),"code"));
        var saved=records.get("7:calendar");assertNull(saved.getStateHash());assertNull(saved.getEncryptedVerifier());
        assertNotEquals("refresh",saved.getEncryptedRefreshToken());assertEquals("refresh",cipher.decrypt(saved.getEncryptedRefreshToken()));
        assertThrows(IllegalArgumentException.class,()->service.finish(7L,result.get("state"),"code"));
    }
    @SuppressWarnings("unchecked")
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"student","coordinator"})
    void repeatedCalendarSyncUpdatesAndArchiveRemovesOnlyAppEvents(String role)throws Exception {
        var c=new GoogleConnection();c.setId("7:calendar");c.setUserId(7L);c.setPurpose("calendar");c.setEncryptedRefreshToken(cipher.encrypt("refresh"));records.put(c.getId(),c);
        String id=GoogleWorkspaceService.eventId(7L,5L);
        var eventExists=new java.util.concurrent.atomic.AtomicBoolean(false);
        List<HttpRequest> requests=new ArrayList<>();
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(i->{
            HttpRequest req=i.getArgument(0);requests.add(req);int status=200;String body="{}";String path=req.uri().getPath();
            if(path.equals("/token"))body="{\"access_token\":\"access\"}";
            else if(path.endsWith("/calendars")&&req.method().equals("POST"))body="{\"id\":\"test-calendar\"}";
            else if(path.endsWith("/events/"+id)&&req.method().equals("PUT"))status=eventExists.get()?200:404;
            else if(path.endsWith("/events")&&req.method().equals("POST"))eventExists.set(true);
            else if(path.endsWith("/events")&&req.method().equals("GET")){
                assertTrue(req.uri().getQuery().contains("privateExtendedProperty=intellitrack=deadline"));
                body=eventExists.get()?"{\"items\":[{\"id\":\""+id+"\"}]}":"{}";
            }
            else if(req.method().equals("DELETE"))eventExists.set(false);
            HttpResponse<String> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(body);return response;
        });
        var row=new com.intellitrack.dto.TrackingRow(1L,7L,"S1","Student","A",2L,"CAP1","2026-2027 / 1",3L,"Alpha",4L,"Adviser",5L,"Proposal",java.time.LocalDateTime.of(2026,10,10,23,59),null,null,null,"AWAITING_SUBMISSION",null,null,true,true,null);
        service.sync(user(role),List.of(row));
        var changed=row.dueAt().plusDays(7);
        service.syncDeadlines(user(role),List.of(new GoogleWorkspaceService.CalendarDeadline(5L,"CAP1","Proposal",row.academicTerm(),changed)));
        service.sync(user(role),List.of());
        var updates=requests.stream().filter(r->r.method().equals("PUT")).toList();
        var initial=new ObjectMapper().readTree(requestBody(updates.get(0)));
        assertEquals("2026-10-10T23:59:00+08:00",initial.path("start").path("dateTime").asText());
        assertEquals("2026-10-11T00:14:00+08:00",initial.path("end").path("dateTime").asText());
        var inserted=requests.stream().filter(r->r.method().equals("POST")&&r.uri().getPath().endsWith("/events")).findFirst().orElseThrow();
        assertEquals(initial.path("start"),new ObjectMapper().readTree(requestBody(inserted)).path("start"));
        assertTrue(requestBody(updates.get(1)).contains(changed.toString()));
        assertTrue(requestBody(updates.get(1)).contains("+08:00"));
        if(role.equals("coordinator"))assertTrue(requestBody(updates.get(1)).contains("Monitor submissions"));
        assertEquals(1,requests.stream().filter(r->r.method().equals("POST")&&r.uri().getPath().endsWith("/calendars")).count());
        assertEquals(1,requests.stream().filter(r->r.method().equals("POST")&&r.uri().getPath().endsWith("/events")).count());
        assertEquals(2,requests.stream().filter(r->r.method().equals("PUT")).count());
        assertEquals(1,requests.stream().filter(r->r.method().equals("DELETE")).count());
    }
    @SuppressWarnings("unchecked")
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"Gemini AI","Computed summary"})
    void reportReusesWorkbookPreservesOtherScopesAndReplacesShrinkingRows(String source)throws Exception {
        var c=new GoogleConnection();c.setId("7:reports");c.setUserId(7L);c.setPurpose("reports");c.setEncryptedRefreshToken(cipher.encrypt("refresh"));records.put(c.getId(),c);
        var json=new ObjectMapper();
        Map<String,String> files=new HashMap<>();Map<String,Map<Integer,String>> tabs=new HashMap<>();
        Map<String,Map<Integer,List<List<Object>>>> contents=new HashMap<>();
        List<String> titles=new ArrayList<>();List<Map<String,Object>> lastBatch=new ArrayList<>();
        when(http.send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenAnswer(i->{
            HttpRequest req=i.getArgument(0);String path=req.uri().getPath();Map<String,Object> response=new HashMap<>();
            if(path.equals("/token"))response.put("access_token","access");
            else if(path.equals("/drive/v3/files")&&req.method().equals("GET")){
                String q=java.net.URLDecoder.decode(req.uri().getRawQuery(),java.nio.charset.StandardCharsets.UTF_8);
                var match=files.entrySet().stream().filter(e->q.contains(e.getKey())).findFirst();
                response.put("files",match.isPresent()?List.of(Map.of("id",match.get().getValue())):List.of());
            }else if(path.equals("/drive/v3/files")&&req.method().equals("POST")){
                var body=json.readTree(requestBody(req));
                if(body.path("mimeType").asText().endsWith("folder"))response.put("id","folder");
                else {String id="file"+files.size();files.put(body.path("appProperties").path("intellitrackReport").asText(),id);titles.add(body.path("name").asText());tabs.put(id,new LinkedHashMap<>());contents.put(id,new HashMap<>());response.put("id",id);}
            }else if(path.equals("/drive/v3/files/folder"))response.put("trashed",false);
            else if(path.startsWith("/v4/spreadsheets/")){
                String id=path.substring("/v4/spreadsheets/".length()).split("[:/]")[0];
                if(path.endsWith(":batchUpdate")){
                    var body=json.readTree(requestBody(req));
                    lastBatch.clear();lastBatch.addAll((List<Map<String,Object>>)json.readValue(requestBody(req),Map.class).get("requests"));
                    for(var op:body.path("requests")){
                        if(op.has("addSheet")){var props=op.path("addSheet").path("properties");tabs.get(id).put(props.path("sheetId").asInt(),props.path("title").asText());}
                        if(op.has("updateCells")){
                            var update=op.path("updateCells");assertEquals("userEnteredValue",update.path("fields").asText());assertFalse(update.path("range").has("endRowIndex"));
                            List<List<Object>> values=new ArrayList<>();
                            for(var row:update.path("rows")){List<Object> cells=new ArrayList<>();for(var cell:row.path("values")){var value=cell.path("userEnteredValue");assertFalse(value.has("formulaValue"));cells.add(value.has("numberValue")?value.get("numberValue").numberValue():value.path("stringValue").asText());}values.add(cells);}
                            contents.get(id).put(update.path("range").path("sheetId").asInt(),values);
                        }
                    }
                }else if(path.contains("/values/")){
                    String name=path.substring(path.indexOf("/values/")+8).replace("'","");
                    int sheet=tabs.get(id).entrySet().stream().filter(e->e.getValue().equals(name)).findFirst().orElseThrow().getKey();
                    response.put("values",contents.get(id).getOrDefault(sheet,List.of()));
                }else response.put("sheets",tabs.get(id).entrySet().stream().map(e->Map.of("properties",Map.of("sheetId",e.getKey(),"title",e.getValue()))).toList());
            }else fail("Unexpected request "+req.method()+" "+path);
            HttpResponse<String> res=mock(HttpResponse.class);when(res.statusCode()).thenReturn(200);when(res.body()).thenReturn(json.writeValueAsString(response));return res;
        });
        var insight=Map.<String,Object>of("source",source,"text","=untrusted text\nTracking explanation.","notice","Selected data only.");
        var first=reportRow(1L,"G1",2L,"2026-2027 / 1");var second=reportRow(2L,"G1",2L,"2026-2027 / 1");
        var g1=Map.of("section","G1");var g2=Map.of("section","G2");
        var firstResult=service.export(user("coordinator"),List.of(first,second),g1,Map.of("expected",2),insight);
        service.export(user("coordinator"),List.of(reportRow(3L,"G2",2L,"2026-2027 / 1")),g2,Map.of("expected",1),insight);
        var again=service.export(user("coordinator"),List.of(first),g1,Map.of("expected",1),insight);
        assertEquals(firstResult.get("url"),again.get("url"));assertEquals(1,files.size());
        assertEquals(Set.of("G1","G2","Groups","Summary"),new HashSet<>(tabs.get("file0").values()));
        var g1Data=contents.get("file0").get(0);assertEquals(5,g1Data.size());assertEquals("=SUM(1,1)",g1Data.get(4).get(0));
        int summaryId=tabs.get("file0").entrySet().stream().filter(e->e.getValue().equals("Summary")).findFirst().orElseThrow().getKey();
        var sums=contents.get("file0").get(summaryId);
        assertTrue(sums.contains(List.of("G1","Explanation source",source)));assertTrue(sums.contains(List.of("G2","Explanation source",source)));
        assertTrue(sums.contains(List.of("G1","Tracking explanation",insight.get("text"))));
        assertEquals(1,sums.stream().filter(row->row.equals(List.of("G1","Expected submissions",1))).count());
        assertTrue(lastBatch.toString().contains("WRAP"));assertTrue(lastBatch.toString().contains("pixelSize=760"));
        service.export(user("coordinator"),List.of(first),Map.of("section","G1","status","OVERDUE"),Map.of(),insight);
        assertEquals(1,files.size());assertEquals(5,tabs.get("file0").size());
        service.export(user("coordinator"),List.of(reportRow(4L,"G1",2L,"2026-2027 / 2")),g1,Map.of(),insight);
        service.export(user("coordinator"),List.of(reportRow(5L,"G1",3L,"2026-2027 / 1")),g1,Map.of(),insight);
        assertEquals(3,files.size());assertTrue(titles.get(0).contains("CAP2 | 2026-2027 | Semester 1"));
        assertThrows(IllegalArgumentException.class,()->service.export(user("coordinator"),List.of(first,reportRow(6L,"G1",3L,"2026-2027 / 1")),g1,Map.of(),insight));
        assertEquals(3,files.size());
    }
    private com.intellitrack.dto.TrackingRow reportRow(Long id,String section,Long subject,String term){return new com.intellitrack.dto.TrackingRow(id,id,"S"+id,"=SUM(1,1)",section,subject,"CAP"+subject,term,id,"Alpha",4L,"Adviser",5L,"Proposal",null,null,null,null,"OVERDUE",null,null,true,true,null);}
    @Test void reportTabNamesAreStableAndDoNotCollideWithReservedOrSanitizedNames(){
        assertEquals("G1",GoogleWorkspaceService.reportTab(Map.of("section","G1")));
        assertNotEquals("Summary",GoogleWorkspaceService.reportTab(Map.of("section","Summary")));
        assertNotEquals(GoogleWorkspaceService.reportTab(Map.of("section","A/B")),GoogleWorkspaceService.reportTab(Map.of("section","A_B")));
        assertEquals(GoogleWorkspaceService.reportTab(Map.of("section","G1","status","OVERDUE","group","2")),GoogleWorkspaceService.reportTab(Map.of("group","2","status","OVERDUE","section","G1","subject","3")));
    }
    private String requestBody(HttpRequest request)throws Exception {
        var result=new java.util.concurrent.CompletableFuture<String>();var bytes=new java.io.ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>(){
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription){subscription.request(Long.MAX_VALUE);}
            public void onNext(java.nio.ByteBuffer buffer){byte[] chunk=new byte[buffer.remaining()];buffer.get(chunk);bytes.writeBytes(chunk);}
            public void onError(Throwable error){result.completeExceptionally(error);}
            public void onComplete(){result.complete(bytes.toString(java.nio.charset.StandardCharsets.UTF_8));}
        });
        return result.get(5,java.util.concurrent.TimeUnit.SECONDS);
    }
}
