import type {ApiResponse,Certification,JobRole,Page,Recommendation,Schedule} from './types';
const BASE=import.meta.env.VITE_API_URL||'http://localhost:8080/api';
async function call<T>(path:string,init?:RequestInit){
  const token=localStorage.getItem('ddayaji-token');
  const isForm=init?.body instanceof FormData;
  const res=await fetch(BASE+path,{...init,headers:{...(isForm?{}:{'Content-Type':'application/json'}),...(token?{Authorization:`Bearer ${token}`}:{})}});
  const json:ApiResponse<T>=await res.json();
  if(!res.ok)throw new Error(json.message||'요청을 처리하지 못했습니다.');
  return json.data;
}
export type ImportError={field:string;message:string};
export type ImportRow={rowNumber:number;key:string;action:'CREATE'|'UPDATE'|'UNCHANGED'|'ERROR';changedFields:string[];errors:ImportError[];values:Record<string,string>};
export type ImportPreview={token:string;kind:'CERTIFICATION'|'SCHEDULE';totalRows:number;validRows:number;errorRows:number;createCount:number;updateCount:number;unchangedCount:number;canCommit:boolean;rows:ImportRow[]};
export const api={
  certifications:(q='')=>call<Page<Certification>>('/certifications'+q),detail:(id:string)=>call<any>(`/certifications/${id}`),jobs:()=>call<JobRole[]>('/job-roles'),schedules:(from:string,to:string)=>call<Schedule[]>(`/schedules?from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`),recommendations:(job:string)=>call<Recommendation[]>(`/recommendations?job=${encodeURIComponent(job)}`),
  login:(email:string,password:string)=>call<any>('/auth/login',{method:'POST',body:JSON.stringify({email,password})}),signup:(body:unknown)=>call<any>('/auth/signup',{method:'POST',body:JSON.stringify(body)}),profile:()=>call<{nickname:string;desiredJobRole:string|null}>('/me/profile'),mine:()=>call<any[]>('/me/certifications'),add:(id:number)=>call<any>(`/me/certifications/${id}`,{method:'POST'}),status:(id:number,status:string)=>call<any>(`/me/certifications/${id}/status`,{method:'PATCH',body:JSON.stringify({status})}),
  validateImport:(kind:'certifications'|'schedules',file:File)=>{const form=new FormData();form.append('file',file);return call<ImportPreview>(`/admin/imports/${kind}/validate`,{method:'POST',body:form});},
  commitImport:(token:string)=>call<{kind:string;created:number;updated:number;unchanged:number}>(`/admin/imports/${token}/commit`,{method:'POST'}),
  downloadTemplate:async(kind:'CERTIFICATION'|'SCHEDULE')=>{const token=localStorage.getItem('ddayaji-token');const res=await fetch(`${BASE}/admin/imports/templates/${kind}`,{headers:token?{Authorization:`Bearer ${token}`}:{}});if(res.status===401||res.status===403)throw new Error('관리자 로그인이 필요합니다.');if(!res.ok)throw new Error('템플릿을 내려받지 못했습니다.');const blob=await res.blob();const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=kind==='CERTIFICATION'?'certifications.csv':'schedules.csv';document.body.appendChild(a);a.click();a.remove();window.setTimeout(()=>URL.revokeObjectURL(url),1000);}
};