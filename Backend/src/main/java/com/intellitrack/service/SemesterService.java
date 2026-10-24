package com.intellitrack.service;
import com.intellitrack.entity.SemesterArchive;
import com.intellitrack.repository.SemesterArchiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
@Service
@Transactional
public class SemesterService {
    private final SemesterArchiveRepository repository;
    private final AuditService audit;
    private final AccessService access;
    public SemesterService(SemesterArchiveRepository r,AuditService a,AccessService u){repository=r;audit=a;access=u;}
    public Set<String> archivedTerms(){return new HashSet<>(repository.findAll().stream().map(SemesterArchive::getTerm).toList());}
    public boolean archived(String term){return term!=null&&repository.existsById(term);}
    public void requireActive(String term){if(archived(term))throw new IllegalArgumentException("This semester is archived. Reopen it in Semester history before making changes.");}
    public void setArchived(String input,boolean archived){
        String term=TrackingPolicy.term(input);
        if(term==null)throw new IllegalArgumentException("Select a semester");
        if(archived==archived(term))return;
        if(archived)repository.save(new SemesterArchive(term,LocalDateTime.now(ZoneId.of("Asia/Manila"))));else repository.deleteById(term);
        audit.log(archived?"SEMESTER_FINISHED":"SEMESTER_REOPENED",access.current().getId().toString(),"Semester",term,"WEB");
    }
}
