package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class JobRole extends BaseEntity {
    @Column(nullable=false, unique=true) private String name;
    private String description;
    private boolean active = true;
    public JobRole(String name, String description) { this.name=name; this.description=description; }
}
