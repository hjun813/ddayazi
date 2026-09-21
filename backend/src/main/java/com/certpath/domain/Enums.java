package com.certpath.domain;

public final class Enums {
    private Enums() {}
    public enum Role { USER, ADMIN }
    public enum ReviewStatus { DRAFT, VERIFIED, OUTDATED }
    public enum CertificationType { NATIONAL, PRIVATE, VENDOR }
    public enum Difficulty { BEGINNER, INTERMEDIATE, ADVANCED }
    public enum ScheduleType { REGISTRATION_OPEN, REGISTRATION_CLOSE, EXAM, RESULT, RENEWAL }
    public enum ProgressStatus { INTERESTED, PLANNED, STUDYING, REGISTERED, TAKEN, PASSED, FAILED }
    public enum ResourceType { OFFICIAL_GUIDE, CBT, PAST_EXAM, FREE_COURSE, PAID_COURSE, BOOK, COMMUNITY, LAB }
}
