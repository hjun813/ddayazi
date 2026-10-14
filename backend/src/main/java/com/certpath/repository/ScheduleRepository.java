package com.certpath.repository;
import com.certpath.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;
public interface ScheduleRepository extends JpaRepository<CertificationSchedule,Long>{
 @Query("select s from CertificationSchedule s join fetch s.certification c where s.startsAt between :from and :to and s.active = true and s.sampleData = false and s.reviewStatus = com.certpath.domain.Enums$ReviewStatus.VERIFIED and c.published = true and c.sampleData = false and c.reviewStatus = com.certpath.domain.Enums$ReviewStatus.VERIFIED order by s.startsAt")
 List<CertificationSchedule> findPublicBetween(@Param("from") Instant from,@Param("to") Instant to);
 Optional<CertificationSchedule> findByCertificationIdAndExamRoundAndType(Long certificationId,String examRound,Enums.ScheduleType type);
}
