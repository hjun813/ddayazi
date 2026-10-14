package com.certpath;

import com.certpath.domain.*;
import com.certpath.repository.*;
import com.certpath.service.*;
import com.certpath.service.CsvImportService.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class CsvRecommendedJobTests {
    @Autowired CsvImportService service;
    @Autowired CertificationRepository certs;
    @Autowired JobRoleRepository jobs;
    @Autowired CertService publicService;
    private static final String HEADERS="name,summary,category,type,organization,difficulty,preparationWeeks,officialSourceUrl,lastVerifiedAt,reviewStatus,published";

    private ImportPreview preview(String name,String summary,String job){
        String csv=HEADERS+(job==null?"":",recommendedJob")+"\n"+name+","+summary+",클라우드,VENDOR,공식기관,INTERMEDIATE,8,https://official.example/cert,2026-09-21,VERIFIED,true"+(job==null?"":","+job)+"\n";
        return service.validate(ImportKind.CERTIFICATION,csv.getBytes(StandardCharsets.UTF_8));
    }

    private ImportPreview previewMultiple(String name,String roles){
        String csv=HEADERS+",recommendedJobs\n"+name+",설명,클라우드,VENDOR,공식기관,INTERMEDIATE,8,https://official.example/cert,2026-09-21,VERIFIED,true,"+roles+"\n";
        return service.validate(ImportKind.CERTIFICATION,csv.getBytes(StandardCharsets.UTF_8));
    }

    @Test void multipleRolesUseExplicitScoresAndDefaultAndRemainIdempotent(){
        String roles=" 백엔드 개발 : 95 | 클라우드·DevOps | 프론트엔드 개발:88 ";
        var p=previewMultiple("CSV복수직무자격",roles);
        assertThat(p.canCommit()).isTrue();service.commit(p.token());certs.flush();
        var c=certs.findByNameIgnoreCase("CSV복수직무자격").orElseThrow();
        assertThat(c.getJobRoles()).extracting(CertificationJobRole::getRelevance).containsExactly(95,85,88);
        for(String role:new String[]{"백엔드 개발","클라우드·DevOps","프론트엔드 개발"})
            assertThat(publicService.recommendations(role)).anyMatch(r->r.certification().id().equals(c.getId()));
        var repeat=previewMultiple("CSV복수직무자격",roles);
        assertThat(repeat.rows().get(0).action()).isEqualTo(RowAction.UNCHANGED);
        service.commit(repeat.token());assertThat(c.getJobRoles()).hasSize(3);
    }

    @Test void explicitScoreUpdatePreservesOtherRolesReasonsAndUnspecifiedScores(){
        service.commit(previewMultiple("CSV점수갱신자격","백엔드 개발:95|정보보안:90").token());
        var c=certs.findByNameIgnoreCase("CSV점수갱신자격").orElseThrow();
        var backend=c.getJobRoles().get(0);backend.setReason("기존 맞춤 추천 사유");
        var p=previewMultiple("CSV점수갱신자격","백엔드 개발:80|클라우드·DevOps");
        assertThat(p.rows().get(0).changedFields()).containsExactly("recommendedJobs");
        assertThat(service.commit(p.token()).updated()).isEqualTo(1);certs.flush();
        assertThat(c.getJobRoles()).hasSize(3);
        assertThat(backend.getRelevance()).isEqualTo(80);assertThat(backend.getReason()).isEqualTo("기존 맞춤 추천 사유");
        assertThat(c.getJobRoles().get(1).getRelevance()).isEqualTo(90);
        assertThat(previewMultiple("CSV점수갱신자격","백엔드 개발").rows().get(0).action()).isEqualTo(RowAction.UNCHANGED);
        var blank=previewMultiple("CSV점수갱신자격","");
        assertThat(blank.rows().get(0).action()).isEqualTo(RowAction.UNCHANGED);
        service.commit(blank.token());assertThat(c.getJobRoles()).hasSize(3);
    }

    @Test void rejectsInvalidScoresEmptyEntriesDuplicateAndUnregisteredRoles(){
        var inactive=new JobRole("복수비활성직무","테스트");inactive.setActive(false);jobs.save(inactive);
        for(String value:new String[]{"백엔드 개발:-1","백엔드 개발:101","백엔드 개발:1.5","백엔드 개발:abc","백엔드 개발:","백엔드 개발:95:90","백엔드 개발||정보보안","백엔드 개발|","|백엔드 개발",":95","백엔드 개발:95|백엔드 개발:90","미등록직무:80","복수비활성직무:80"}){
            var p=previewMultiple("CSV복수오류자격",value);
            assertThat(p.canCommit()).as(value).isFalse();
            assertThat(p.rows().get(0).errors()).extracting(FieldError::field).contains("recommendedJobs");
            assertThatThrownBy(()->service.commit(p.token())).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(certs.findByNameIgnoreCase("CSV복수오류자격")).isEmpty();
    }

    @Test void acceptsBoundaryScores(){
        var p=previewMultiple("CSV경계점수자격","백엔드 개발:0|정보보안:100");
        assertThat(p.canCommit()).isTrue();service.commit(p.token());certs.flush();
        assertThat(certs.findByNameIgnoreCase("CSV경계점수자격").orElseThrow().getJobRoles())
            .extracting(CertificationJobRole::getRelevance).containsExactly(0,100);
    }

    @Test void rejectsTwoPopulatedRecommendationColumnsButAcceptsBlankLegacyColumn(){
        String row="CSV컬럼충돌자격,설명,클라우드,VENDOR,공식기관,INTERMEDIATE,8,https://official.example/cert,2026-09-21,VERIFIED,true,";
        String head=HEADERS+",recommendedJob,recommendedJobs\n";
        var invalid=service.validate(ImportKind.CERTIFICATION,(head+row+"백엔드 개발,정보보안:95\n").getBytes(StandardCharsets.UTF_8));
        assertThat(invalid.canCommit()).isFalse();
        assertThat(invalid.rows().get(0).errors()).extracting(FieldError::field).contains("recommendedJobs");
        var valid=service.validate(ImportKind.CERTIFICATION,(head+row+",정보보안:95\n").getBytes(StandardCharsets.UTF_8));
        assertThat(valid.canCommit()).isTrue();service.commit(valid.token());
        assertThat(certs.findByNameIgnoreCase("CSV컬럼충돌자격").orElseThrow().getJobRoles()).hasSize(1);
    }

    @Test void downloadableTemplateSupportsMultipleRoles(){
        var p=service.validate(ImportKind.CERTIFICATION,service.template(ImportKind.CERTIFICATION).getBytes(StandardCharsets.UTF_8));
        assertThat(p.canCommit()).isTrue();
        assertThat(p.rows().get(0).values().get("recommendedJobs")).isEqualTo("백엔드 개발:95|프론트엔드 개발:88");
    }

    @Test void newCertificationAppearsInRecommendationsAndRepeatedImportAddsNoDuplicates(){
        var p=preview("CSV추천자격","설명","백엔드 개발");
        assertThat(p.canCommit()).isTrue();assertThat(service.commit(p.token()).created()).isEqualTo(1);
        certs.flush();
        var cert=certs.findByNameIgnoreCase("CSV추천자격").orElseThrow();
        assertThat(cert.getJobRoles()).hasSize(1);
        assertThat(cert.getJobRoles().get(0).getRelevance()).isEqualTo(85);
        assertThat(publicService.recommendations("백엔드 개발")).anyMatch(r->r.certification().id().equals(cert.getId()));
        var repeat=preview("CSV추천자격","설명","백엔드 개발");
        assertThat(repeat.rows().get(0).action()).isEqualTo(RowAction.UNCHANGED);
        assertThat(service.commit(repeat.token()).unchanged()).isEqualTo(1);
        assertThat(cert.getJobRoles()).hasSize(1);
    }

    @Test void mappingOnlyChangeIsDetectedAndPreservesExistingMappingsAndScores(){
        service.commit(preview("CSV직무추가자격","설명","백엔드 개발").token());
        var cert=certs.findByNameIgnoreCase("CSV직무추가자격").orElseThrow();
        cert.getJobRoles().get(0).setRelevance(94);
        var addition=preview("CSV직무추가자격","설명","정보보안");
        assertThat(addition.rows().get(0).action()).isEqualTo(RowAction.UPDATE);
        assertThat(addition.rows().get(0).changedFields()).containsExactly("recommendedJob");
        assertThat(service.commit(addition.token()).updated()).isEqualTo(1);
        certs.flush();
        assertThat(cert.getJobRoles()).extracting(link->link.getJobRole().getName()).containsExactlyInAnyOrder("백엔드 개발","정보보안");
        assertThat(cert.getJobRoles().get(0).getRelevance()).isEqualTo(94);
        assertThat(preview("CSV직무추가자격","설명","백엔드 개발").rows().get(0).action()).isEqualTo(RowAction.UNCHANGED);
    }

    @Test void rejectsUnknownAndInactiveJobsBeforeCommit(){
        var inactive=new JobRole("비활성직무","테스트");inactive.setActive(false);jobs.save(inactive);
        for(String name:new String[]{"등록되지않은직무","비활성직무"}){
            var p=preview("CSV오류직무자격","설명",name);
            assertThat(p.canCommit()).isFalse();
            assertThat(p.rows().get(0).errors()).extracting(FieldError::field).contains("recommendedJob");
            assertThatThrownBy(()->service.commit(p.token())).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(certs.findByNameIgnoreCase("CSV오류직무자격")).isEmpty();
    }

    @Test void omittedAndBlankJobPreserveDetailsCollectionsAndExistingMapping(){
        service.commit(preview("CSV상세보존자격","설명","백엔드 개발").token());
        var cert=certs.findByNameIgnoreCase("CSV상세보존자격").orElseThrow();
        cert.setExamMethod("실기");cert.setPassingCriteria("75점");cert.setEligibility("경력 2년");
        cert.setCost("100000원");cert.setValidity("3년");cert.setRegistrationUrl("https://official.example/register");
        cert.getSubjects().add(new CertificationSubject(cert,"실무 과목",1));
        var resource=new StudyResource();resource.setCertification(cert);resource.setTitle("공식 가이드");resource.setUrl("https://official.example/guide");cert.getResources().add(resource);
        cert.getSchedules().add(new CertificationSchedule(cert,"기존 시험",Enums.ScheduleType.EXAM,java.time.Instant.parse("2027-01-01T00:00:00Z")));
        for(String job:new String[]{null,""}){
            service.commit(preview("CSV상세보존자격",job==null?"수정 설명":"재수정 설명",job).token());
            certs.flush();
            assertThat(cert.getJobRoles()).hasSize(1);
            assertThat(cert.getSubjects()).extracting(CertificationSubject::getName).containsExactly("실무 과목");
            assertThat(cert.getResources()).containsExactly(resource);
            assertThat(cert.getSchedules()).hasSize(1);
            assertThat(cert.getExamMethod()).isEqualTo("실기");assertThat(cert.getPassingCriteria()).isEqualTo("75점");
            assertThat(cert.getEligibility()).isEqualTo("경력 2년");assertThat(cert.getCost()).isEqualTo("100000원");
            assertThat(cert.getValidity()).isEqualTo("3년");assertThat(cert.getRegistrationUrl()).endsWith("/register");
        }
    }

    @Test void otherwiseUnchangedSampleIsDetectedForConversionToImportedData(){
        service.commit(preview("CSV샘플전환자격","설명",null).token());
        var cert=certs.findByNameIgnoreCase("CSV샘플전환자격").orElseThrow();cert.setSampleData(true);
        var p=preview("CSV샘플전환자격","설명",null);
        assertThat(p.rows().get(0).changedFields()).containsExactly("sampleData");
        service.commit(p.token());assertThat(cert.isSampleData()).isFalse();
    }
}
