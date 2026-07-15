package com.example.Projectly.dao.facade.core.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import com.example.Projectly.bean.core.task.Task;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskDao extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    Task findByRef(String ref);
    int deleteByRef(String ref);

    @EntityGraph(attributePaths = {"project", "assignee", "comments"})
    @Override
    Page<Task> findAll(@Nullable Specification<Task> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee", "comments"})
    @Query("SELECT e FROM Task e WHERE e.id = :id")
    Optional<Task> findWithAssociationsById(@Param("id") Long id);
}

