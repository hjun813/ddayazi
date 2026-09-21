package com.certpath.service;

import com.certpath.api.ApiModels.*;
import com.certpath.domain.*;
import com.certpath.repository.*;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class CertService {
    private final CertificationRepository certs; private final ScheduleRepository schedules; private final JobRoleRepository jobs;
    public Page<CertificationCardDto> search(String query,String job,String category,String type,String difficulty,String sort,int page,int size){
        Specification<Certification> s=(r,q,b)->b.and(b.isTrue(r.get("published")),b.isFalse(r.get("sampleData")),b.equal(r.get("reviewStatus"),Enums.ReviewStatus.VERIFIED));
        if(query!=null&&!query.isBlank())s=s.and((r,q,b)->b.like(b.lower(r.get("name")),"%"+query.toLowerCase()+"%"));
        if(category!=null&&!category.isBlank())s=s.and((r,q,b)->b.equal(r.get("category"),category));
        if(type!=null&&!type.isBlank())s=s.and((r,q,b)->b.equal(r.get("type"),Enums.CertificationType.valueOf(type)));
        if(difficulty!=null&&!difficulty.isBlank())s=s.and((r,q,b)->b.equal(r.get("difficulty"),Enums.Difficulty.valueOf(difficulty)));
        if(job!=null&&!job.isBlank())s=s.and((r,q,b)->b.equal(r.join("jobRoles",JoinType.LEFT).join("jobRole").get("name"),job));
        Sort order=switch(sort==null?"recommended":sort){case "name"->Sort.by("name");case "difficulty"->Sort.by("difficulty");case "duration"->Sort.by("preparationWeeks");default->Sort.by(Sort.Direction.DESC,"popularityScore");};
        return certs.findAll(s,PageRequest.of(page,size,order)).map(this::card);
    }
    public CertificationDetailDto detail(Long id){var c=certs.findById(id).filter(x->x.isPublished()&&!x.isSampleData()&&x.getReviewStatus()==Enums.ReviewStatus.VERIFIED).orElseThrow(()->new NoSuchElementException("자격증을 찾을 수 없습니다."));return new CertificationDetailDto(c.getId(),c.getName(),c.getSummary(),c.getCategory(),c.getType().name(),c.getOrganization(),c.getDifficulty().name(),c.getPreparationWeeks(),c.getExamMethod(),c.getSubjects().stream().sorted(Comparator.comparingInt(CertificationSubject::getDisplayOrder)).map(CertificationSubject::getName).toList(),n(c.getPassingCriteria()),n(c.getEligibility()),n(c.getCost()),n(c.getValidity()),c.getOfficialUrl(),c.getRegistrationUrl(),c.getLastVerifiedAt(),c.getJobRoles().stream().map(j->new JobRecommendationDto(j.getJobRole().getId(),j.getJobRole().getName(),j.getRelevance(),j.getReason(),j.getLevel())).toList(),c.getSchedules().stream().filter(s->s.isActive()&&!s.isSampleData()&&s.getReviewStatus()==Enums.ReviewStatus.VERIFIED).map(this::schedule).toList(),c.getResources().stream().filter(StudyResource::isActive).map(r->new ResourceDto(r.getId(),r.getTitle(),r.getDescription(),r.getType().name(),r.getUrl(),r.isFree(),r.isOfficial(),r.getLastCheckedAt())).toList(),c.isSampleData());}
    public List<ScheduleDto> schedules(Instant from,Instant to){return schedules.findByStartsAtBetweenOrderByStartsAt(from,to).stream().filter(x->x.isActive()&&!x.isSampleData()&&x.getReviewStatus()==Enums.ReviewStatus.VERIFIED&&x.getCertification().isPublished()&&!x.getCertification().isSampleData()&&x.getCertification().getReviewStatus()==Enums.ReviewStatus.VERIFIED).map(this::schedule).toList();}
    public List<JobRoleDto> jobs(){return jobs.findAll().stream().filter(JobRole::isActive).map(j->new JobRoleDto(j.getId(),j.getName(),j.getDescription())).toList();}
    public List<RecommendationDto> recommendations(String job){var role=jobs.findByName(job).orElseThrow(()->new NoSuchElementException("직무를 찾을 수 없습니다."));return certs.findAll().stream().filter(c->c.isPublished()&&!c.isSampleData()&&c.getReviewStatus()==Enums.ReviewStatus.VERIFIED).filter(c->c.getJobRoles().stream().anyMatch(j->j.getJobRole().getId().equals(role.getId()))).map(c->{var link=c.getJobRoles().stream().filter(j->j.getJobRole().getId().equals(role.getId())).findFirst().orElseThrow();int score=(int)Math.round(link.getRelevance()*.5+(100-c.getDifficulty().ordinal()*20)*.2+Math.max(20,100-c.getPreparationWeeks()*3)*.1+70*.1+c.getPopularityScore()*.1);return new RecommendationDto(0,score,card(c),link.getReason(),null);}).sorted(Comparator.comparingInt(RecommendationDto::score).reversed()).limit(5).toList().stream().map(r->new RecommendationDto(0,r.score(),r.certification(),r.reason(),r.progressStatus())).toList();}
    private CertificationCardDto card(Certification c){return new CertificationCardDto(c.getId(),c.getName(),c.getSummary(),c.getCategory(),c.getType().name(),c.getOrganization(),c.getDifficulty().name(),c.getPreparationWeeks(),c.getJobRoles().stream().map(j->j.getJobRole().getName()).toList(),c.getSchedules().stream().filter(s->s.isActive()&&!s.isSampleData()&&s.getReviewStatus()==Enums.ReviewStatus.VERIFIED&&s.getStartsAt().isAfter(Instant.now())).min(Comparator.comparing(CertificationSchedule::getStartsAt)).map(this::schedule).orElse(null),c.isSampleData());}
    private ScheduleDto schedule(CertificationSchedule s){return new ScheduleDto(s.getId(),s.getCertification().getId(),s.getCertification().getName(),s.getExamRound(),s.getTitle(),s.getType().name(),s.getStartsAt(),s.getEndsAt(),s.isSampleData());}
    private String n(String v){return v==null||v.isBlank()?"정보 확인 필요":v;}
}
