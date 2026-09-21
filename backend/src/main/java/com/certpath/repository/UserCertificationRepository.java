package com.certpath.repository;
import com.certpath.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserCertificationRepository extends JpaRepository<UserCertification,Long>{List<UserCertification> findByUserId(Long userId);Optional<UserCertification> findByUserIdAndCertificationId(Long userId,Long certId);}
