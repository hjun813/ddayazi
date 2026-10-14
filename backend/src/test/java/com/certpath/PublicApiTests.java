package com.certpath;

import com.certpath.domain.*;
import com.certpath.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class PublicApiTests {
    @Autowired MockMvc mvc;
    @Autowired CertificationRepository certs;
    @Autowired ScheduleRepository schedules;

    @Test void anonymousRecommendationsIncludeScoresAndReasons() throws Exception {
        mvc.perform(get("/api/recommendations").param("job","백엔드 개발"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data",hasSize(2)))
            .andExpect(jsonPath("$.data[0].score",greaterThan(0)))
            .andExpect(jsonPath("$.data[0].reason",not(emptyOrNullString())));
    }

    @Test void enumDifficultyFiltersWork() throws Exception {
        for(var difficulty:Enums.Difficulty.values()) {
            mvc.perform(get("/api/certifications").param("difficulty",difficulty.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content",not(empty())))
                .andExpect(jsonPath("$.data.content[*].difficulty",everyItem(is(difficulty.name()))));
        }
    }

    @Test void recommendationsEnforceEveryCertificationGate() throws Exception {
        var c=certs.findByNameIgnoreCase("정보처리기사").orElseThrow();
        c.setSampleData(true); assertHiddenRecommendation(c);
        c.setSampleData(false); c.setPublished(false); assertHiddenRecommendation(c);
        c.setPublished(true); c.setReviewStatus(Enums.ReviewStatus.DRAFT); assertHiddenRecommendation(c);
    }
    private void assertHiddenRecommendation(Certification c) throws Exception {
        certs.flush();
        mvc.perform(get("/api/recommendations").param("job","백엔드 개발"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].certification.id",not(hasItem(c.getId().intValue()))));
    }

    @Test void schedulesEnforceScheduleAndParentGatesAndDateRange() throws Exception {
        var c=certs.findByNameIgnoreCase("정보처리기사").orElseThrow();
        var s=c.getSchedules().get(0);
        var from=s.getStartsAt(); var to=from;
        assertSchedule(s,from,to,true);
        assertSchedule(s,from.plusSeconds(1),from.plusSeconds(2),false);
        s.setActive(false); assertSchedule(s,from,to,false);
        s.setActive(true); s.setSampleData(true); assertSchedule(s,from,to,false);
        s.setSampleData(false); s.setReviewStatus(Enums.ReviewStatus.DRAFT); assertSchedule(s,from,to,false);
        s.setReviewStatus(Enums.ReviewStatus.VERIFIED); c.setPublished(false); assertSchedule(s,from,to,false);
        c.setPublished(true); c.setSampleData(true); assertSchedule(s,from,to,false);
        c.setSampleData(false); c.setReviewStatus(Enums.ReviewStatus.DRAFT); assertSchedule(s,from,to,false);
    }
    private void assertSchedule(CertificationSchedule s,Instant from,Instant to,boolean visible) throws Exception {
        schedules.flush();
        mvc.perform(get("/api/schedules").param("from",from.toString()).param("to",to.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[*].id",visible?hasItem(s.getId().intValue()):not(hasItem(s.getId().intValue()))));
    }
}
