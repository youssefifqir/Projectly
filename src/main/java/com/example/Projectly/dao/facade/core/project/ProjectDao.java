package com.example.Projectly.dao.facade.core.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import com.example.Projectly.bean.core.project.Project;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectDao extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {

    Project findByRef(String ref);
    int deleteByRef(String ref);

    @EntityGraph(attributePaths = {"owner", "tasks"})
    @Override
    Page<Project> findAll(@Nullable Specification<Project> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "tasks"})
    @Query("SELECT e FROM Project e WHERE e.id = :id")
    Optional<Project> findWithAssociationsById(@Param("id") Long id);
}

