package com.certpath.domain;

import jakarta.persistence.*;
import lombok.*;
import static com.certpath.domain.Enums.Role;

@Entity @Table(name="users") @Getter @Setter @NoArgsConstructor
public class User extends BaseEntity {
    @Column(nullable=false, unique=true) private String email;
    @Column(nullable=false) private String passwordHash;
    @Column(nullable=false) private String nickname;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role = Role.USER;
    @ManyToOne(fetch=FetchType.LAZY) private JobRole desiredJobRole;
    public User(String email, String passwordHash, String nickname) { this.email=email; this.passwordHash=passwordHash; this.nickname=nickname; }
}
