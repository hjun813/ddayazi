package com.certpath.service;

import com.certpath.api.ApiModels.*;
import com.certpath.domain.User;
import com.certpath.repository.*;
import com.certpath.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class AuthService {
    private final UserRepository users; private final JobRoleRepository jobs; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthResponse signup(SignupRequest r){if(users.existsByEmail(r.email()))throw new IllegalArgumentException("이미 가입된 이메일입니다.");var u=new User(r.email(),encoder.encode(r.password()),r.nickname());if(r.desiredJobRoleId()!=null)u.setDesiredJobRole(jobs.findById(r.desiredJobRoleId()).orElseThrow());users.save(u);return token(u);}
    public AuthResponse login(LoginRequest r){var u=users.findByEmail(r.email()).orElseThrow(()->new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));if(!encoder.matches(r.password(),u.getPasswordHash()))throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");return token(u);}
    private AuthResponse token(User u){return new AuthResponse(jwt.create(u.getEmail(),u.getRole().name()),"Bearer",jwt.expiration(),u.getNickname(),u.getRole().name());}
}
