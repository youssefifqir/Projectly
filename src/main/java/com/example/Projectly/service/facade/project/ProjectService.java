package com.example.Projectly.service.facade.project;

import com.example.Projectly.bean.core.project.Project;
import com.example.Projectly.dao.criteria.core.project.ProjectCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Optional;

@Validated
public interface ProjectService {

    Project create(Project t);
    Project update(Project t);
    List<Project> update(List<Project> ts, boolean createIfNotExist);
    Optional<Project> findById(Long id);
    Project save(Project entity);
    void deleteById(Long id);
    Optional<Project> findAndDeleteById(Long id);
    Project findOrSave(Project t);
    Project findByReferenceEntity(Project t);
    Project findWithAssociatedLists(Long id);
    List<Project> findAll();
    List<Project> findByCriteria(ProjectCriteria criteria);
    Page<Project> findPaginatedByCriteria(ProjectCriteria criteria, Pageable pageable);
    int getDataSize(ProjectCriteria criteria);
    List<Project> delete(List<Project> ts);
    Project findByRef(String ref);
}

