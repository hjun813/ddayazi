export type Schedule={id:number;certificationId:number;certificationName:string;examRound?:string;title:string;type:string;startsAt:string;endsAt?:string;sampleData:boolean};
export type Certification={id:number;name:string;summary:string;category:string;type:string;organization:string;difficulty:string;preparationWeeks:number;recommendedJobs:string[];nextSchedule?:Schedule;sampleData:boolean};
export type Recommendation={rank:number;score:number;certification:Certification;reason:string;progressStatus?:string};
export type JobRecommendation={jobRoleId:number;jobRole:string;score:number;reason:string;level:string};
export type Resource={id:number;title:string;description:string;type:string;url:string;free:boolean;official:boolean;lastCheckedAt?:string};
export type CertificationDetail=Omit<Certification,'recommendedJobs'|'nextSchedule'>&{examMethod:string|null;subjects:string[];passingCriteria:string;eligibility:string;cost:string;validity:string;officialUrl:string|null;registrationUrl:string|null;lastVerifiedAt:string|null;recommendedJobs:JobRecommendation[];schedules:Schedule[];resources:Resource[]};
export type JobRole={id:number;name:string;description:string};
export type ApiResponse<T>={success:boolean;data:T;message?:string};
export type Page<T>={content:T[];totalElements:number;totalPages:number;number:number};

export type ResourceGroup={id:number;name:string;category:string;organization:string;officialUrl:string|null;lastVerifiedAt:string|null;resources:Resource[]};
