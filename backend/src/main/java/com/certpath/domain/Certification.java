package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.*;
import static com.certpath.domain.Enums.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Certification extends BaseEntity {
    @Column(nullable=false, unique=true) private String name;
    @Column(nullable=false, length=1500) private String summary;
    @Column(nullable=false) private String category;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private CertificationType type;
    @Column(nullable=false) private String organization;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Difficulty difficulty;
    private Integer preparationWeeks;
    private String examMethod;
    private String passingCriteria;
    private String eligibility;
    private String cost;
    private String validity;
    private String officialUrl;
    private String registrationUrl;
    private LocalDate lastVerifiedAt;
    @Enumerated(EnumType.STRING) private ReviewStatus reviewStatus = ReviewStatus.DRAFT;
    private Integer popularityScore = 50;
    private boolean sampleData = true;
    private boolean published = true;
    @OneToMany(mappedBy="certification", cascade=CascadeType.ALL, orphanRemoval=true) private List<CertificationJobRole> jobRoles = new ArrayList<>();
    @OneToMany(mappedBy="certification", cascade=CascadeType.ALL, orphanRemoval=true) private List<CertificationSchedule> schedules = new ArrayList<>();
    @OneToMany(mappedBy="certification", cascade=CascadeType.ALL, orphanRemoval=true) private List<CertificationSubject> subjects = new ArrayList<>();
    @OneToMany(mappedBy="certification", cascade=CascadeType.ALL, orphanRemoval=true) private List<StudyResource> resources = new ArrayList<>();
}
