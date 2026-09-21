package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import static com.certpath.domain.Enums.*;

@Entity @Getter @Setter @NoArgsConstructor
@Table(uniqueConstraints=@UniqueConstraint(name="uk_schedule_cert_round_type", columnNames={"certification_id","exam_round","type"}))
public class CertificationSchedule extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="certification_id", nullable=false) private Certification certification;
    @Column(name="exam_round") private String examRound = "UNSPECIFIED";
    @Column(nullable=false) private String title;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private ScheduleType type;
    @Column(nullable=false) private Instant startsAt;
    private Instant endsAt;
    private String sourceUrl = "SAMPLE";
    private LocalDate lastVerifiedAt;
    @Enumerated(EnumType.STRING) private ReviewStatus reviewStatus = ReviewStatus.DRAFT;
    private boolean sampleData = true;
    private boolean active = true;
    public CertificationSchedule(Certification c,String title,ScheduleType type,Instant startsAt){this.certification=c;this.title=title;this.type=type;this.startsAt=startsAt;}
}