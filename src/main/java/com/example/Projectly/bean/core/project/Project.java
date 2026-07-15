package com.example.Projectly.bean.core.project;

import java.util.Set;
import java.util.LinkedHashSet;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.hibernate.annotations.SQLRestriction;
import com.example.Projectly.common.entity.BaseEntity;
import com.example.Projectly.bean.core.user.User;
import com.example.Projectly.bean.core.task.Task;
import com.example.Projectly.bean.core.enums.ProjectStatus;

@Entity
@Table(name = "app_project")
@JsonInclude(JsonInclude.Include.NON_NULL)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(of = "id", callSuper = false)
@ToString(of = "id")
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @PrePersist
    void stampAuthzDefaults() {
        if (this.owner == null) {
            com.example.Projectly.config.security.authz.PrincipalContext.Snapshot principal =
                    com.example.Projectly.config.security.authz.PrincipalContext.currentOrAnonymous();
            if (!principal.isAnonymous()) {
                this.owner = principal.user();
            }
        }
    }

    @Column(length = 500, nullable = false)
    private String name;
    @Column(length = 500)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProjectStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
     @OneToMany(mappedBy = "project", orphanRemoval = true, fetch = FetchType.LAZY)
     @Builder.Default
     private Set<Task> tasks = new LinkedHashSet<>();

}

