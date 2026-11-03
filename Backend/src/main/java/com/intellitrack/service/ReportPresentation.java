package com.intellitrack.service;

import com.intellitrack.dto.TrackingRow;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

final class ReportPresentation {
    private ReportPresentation() {}
    static String date(LocalDateTime value){return value==null?"Not submitted":value.format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a",Locale.ENGLISH));}
    static String status(String value){return switch(Objects.toString(value,"")){
        case "SUBMITTED_ON_TIME" -> "On time";case "SUBMITTED_LATE" -> "Late";
        case "OVERDUE" -> "Overdue";case "DUE_TODAY" -> "Due today";
        case "AWAITING_SUBMISSION" -> "Awaiting";default -> "Not assigned";};}
    static List<TrackingRow> requirements(List<TrackingRow> rows){
        var result=new LinkedHashMap<Long,TrackingRow>();
        rows.stream().sorted(Comparator.comparing(TrackingRow::dueAt,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(TrackingRow::deliverable)).forEach(r->result.putIfAbsent(r.deliverableId(),r));
        return new ArrayList<>(result.values());
    }
    static List<List<TrackingRow>> students(List<TrackingRow> rows){
        var result=new LinkedHashMap<Long,List<TrackingRow>>();
        rows.stream().sorted(Comparator.comparing(TrackingRow::group,Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).thenComparing(TrackingRow::studentName)).forEach(r->result.computeIfAbsent(r.enrollmentId(),k->new ArrayList<>()).add(r));
        return new ArrayList<>(result.values());
    }
    static List<List<Object>> matrix(List<TrackingRow> rows,String scope,String stamp,String filters){
        var requirements=requirements(rows);List<List<Object>> table=new ArrayList<>();
        table.add(List.of("Report scope",scope));table.add(List.of("Updated (Philippine time)",stamp));
        table.add(List.of("Read this report",filters+". One row per student. Status uses the first submission. Submitted cells link to documents; notes contain timestamps. A dash means not assigned or excluded by filters. See Summary for definitions."));
        List<Object> header=new ArrayList<>(List.of("Student","Student ID","Section","Group Code","Adviser"));
        requirements.forEach(r->header.add(r.deliverable()+"\n"+(r.dueAt()==null?"No deadline":"Due "+date(r.dueAt()))));table.add(header);
        for(var student:students(rows)){
            var first=student.get(0);List<Object> line=new ArrayList<>(List.of(first.studentName(),Objects.toString(first.studentNumber(),""),Objects.toString(first.section(),""),Objects.toString(first.group(),"Unassigned"),Objects.toString(first.adviser(),"Unassigned")));
            for(var requirement:requirements){var record=student.stream().filter(r->Objects.equals(r.deliverableId(),requirement.deliverableId())).findFirst();line.add(record.map(r->status(r.status())+("SUBMITTED_ON_TIME".equals(r.status())&&"SUBMITTED_LATE".equals(r.latestStatus())?" *":"")).orElse("-"));}
            table.add(line);
        }return table;
    }
    static String scope(List<TrackingRow> rows,Map<String,String> filters){
        var first=rows.get(0);var parts=new ArrayList<String>();parts.add(first.subject());parts.add(Objects.toString(first.academicTerm(),"Unassigned term"));
        for(String key:List.of("section","status","search"))if(!filters.getOrDefault(key,"").isBlank())parts.add(key+": "+("status".equals(key)?status(filters.get(key)):filters.get(key)));
        if(!filters.getOrDefault("group","").isBlank())parts.add("Group: "+first.group());
        if(!filters.getOrDefault("adviser","").isBlank())parts.add("Adviser: "+first.adviser());
        if(!filters.getOrDefault("deliverable","").isBlank())parts.add("Requirement: "+first.deliverable());
        return String.join(" | ",parts);
    }
    static List<List<Object>> groups(List<TrackingRow> rows,String scope,String stamp){
        List<List<Object>> table=new ArrayList<>();table.add(List.of("Report scope","Updated (Philippine time)","Group Code","Adviser","Members (selected scope)","Students","Submitted / expected","On time","Late","Overdue missing","Awaiting / due today"));
        var groups=new TreeMap<String,List<TrackingRow>>();rows.forEach(r->groups.computeIfAbsent(r.groupId()==null?"Unassigned / "+r.section():r.group()+" / "+r.groupId(),k->new ArrayList<>()).add(r));
        for(var group:groups.values()){
            var first=group.get(0);var members=students(group);long on=count(group,"SUBMITTED_ON_TIME"),late=count(group,"SUBMITTED_LATE");
            table.add(List.of(scope,stamp,Objects.toString(first.group(),"Unassigned"),Objects.toString(first.adviser(),"Unassigned"),members.stream().map(m->m.get(0).studentName()+" ("+m.get(0).section()+")").collect(Collectors.joining("\n")),members.size(),(on+late)+" / "+group.size(),on,late,count(group,"OVERDUE"),count(group,"AWAITING_SUBMISSION")+count(group,"DUE_TODAY")));
        }return table;
    }
    static long count(List<TrackingRow> rows,String status){return rows.stream().filter(r->status.equals(r.status())).count();}
}
