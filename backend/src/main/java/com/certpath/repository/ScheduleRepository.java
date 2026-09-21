package com.certpath.repository;
import com.certpath.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.*;
public interface ScheduleRepository extends JpaRepository<CertificationSchedule,Long>{
 List<CertificationSchedule> findByStartsAtBetweenOrderByStartsAt(Instant from,Instant to);
 Optional<CertificationSchedule> findByCertificationIdAndExamRoundAndType(Long certificationId,String examRound,Enums.ScheduleType type);
}