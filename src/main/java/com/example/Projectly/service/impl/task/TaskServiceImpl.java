package com.example.Projectly.service.impl.task;

import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.dao.criteria.core.task.TaskCriteria;
import com.example.Projectly.dao.facade.core.task.TaskDao;
import com.example.Projectly.dao.specification.core.task.TaskSpecification;
import com.example.Projectly.service.facade.task.TaskService;
import com.example.Projectly.service.facade.project.ProjectService;
import com.example.Projectly.bean.core.project.Project;
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
public class TaskServiceImpl implements TaskService {

    private final TaskDao dao;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthorizationSpecificationAdvisor rowFilter;
    private final UserDao userDao;
    private final ProjectService projectService;

    public TaskServiceImpl(TaskDao dao, ApplicationEventPublisher eventPublisher, AuthorizationSpecificationAdvisor rowFilter, UserDao userDao, @Lazy ProjectService projectService) {
        this.dao = dao;
        this.eventPublisher = eventPublisher;
        this.rowFilter = rowFilter;
        this.userDao = userDao;
        this.projectService = projectService;
    }

    @Override
    @Transactional(readOnly = true)
    @Permit("Task:READ")
    public List<Task> findAll() {
        Specification<Task> spec = rowFilter.forList(Task.class);
        return spec != null ? dao.findAll(spec) : dao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    @Permit("Task:READ")
    public Optional<Task> findById(Long id) {
        Specification<Task> spec = rowFilter.forList(Task.class);
        return spec != null
                ? dao.findOne(spec.and((root, query, cb) -> cb.equal(root.get("id"), id)))
                : dao.findById(id);
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_task", allEntries = true)
    public Task save(Task entity) {
        if (entity == null) return null;

        // Validate and handle relationships before saving
        validateAndPrepareRelationships(entity);

        return dao.save(entity);
    }

    private void validateAndPrepareRelationships(Task entity) {
        if (entity == null) return;

        // Validate ManyToOne relationships exist
        if (entity.getProject() != null) {
            Project projectEntity = entity.getProject();
            if (projectEntity.getId() != null) {
                Project existingProject = projectService.findById(projectEntity.getId()).orElse(null);
                if (existingProject == null) {
                    throw new IllegalArgumentException("Project with id " + projectEntity.getId() + " does not exist");
                }
                entity.setProject(existingProject);
            } else if (projectEntity.getRef() != null) {
                Project existingProject = projectService.findByRef(projectEntity.getRef());
                if (existingProject == null) {
                    throw new IllegalArgumentException("Project with ref '" + projectEntity.getRef() + "' does not exist");
                }
                entity.setProject(existingProject);
            } else {
                throw new IllegalArgumentException("Project must be referenced by id or ref");
            }
        }
        // Validate ManyToOne to User (security entity with String UUID id)
        if (entity.getAssignee() != null) {
            User assigneeEntity = entity.getAssignee();
            if (assigneeEntity.getId() != null) {
                User existingAssignee = this.userDao.findById(assigneeEntity.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Assignee with id " + assigneeEntity.getId() + " does not exist"));
                entity.setAssignee(existingAssignee);
            } else {
                throw new IllegalArgumentException("Assignee must be referenced by id");
            }
        }
        // Handle OneToMany relationships - set parent reference
        if (entity.getComments() != null) {
            entity.getComments().forEach(child -> {
                if (child != null) {
                    child.setTask(entity);
                }
            });
        }
    }

    private void validateDeletionAllowed(Task entity) {
        if (entity == null) return;

        // Keep as an extension point for domain-specific delete guards.
    }

    private void prepareForDeletion(Task entity) {
        if (entity == null) return;

        // No-op for OneToMany: rely on JPA cascade/orphanRemoval configuration.
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_task", allEntries = true)
    @Permit("Task:DELETE")
    public void deleteById(Long id) {
        if (id == null) return;

        findById(id).ifPresent(entity -> {
            validateDeletionAllowed(entity);
            prepareForDeletion(entity);
            entity.setDeletedAt(LocalDateTime.now());
            dao.save(entity);
            eventPublisher.publishEvent(EntityEvent.deleted("Task", entity));
        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_task", allEntries = true)
    @Permit("Task:DELETE")
    public Optional<Task> findAndDeleteById(Long id) {
        if (id == null) return Optional.empty();
        return findById(id).map(entity -> {
            validateDeletionAllowed(entity);
            prepareForDeletion(entity);
            entity.setDeletedAt(LocalDateTime.now());
            dao.save(entity);
            eventPublisher.publishEvent(EntityEvent.deleted("Task", entity));
            return entity;
        });
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_task", allEntries = true)
    @Permit("Task:CREATE")
    public Task create(Task t) {
        Task result = save(t);
        if (result != null) {
            eventPublisher.publishEvent(EntityEvent.created("Task", result));
        }
        return result;
    }

    @Override
    @Transactional(timeout = 30)
    @CacheEvict(value = "entity_task", allEntries = true)
    @Permit("Task:UPDATE")
    public Task update(Task t) {
        if (t == null || t.getId() == null) return null;
        Task existing = findById(t.getId()).orElse(null);
        if (existing == null) return null;

        validateAndPrepareRelationships(t);
        mergeEntityData(existing, t);

        Task result = save(existing);
        if (result != null) {
            eventPublisher.publishEvent(EntityEvent.updated("Task", result));
        }
        return result;
    }

    private void mergeEntityData(Task existing, Task updated) {
        if (existing == null || updated == null) return;

        if (updated.getTitle() != null) {
            existing.setTitle(updated.getTitle().isEmpty() ? null : updated.getTitle());
        }
        if (updated.getDescription() != null) {
            existing.setDescription(updated.getDescription().isEmpty() ? null : updated.getDescription());
        }
        if (updated.getPriority() != null) {
            existing.setPriority(updated.getPriority());
        }
        if (updated.getCompleted() != null) {
            existing.setCompleted(updated.getCompleted());
        }
        if (updated.getDueDate() != null) {
            existing.setDueDate(updated.getDueDate());
        }
        if (updated.getInternalNote() != null) {
            existing.setInternalNote(updated.getInternalNote().isEmpty() ? null : updated.getInternalNote());
        }

        // Handle relationships
        if (updated.getProject() != null) {
            existing.setProject(updated.getProject());
        }
        if (updated.getAssignee() != null) {
            existing.setAssignee(updated.getAssignee());
        }
        if (updated.getComments() != null) {
            if (existing.getComments() == null) {
                existing.setComments(new LinkedHashSet<>());
            } else {
                existing.getComments().clear();
            }

            updated.getComments().forEach(child -> {
                if (child != null) {
                    child.setTask(existing);
                    existing.getComments().add(child);
                }
            });
        }
    }

    @Override
    @Transactional(timeout = 30)
    public List<Task> update(List<Task> ts, boolean createIfNotExist) { 
        if (ts == null || ts.isEmpty()) return new ArrayList<>();
        
        List<Task> result = new ArrayList<>();
        for (Task entity : ts) {
            if (entity.getId() != null) {
                Task updated = update(entity);
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
    public Task findOrSave(Task t) { 
        if (t == null) return null;
        
        Task existing = null;
        if (t.getId() != null) {
            existing = findById(t.getId()).orElse(null);
        } else if (t.getRef() != null) {
            existing = findByRef(t.getRef());
        }
        
        return existing != null ? existing : save(t);
    }

    @Override
    @Transactional(readOnly = true)
    public Task findByReferenceEntity(Task t) { 
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
    public Task findWithAssociatedLists(Long id) {
        return dao.findWithAssociationsById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Task> findByCriteria(TaskCriteria criteria) {
        Specification<Task> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new TaskSpecification(criteria),
                rowFilter.forList(Task.class)
        );
        return spec != null ? dao.findAll(spec) : dao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Task> findPaginatedByCriteria(TaskCriteria criteria, Pageable pageable) {
        if (pageable == null) {
            return Page.empty();
        }
        Specification<Task> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new TaskSpecification(criteria),
                rowFilter.forList(Task.class)
        );
        return dao.findAll(spec, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public int getDataSize(TaskCriteria criteria) {
        Specification<Task> spec = combineSpecs(
                (criteria == null || criteria.isEmpty()) ? null : new TaskSpecification(criteria),
                rowFilter.forList(Task.class)
        );
        return (int) dao.count(spec);
    }

    @Override
    @Transactional(timeout = 30)
    public List<Task> delete(List<Task> ts) { 
        if (ts == null || ts.isEmpty()) return new ArrayList<>();
        
        List<Task> deleted = new ArrayList<>();
        for (Task entity : ts) {
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
    @Permit("Task:READ")
    public Task findByRef(String ref) {
        if (ref == null || ref.trim().isEmpty()) return null;
        Specification<Task> spec = rowFilter.forList(Task.class);
        return spec != null
                ? dao.findOne(spec.and((root, query, cb) -> cb.equal(root.get("ref"), ref))).orElse(null)
                : dao.findByRef(ref);
    }

    /**
     * Combines the caller's criteria Specification with the PolicyEngine row filter
     * (AND logic, null-tolerant on both sides).
     */
    private Specification<Task> combineSpecs(Specification<Task> criteriaSpec,
                                                       Specification<Task> authzSpec) {
        if (criteriaSpec == null && authzSpec == null) return null;
        if (criteriaSpec == null) return authzSpec;
        if (authzSpec == null) return criteriaSpec;
        return criteriaSpec.and(authzSpec);
    }
}

