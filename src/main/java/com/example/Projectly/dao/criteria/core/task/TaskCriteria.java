package com.example.Projectly.dao.criteria.core.task;

import java.time.LocalDateTime;
import java.time.LocalDate;
import com.example.Projectly.bean.core.enums.TaskPriority;

public class TaskCriteria {

    private Long id;
    private String ref;
    private LocalDateTime createdAt;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
    private LocalDateTime updatedAt;
    private LocalDateTime updatedAtFrom;
    private LocalDateTime updatedAtTo;

    private String title;
    private String titleLike;
    private String description;
    private String descriptionLike;
    private TaskPriority priority;
    private Boolean completed;
    private LocalDate dueDate;
    private String internalNote;
    private String internalNoteLike;

    private Long projectId;
    private String projectRef;
    private String assigneeId;

    // Constructors
    public TaskCriteria() {
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCreatedAtFrom() { return createdAtFrom; }
    public void setCreatedAtFrom(LocalDateTime createdAtFrom) { this.createdAtFrom = createdAtFrom; }

    public LocalDateTime getCreatedAtTo() { return createdAtTo; }
    public void setCreatedAtTo(LocalDateTime createdAtTo) { this.createdAtTo = createdAtTo; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getUpdatedAtFrom() { return updatedAtFrom; }
    public void setUpdatedAtFrom(LocalDateTime updatedAtFrom) { this.updatedAtFrom = updatedAtFrom; }

    public LocalDateTime getUpdatedAtTo() { return updatedAtTo; }
    public void setUpdatedAtTo(LocalDateTime updatedAtTo) { this.updatedAtTo = updatedAtTo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTitleLike() { return titleLike; }
    public void setTitleLike(String titleLike) { this.titleLike = titleLike; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDescriptionLike() { return descriptionLike; }
    public void setDescriptionLike(String descriptionLike) { this.descriptionLike = descriptionLike; }

    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public String getInternalNote() { return internalNote; }
    public void setInternalNote(String internalNote) { this.internalNote = internalNote; }

    public String getInternalNoteLike() { return internalNoteLike; }
    public void setInternalNoteLike(String internalNoteLike) { this.internalNoteLike = internalNoteLike; }


    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getProjectRef() { return projectRef; }
    public void setProjectRef(String projectRef) { this.projectRef = projectRef; }

    public String getAssigneeId() { return assigneeId; }
    public void setAssigneeId(String assigneeId) { this.assigneeId = assigneeId; }


    // Utility method to check if criteria is empty
    public boolean isEmpty() {
        if (id != null) return false;
        if (ref != null && !ref.trim().isEmpty()) return false;
        if (createdAt != null) return false;
        if (createdAtFrom != null) return false;
        if (createdAtTo != null) return false;
        if (updatedAt != null) return false;
        if (updatedAtFrom != null) return false;
        if (updatedAtTo != null) return false;

        if (title != null && !title.trim().isEmpty()) return false;
        if (titleLike != null && !titleLike.trim().isEmpty()) return false;
        if (description != null && !description.trim().isEmpty()) return false;
        if (descriptionLike != null && !descriptionLike.trim().isEmpty()) return false;
        if (priority != null) return false;
        if (completed != null) return false;
        if (dueDate != null) return false;
        if (internalNote != null && !internalNote.trim().isEmpty()) return false;
        if (internalNoteLike != null && !internalNoteLike.trim().isEmpty()) return false;

        if (projectId != null) return false;
        if (projectRef != null && !projectRef.trim().isEmpty()) return false;
        if (assigneeId != null) return false;

        return true;
    }
}