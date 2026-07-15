package com.example.Projectly.dao.specification.core.task;

import com.example.Projectly.dao.criteria.core.task.TaskCriteria;
import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.bean.core.enums.TaskPriority;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TaskSpecification implements Specification<Task> {

    private final TaskCriteria criteria;
    private final boolean distinct;

    public TaskSpecification(TaskCriteria criteria) {
        this(criteria, false);
    }

    public TaskSpecification(TaskCriteria criteria, boolean distinct) {
        this.criteria = criteria;
        this.distinct = distinct;
    }

    @Override
    public Predicate toPredicate(Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();

        if (distinct) {
            query.distinct(true);
        }

        // ── Base entity fields ──────────────────────────────────────
        if (criteria.getId() != null) {
            predicates.add(cb.equal(root.get("id"), criteria.getId()));
        }

        if (criteria.getRef() != null && !criteria.getRef().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("ref")),
                "%" + criteria.getRef().toLowerCase() + "%"));
        }

        if (criteria.getCreatedAt() != null) {
            predicates.add(cb.equal(root.get("createdDate"), criteria.getCreatedAt()));
        }
        if (criteria.getCreatedAtFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), criteria.getCreatedAtFrom()));
        }
        if (criteria.getCreatedAtTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), criteria.getCreatedAtTo()));
        }

        if (criteria.getUpdatedAt() != null) {
            predicates.add(cb.equal(root.get("lastModifiedDate"), criteria.getUpdatedAt()));
        }
        if (criteria.getUpdatedAtFrom() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("lastModifiedDate"), criteria.getUpdatedAtFrom()));
        }
        if (criteria.getUpdatedAtTo() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("lastModifiedDate"), criteria.getUpdatedAtTo()));
        }

        // ── Entity-specific fields ──────────────────────────────────
        // title - String field (supports like search)
        if (criteria.getTitle() != null && !criteria.getTitle().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("title")),
                "%" + criteria.getTitle().toLowerCase() + "%"));
        }
        if (criteria.getTitleLike() != null && !criteria.getTitleLike().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("title")),
                "%" + criteria.getTitleLike().toLowerCase() + "%"));
        }
        // description - String field (supports like search)
        if (criteria.getDescription() != null && !criteria.getDescription().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("description")),
                "%" + criteria.getDescription().toLowerCase() + "%"));
        }
        if (criteria.getDescriptionLike() != null && !criteria.getDescriptionLike().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("description")),
                "%" + criteria.getDescriptionLike().toLowerCase() + "%"));
        }
        // priority - Enum field
        if (criteria.getPriority() != null) {
            predicates.add(cb.equal(root.get("priority"), criteria.getPriority()));
        }
        // completed - Boolean field
        if (criteria.getCompleted() != null) {
            predicates.add(cb.equal(root.get("completed"), criteria.getCompleted()));
        }
        // dueDate - LocalDate field
        if (criteria.getDueDate() != null) {
            predicates.add(cb.equal(root.get("dueDate"), criteria.getDueDate()));
        }
        // internalNote - String field (supports like search)
        if (criteria.getInternalNote() != null && !criteria.getInternalNote().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("internalNote")),
                "%" + criteria.getInternalNote().toLowerCase() + "%"));
        }
        if (criteria.getInternalNoteLike() != null && !criteria.getInternalNoteLike().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("internalNote")),
                "%" + criteria.getInternalNoteLike().toLowerCase() + "%"));
        }

        // ── Relationship fields (foreign key lookups) ───────────────
        // project - ManyToOne relationship
        if (criteria.getProjectId() != null) {
            predicates.add(cb.equal(root.get("project").get("id"), criteria.getProjectId()));
        }
        if (criteria.getProjectRef() != null && !criteria.getProjectRef().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("project").get("ref")),
                "%" + criteria.getProjectRef().toLowerCase() + "%"));
        }
        // assignee - ManyToOne relationship
        if (criteria.getAssigneeId() != null) {
            predicates.add(cb.equal(root.get("assignee").get("id"), criteria.getAssigneeId()));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
