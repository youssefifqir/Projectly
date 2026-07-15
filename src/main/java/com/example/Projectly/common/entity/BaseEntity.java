package com.example.Projectly.common.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Abstract auditable base entity.
 * Provides audit fields (ref, timestamps, created/modified by).
 * Subclasses define their own @Id (Long for business entities, UUID for security entities).
 */
@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Audited
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity implements Serializable {

    @Column(length = 500, unique = true)
    protected String ref;

    @PrePersist
    protected void generateRef() {
        if (this.ref == null || this.ref.isBlank()) {
            this.ref = refGen(getClass().getSimpleName().toLowerCase());
        }
    }

    public static String refGen(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    @Version
    @Column(name = "version")
    private Long version;

    @CreatedDate
    @Column(name = "created_date", updatable = false, nullable = false)
    protected LocalDateTime createdDate;

    @LastModifiedDate
    @Column(name = "last_modified_date", insertable = false)
    protected LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    protected String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", insertable = false)
    protected String lastModifiedBy;

    @Column(name = "deleted_at")
    protected LocalDateTime deletedAt;

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

