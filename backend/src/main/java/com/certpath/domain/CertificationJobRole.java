package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"certification_id","job_role_id"}))
public class CertificationJobRole extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="certification_id", nullable=false) private Certification certification;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="job_role_id", nullable=false) private JobRole jobRole;
    private int relevance;
    @Column(length=600) private String reason;
    private String level;
    public CertificationJobRole(Certification c, JobRole j, int relevance, String reason, String level) { this.certification=c; this.jobRole=j; this.relevance=relevance; this.reason=reason; this.level=level; }
}
