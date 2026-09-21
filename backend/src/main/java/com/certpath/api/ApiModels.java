package com.certpath.api;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class ApiModels {
    private ApiModels(){}
    public record ApiResponse<T>(boolean success,T data,String message){public static <T> ApiResponse<T> ok(T data){return new ApiResponse<>(true,data,null);}}
    public record SignupRequest(@Email @NotBlank String email,@Size(min=8,max=72) String password,@NotBlank String nickname,Long desiredJobRoleId){}
    public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){}
    public record AuthResponse(String accessToken,String tokenType,long expiresIn,String nickname,String role){}
    public record JobRoleDto(Long id,String name,String description){}
    public record ScheduleDto(Long id,Long certificationId,String certificationName,String examRound,String title,String type,Instant startsAt,Instant endsAt,boolean sampleData){}
    public record CertificationCardDto(Long id,String name,String summary,String category,String type,String organization,String difficulty,Integer preparationWeeks,List<String> recommendedJobs,ScheduleDto nextSchedule,boolean sampleData){}
    public record ResourceDto(Long id,String title,String description,String type,String url,boolean free,boolean official,LocalDate lastCheckedAt){}
    public record CertificationDetailDto(Long id,String name,String summary,String category,String type,String organization,String difficulty,Integer preparationWeeks,String examMethod,List<String> subjects,String passingCriteria,String eligibility,String cost,String validity,String officialUrl,String registrationUrl,LocalDate lastVerifiedAt,List<JobRecommendationDto> recommendedJobs,List<ScheduleDto> schedules,List<ResourceDto> resources,boolean sampleData){}
    public record JobRecommendationDto(Long jobRoleId,String jobRole,int score,String reason,String level){}
    public record RecommendationDto(int rank,int score,CertificationCardDto certification,String reason,String progressStatus){}
    public record StatusRequest(@NotBlank String status){}
    public record UserCertificationDto(Long certificationId,String certificationName,String status,ScheduleDto nextSchedule){}
}
