package com.example.Projectly.ws.converter.task.member;

import org.springframework.stereotype.Component;
import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.ws.dto.task.member.request.CreateTaskRequest;
import com.example.Projectly.ws.dto.task.member.request.UpdateTaskRequest;
import com.example.Projectly.ws.dto.task.member.response.TaskMemberDto;

/**
 * Converter for Task entity - Member view.
 * Access: MEMBER
 */
@Component
public class TaskMemberConverter {

    public TaskMemberDto toDto(Task entity) {
        if (entity == null) return null;
        TaskMemberDto dto = new TaskMemberDto();
        dto.setId(entity.getId());
        dto.setRef(entity.getRef());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setPriority(entity.getPriority());
        dto.setCompleted(entity.getCompleted());
        dto.setDueDate(entity.getDueDate());
        return dto;
    }

    public Task toEntity(CreateTaskRequest request) {
        if (request == null) return null;
        Task entity = new Task();
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setPriority(request.getPriority());
        entity.setCompleted(request.getCompleted());
        entity.setDueDate(request.getDueDate());
        assertFieldsWritable(request, entity);
        return entity;
    }

    public Task toEntity(UpdateTaskRequest request) {
        if (request == null) return null;
        Task entity = new Task();
        entity.setTitle(request.getTitle());
        entity.setDescription(request.getDescription());
        entity.setPriority(request.getPriority());
        entity.setCompleted(request.getCompleted());
        entity.setDueDate(request.getDueDate());
        assertFieldsWritable(request, entity);
        return entity;
    }

    /**
     * RBAC_V3 §13 "Write" — {@code fieldPolicies.Task.*.write}. Any field the request
     * DTO sets (non-null) that the current principal's roles are not authorized to write (or whose
     * {@code when} condition fails) is rejected as a batch, listing every offending field.
     */
    private void assertFieldsWritable(Object request, Task entity) {
        com.example.Projectly.config.security.authz.PrincipalContext.Snapshot principal =
                com.example.Projectly.config.security.authz.PrincipalContext.currentOrAnonymous();
        java.util.List<String> forbidden = new java.util.ArrayList<>();
        try {
            Object requestValue = request.getClass().getMethod("getInternalNote").invoke(request);
            if (requestValue != null) {
                boolean roleOk = false || principal.roleNames().stream().anyMatch(
                        java.util.Set.of("ADMIN", "MANAGER")::contains);
                boolean whenOk = roleOk;
                if (!whenOk) forbidden.add("internalNote");
            }
        } catch (ReflectiveOperationException e) {
            // request DTO has no getter for this field — nothing to guard.
        }
        if (!forbidden.isEmpty()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Cannot write field(s): " + String.join(", ", forbidden));
        }
    }
}
