package com.intellitrack.service;

import com.intellitrack.entity.*;
import com.intellitrack.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service("access")
public class AccessService {
    private final UserRepository users;
    public AccessService(UserRepository users) { this.users = users; }
    public User current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) throw new AccessDeniedException("Sign in required");
        return users.findById(id).orElseThrow(() -> new AccessDeniedException("Account no longer exists"));
    }
    public boolean staff(User u) { return "coordinator".equals(u.getRole()) || "administrator".equals(u.getRole()); }
    public boolean own(Long id) { var u=current(); return u.getId().equals(id) || "administrator".equals(u.getRole()); }
    public void group(User u, ProjectGroup g) {
        if (staff(u)) return;
        if ("adviser".equals(u.getRole()) && g.getAdviser()!=null && g.getAdviser().getId().equals(u.getId())) return;
        if ("student".equals(u.getRole()) && g.getStudents().stream().anyMatch(e -> e.getStudent()!=null && e.getStudent().getId().equals(u.getId()))) return;
        throw new AccessDeniedException("This group is not assigned to you");
    }
    public void submission(Submission s) {
        User u=current();
        if ("student".equals(u.getRole()) && (s.getStudent()==null || !u.getId().equals(s.getStudent().getId())))
            throw new AccessDeniedException("This submission belongs to another student");
        // Former members retain read access to their own submission history.
        if ("student".equals(u.getRole())) return;
        group(u,s.getGroup());
    }
}
