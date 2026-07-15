package com.example.Projectly.bean.core.comment;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.SQLRestriction;
import com.example.Projectly.common.entity.BaseEntity;
import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.bean.core.user.User;

@Entity
@Table(name = "app_comment")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = "id")
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @PrePersist
    void stampAuthzDefaults() {
        if (this.author == null) {
            com.example.Projectly.config.security.authz.PrincipalContext.Snapshot principal =
                    com.example.Projectly.config.security.authz.PrincipalContext.currentOrAnonymous();
            if (!principal.isAnonymous()) {
                this.author = principal.user();
            }
        }
    }

    @Column(length = 500, nullable = false)
    private String body;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

}

