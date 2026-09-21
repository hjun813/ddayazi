package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity @Getter @Setter @NoArgsConstructor
public class DataSource extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) private Certification certification;
    @Column(nullable=false) private String title;
    @Column(nullable=false) private String url;
    private LocalDate lastCheckedAt;
    private boolean active=true;
}
