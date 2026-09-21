package com.certpath.service;

import com.certpath.api.ApiModels.*;
import com.certpath.domain.*;
import com.certpath.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class MyService {
    private final UserRepository users;private final CertificationRepository certs;private final UserCertificationRepository saved;
    @Transactional(readOnly=true) public List<UserCertificationDto> list(String email){var u=user(email);return saved.findByUserId(u.getId()).stream().map(x->new UserCertificationDto(x.getCertification().getId(),x.getCertification().getName(),x.getStatus().name(),null)).toList();}
    @Transactional public UserCertificationDto add(String email,Long certId){var u=user(email);if(saved.findByUserIdAndCertificationId(u.getId(),certId).isPresent())throw new IllegalArgumentException("이미 관심 자격증에 등록되어 있습니다.");var x=saved.save(new UserCertification(u,certs.findById(certId).orElseThrow(),Enums.ProgressStatus.INTERESTED));return new UserCertificationDto(certId,x.getCertification().getName(),x.getStatus().name(),null);}
    @Transactional public UserCertificationDto status(String email,Long certId,String status){var u=user(email);var x=saved.findByUserIdAndCertificationId(u.getId(),certId).orElseThrow();x.setStatus(Enums.ProgressStatus.valueOf(status));return new UserCertificationDto(certId,x.getCertification().getName(),x.getStatus().name(),null);}
    @Transactional public void remove(String email,Long certId){var u=user(email);saved.delete(saved.findByUserIdAndCertificationId(u.getId(),certId).orElseThrow());}
    private User user(String e){return users.findByEmail(e).orElseThrow();}
}
