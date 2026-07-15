package com.example.Projectly.service.facade.task;

import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.dao.criteria.core.task.TaskCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Optional;

@Validated
public interface TaskService {

    Task create(Task t);
    Task update(Task t);
    List<Task> update(List<Task> ts, boolean createIfNotExist);
    Optional<Task> findById(Long id);
    Task save(Task entity);
    void deleteById(Long id);
    Optional<Task> findAndDeleteById(Long id);
    Task findOrSave(Task t);
    Task findByReferenceEntity(Task t);
    Task findWithAssociatedLists(Long id);
    List<Task> findAll();
    List<Task> findByCriteria(TaskCriteria criteria);
    Page<Task> findPaginatedByCriteria(TaskCriteria criteria, Pageable pageable);
    int getDataSize(TaskCriteria criteria);
    List<Task> delete(List<Task> ts);
    Task findByRef(String ref);
}

