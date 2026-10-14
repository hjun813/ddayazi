package com.certpath;

import com.certpath.security.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.assertThat;

class JwtFilterTests {
    private final JwtService jwt=new JwtService("test-signing-secret-at-least-thirty-two-bytes",3600);
    @AfterEach void clear(){SecurityContextHolder.clearContext();}

    @Test void authenticatesUsingSignedRoleWithoutAUserLookup() throws Exception {
        filter(jwt.create("admin@example.com","ADMIN"));
        var auth=SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getName()).isEqualTo("admin@example.com");
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }
    @Test void rejectsInvalidMissingUnknownAndExpiredClaims() throws Exception {
        var expired=new JwtService("test-signing-secret-at-least-thirty-two-bytes",-1);
        for(var token:new String[]{"invalid",jwt.create("user@example.com",null),jwt.create("user@example.com","UNKNOWN"),expired.create("user@example.com","ADMIN")}) {
            filter(token);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }
    private void filter(String token) throws Exception {
        var req=new MockHttpServletRequest();req.addHeader("Authorization","Bearer "+token);
        new JwtFilter(jwt).doFilter(req,new MockHttpServletResponse(),new MockFilterChain());
    }
}
