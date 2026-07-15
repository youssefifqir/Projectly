package com.example.Projectly.dao.specification.core.comment;

import com.example.Projectly.dao.criteria.core.comment.CommentCriteria;
import com.example.Projectly.bean.core.comment.Comment;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CommentSpecification implements Specification<Comment> {

    private final CommentCriteria criteria;
    private final boolean distinct;

    public CommentSpecification(CommentCriteria criteria) {
        this(criteria, false);
    }

    public CommentSpecification(CommentCriteria criteria, boolean distinct) {
        this.criteria = criteria;
        this.distinct = distinct;
    }

    @Override
    public Predicate toPredicate(Root<Comment> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
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
        // body - String field (supports like search)
        if (criteria.getBody() != null && !criteria.getBody().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("body")),
                "%" + criteria.getBody().toLowerCase() + "%"));
        }
        if (criteria.getBodyLike() != null && !criteria.getBodyLike().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("body")),
                "%" + criteria.getBodyLike().toLowerCase() + "%"));
        }

        // ── Relationship fields (foreign key lookups) ───────────────
        // task - ManyToOne relationship
        if (criteria.getTaskId() != null) {
            predicates.add(cb.equal(root.get("task").get("id"), criteria.getTaskId()));
        }
        if (criteria.getTaskRef() != null && !criteria.getTaskRef().trim().isEmpty()) {
            predicates.add(cb.like(cb.lower(root.get("task").get("ref")),
                "%" + criteria.getTaskRef().toLowerCase() + "%"));
        }
        // author - ManyToOne relationship
        if (criteria.getAuthorId() != null) {
            predicates.add(cb.equal(root.get("author").get("id"), criteria.getAuthorId()));
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }
}
