import {beforeEach,describe,expect,it,vi} from 'vitest';
import {renderToStaticMarkup} from 'react-dom/server';
import {MemoryRouter} from 'react-router-dom';
import App from './App';

const fixtures=vi.hoisted(()=>({detail:{
  id:99,name:'검증 자격증',summary:'실제 설명',category:'개발',type:'VENDOR',organization:'기관',difficulty:'BEGINNER',preparationWeeks:4,sampleData:false,
  examMethod:'실기 평가',passingCriteria:'75점 이상',eligibility:'경력 2년',cost:'123000원',validity:'3년',officialUrl:'https://example.com/official',lastVerifiedAt:'2026-10-13',
  subjects:['네트워크 설계','보안 운영'],recommendedJobs:[{jobRoleId:7,jobRole:'백엔드 개발',score:83,reason:'서버 운영 역량 검증',level:'BEGINNER'}],
  schedules:[{id:8,title:'실제 시험',startsAt:'2099-01-01T00:00:00Z'}],resources:[]
},recommendations:[{score:73,reason:'API 추천 사유',certification:{id:99,name:'검증 자격증',category:'개발',difficulty:'BEGINNER',preparationWeeks:4}}]}));
vi.mock('@tanstack/react-query',()=>({useQuery:({queryKey}:{queryKey:string[]})=>({data:queryKey[0]==='certification'?fixtures.detail:queryKey[0]==='recommendations'?fixtures.recommendations:{content:[]}})}));
vi.mock('./auth',()=>({useSession:()=>null,clearSession:vi.fn(),saveSession:vi.fn(),getToken:()=>null}));
vi.mock('./saved',()=>({useSaved:()=>({saved:{},update:vi.fn()})}));

beforeEach(()=>{
  vi.stubGlobal('localStorage',{getItem:()=>null});
  vi.stubGlobal('window',{matchMedia:()=>({matches:false})});
  vi.stubGlobal('location',{search:''});
});
const render=(path:string)=>renderToStaticMarkup(<MemoryRouter initialEntries={[path]}><App/></MemoryRouter>);
describe('API data bindings',()=>{
  it('renders detail fields, job objects, subjects, schedules and official link',()=>{
    const html=render('/certifications/99');
    for(const value of ['실기 평가','75점 이상','경력 2년','123000원','3년','2026-10-13','네트워크 설계','보안 운영','백엔드 개발','83점','서버 운영 역량 검증','실제 시험'])expect(html).toContain(value);
    expect(html).toContain('href="https://example.com/official" target="_blank" rel="noopener noreferrer"');
    expect(html).not.toContain('핵심 이론');
  });
  it('renders the API recommendation score and reason',()=>{
    const html=render('/recommendations');
    expect(html).toContain('<strong>73</strong>');expect(html).toContain('API 추천 사유');
  });
  it('uses enum values for difficulty options',()=>{
    const html=render('/certifications');
    for(const value of ['BEGINNER','INTERMEDIATE','ADVANCED'])expect(html).toContain(`value="${value}"`);
  });
});
