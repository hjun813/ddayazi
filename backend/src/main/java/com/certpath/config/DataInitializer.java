package com.certpath.config;

import com.certpath.domain.*;import com.certpath.repository.*;import lombok.RequiredArgsConstructor;import org.springframework.boot.CommandLineRunner;import org.springframework.context.annotation.*;import org.springframework.data.jpa.repository.config.EnableJpaAuditing;import org.springframework.security.crypto.password.PasswordEncoder;import java.time.*;import java.util.*;import static com.certpath.domain.Enums.*;
@Configuration @EnableJpaAuditing @RequiredArgsConstructor
public class DataInitializer {
 @Bean CommandLineRunner seed(JobRoleRepository jobs,CertificationRepository certs,UserRepository users,PasswordEncoder encoder){return args->{if(certs.count()>0)return;var roleNames=List.of("백엔드 개발","프론트엔드 개발","데이터 엔지니어","AI 개발","클라우드·DevOps","정보보안","네트워크","QA·테스트");for(String n:roleNames)jobs.save(new JobRole(n,n+" 역량과 연관된 자격증을 확인합니다."));var specs=List.of(
  new String[]{"정보처리기사","소프트웨어 개발 전반의 기초 역량을 점검하는 국가기술자격","소프트웨어","NATIONAL","한국산업인력공단","INTERMEDIATE","12","백엔드 개발"},
  new String[]{"SQLD","데이터 모델링과 SQL 활용 역량을 검증하는 자격","데이터베이스","PRIVATE","한국데이터산업진흥원","BEGINNER","6","데이터 엔지니어"},
  new String[]{"SQLP","고급 데이터 모델링과 SQL 튜닝 역량을 검증하는 자격","데이터베이스","PRIVATE","한국데이터산업진흥원","ADVANCED","16","데이터 엔지니어"},
  new String[]{"ADsP","데이터 분석 기획과 기초 분석 역량을 다루는 자격","데이터·AI","PRIVATE","한국데이터산업진흥원","BEGINNER","5","AI 개발"},
  new String[]{"빅데이터분석기사","빅데이터 분석 기획부터 결과 해석까지 평가하는 국가기술자격","데이터·AI","NATIONAL","한국데이터산업진흥원","INTERMEDIATE","14","데이터 엔지니어"},
  new String[]{"리눅스마스터","리눅스 운영과 관리 역량을 확인하는 자격","인프라","PRIVATE","한국정보통신진흥협회","BEGINNER","7","클라우드·DevOps"},
  new String[]{"네트워크관리사","네트워크 구축과 운용 능력을 평가하는 자격","네트워크","PRIVATE","한국정보통신자격협회","INTERMEDIATE","8","네트워크"},
  new String[]{"정보보안기사","정보보호 시스템과 관리 역량을 검증하는 국가기술자격","보안","NATIONAL","한국방송통신전파진흥원","ADVANCED","18","정보보안"},
  new String[]{"AWS Solutions Architect Associate","AWS 기반 분산 시스템 설계 역량을 검증하는 벤더 자격","클라우드","VENDOR","AWS","INTERMEDIATE","10","클라우드·DevOps"},
  new String[]{"AWS Developer Associate","AWS 애플리케이션 개발과 배포 역량을 검증하는 벤더 자격","클라우드","VENDOR","AWS","INTERMEDIATE","9","백엔드 개발"},
  new String[]{"CKA","Kubernetes 클러스터 운영 능력을 실기로 검증하는 벤더 자격","클라우드","VENDOR","CNCF","ADVANCED","12","클라우드·DevOps"},
  new String[]{"CCNA","네트워크 기초와 Cisco 장비 운용 역량을 검증하는 벤더 자격","네트워크","VENDOR","Cisco","INTERMEDIATE","12","네트워크"},
  new String[]{"ISTQB","소프트웨어 테스팅의 원칙과 실무 지식을 검증하는 국제 자격","테스트","VENDOR","ISTQB","BEGINNER","6","QA·테스트"});
 int i=0;for(var s:specs){var c=new Certification();c.setName(s[0]);c.setSummary(s[1]);c.setCategory(s[2]);c.setType(CertificationType.valueOf(s[3]));c.setOrganization(s[4]);c.setDifficulty(Difficulty.valueOf(s[5]));c.setPreparationWeeks(Integer.parseInt(s[6]));c.setExamMethod("필기 또는 CBT — 공식 공고에서 확인 필요");c.setPassingCriteria("정보 확인 필요");c.setEligibility("정보 확인 필요");c.setCost("정보 확인 필요");c.setValidity("정보 확인 필요");c.setLastVerifiedAt(LocalDate.of(2026,1,15));c.setPopularityScore(80-i);var role=jobs.findByName(s[7]).orElseThrow();c.getJobRoles().add(new CertificationJobRole(c,role,88-i%12,s[7]+" 직무의 핵심 기초와 실무 용어를 체계적으로 점검할 수 있습니다.",c.getDifficulty().name()));c.getSubjects().add(new CertificationSubject(c,"핵심 이론",1));c.getSubjects().add(new CertificationSubject(c,"실무 응용",2));var event=Instant.now().plus(10L+i*6,java.time.temporal.ChronoUnit.DAYS);c.getSchedules().add(new CertificationSchedule(c,"데모 접수 마감",ScheduleType.REGISTRATION_CLOSE,event));c.getSchedules().add(new CertificationSchedule(c,"데모 시험일",ScheduleType.EXAM,event.plus(21,java.time.temporal.ChronoUnit.DAYS)));certs.save(c);i++;}
 var admin=new User("admin@certpath.local",encoder.encode("Admin123!"),"관리자");admin.setRole(Enums.Role.ADMIN);users.save(admin);};}
}
