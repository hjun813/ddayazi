package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;
import static com.certpath.domain.Enums.ProgressStatus;

@Entity @Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"user_id","certification_id"}))
public class UserCertification extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="certification_id", nullable=false) private Certification certification;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ProgressStatus status = ProgressStatus.INTERESTED;
    public UserCertification(User u, Certification c, ProgressStatus status){this.user=u;this.certification=c;this.status=status;}
}
