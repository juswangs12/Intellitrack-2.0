package com.intellitrack.service;
import com.intellitrack.dto.TrackingRow;
import com.intellitrack.entity.User;
import com.intellitrack.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.*;
@Service
public class WorkspaceHistoryService {
 private final AuditService audit; private final AuditLogRepository logs; private final ObjectMapper json;
 public WorkspaceHistoryService(AuditService a,AuditLogRepository l,ObjectMapper j){audit=a;logs=l;json=j;}
 static String key(TrackingRow r){return r.enrollmentId()+":"+r.deliverableId()+":"+r.groupId();}
 public void save(User user,String kind,Map<String,String> filters,List<TrackingRow> rows,Map<String,?> result){
  try {audit.log(kind,"workspace:"+user.getId(),"WORKSPACE",json.writeValueAsString(Map.of("role",user.getRole(),"scope",describe(filters,rows),"keys",rows.stream().map(WorkspaceHistoryService::key).distinct().toList(),"result",result)),null);}
  catch(com.fasterxml.jackson.core.JsonProcessingException e){throw new IllegalStateException("Could not save workspace history",e);}
 }
 private Map<String,String> describe(Map<String,String> filters,List<TrackingRow> rows){
  Map<String,String> scope=new LinkedHashMap<>(filters);
  if(!rows.isEmpty()) {var first=rows.get(0);
   if(scope.containsKey("subject")&&!scope.get("subject").isBlank())scope.put("subject",first.subject());
   if(scope.containsKey("group")&&!scope.get("group").isBlank())scope.put("group",Objects.toString(first.group(),"Unassigned"));
   if(scope.containsKey("adviser")&&!scope.get("adviser").isBlank())scope.put("adviser",Objects.toString(first.adviser(),"Unassigned"));
   if(scope.containsKey("deliverable")&&!scope.get("deliverable").isBlank())scope.put("deliverable",first.deliverable());
  }
  return scope;
 }
 public List<Map<String,Object>> list(User user,String kind,List<TrackingRow> allowed){
  var keys=new HashSet<>(allowed.stream().map(WorkspaceHistoryService::key).toList());
  List<Map<String,Object>> result=new ArrayList<>();
  for(var log:logs.findTop50ByActionAndPerformedByOrderByTimestampDesc(kind,"workspace:"+user.getId()))try{
   Map<?,?> record=json.readValue(log.getDetails(),Map.class);
   if(!Objects.equals(record.get("role"),user.getRole())||!(record.get("keys") instanceof List<?> saved)||!keys.containsAll(saved))continue;
   result.add(Map.of("id",log.getId(),"createdAt",log.getTimestamp(),"scope",record.get("scope"),"result",record.get("result")));
  }catch(com.fasterxml.jackson.core.JsonProcessingException ignored){}
  return result;
 }
 public List<Map<String,Object>> imports(User user){return logs.findTop50ByActionAndPerformedByOrderByTimestampDesc("CLASSLIST_IMPORT",user.getEmail()).stream().map(l->Map.<String,Object>of("id",l.getId(),"createdAt",l.getTimestamp(),"details",l.getDetails())).toList();}
}
