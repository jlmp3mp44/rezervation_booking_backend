package com.horovod.hub.service;

import com.horovod.hub.domain.Issue;
import com.horovod.hub.dto.RealtimeEvent;
import com.horovod.hub.repository.IssueRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final EventBroadcastService eventBroadcastService;

    public IssueService(IssueRepository issueRepository, EventBroadcastService eventBroadcastService) {
        this.issueRepository = issueRepository;
        this.eventBroadcastService = eventBroadcastService;
    }

    public List<Issue> findAll() {
        return issueRepository.findAll();
    }

    @Transactional
    public Issue create(Issue issue) {
        Issue saved = issueRepository.save(issue);
        eventBroadcastService.broadcast(new RealtimeEvent("issues", "INSERT", null, saved));
        return saved;
    }

    @Transactional
    public Optional<Issue> update(String id, Issue patch) {
        return issueRepository.findById(id).map(existing -> {
            if (patch.getCategory() != null) {
                existing.setCategory(patch.getCategory());
            }
            if (patch.getTitle() != null) {
                existing.setTitle(patch.getTitle());
            }
            if (patch.getDescription() != null) {
                existing.setDescription(patch.getDescription());
            }
            existing.setResolved(patch.isResolved());
            if (patch.getResolvedAt() != null) {
                existing.setResolvedAt(patch.getResolvedAt());
            }
            if (patch.getResolution() != null) {
                existing.setResolution(patch.getResolution());
            }
            Issue saved = issueRepository.save(existing);
            eventBroadcastService.broadcast(new RealtimeEvent("issues", "UPDATE", null, saved));
            return saved;
        });
    }

    @Transactional
    public boolean delete(String id) {
        if (!issueRepository.existsById(id)) {
            return false;
        }
        issueRepository.deleteById(id);
        eventBroadcastService.broadcast(new RealtimeEvent("issues", "DELETE", id, null));
        return true;
    }
}
