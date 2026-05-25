package com.intellitrack.service;

import com.intellitrack.repository.DeadlineRepository;
import com.intellitrack.repository.DeliverableRepository;
import com.intellitrack.repository.SubmissionCommentRepository;
import com.intellitrack.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeliverableService {

    @Autowired
    private DeliverableRepository deliverableRepository;

    @Autowired
    private DeadlineRepository deadlineRepository;

    @Autowired
    private SubmissionCommentRepository submissionCommentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    public void deleteDeliverable(Long id) {
        deadlineRepository.deleteByDeliverableId(id);
        submissionCommentRepository.deleteByDeliverableId(id);
        submissionRepository.deleteByDeliverableId(id);
        deliverableRepository.deleteById(id);
    }
}
