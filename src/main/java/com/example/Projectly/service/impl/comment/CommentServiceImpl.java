package com.example.Projectly.service.impl.comment;

import com.example.Projectly.bean.core.comment.Comment;
import com.example.Projectly.dao.criteria.core.comment.CommentCriteria;
import com.example.Projectly.dao.facade.core.comment.CommentDao;
import com.example.Projectly.dao.specification.core.comment.CommentSpecification;
import com.example.Projectly.service.facade.comment.CommentService;
import com.example.Projectly.service.facade.task.TaskService;
import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.dao.facade.security.UserDao;
import com.example.Projectly.config.security.authz.Authorize.Permit;
import com.example.Projectly.config.security.authz.AuthorizationSpecificationAdvisor;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import com.example.Projectly.common.event.EntityEvent;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.time.LocalDateTime;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentDao dao;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthorizationSpecificationAdvisor rowFilter;
    private final UserDao userDao;
    private final TaskService taskService;

    public CommentServiceImpl(CommentDao dao, ApplicationEventPublisher eventPublisher, AuthorizationSpecificationAdvisor rowFilter, UserDao userDao, @Lazy TaskService taskService) {
        this.dao = dao;
        this.eventPublisher = eventPublisher;
        this.rowFilter = rowFilter;
        this.userDao = userDao;
        this.taskService = taskService;
    }

    @Override
    @Transactional(readOnly = true)
    @Permit("Comment:READ")
    public List<Comment> findAll() {
        Specification<Comment> spec = rowFilter.forList(Comment.class);
        return spec != null ? dao.findAll(spec) : dao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Permit("Comment:READ")
    public Optional<Comment> findById(Long id) {
        Specification<Comment> spec = rowFilter.forList(Comment.class);
        return spec != null
                ? dao.findOne(spec.and((root, query, cb) -> cb.equal(root.get("id"), id)))
                : dao.findById(id);
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_comment", allEntries = true)
    public Comment save(Comment entity) {
        if (entity == null) return null;

        // Validate and handle relationships before saving
        validateAndPrepareRelationships(entity);

        return dao.save(entity);
    }

    private void validateAndPrepareRelationships(Comment entity) {
        if (entity == null) return;

        // Validate ManyToOne relationships exist
        if (entity.getTask() != null) {
            Task taskEntity = entity.getTask();
            if (taskEntity.getId() != null) {
                Task existingTask = taskService.findById(taskEntity.getId()).orElse(null);
                if (existingTask == null) {
                    throw new IllegalArgumentException("Task with id " + taskEntity.getId() + " does not exist");
                }
                entity.setTask(existingTask);
            } else if (taskEntity.getRef() != null) {
                Task existingTask = taskService.findByRef(taskEntity.getRef());
                if (existingTask == null) {
                    throw new IllegalArgumentException("Task with ref '" + taskEntity.getRef() + "' does not exist");
                }
                entity.setTask(existingTask);
            } else {
                throw new IllegalArgumentException("Task must be referenced by id or ref");
            }
        }
        // Validate ManyToOne to User (security entity with String UUID id)
        if (entity.getAuthor() != null) {
            User authorEntity = entity.getAuthor();
            if (authorEntity.getId() != null) {
                User existingAuthor = this.userDao.findById(authorEntity.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Author with id " + authorEntity.getId() + " does not exist"));
                entity.setAuthor(existingAuthor);
            } else {
                throw new IllegalArgumentException("Author must be referenced by id");
            }
        }
    }

    private void validateDeletionAllowed(Comment entity) {
        if (entity == null) return;

    }

    private void prepareForDeletion(Comment entity) {
        if (entity == null) return;

    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_comment", allEntries = true)
    @Permit("Comment:DELETE")
    public void deleteById(Long id) {
        if (id == null) return;

        findById(id).ifPresent(entity -> {
            validateDeletionAllowed(entity);
            prepareForDeletion(entity);
            entity.setDeletedAt(LocalDateTime.now());
            dao.save(entity);
            eventPublisher.publishEvent(EntityEvent.deleted("Comment", entity));
        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_comment", allEntries = true)
    @Permit("Comment:DELETE")
    public Optional<Comment> findAndDeleteById(Long id) {
        if (id == null) return Optional.empty();
        return findById(id).map(entity -> {
            validateDeletionAllowed(entity);
            prepareForDeletion(entity);
            entity.setDeletedAt(LocalDateTime.now());
            dao.save(entity);
            eventPublisher.publishEvent(EntityEvent.deleted("Comment", entity));
            return entity;
        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_comment", allEntries = true)
    @Permit("Comment:CREATE")
    public Comment create(Comment t) {
        Comment result = save(t);
        if (result != null) {
            eventPublisher.publishEvent(EntityEvent.created("Comment", result));
        }
        return result;
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_comment", allEntries = true)
    @Permit("Comment:UPDATE")
    public Comment update(Comment t) {
        if (t == null || t.getId() == null) return null;
        Comment existing = findById(t.getId()).orElse(null);
        if (existing == null) return null;

        validateAndPrepareRelationships(t);
        mergeEntityData(existing, t);

        Comment result = save(existing);
        if (result != null) {
            eventPublisher.publishEvent(EntityEvent.updated("Comment", result));
        }
        return result;
    }

    private void mergeEntityData(Comment existing, Comment updated) {
        if (existing == null || updated == null) return;

        if (updated.getBody() != null) {
            existing.setBody(updated.getBody().isEmpty() ? null : updated.getBody());
        }

        // Handle relationships
        if (updated.getTask() != null) {
            existing.setTask(updated.getTask());
        }
        if (updated.getAuthor() != null) {
            existing.setAuthor(updated.getAuthor());
        }
    }

    @Override
    @Transactional(timeout = 30)
    public List<Comment> update(List<Comment> ts, boolean createIfNotExist) { 
        if (ts == null || ts.isEmpty()) return new ArrayList<>();
        
        List<Comment> result = new ArrayList<>();
        for (Comment entity : ts) {
            if (entity.getId() != null) {
                Comment updated = update(entity);
                if (updated != null) {
                    result.add(updated);
                }
            } else if (createIfNotExist) {
                result.add(create(entity));
            }
        }
        return result;
    }

    @Override
    @Transactional(timeout = 30)
    public Comment findOrSave(Comment t) { 
        if (t == null) return null;
        
        Comment existing = null;
        if (t.getId() != null) {
            existing = findById(t.getId()).orElse(null);
        } else if (t.getRef() != null) {
            existing = findByRef(t.getRef());
        }
        
        return existing != null ? existing : save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public Comment findByReferenceEntity(Comment t) { 
        if (t == null) return null;
        
        if (t.getId() != null) {
            return findById(t.getId()).orElse(null);
        } else if (t.getRef() != null) {
            return findByRef(t.getRef());
        }
        
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Comment findWithAssociatedLists(Long id) {
        return dao.findWithAssociationsById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Comment> findByCriteria(CommentCriteria criteria) {
        Specification<Comment> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new CommentSpecification(criteria),
                rowFilter.forList(Comment.class)
        );
        return spec != null ? dao.findAll(spec) : dao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> findPaginatedByCriteria(CommentCriteria criteria, Pageable pageable) {
        if (pageable == null) {
            return Page.empty();
        }
        Specification<Comment> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new CommentSpecification(criteria),
                rowFilter.forList(Comment.class)
        );
        return dao.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public int getDataSize(CommentCriteria criteria) {
        Specification<Comment> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new CommentSpecification(criteria),
                rowFilter.forList(Comment.class)
        );
        return (int) dao.count(spec);
    }

    @Override
    @Transactional(timeout = 30)
    public List<Comment> delete(List<Comment> ts) { 
        if (ts == null || ts.isEmpty()) return new ArrayList<>();
        
        List<Comment> deleted = new ArrayList<>();
        for (Comment entity : ts) {
            if (entity != null && entity.getId() != null) {
                findById(entity.getId()).ifPresent(e -> {
                    e.setDeletedAt(LocalDateTime.now());
                    dao.save(e);
                    deleted.add(e);
                });
            }
        }
        return deleted;
    }

    @Override
    @Transactional(readOnly = true)
    @Permit("Comment:READ")
    public Comment findByRef(String ref) {
        if (ref == null || ref.trim().isEmpty()) return null;
        Specification<Comment> spec = rowFilter.forList(Comment.class);
        return spec != null
                ? dao.findOne(spec.and((root, query, cb) -> cb.equal(root.get("ref"), ref))).orElse(null)
                : dao.findByRef(ref);
    }

    /**
     * Combines the caller's criteria Specification with the PolicyEngine row filter
     * (AND logic, null-tolerant on both sides).
     */
    private Specification<Comment> combineSpecs(Specification<Comment> criteriaSpec,
                                                       Specification<Comment> authzSpec) {
        if (criteriaSpec == null && authzSpec == null) return null;
        if (criteriaSpec == null) return authzSpec;
        if (authzSpec == null) return criteriaSpec;
        return criteriaSpec.and(authzSpec);
    }
}

