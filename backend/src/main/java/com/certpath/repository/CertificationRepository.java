package com.certpath.repository;
import com.certpath.domain.Certification;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
import java.util.List;
import org.springframework.data.repository.query.Param;
public interface CertificationRepository extends JpaRepository<Certification,Long>, JpaSpecificationExecutor<Certification>{
 Optional<Certification> findByNameIgnoreCase(String name);
 @Query("select c from Certification c join c.jobRoles link join link.jobRole job where job.name = :job and c.published = true and c.sampleData = false and c.reviewStatus = com.certpath.domain.Enums$ReviewStatus.VERIFIED")
 List<Certification> findPublicRecommendations(@Param("job") String job);
}
