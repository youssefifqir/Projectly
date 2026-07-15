package com.example.Projectly.common.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EntityAuditListener {

    @Async
    @EventListener
    public void handleEntityEvent(EntityEvent<?> event) {
        log.info("Domain event: {} {} (id={})",
                event.eventType(),
                event.entityName(),
                event.entity() != null ? extractId(event.entity()) : "null");
    }

    private Object extractId(Object entity) {
        try {
            return entity.getClass().getMethod("getId").invoke(entity);
        } catch (Exception e) {
            return "unknown";
        }
    }
}
