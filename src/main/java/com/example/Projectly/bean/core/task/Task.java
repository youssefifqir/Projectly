package com.example.Projectly.bean.core.task;

import java.time.LocalDate;
import java.util.Set;
import java.util.LinkedHashSet;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.SQLRestriction;
import com.example.Projectly.common.entity.BaseEntity;
import com.example.Projectly.bean.core.project.Project;
import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.bean.core.comment.Comment;
import com.example.Projectly.bean.core.enums.TaskPriority;

@Entity
@Table(name = "app_task")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = "id")
public class Task extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @PrePersist
    void stampAuthzDefaults() {
        if (this.assignee == null) {
            com.example.Projectly.config.security.authz.PrincipalContext.Snapshot principal =
                    com.example.Projectly.config.security.authz.PrincipalContext.currentOrAnonymous();
            if (!principal.isAnonymous()) {
                this.assignee = principal.user();
            }
        }
    }

    @Column(length = 500, nullable = false)
    private String title;
    @Column(length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;
    @Column(nullable = false)
    private Boolean completed;
    private LocalDate dueDate;
    @Column(length = 500)
    private String internalNote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;
     @OneToMany(mappedBy = "task", orphanRemoval = true, fetch = FetchType.LAZY)
     @Builder.Default
     private Set<Comment> comments = new LinkedHashSet<>();

}

