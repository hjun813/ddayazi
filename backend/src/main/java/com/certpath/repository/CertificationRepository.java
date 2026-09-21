package com.certpath.repository;
import com.certpath.domain.Certification;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface CertificationRepository extends JpaRepository<Certification,Long>, JpaSpecificationExecutor<Certification>{Optional<Certification> findByNameIgnoreCase(String name);}