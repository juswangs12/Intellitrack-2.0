package com.intellitrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellitrack.entity.*;
import com.intellitrack.dto.TrackingRow;
import com.intellitrack.repository.GoogleConnectionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class GoogleWorkspaceService {
    private final GoogleConnectionRepository connections; private final TokenCipher cipher; private final ObjectMapper json;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    @Value("${spring.security.oauth2.client.registration.google.client-id:}") private String clientId;
    @Value("${spring.security.oauth2.client.registration.google.client-secret:}") private String clientSecret;
    @Value("${google.workspace.redirect-uri:}") private String redirectUri;
    @Value("${google.reports.owner-email:}") private String reportOwner;
    @Value("${google.deadlines.sender-email:}") private String deadlineSender="";
    public GoogleWorkspaceService(GoogleConnectionRepository r,TokenCipher c,ObjectMapper j){connections=r;cipher=c;json=j;}
    public List<String> configurationProblems(){
        List<String> problems=new ArrayList<>();
        if(clientId==null||clientId.isBlank())problems.add("Set GOOGLE_CLIENT_ID in the backend environment.");
        if(clientSecret==null||clientSecret.isBlank())problems.add("Set GOOGLE_CLIENT_SECRET in the backend environment.");
        try{var uri=URI.create(Objects.toString(redirectUri,"").strip());if(uri.getHost()==null||!Set.of("https","http").contains(uri.getScheme())||!"/api/google/callback".equals(uri.getPath()))throw new IllegalArgumentException();}
        catch(Exception e){problems.add("Set GOOGLE_WORKSPACE_REDIRECT_URI to the backend URL ending in /api/google/callback.");}
        if(!cipher.configured())problems.add("GOOGLE_TOKEN_ENCRYPTION_KEY must be Base64 encoding of exactly 32 random bytes. Paste the value without quotes, then restart the backend.");
        return problems;
    }
    public boolean configured(){return configurationProblems().isEmpty();}
    public boolean connected(Long id,String purpose){return connections.findById(id+":"+purpose).map(c->c.getEncryptedRefreshToken()!=null).orElse(false);}
    public List<String> tools(String role){return switch(role){case "student" -> List.of("calendar");case "coordinator" -> List.of("calendar","imports","reports");case "administrator" -> List.of("imports","reports");default -> List.of();};}
    private List<String> bundle(String purpose){return purpose.endsWith("-tools")?tools(purpose.replace("-tools","")):List.of(purpose);}
    private String scope(String purpose){
        if(purpose.endsWith("-tools"))return String.join(" ",bundle(purpose).stream().map(this::scope).toList());
        return "imports".equals(purpose)?"https://www.googleapis.com/auth/spreadsheets.readonly https://www.googleapis.com/auth/drive.metadata.readonly":"mail".equals(purpose)?"https://www.googleapis.com/auth/gmail.send":"calendar".equals(purpose)?"https://www.googleapis.com/auth/calendar.app.created":"https://www.googleapis.com/auth/drive.file";
    }
    @Transactional
    public Map<String,String> start(User user,String purpose){
        if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,String.join(" ",configurationProblems()));
        if(!Set.of("calendar","reports","mail","imports","student-tools","coordinator-tools","administrator-tools").contains(purpose))throw new IllegalArgumentException("Unknown connection purpose");
        if(purpose.endsWith("-tools")&&!purpose.equals(user.getRole()+"-tools"))throw new org.springframework.security.access.AccessDeniedException("Google tools must match your assigned role");
        if("imports".equals(purpose)&&!Set.of("coordinator","administrator").contains(user.getRole()))throw new org.springframework.security.access.AccessDeniedException("Only coordinators and administrators can connect classlist imports");
        if("mail".equals(purpose)&&!"administrator".equals(user.getRole()))throw new org.springframework.security.access.AccessDeniedException("Only administrators can connect the deadline email sender");
        if("mail".equals(purpose)&&deadlineSender.isBlank())throw new IllegalArgumentException("Configure GOOGLE_DEADLINE_SENDER_EMAIL before connecting the sender");
        if("calendar".equals(purpose)&&!Set.of("student","coordinator").contains(user.getRole()))throw new org.springframework.security.access.AccessDeniedException("Calendar sync is for students and coordinators");
        if("reports".equals(purpose)&&!Set.of("coordinator","administrator").contains(user.getRole()))throw new org.springframework.security.access.AccessDeniedException("Report export is for coordinators");
        String state=random(),verifier=random();
        var c=connections.findById(user.getId()+":"+purpose).orElseGet(GoogleConnection::new);
        c.setId(user.getId()+":"+purpose);c.setUserId(user.getId());c.setPurpose(purpose);c.setStateHash(hash(state));c.setStateExpiresAt(Instant.now().plusSeconds(600));c.setEncryptedVerifier(cipher.encrypt(verifier));connections.save(c);
        String url="https://accounts.google.com/o/oauth2/v2/auth?"+form(Map.of("client_id",clientId,"redirect_uri",redirectUri,"response_type","code","scope","openid email "+scope(purpose),"state",state,"access_type","offline","prompt","consent","code_challenge",challenge(verifier),"code_challenge_method","S256"));
        return Map.of("url",url,"state",state);
    }
    @Transactional
    public String finish(User user,String state,String code){
        var pending=connections.findByStateHash(hash(state)).orElseThrow(()->new IllegalArgumentException("Google connection expired; reconnect"));
        String purpose=pending.getPurpose();
        if(purpose.endsWith("-tools")&&!purpose.equals(user.getRole()+"-tools"))throw new org.springframework.security.access.AccessDeniedException("Your role changed; reconnect your Google tools");
        return finish(user.getId(),state,code);
    }
    @Transactional
    public String finish(Long userId,String state,String code){
        var c=connections.findByStateHash(hash(state)).orElseThrow(()->new IllegalArgumentException("Google connection expired; reconnect"));
        if(!c.getUserId().equals(userId))throw new org.springframework.security.access.AccessDeniedException("Google connection belongs to another signed-in user");
        if(c.getStateExpiresAt()==null||c.getStateExpiresAt().isBefore(Instant.now()))throw new IllegalArgumentException("Google connection expired; reconnect");
        var token=token(Map.of("code",code,"client_id",clientId,"client_secret",clientSecret,"redirect_uri",redirectUri,"grant_type","authorization_code","code_verifier",cipher.decrypt(c.getEncryptedVerifier())));
        String granted=Objects.toString(token.get("scope"),"");
        if(!Arrays.asList(granted.split(" ")).containsAll(Arrays.asList(scope(c.getPurpose()).split(" "))))throw new IllegalArgumentException("Required Google permission was not granted");
        String accessToken=(String)token.get("access_token");
        var info=api(accessToken,"GET","https://www.googleapis.com/oauth2/v3/userinfo",null);
        String email=Objects.toString(info.get("email"),"");
        if(!Boolean.TRUE.equals(info.get("email_verified")))throw new IllegalArgumentException("A verified Google account is required");
        if(bundle(c.getPurpose()).contains("reports")&&!reportOwner.isBlank()&&!reportOwner.equalsIgnoreCase(email))throw new IllegalArgumentException("Connect the designated report-owner Google account");
        if("mail".equals(c.getPurpose())&&(deadlineSender.isBlank()||!deadlineSender.equalsIgnoreCase(email)))throw new IllegalArgumentException("Connect the configured deadline sender Gmail");
        if(c.getGoogleEmail()!=null&&!c.getGoogleEmail().equalsIgnoreCase(email)){c.setCalendarId(null);c.setFolderId(null);c.setEncryptedRefreshToken(null);c.setLastSuccessAt(null);c.setCalendarFingerprint(null);}
        String refresh=(String)token.get("refresh_token");
        if(refresh!=null)c.setEncryptedRefreshToken(cipher.encrypt(refresh));
        if(c.getEncryptedRefreshToken()==null)throw new IllegalArgumentException("Google did not grant offline access; reconnect and grant consent");
        c.setGoogleEmail(email);c.setStateHash(null);c.setStateExpiresAt(null);c.setEncryptedVerifier(null);c.setNextAutoSyncAt(null);connections.save(c);
        if(c.getPurpose().endsWith("-tools"))for(String purpose:bundle(c.getPurpose())){
            var destination=connections.findById(userId+":"+purpose).orElseGet(GoogleConnection::new);
            if(destination.getGoogleEmail()!=null&&!destination.getGoogleEmail().equalsIgnoreCase(email)){
                destination.setCalendarId(null);destination.setFolderId(null);destination.setCalendarFingerprint(null);destination.setLastSuccessAt(null);
            }
            destination.setId(userId+":"+purpose);destination.setUserId(userId);destination.setPurpose(purpose);destination.setGoogleEmail(email);
            destination.setEncryptedRefreshToken(c.getEncryptedRefreshToken());destination.setNextAutoSyncAt(null);connections.save(destination);
        }
        return c.getPurpose();
    }
    public String deadlineSenderEmail(){return deadlineSender;}
    public static String spreadsheetId(String url){
        try{var uri=URI.create(url.strip());if(!"https".equals(uri.getScheme())||!"docs.google.com".equals(uri.getHost())||uri.getUserInfo()!=null||uri.getPort()!=-1)throw new IllegalArgumentException();
            var match=java.util.regex.Pattern.compile("^/spreadsheets/d/([A-Za-z0-9_-]+)(?:/.*)?$").matcher(uri.getPath());if(match.matches())return match.group(1);
        }catch(Exception ignored){}
        throw new IllegalArgumentException("Paste a Google Sheets link beginning with https://docs.google.com/spreadsheets/d/");
    }
    @SuppressWarnings("unchecked")
    public Map<String,Object> classlistFiles(User user,String search,String page){
        if(!Set.of("coordinator","administrator").contains(user.getRole()))throw new org.springframework.security.access.AccessDeniedException("Staff imports only");
        if(search.length()>200||page.length()>2048)throw new IllegalArgumentException("Search or page token is too long");
        String query="trashed = false and mimeType = 'application/vnd.google-apps.spreadsheet'";
        if(!search.isBlank())query+=" and name contains '"+search.replace("\\","\\\\").replace("'","\\'")+"'";
        var result=api(accessToken(connection(user,"imports")),"GET","https://www.googleapis.com/drive/v3/files?q="+encode(query)+"&fields="+encode("nextPageToken,files(id,name,modifiedTime)")+"&pageSize=50&orderBy=modifiedTime%20desc"+(page.isBlank()?"":"&pageToken="+encode(page)),null);
        return Map.of("files",maps(result.get("files")),"nextPageToken",Objects.toString(result.get("nextPageToken"),""));
    }
    public List<List<Object>> classlistRows(User user,String url,String tab){
        if(!Set.of("coordinator","administrator").contains(user.getRole()))throw new org.springframework.security.access.AccessDeniedException("Staff only");
        String id=spreadsheetId(url),token=accessToken(connection(user,"imports"));
        String base="https://sheets.googleapis.com/v4/spreadsheets/"+id;
        String title=tab==null?"":tab.strip();
        if(title.isBlank()){
            var metadata=api(token,"GET",base+"?fields=sheets.properties.title",null);
            var sheets=(List<Map<String,Object>>)metadata.getOrDefault("sheets",List.of());
            if(sheets.isEmpty())throw new IllegalArgumentException("This spreadsheet has no worksheets");
            title=Objects.toString(((Map<String,Object>)sheets.get(0).get("properties")).get("title"));
        }
        if(title.length()>100)throw new IllegalArgumentException("Worksheet name is too long");
        var result=api(token,"GET",base+"/values/"+encode("'"+title.replace("'","''")+"'!A1:G5002").replace("+","%20")+"?valueRenderOption=FORMATTED_VALUE",null);
        var rows=(List<List<Object>>)result.getOrDefault("values",List.of());
        if(rows.size()<2)throw new IllegalArgumentException("The worksheet needs a header and at least one student row");
        if(rows.size()>5001)throw new IllegalArgumentException("Import at most 5,000 student rows at a time");
        return rows;
    }
    private Optional<GoogleConnection> deadlineSenderConnection(){return connections.findAll().stream().filter(c->"mail".equals(c.getPurpose())&&c.getEncryptedRefreshToken()!=null&&!deadlineSender.isBlank()&&deadlineSender.equalsIgnoreCase(c.getGoogleEmail())).sorted(Comparator.comparing(GoogleConnection::getId)).findFirst();}
    public boolean deadlineMailConnected(){return deadlineSenderConnection().isPresent();}
    public void sendDeadlineEmail(String to,String subject,String body){
        if(to==null||!to.matches("[^\\s<>@]+@[^\\s<>@]+\\.[^\\s<>@]+"))throw new IllegalArgumentException("Invalid recipient email");
        var c=deadlineSenderConnection().orElseThrow(()->new IllegalStateException("Connect the designated Gmail sender first"));
        String encodedSubject=Base64.getEncoder().encodeToString(subject.getBytes(StandardCharsets.UTF_8));
        String mime="From: "+c.getGoogleEmail()+"\r\nTo: "+to+"\r\nSubject: =?UTF-8?B?"+encodedSubject+"?=\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nContent-Transfer-Encoding: base64\r\n\r\n"+Base64.getMimeEncoder().encodeToString(body.getBytes(StandardCharsets.UTF_8));
        api(accessToken(c),"POST","https://gmail.googleapis.com/gmail/v1/users/me/messages/send",Map.of("raw",Base64.getUrlEncoder().withoutPadding().encodeToString(mime.getBytes(StandardCharsets.UTF_8))));
        c.setLastSuccessAt(Instant.now());connections.save(c);
    }
    private GoogleConnection connection(User user,String purpose){return connections.findById(user.getId()+":"+purpose).filter(c->c.getEncryptedRefreshToken()!=null).orElseThrow(()->new ResponseStatusException(HttpStatus.CONFLICT,"Connect Google before continuing"));}
    private String accessToken(GoogleConnection c){
        try{return (String)token(Map.of("client_id",clientId,"client_secret",clientSecret,"refresh_token",cipher.decrypt(c.getEncryptedRefreshToken()),"grant_type","refresh_token")).get("access_token");}
        catch(ResponseStatusException ex){throw new ResponseStatusException(HttpStatus.CONFLICT,"Google access expired or was revoked. Reconnect Google.");}
    }
    public record CalendarDeadline(Long deliverableId,String subject,String deliverable,String academicTerm,LocalDateTime dueAt) {}
    public Map<String,Object> sync(User user,List<TrackingRow> rows){
        return syncDeadlines(user,rows.stream().map(r->new CalendarDeadline(r.deliverableId(),r.subject(),r.deliverable(),r.academicTerm(),r.dueAt())).toList());
    }
    public synchronized Map<String,Object> syncDeadlines(User user,List<CalendarDeadline> rows){
        var c=connection(user,"calendar");String token=accessToken(c);
        if(c.getCalendarId()!=null)try{api(token,"GET","https://www.googleapis.com/calendar/v3/calendars/"+encode(c.getCalendarId()),null);}
        catch(ResponseStatusException ex){if(ex.getStatusCode().value()!=404&&ex.getStatusCode().value()!=410)throw ex;c.setCalendarId(null);}
        if(c.getCalendarId()==null){var calendar=api(token,"POST","https://www.googleapis.com/calendar/v3/calendars",Map.of("summary","IntelliTrack Deadlines","timeZone","Asia/Manila"));c.setCalendarId((String)calendar.get("id"));connections.save(c);}
        String base="https://www.googleapis.com/calendar/v3/calendars/"+encode(c.getCalendarId())+"/events";
        Map<Long,CalendarDeadline> unique=new LinkedHashMap<>();for(var row:rows)if(row.dueAt()!=null)unique.put(row.deliverableId(),row);
        Set<String> wanted=new HashSet<>(); int updated=0;
        for(var row:unique.values()){
            String eventId=eventId(user.getId(),row.deliverableId());wanted.add(eventId);
            var start=row.dueAt().atZone(ZoneId.of("Asia/Manila"));
            Map<String,Object> body=new LinkedHashMap<>();body.put("summary",row.subject()+": "+row.deliverable());body.put("description","IntelliTrack deadline • "+Objects.toString(row.academicTerm(),"Legacy term")+("coordinator".equals(user.getRole())?"\nMonitor submissions in IntelliTrack. Change deadlines in Academic setup, then sync again.":"\nSubmit your Google Docs link in IntelliTrack."));
            // OffsetDateTime.toString() omits zero seconds, which is not RFC 3339.
            var timestamp=java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            body.put("start",Map.of("dateTime",start.format(timestamp),"timeZone","Asia/Manila"));body.put("end",Map.of("dateTime",start.plusMinutes(15).format(timestamp),"timeZone","Asia/Manila"));
            body.put("reminders",Map.of("useDefault",false,"overrides",List.of(Map.of("method","popup","minutes",1440),Map.of("method","popup","minutes",60))));
            body.put("extendedProperties",Map.of("private",Map.of("intellitrack","deadline")));
            try{api(token,"PUT",base+"/"+eventId,body);}catch(ResponseStatusException ex){if(ex.getStatusCode().value()!=404&&ex.getStatusCode().value()!=410)throw ex;body.put("id",eventId);try{api(token,"POST",base,body);}catch(ResponseStatusException race){if(race.getStatusCode().value()!=409)throw race;body.remove("id");api(token,"PUT",base+"/"+eventId,body);}}
            updated++;
        }
        // Remove only this application's stale events (archived requirements or removed enrollment).
        String page="";
        do{
            var listed=api(token,"GET",base+"?privateExtendedProperty=intellitrack%3Ddeadline&maxResults=2500"+(page.isEmpty()?"":"&pageToken="+encode(page)),null);
            for(var event:maps(listed.get("items"))){String id=(String)event.get("id");if(!wanted.contains(id))api(token,"DELETE",base+"/"+id,null);}
            page=Objects.toString(listed.get("nextPageToken"),"");
        }while(!page.isEmpty());
        c.setLastSuccessAt(Instant.now());connections.save(c);
        return Map.of("synced",updated,"calendarUrl","https://calendar.google.com/calendar/u/0/r");
    }
    public synchronized Map<String,String> export(User user,List<TrackingRow> rows,Map<String,String> filters,Map<String,Object> summary,Map<String,Object> insight){
        if(rows.isEmpty())throw new IllegalArgumentException("No submissions match this export. Adjust the filters first.");
        var firstRow=rows.get(0);
        if(rows.stream().anyMatch(r->!Objects.equals(r.subjectId(),firstRow.subjectId())||!Objects.equals(r.academicTerm(),firstRow.academicTerm())))
            throw new IllegalArgumentException("Choose one subject and academic term before exporting. Each subject and semester has its own report file.");
        String tab=reportTab(filters);
        String workbookKey=hash("reports-v2:"+user.getId()+":"+firstRow.subjectId()+":"+Objects.toString(firstRow.academicTerm(),"Legacy"));
        String title="IntelliTrack | "+firstRow.subject()+" | "+Objects.toString(firstRow.academicTerm(),"Unassigned term").replace(" / "," | Semester ");
        var c=connection(user,"reports");String token=accessToken(c);
        if(c.getFolderId()!=null)try{var folder=api(token,"GET","https://www.googleapis.com/drive/v3/files/"+encode(c.getFolderId())+"?fields=id,trashed",null);if(Boolean.TRUE.equals(folder.get("trashed")))c.setFolderId(null);}
        catch(ResponseStatusException ex){if(ex.getStatusCode().value()!=404)throw ex;c.setFolderId(null);}
        if(c.getFolderId()==null){var folder=api(token,"POST","https://www.googleapis.com/drive/v3/files",Map.of("name","IntelliTrack Reports","mimeType","application/vnd.google-apps.folder"));c.setFolderId((String)folder.get("id"));connections.save(c);}
        String stamp=LocalDateTime.now(ZoneId.of("Asia/Manila")).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String search="trashed = false and mimeType = 'application/vnd.google-apps.spreadsheet' and appProperties has { key='intellitrackReport' and value='"+workbookKey+"' }";
        var found=maps(api(token,"GET","https://www.googleapis.com/drive/v3/files?q="+encode(search)+"&fields=files(id)&orderBy=createdTime&pageSize=1",null).get("files"));
        boolean created=found.isEmpty();
        // Tag at creation so retries can recover the same file after a failed Sheets update.
        String id=created?(String)api(token,"POST","https://www.googleapis.com/drive/v3/files",Map.of("name",title,"mimeType","application/vnd.google-apps.spreadsheet","parents",List.of(c.getFolderId()),"appProperties",Map.of("intellitrackReport",workbookKey))).get("id"):(String)found.get(0).get("id");
        String base="https://sheets.googleapis.com/v4/spreadsheets/"+id;
        var sheets=maps(api(token,"GET",base+"?fields=sheets(properties)",null).get("sheets"));
        Map<String,Integer> sheetIds=new LinkedHashMap<>();
        Map<Integer,Integer> rowCounts=new HashMap<>();
        for(var sheet:sheets){var props=(Map<?,?>)sheet.get("properties");int sid=((Number)props.get("sheetId")).intValue();sheetIds.put(props.get("title").toString(),sid);if(props.get("gridProperties") instanceof Map<?,?> grid&&grid.get("rowCount") instanceof Number count)rowCounts.put(sid,count.intValue());}
        List<Object> additions=new ArrayList<>();
        if(created&&sheetIds.size()==1&&sheetIds.containsKey("Sheet1")){
            int defaultId=sheetIds.remove("Sheet1");sheetIds.put(tab,defaultId);
            additions.add(Map.of("updateSheetProperties",Map.of("properties",Map.of("sheetId",defaultId,"title",tab),"fields","title")));
        }
        int nextId=sheetIds.values().stream().mapToInt(Integer::intValue).max().orElse(-1)+1;
        Set<String> newTabs=new HashSet<>();
        for(String name:List.of(tab,"Groups","Summary"))if(!sheetIds.containsKey(name)){
            sheetIds.put(name,nextId++);newTabs.add(name);
            additions.add(Map.of("addSheet",Map.of("properties",Map.of("sheetId",sheetIds.get(name),"title",name))));
        }
        if(!additions.isEmpty())api(token,"POST",base+":batchUpdate",Map.of("requests",additions));
        String filterText=new TreeMap<>(filters).toString();
        List<List<Object>> values=new ArrayList<>();values.add(List.of("Report scope",tab));values.add(List.of("Last updated (Asia/Manila)",stamp));values.add(List.of("Filters",filterText));values.add(List.of("Student","Student ID","Section","Subject","Term","Group","Adviser","Requirement","Deadline (Asia/Manila)","First submitted","Latest submitted","First status","Latest status","Google Docs"));
        for(var r:rows)values.add(Arrays.asList(r.studentName(),r.studentNumber(),r.section(),r.subject(),Objects.toString(r.academicTerm(),"Legacy"),r.group(),r.adviser(),r.deliverable(),text(r.dueAt()),text(r.firstSubmittedAt()),text(r.latestSubmittedAt()),r.status(),Objects.toString(r.latestStatus(),""),Objects.toString(r.googleDocUrl(),"")));
        List<List<Object>> groupValues=new ArrayList<>();groupValues.add(List.of("Report scope","Last updated (Asia/Manila)","Group ID","Group","Subject","Term","On time","Late","Overdue","Due today","Awaiting"));
        var byGroup=rows.stream().collect(java.util.stream.Collectors.groupingBy(r->r.subjectId()+":"+Objects.toString(r.academicTerm(),"Legacy")+":"+r.groupId()));
        for(var entry:byGroup.entrySet()){var first=entry.getValue().get(0);List<Object> g=new ArrayList<>(List.of(tab,stamp,first.groupId()==null?"Unassigned":first.groupId(),first.group(),first.subject(),Objects.toString(first.academicTerm(),"Legacy")));for(String status:List.of("SUBMITTED_ON_TIME","SUBMITTED_LATE","OVERDUE","DUE_TODAY","AWAITING_SUBMISSION"))g.add(entry.getValue().stream().filter(r->r.status().equals(status)).count());groupValues.add(g);}
        List<List<Object>> sums=new ArrayList<>();sums.add(List.of("Report summary","Selected submission tracking data"));sums.add(List.of("Generated (Asia/Manila)",stamp));sums.add(List.of("Filters",filters.isEmpty()?"All terms and subjects":filters.toString()));sums.add(List.of("Units","Status counts are student-requirement assignments, not distinct students. First submission determines compliance."));
        var labels=Map.of("students","Distinct students","expected","Expected submissions","SUBMITTED_ON_TIME","Submitted on time","SUBMITTED_LATE","Submitted late","OVERDUE","Overdue","DUE_TODAY","Due today","AWAITING_SUBMISSION","Awaiting submission","lateReplacements","On-time submissions with late replacements");
        summary.forEach((k,v)->sums.add(List.of(labels.getOrDefault(k,k),v)));
        sums.add(List.of("Explanation source",text(insight.get("source"))));
        sums.add(List.of("Tracking explanation",text(insight.get("text"))));
        sums.add(List.of("Explanation notice",text(insight.get("notice"))));
        sums.add(List.of("Data used","Selected tracking data only; no document content was read. Each scope is a separate snapshot; do not add overlapping scopes together."));
        List<List<Object>> scopedSummary=new ArrayList<>();scopedSummary.add(List.of("Report scope","Field","Value"));
        for(var line:sums)scopedSummary.add(List.of(tab,line.get(0),line.get(1)));
        groupValues=mergeReportScope(token,base,"Groups",tab,groupValues,newTabs.contains("Groups"));
        scopedSummary=mergeReportScope(token,base,"Summary",tab,scopedSummary,newTabs.contains("Summary"));
        List<Object> formatting=new ArrayList<>();
        var names=List.of(tab,"Groups","Summary");var tables=List.of(values,groupValues,scopedSummary);
        for(int index=0;index<3;index++){
            int sheet=sheetIds.get(names.get(index)),columns=index==0?14:index==1?11:3,header=index==0?3:0;
            var table=tables.get(index);
            int currentRows=rowCounts.getOrDefault(sheet,1000);
            if(table.size()>currentRows)formatting.add(Map.of("appendDimension",Map.of("sheetId",sheet,"dimension","ROWS","length",table.size()-currentRows)));
            formatting.add(Map.of("updateSheetProperties",Map.of("properties",Map.of("sheetId",sheet,"gridProperties",Map.of("frozenRowCount",header+1)),"fields","gridProperties.frozenRowCount")));
            // One atomic Sheets batch replaces values and clears trailing old rows, without formulas.
            formatting.add(Map.of("updateCells",Map.of("range",Map.of("sheetId",sheet,"endColumnIndex",columns),"rows",table.stream().map(row->Map.of("values",row.stream().map(value->Map.of("userEnteredValue",value instanceof Number?Map.of("numberValue",value):Map.of("stringValue",text(value)))).toList())).toList(),"fields","userEnteredValue")));
            formatting.add(Map.of("repeatCell",Map.of("range",Map.of("sheetId",sheet,"startRowIndex",header,"endRowIndex",header+1,"endColumnIndex",columns),"cell",Map.of("userEnteredFormat",Map.of("backgroundColor",Map.of("red",0.5,"green",0.0,"blue",0.125),"textFormat",Map.of("bold",true,"foregroundColor",Map.of("red",1,"green",1,"blue",1)))),"fields","userEnteredFormat")));
            formatting.add(Map.of("repeatCell",Map.of("range",Map.of("sheetId",sheet,"endColumnIndex",columns),"cell",Map.of("userEnteredFormat",Map.of("wrapStrategy","WRAP","verticalAlignment","TOP")),"fields","userEnteredFormat.wrapStrategy,userEnteredFormat.verticalAlignment")));
            formatting.add(columnWidth(sheet,0,columns,index==2?260:180));
            if(index<2)formatting.add(Map.of("setBasicFilter",Map.of("filter",Map.of("range",Map.of("sheetId",sheet,"startRowIndex",header,"endRowIndex",table.size(),"endColumnIndex",columns)))));
        }
        formatting.add(columnWidth(sheetIds.get("Summary"),2,3,760));
        api(token,"POST","https://sheets.googleapis.com/v4/spreadsheets/"+id+":batchUpdate",Map.of("requests",formatting));
        c.setLastSuccessAt(Instant.now());connections.save(c);
        return Map.of("url","https://docs.google.com/spreadsheets/d/"+id+"/edit#gid="+sheetIds.get(tab),"tab",tab,"message","Updated "+tab+" in "+title+". Other report scopes retain their last exported snapshots.");
    }
    static String reportTab(Map<String,String> filters){
        String section=filters.getOrDefault("section","");
        String name=section.isBlank()?"All Students":section;
        var extra=new TreeMap<String,String>();
        for(String key:List.of("group","adviser","deliverable","status","search"))if(!filters.getOrDefault(key,"").isBlank())extra.put(key,filters.get(key));
        String clean=name.replaceAll("[\\[\\]:*?/\\\\\\p{Cntrl}]","_");
        boolean altered=!clean.equals(name)||clean.length()>60||Set.of("groups","summary","all students").contains(clean.toLowerCase(Locale.ROOT))&&!section.isBlank();
        clean=clean.substring(0,Math.min(clean.length(),60));
        return clean+(!extra.isEmpty()?" - filtered "+hash(section+extra).substring(0,10):altered?" - "+hash(section).substring(0,10):"");
    }
    @SuppressWarnings("unchecked")
    private List<List<Object>> mergeReportScope(String token,String base,String name,String scope,List<List<Object>> replacement,boolean fresh){
        List<List<Object>> result=new ArrayList<>();result.add(replacement.get(0));
        if(!fresh){
            var response=api(token,"GET",base+"/values/"+encode("'"+name+"'")+"?valueRenderOption=UNFORMATTED_VALUE",null);
            var existing=(List<List<Object>>)response.getOrDefault("values",List.of());
            existing.stream().skip(1).filter(row->!row.isEmpty()&&!Objects.equals(row.get(0),scope)).forEach(result::add);
        }
        result.addAll(replacement.subList(1,replacement.size()));return result;
    }
    private Map<String,Object> columnWidth(int sheet,int start,int end,int width){return Map.of("updateDimensionProperties",Map.of("range",Map.of("sheetId",sheet,"dimension","COLUMNS","startIndex",start,"endIndex",end),"properties",Map.of("pixelSize",width),"fields","pixelSize"));}
    public static String eventId(Long user,Long deliverable){return hash("intellitrack:"+user+":"+deliverable);}
    private String text(Object value){return value==null?"":value.toString();}
    private Map<String,Object> token(Map<String,String> body){return send(HttpRequest.newBuilder(URI.create("https://oauth2.googleapis.com/token")).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form(body))).timeout(Duration.ofSeconds(30)).build());}
    private Map<String,Object> api(String token,String method,String url,Object body){
        try{return send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).header("Authorization","Bearer "+token).header("Content-Type","application/json").method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build());}catch(java.io.IOException ex){throw new IllegalStateException("Could not prepare Google request");}
    }
    @SuppressWarnings("unchecked")
    private Map<String,Object> send(HttpRequest req){
        try{var res=http.send(req,HttpResponse.BodyHandlers.ofString());if(res.statusCode()<200||res.statusCode()>=300)throw new ResponseStatusException((res.statusCode()==401?HttpStatus.CONFLICT:HttpStatus.valueOf(res.statusCode())),"Google request failed ("+res.statusCode()+"). Check permissions, API enablement and quota; reconnect if access expired.");return res.body().isBlank()?Map.of():json.readValue(res.body(),Map.class);}catch(InterruptedException ex){Thread.currentThread().interrupt();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Google request interrupted");}catch(java.io.IOException ex){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Google is temporarily unreachable; try again");}
    }
    @SuppressWarnings("unchecked") private List<Map<String,Object>> maps(Object value){return value instanceof List<?>?(List<Map<String,Object>>)value:List.of();}
    private static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8);}
    private static String form(Map<String,String> fields){return fields.entrySet().stream().map(e->encode(e.getKey())+"="+encode(e.getValue())).collect(java.util.stream.Collectors.joining("&"));}
    private static String random(){byte[] b=new byte[32];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static byte[] digest(String value){try{return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));}catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}}
    private static String hash(String value){return HexFormat.of().formatHex(digest(value));}
    private static String challenge(String value){return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(value));}
}
