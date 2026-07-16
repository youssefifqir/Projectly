package com.example.Projectly.service.impl.project;

import com.example.Projectly.bean.core.project.Project;
import com.example.Projectly.dao.criteria.core.project.ProjectCriteria;
import com.example.Projectly.dao.facade.core.project.ProjectDao;
import com.example.Projectly.dao.specification.core.project.ProjectSpecification;
import com.example.Projectly.service.facade.project.ProjectService;
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
public class ProjectServiceImpl implements ProjectService {

    private final ProjectDao dao;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthorizationSpecificationAdvisor rowFilter;
    private final UserDao userDao;

    public ProjectServiceImpl(ProjectDao dao, ApplicationEventPublisher eventPublisher, AuthorizationSpecificationAdvisor rowFilter, UserDao userDao) {
        this.dao = dao;
        this.eventPublisher = eventPublisher;
        this.rowFilter = rowFilter;
        this.userDao = userDao;
    }

    @Override
    @Transactional(readOnly = true)
    @Permit("Project:READ")
    public List<Project> findAll() {
        Specification<Project> spec = rowFilter.forList(Project.class);
        return spec != null ? dao.findAll(spec) : dao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Permit("Project:READ")
    public Optional<Project> findById(Long id) {
        Specification<Project> spec = rowFilter.forList(Project.class);
        return spec != null
                ? dao.findOne(spec.and((root, query, cb) -> cb.equal(root.get("id"), id)))
                : dao.findById(id);
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_project", allEntries = true)
    public Project save(Project entity) {
        if (entity == null) return null;

        // Validate and handle relationships before saving
        validateAndPrepareRelationships(entity);

        return dao.save(entity);
    }

    private void validateAndPrepareRelationships(Project entity) {
        if (entity == null) return;

        // Validate ManyToOne to User (security entity with String UUID id)
        if (entity.getOwner() != null) {
            User ownerEntity = entity.getOwner();
            if (ownerEntity.getId() != null) {
                User existingOwner = this.userDao.findById(ownerEntity.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Owner with id " + ownerEntity.getId() + " does not exist"));
                entity.setOwner(existingOwner);
            } else {
                throw new IllegalArgumentException("Owner must be referenced by id");
            }
        }
        // Handle OneToMany relationships - set parent reference
        // NOTE: checked for non-empty, not just non-null — the field is always non-null (empty
        // collection literal in the entity), so a plain != null check here misfires on every
        // request that never touches this relationship (e.g. converters that only set scalar
        // fields), wiping/reattaching a collection nothing actually asked to change.
        if (entity.getTasks() != null && !entity.getTasks().isEmpty()) {
            entity.getTasks().forEach(child -> {
                if (child != null) {
                    child.setProject(entity);
                }
            });
        }
    }

    private void validateDeletionAllowed(Project entity) {
        if (entity == null) return;

        // Keep as an extension point for domain-specific delete guards.
    }

    private void prepareForDeletion(Project entity) {
        if (entity == null) return;

        // No-op for OneToMany: rely on JPA cascade/orphanRemoval configuration.
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_project", allEntries = true)
    @Permit("Project:DELETE")
    public void deleteById(Long id) {
        if (id == null) return;

        findById(id).ifPresent(entity -> {
            validateDeletionAllowed(entity);
            prepareForDeletion(entity);
            entity.setDeletedAt(LocalDateTime.now());
            dao.save(entity);
            eventPublisher.publishEvent(EntityEvent.deleted("Project", entity));
        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_project", allEntries = true)
    @Permit("Project:DELETE")
    public Optional<Project> findAndDeleteById(Long id) {
        if (id == null) return Optional.empty();
        return findById(id).map(entity -> {
            validateDeletionAllowed(entity);
            prepareForDeletion(entity);
            entity.setDeletedAt(LocalDateTime.now());
            dao.save(entity);
            eventPublisher.publishEvent(EntityEvent.deleted("Project", entity));
            return entity;
        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_project", allEntries = true)
    @Permit("Project:CREATE")
    public Project create(Project t) {
        Project result = save(t);
        if (result != null) {
            eventPublisher.publishEvent(EntityEvent.created("Project", result));
        }
        return result;
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_project", allEntries = true)
    @Permit("Project:UPDATE")
    public Project update(Project t) {
        if (t == null || t.getId() == null) return null;
        Project existing = findById(t.getId()).orElse(null);
        if (existing == null) return null;

        validateAndPrepareRelationships(t);
        mergeEntityData(existing, t);

        Project result = save(existing);
        if (result != null) {
            eventPublisher.publishEvent(EntityEvent.updated("Project", result));
        }
        return result;
    }

    private void mergeEntityData(Project existing, Project updated) {
        if (existing == null || updated == null) return;

        if (updated.getName() != null) {
            existing.setName(updated.getName().isEmpty() ? null : updated.getName());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription().isEmpty() ? null : updated.getDescription());
        }
        if (updated.getStatus() != null) {
            existing.setStatus(updated.getStatus());
        }

        // Handle relationships
        if (updated.getOwner() != null) {
            existing.setOwner(updated.getOwner());
        }
        // NOTE: checked for non-empty, not just non-null — this field is always non-null
        // (initialized to an empty collection on the entity), so a plain != null check fires on
        // every update() call, even DTOs/converters that never populate this relationship at
        // all. That unconditionally clears the real, persisted collection (deleting rows via
        // orphanRemoval) and, worse, can hand Hibernate a still-transient child with no cascade
        // configured on the association, throwing TransientObjectException on flush.
        if (updated.getTasks() != null && !updated.getTasks().isEmpty()) {
            if (existing.getTasks() == null) {
                existing.setTasks(new LinkedHashSet<>());
            } else {
                existing.getTasks().clear();
            }

            updated.getTasks().forEach(child -> {
                if (child != null) {
                    child.setProject(existing);
                    existing.getTasks().add(child);
                }
            });
        }
    }

    @Override
    @Transactional(timeout = 30)
    public List<Project> update(List<Project> ts, boolean createIfNotExist) { 
        if (ts == null || ts.isEmpty()) return new ArrayList<>();
        
        List<Project> result = new ArrayList<>();
        for (Project entity : ts) {
            if (entity.getId() != null) {
                Project updated = update(entity);
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
    public Project findOrSave(Project t) { 
        if (t == null) return null;
        
        Project existing = null;
        if (t.getId() != null) {
            existing = findById(t.getId()).orElse(null);
        } else if (t.getRef() != null) {
            existing = findByRef(t.getRef());
        }
        
        return existing != null ? existing : save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public Project findByReferenceEntity(Project t) { 
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
    public Project findWithAssociatedLists(Long id) {
        return dao.findWithAssociationsById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Project> findByCriteria(ProjectCriteria criteria) {
        Specification<Project> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new ProjectSpecification(criteria),
                rowFilter.forList(Project.class)
        );
        return spec != null ? dao.findAll(spec) : dao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Project> findPaginatedByCriteria(ProjectCriteria criteria, Pageable pageable) {
        if (pageable == null) {
            return Page.empty();
        }
        Specification<Project> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new ProjectSpecification(criteria),
                rowFilter.forList(Project.class)
        );
        return dao.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public int getDataSize(ProjectCriteria criteria) {
        Specification<Project> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new ProjectSpecification(criteria),
                rowFilter.forList(Project.class)
        );
        return (int) dao.count(spec);
    }

    @Override
    @Transactional(timeout = 30)
    public List<Project> delete(List<Project> ts) { 
        if (ts == null || ts.isEmpty()) return new ArrayList<>();
        
        List<Project> deleted = new ArrayList<>();
        for (Project entity : ts) {
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
    @Permit("Project:READ")
    public Project findByRef(String ref) {
        if (ref == null || ref.trim().isEmpty()) return null;
        Specification<Project> spec = rowFilter.forList(Project.class);
        return spec != null
                ? dao.findOne(spec.and((root, query, cb) -> cb.equal(root.get("ref"), ref))).orElse(null)
                : dao.findByRef(ref);
    }

    /**
     * Combines the caller's criteria Specification with the PolicyEngine row filter
     * (AND logic, null-tolerant on both sides).
     */
    private Specification<Project> combineSpecs(Specification<Project> criteriaSpec,
                                                       Specification<Project> authzSpec) {
        if (criteriaSpec == null && authzSpec == null) return null;
        if (criteriaSpec == null) return authzSpec;
        if (authzSpec == null) return criteriaSpec;
        return criteriaSpec.and(authzSpec);
    }
}

