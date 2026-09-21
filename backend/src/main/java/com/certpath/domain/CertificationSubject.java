package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Getter @Setter @NoArgsConstructor
public class CertificationSubject extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(nullable=false) private Certification certification;
    @Column(nullable=false) private String name;
    private int displayOrder;
    public CertificationSubject(Certification c,String name,int displayOrder){this.certification=c;this.name=name;this.displayOrder=displayOrder;}
}
