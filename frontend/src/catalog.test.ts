import {describe,expect,it,vi,afterEach} from 'vitest';
import {filterCatalog} from './catalog';
import {api} from './api';
import type {Certification} from './types';
vi.mock('./auth',()=>({getToken:()=>null,clearSession:vi.fn()}));
const certs:Certification[]=[
  {id:1,name:'SQLD',category:'DB',summary:'',type:'PRIVATE',organization:'기관',difficulty:'BEGINNER',preparationWeeks:6,recommendedJobs:['백엔드 개발'],sampleData:false},
  {id:2,name:'CKA',category:'클라우드',summary:'',type:'VENDOR',organization:'기관',difficulty:'ADVANCED',preparationWeeks:12,recommendedJobs:['클라우드·DevOps'],sampleData:false}
];
afterEach(()=>vi.unstubAllGlobals());
describe('cached certification catalog',()=>{
  it('filters and sorts locally without mutating server recommendation order',()=>{
    const filters={query:' sqld ',job:'백엔드 개발',type:'PRIVATE',difficulty:'BEGINNER',sort:'recommended'};
    expect(filterCatalog(certs,filters).map(c=>c.id)).toEqual([1]);
    expect(filterCatalog(certs,{...filters,difficulty:'ADVANCED'})).toEqual([]);
    expect(filterCatalog(certs,{query:'',job:'',type:'',difficulty:'',sort:'name'}).map(c=>c.id)).toEqual([2,1]);
    expect(certs.map(c=>c.id)).toEqual([1,2]);
  });
  it('fetches all pages and reuses the result for filter changes',async()=>{
    const fetch=vi.fn().mockResolvedValueOnce({ok:true,status:200,json:async()=>({data:{content:[certs[0]],totalPages:2}})}).mockResolvedValueOnce({ok:true,status:200,json:async()=>({data:{content:[certs[1]],totalPages:2}})});
    vi.stubGlobal('fetch',fetch);
    const controller=new AbortController();const data=await api.allCertifications(controller.signal);
    expect(data).toHaveLength(2);expect(fetch.mock.calls[1][0]).toContain('page=1');
    for(const difficulty of ['BEGINNER','ADVANCED',''])filterCatalog(data,{query:'',job:'',type:'',difficulty,sort:'recommended'});
    expect(fetch).toHaveBeenCalledTimes(2);expect(fetch.mock.calls[0][1].signal).toBe(controller.signal);
  });
  it('rejects incomplete results if a later page fails',async()=>{
    vi.stubGlobal('fetch',vi.fn().mockResolvedValueOnce({ok:true,status:200,json:async()=>({data:{content:[certs[0]],totalPages:2}})}).mockResolvedValueOnce({ok:false,status:500,json:async()=>({message:'서버 오류'})}));
    await expect(api.allCertifications()).rejects.toThrow('서버 오류');
  });
});
