import {useQuery} from '@tanstack/react-query';
import {api} from './api';
import type {Certification} from './types';

export const catalogQuery={queryKey:['public-certification-catalog'],queryFn:({signal}:{signal:AbortSignal})=>api.allCertifications(signal),staleTime:5*60*1000};
export function useCatalog(){return useQuery(catalogQuery);}
export type CatalogFilters={query:string;job:string;type:string;difficulty:string;sort:string};
export function filterCatalog(certs:Certification[],filters:CatalogFilters){
  const query=filters.query.trim().toLocaleLowerCase();
  const result=certs.filter(c=>(!query||c.name.toLocaleLowerCase().includes(query))&&(!filters.job||c.recommendedJobs.includes(filters.job))&&(!filters.type||c.type===filters.type)&&(!filters.difficulty||c.difficulty===filters.difficulty));
  if(filters.sort==='name')result.sort((a,b)=>a.name.localeCompare(b.name,'ko'));
  if(filters.sort==='duration')result.sort((a,b)=>a.preparationWeeks-b.preparationWeeks||a.name.localeCompare(b.name,'ko'));
  return result;
}
