package com.certpath;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication
@OpenAPIDefinition(info=@Info(title="따야지 API",version="v1",description="IT 자격증 추천과 검수된 시험 일정 API"))
public class CertPathApplication {
    public static void main(String[] args) { SpringApplication.run(CertPathApplication.class, args); }
}
