export type Schedule={id:number;certificationId:number;certificationName:string;examRound?:string;title:string;type:string;startsAt:string;endsAt?:string;sampleData:boolean};
export type Certification={id:number;name:string;summary:string;category:string;type:string;organization:string;difficulty:string;preparationWeeks:number;recommendedJobs:string[];nextSchedule?:Schedule;sampleData:boolean};
export type Recommendation={rank:number;score:number;certification:Certification;reason:string;progressStatus?:string};
export type JobRole={id:number;name:string;description:string};
export type ApiResponse<T>={success:boolean;data:T;message?:string};
export type Page<T>={content:T[];totalElements:number;totalPages:number;number:number};
