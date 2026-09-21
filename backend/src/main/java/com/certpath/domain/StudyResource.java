package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import static com.certpath.domain.Enums.ResourceType;

@Entity @Getter @Setter @NoArgsConstructor
public class StudyResource extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private Certification certification;
    @Column(nullable=false) private String title;
    private String description;
    @Enumerated(EnumType.STRING) private ResourceType type;
    @Column(nullable=false) private String url;
    private boolean free;
    private boolean official;
    private LocalDate lastCheckedAt;
    private boolean active = true;
}
