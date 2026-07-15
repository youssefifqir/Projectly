package com.example.Projectly.ws.controller.comment;

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

import com.example.Projectly.bean.core.comment.Comment;
import com.example.Projectly.dao.criteria.core.comment.CommentCriteria;
import com.example.Projectly.service.facade.comment.CommentService;
import com.example.Projectly.ws.converter.comment.CommentConverter;
import com.example.Projectly.ws.dto.PageResponse;
import com.example.Projectly.ws.dto.comment.request.CreateCommentRequest;
import com.example.Projectly.ws.dto.comment.request.UpdateCommentRequest;
import com.example.Projectly.ws.dto.comment.response.CommentResponse;

@RestController
@RequestMapping("/api/v1/comments")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Comment", description = "Comment management API")
public class CommentRestAdmin {

    private static final Set<String> ALLOWED_SORT_COLUMNS = Set.of(
        "id", "ref", "createdDate", "lastModifiedDate", "body"
    );

    private final CommentService service;
    private final CommentConverter converter;

    public CommentRestAdmin(CommentService service, CommentConverter converter) {
        this.service = service;
        this.converter = converter;
    }

    @GetMapping
    @Operation(summary = "List Comment records (paginated)")
    public ResponseEntity<PageResponse<CommentResponse>> findAll(
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
        final var result = service.findPaginatedByCriteria(new CommentCriteria(), pageable)
                .map(converter::toResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommentResponse> findById(@PathVariable Long id) {
        Comment entity = service.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + id));
        return ResponseEntity.ok(converter.toResponse(entity));
    }

    @PostMapping
    @Operation(summary = "Create a new Comment")
    public ResponseEntity<CommentResponse> create(@Valid @RequestBody CreateCommentRequest request) {
        Comment entity = converter.toEntity(request);
        Comment created = service.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(converter.toResponse(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing Comment")
    public ResponseEntity<CommentResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCommentRequest request) {
        Comment entity = converter.toEntity(request);
        entity.setId(id);
        Comment updated = service.update(entity);
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(converter.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Comment")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        service.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + id));
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

