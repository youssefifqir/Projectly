package com.example.Projectly.service.facade.comment;

import com.example.Projectly.bean.core.comment.Comment;
import com.example.Projectly.dao.criteria.core.comment.CommentCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import java.util.List;
import java.util.Optional;

@Validated
public interface CommentService {

    Comment create(Comment t);
    Comment update(Comment t);
    List<Comment> update(List<Comment> ts, boolean createIfNotExist);
    Optional<Comment> findById(Long id);
    Comment save(Comment entity);
    void deleteById(Long id);
    Optional<Comment> findAndDeleteById(Long id);
    Comment findOrSave(Comment t);
    Comment findByReferenceEntity(Comment t);
    Comment findWithAssociatedLists(Long id);
    List<Comment> findAll();
    List<Comment> findByCriteria(CommentCriteria criteria);
    Page<Comment> findPaginatedByCriteria(CommentCriteria criteria, Pageable pageable);
    int getDataSize(CommentCriteria criteria);
    List<Comment> delete(List<Comment> ts);
    Comment findByRef(String ref);
}

