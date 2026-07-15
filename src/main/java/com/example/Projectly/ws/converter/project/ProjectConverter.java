package com.example.Projectly.ws.converter.project;

import org.springframework.stereotype.Component;
import com.example.Projectly.bean.core.project.Project;
import com.example.Projectly.ws.dto.project.request.CreateProjectRequest;
import com.example.Projectly.ws.dto.project.request.UpdateProjectRequest;
import com.example.Projectly.ws.dto.project.response.ProjectResponse;

@Component
public class ProjectConverter {

    public ProjectResponse toResponse(Project entity) {
        if (entity == null) return null;
        ProjectResponse response = new ProjectResponse();
        response.setId(entity.getId());
        response.setRef(entity.getRef());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastModifiedDate(entity.getLastModifiedDate());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        if (entity.getOwner() != null) {
            response.setOwnerId(entity.getOwner().getId());
        }
        return response;
    }

    public Project toEntity(CreateProjectRequest request) {
        if (request == null) return null;
        Project entity = new Project();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        return entity;
    }

    public Project toEntity(UpdateProjectRequest request) {
        if (request == null) return null;
        Project entity = new Project();
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStatus(request.getStatus());
        return entity;
    }
}

