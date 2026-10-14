package com.certpath;

import com.certpath.domain.*;
import com.certpath.repository.CertificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class ResourceDirectoryTests {
    @Autowired MockMvc mvc;
    @Autowired CertificationRepository certs;

    @Test void servesOfficialPortalsAndOnlyActiveOfficialResourcesAnonymously()throws Exception{
        var c=certs.findByNameIgnoreCase("SQLD").orElseThrow();
        c.setOfficialUrl("https://official.example/exam");
        add(c,"공식 가이드",true,true);add(c,"비공식 자료",false,true);add(c,"비활성 자료",true,false);
        certs.flush();
        mvc.perform(get("/api/certifications/resources")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].officialUrl",hasItem("https://official.example/exam")))
            .andExpect(jsonPath("$.data[*].resources[*].title",hasItem("공식 가이드")))
            .andExpect(jsonPath("$.data[*].resources[*].title",not(hasItem("비공식 자료"))))
            .andExpect(jsonPath("$.data[*].resources[*].title",not(hasItem("비활성 자료"))));
    }
    @Test void rejectsSampleDraftAndUnpublishedParents()throws Exception{
        var c=certs.findByNameIgnoreCase("SQLD").orElseThrow();c.setOfficialUrl("https://official.example/exam");
        c.setSampleData(true);assertHidden(c);
        c.setSampleData(false);c.setPublished(false);assertHidden(c);
        c.setPublished(true);c.setReviewStatus(Enums.ReviewStatus.DRAFT);assertHidden(c);
    }
    private void assertHidden(Certification c)throws Exception{
        certs.flush();mvc.perform(get("/api/certifications/resources")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].id",not(hasItem(c.getId().intValue()))));
    }
    private void add(Certification c,String title,boolean official,boolean active){
        var r=new StudyResource();r.setCertification(c);r.setTitle(title);r.setUrl("https://official.example/guide");
        r.setType(Enums.ResourceType.OFFICIAL_GUIDE);r.setOfficial(official);r.setActive(active);c.getResources().add(r);
    }
}
