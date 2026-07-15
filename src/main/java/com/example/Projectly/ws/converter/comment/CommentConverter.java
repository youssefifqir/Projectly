package com.example.Projectly.ws.converter.comment;

import org.springframework.stereotype.Component;
import com.example.Projectly.bean.core.comment.Comment;
import com.example.Projectly.ws.dto.comment.request.CreateCommentRequest;
import com.example.Projectly.ws.dto.comment.request.UpdateCommentRequest;
import com.example.Projectly.ws.dto.comment.response.CommentResponse;
import com.example.Projectly.bean.core.task.Task;

@Component
public class CommentConverter {

    public CommentResponse toResponse(Comment entity) {
        if (entity == null) return null;
        CommentResponse response = new CommentResponse();
        response.setId(entity.getId());
        response.setRef(entity.getRef());
        response.setCreatedDate(entity.getCreatedDate());
        response.setLastModifiedDate(entity.getLastModifiedDate());
        response.setBody(entity.getBody());
        if (entity.getTask() != null) {
            response.setTaskId(entity.getTask().getId());
            response.setTaskRef(entity.getTask().getRef());
        }
        if (entity.getAuthor() != null) {
            response.setAuthorId(entity.getAuthor().getId());
        }
        return response;
    }

    public Comment toEntity(CreateCommentRequest request) {
        if (request == null) return null;
        Comment entity = new Comment();
        entity.setBody(request.getBody());
        if (request.getTaskId() != null || request.getTaskRef() != null) {
            Task taskRef = new Task();
            taskRef.setId(request.getTaskId());
            taskRef.setRef(request.getTaskRef());
            entity.setTask(taskRef);
        }
        return entity;
    }

    public Comment toEntity(UpdateCommentRequest request) {
        if (request == null) return null;
        Comment entity = new Comment();
        entity.setBody(request.getBody());
        if (request.getTaskId() != null || request.getTaskRef() != null) {
            Task taskRef = new Task();
            taskRef.setId(request.getTaskId());
            taskRef.setRef(request.getTaskRef());
            entity.setTask(taskRef);
        }
        return entity;
    }
}

