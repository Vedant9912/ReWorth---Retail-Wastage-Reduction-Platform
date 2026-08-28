package com.nearsave.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(String actor, String action, String entityName, Long entityId, String metadata) {
        AuditEvent event = AuditEvent.builder()
                .actor(actor)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .metadata(metadata)
                .build();
        auditEventRepository.save(event);
    }
}
