package com.certpath.repository;
import com.certpath.domain.JobRole;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface JobRoleRepository extends JpaRepository<JobRole,Long>{Optional<JobRole> findByName(String name);}
