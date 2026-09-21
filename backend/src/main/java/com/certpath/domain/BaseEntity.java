package com.certpath.domain;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;

@MappedSuperclass @EntityListeners(AuditingEntityListener.class) @Getter
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) protected Long id;
    @CreatedDate @Column(nullable=false, updatable=false) protected Instant createdAt;
    @LastModifiedDate @Column(nullable=false) protected Instant updatedAt;
}
