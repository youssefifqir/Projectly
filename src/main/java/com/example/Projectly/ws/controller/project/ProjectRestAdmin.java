package com.example.Projectly.ws.controller.project;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

import com.example.Projectly.bean.core.project.Project;
import com.example.Projectly.dao.criteria.core.project.ProjectCriteria;
import com.example.Projectly.service.facade.project.ProjectService;
import com.example.Projectly.ws.converter.project.ProjectConverter;
import com.example.Projectly.ws.dto.PageResponse;
import com.example.Projectly.ws.dto.project.request.CreateProjectRequest;
import com.example.Projectly.ws.dto.project.request.UpdateProjectRequest;
import com.example.Projectly.ws.dto.project.response.ProjectResponse;

@RestController
@RequestMapping("/api/v1/projects")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Project", description = "Project management API")
public class ProjectRestAdmin {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "id", "ref", "createdDate", "lastModifiedDate", "name", "description", "status"
    );

    private final ProjectService service;
    private final ProjectConverter converter;

    public ProjectRestAdmin(ProjectService service, ProjectConverter converter) {
        this.service = service;
        this.converter = converter;
    }

    @GetMapping
    @Operation(summary = "List Project records (paginated)")
    public ResponseEntity<PageResponse<ProjectResponse>> findAll(
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
        final var result = service.findPaginatedByCriteria(new ProjectCriteria(), pageable)
                .map(converter::toResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> findById(@PathVariable Long id) {
        Project entity = service.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
        return ResponseEntity.ok(converter.toResponse(entity));
    }

    @PostMapping
    @Operation(summary = "Create a new Project")
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        Project entity = converter.toEntity(request);
        Project created = service.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(converter.toResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing Project")
    public ResponseEntity<ProjectResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateProjectRequest request) {
        Project entity = converter.toEntity(request);
        entity.setId(id);
        Project updated = service.update(entity);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(converter.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Project")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        service.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project not found with id: " + id));
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

