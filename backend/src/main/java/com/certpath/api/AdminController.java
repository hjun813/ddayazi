package com.certpath.api;

import com.certpath.api.ApiModels.ApiResponse;
import com.certpath.domain.*;
import com.certpath.domain.Enums.*;
import com.certpath.repository.*;
import com.certpath.service.CsvImportService;
import com.certpath.service.CsvImportService.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CertificationRepository certifications;
    private final ScheduleRepository schedules;
    private final CsvImportService imports;

    public record CertificationRequest(@NotBlank String name,@NotBlank String summary,@NotBlank String category,
        @NotNull CertificationType type,@NotBlank String organization,@NotNull Difficulty difficulty,
        @Min(1) Integer preparationWeeks,@NotBlank String officialUrl,@NotNull LocalDate lastVerifiedAt,
        @NotNull ReviewStatus reviewStatus,String registrationUrl,Boolean published) {}
    public record ScheduleRequest(@NotNull Long certificationId,@NotBlank String examRound,@NotBlank String title,
        @NotNull ScheduleType type,@NotNull Instant startsAt,Instant endsAt,@NotBlank String sourceUrl,
        @NotNull LocalDate lastVerifiedAt,@NotNull ReviewStatus reviewStatus) {}

    @PostMapping("/imports/certifications/validate")
    public ApiResponse<ImportPreview> validateCertifications(@RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(imports.validate(ImportKind.CERTIFICATION,file.getBytes()));
    }
    @PostMapping("/imports/schedules/validate")
    public ApiResponse<ImportPreview> validateSchedules(@RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(imports.validate(ImportKind.SCHEDULE,file.getBytes()));
    }
    @PostMapping("/imports/{token}/commit")
    public ApiResponse<CommitResult> commit(@PathVariable String token){return ApiResponse.ok(imports.commit(token));}
    @GetMapping(value="/imports/templates/{kind}",produces="text/csv;charset=UTF-8")
    public ResponseEntity<String> template(@PathVariable ImportKind kind){
        String name=kind==ImportKind.CERTIFICATION?"certifications.csv":"schedules.csv";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+name).body("\uFEFF"+imports.template(kind));
    }

    @PostMapping("/certifications") @Transactional
    public ApiResponse<Long> create(@Valid @RequestBody CertificationRequest r){var c=new Certification();apply(c,r);return ApiResponse.ok(certifications.save(c).getId());}
    @PutMapping("/certifications/{id}") @Transactional
    public ApiResponse<Long> update(@PathVariable Long id,@Valid @RequestBody CertificationRequest r){var c=certifications.findById(id).orElseThrow();apply(c,r);return ApiResponse.ok(c.getId());}
    @PatchMapping("/certifications/{id}/deactivate") @Transactional
    public ApiResponse<Void> deactivate(@PathVariable Long id){certifications.findById(id).orElseThrow().setPublished(false);return ApiResponse.ok(null);}

    @PostMapping("/schedules") @Transactional
    public ApiResponse<Long> createSchedule(@Valid @RequestBody ScheduleRequest r){
        var c=certifications.findById(r.certificationId()).orElseThrow();
        if(schedules.findByCertificationIdAndExamRoundAndType(c.getId(),r.examRound(),r.type()).isPresent())throw new IllegalArgumentException("동일한 자격증·회차·일정 유형이 이미 존재합니다.");
        var s=new CertificationSchedule(c,r.title(),r.type(),r.startsAt());s.setExamRound(r.examRound());s.setEndsAt(r.endsAt());s.setSourceUrl(r.sourceUrl());s.setLastVerifiedAt(r.lastVerifiedAt());s.setReviewStatus(r.reviewStatus());s.setSampleData(false);return ApiResponse.ok(schedules.save(s).getId());
    }
    @PatchMapping("/schedules/{id}/deactivate") @Transactional
    public ApiResponse<Void> deactivateSchedule(@PathVariable Long id){schedules.findById(id).orElseThrow().setActive(false);return ApiResponse.ok(null);}

    private void apply(Certification c,CertificationRequest r){c.setName(r.name());c.setSummary(r.summary());c.setCategory(r.category());c.setType(r.type());c.setOrganization(r.organization());c.setDifficulty(r.difficulty());c.setPreparationWeeks(r.preparationWeeks());c.setOfficialUrl(r.officialUrl());c.setLastVerifiedAt(r.lastVerifiedAt());c.setReviewStatus(r.reviewStatus());c.setRegistrationUrl(r.registrationUrl());c.setSampleData(false);c.setPublished(r.published()==null||r.published());}
}