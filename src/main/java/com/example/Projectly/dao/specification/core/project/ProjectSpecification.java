package com.example.Projectly.dao.specification.core.project;

import com.example.Projectly.dao.criteria.core.project.ProjectCriteria;
import com.example.Projectly.bean.core.project.Project;
import com.example.Projectly.bean.core.enums.ProjectStatus;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class ProjectSpecification implements Specification<Project> {

    private final ProjectCriteria criteria;
    private final boolean distinct;

    public ProjectSpecification(ProjectCriteria criteria) {
        this(criteria, false);
    }

    public ProjectSpecification(ProjectCriteria criteria, boolean distinct) {
        this.criteria = criteria;
        this.distinct = distinct;
    }

    @Override
    public Predicate toPredicate(Root<Project> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
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
        // name - String field (supports like search)
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")),
                "%" + criteria.getName().toLowerCase() + "%"));
        }
        if (criteria.getNameLike() != null && !criteria.getNameLike().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("name")),
                "%" + criteria.getNameLike().toLowerCase() + "%"));
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
        // status - Enum field
        if (criteria.getStatus() != null) {
            predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
        }

        // ── Relationship fields (foreign key lookups) ───────────────
        // owner - ManyToOne relationship
        if (criteria.getOwnerId() != null) {
            predicates.add(cb.equal(root.get("owner").get("id"), criteria.getOwnerId()));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
