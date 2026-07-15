package com.example.Projectly.common.event;

import java.time.LocalDateTime;

public record EntityEvent<T>(
    String eventType,
    String entityName,
    T entity,
    LocalDateTime timestamp
) {
    public static <T> EntityEvent<T> created(String entityName, T entity) {
        return new EntityEvent<>("CREATED", entityName, entity, LocalDateTime.now());
    }

    public static <T> EntityEvent<T> updated(String entityName, T entity) {
        return new EntityEvent<>("UPDATED", entityName, entity, LocalDateTime.now());
    }

    public static <T> EntityEvent<T> deleted(String entityName, T entity) {
        return new EntityEvent<>("DELETED", entityName, entity, LocalDateTime.now());
    }
}
