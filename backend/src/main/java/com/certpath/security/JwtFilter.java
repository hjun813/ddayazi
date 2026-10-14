package com.certpath.security;

import com.certpath.domain.Enums.Role;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component @RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
        var h=req.getHeader("Authorization");
        if(h!=null&&h.startsWith("Bearer "))try{
            var claims=jwt.claims(h.substring(7));
            var email=claims.getSubject();
            var roleClaim=claims.get("role",String.class);
            if(roleClaim==null)throw new IllegalArgumentException("Missing role claim");
            var role=Role.valueOf(roleClaim);
            if(email!=null&&!email.isBlank())SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,null,List.of(new SimpleGrantedAuthority("ROLE_"+role.name()))));
        }catch(io.jsonwebtoken.JwtException|IllegalArgumentException ignored){}
        chain.doFilter(req,res);
    }
}
