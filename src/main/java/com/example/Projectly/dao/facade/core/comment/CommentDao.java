package com.example.Projectly.dao.facade.core.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.Nullable;
import com.example.Projectly.bean.core.comment.Comment;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommentDao extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    Comment findByRef(String ref);
    int deleteByRef(String ref);

    @EntityGraph(attributePaths = {"task", "author"})
    @Override
    Page<Comment> findAll(@Nullable Specification<Comment> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"task", "author"})
    @Query("SELECT e FROM Comment e WHERE e.id = :id")
    Optional<Comment> findWithAssociationsById(@Param("id") Long id);
}

