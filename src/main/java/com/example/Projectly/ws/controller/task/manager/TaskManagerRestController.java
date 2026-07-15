package com.example.Projectly.ws.controller.task.manager;

import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.dao.criteria.core.task.TaskCriteria;
import com.example.Projectly.service.facade.task.TaskService;
import com.example.Projectly.ws.converter.task.manager.TaskManagerConverter;
import com.example.Projectly.ws.dto.PageResponse;
import com.example.Projectly.ws.dto.task.manager.request.CreateTaskRequest;
import com.example.Projectly.ws.dto.task.manager.request.UpdateTaskRequest;
import com.example.Projectly.ws.dto.task.manager.response.TaskManagerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.persistence.EntityNotFoundException;

import java.util.Set;

/**
 * REST Controller for Task - Manager endpoints.
 * Access: ADMIN, MANAGER
 */
@RestController
@RequestMapping("/api/v1/manager/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER')")
public class TaskManagerRestController {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "id", "ref", "createdDate", "lastModifiedDate", "title", "description", "priority", "completed", "dueDate", "internalNote"
    );

    private final TaskService taskService;
    private final TaskManagerConverter converter;

    @PostMapping
    public ResponseEntity<TaskManagerDto> create(@Valid @RequestBody CreateTaskRequest request) {
        Task entity = converter.toEntity(request);
        Task saved = taskService.save(entity);
        return new ResponseEntity<>(converter.toDto(saved), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskManagerDto> findById(@PathVariable Long id) {
        Task entity = taskService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task not found with id: " + id));
        return ResponseEntity.ok(converter.toDto(entity));
    }

    @GetMapping
    public ResponseEntity<PageResponse<TaskManagerDto>> findAll(
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") @Max(200) final int size,
            @RequestParam(defaultValue = "createdDate") final String sortBy,
            @RequestParam(defaultValue = "desc") final String sortDir) {
        if (!ALLOWED_SORT_COLUMNS.contains(sortBy)) {
            return ResponseEntity.badRequest().body(null);
        }
        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            return ResponseEntity.badRequest().body(null);
        }
        final int effectiveSize = Math.min(size, 200);
        final var pageable = PageRequest.of(page, effectiveSize,
                "asc".equalsIgnoreCase(sortDir) ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        final var result = taskService.findPaginatedByCriteria(new TaskCriteria(), pageable)
                .map(converter::toDto);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskManagerDto> update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        Task entity = converter.toEntity(request);
        entity.setId(id);
        Task updated = taskService.update(entity);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(converter.toDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
