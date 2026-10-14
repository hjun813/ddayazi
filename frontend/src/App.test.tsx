import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';
import {renderToStaticMarkup} from 'react-dom/server';
import {MemoryRouter} from 'react-router-dom';
import App from './App';

const fixtures=vi.hoisted(()=>({detail:{
  id:99,name:'검증 자격증',summary:'실제 설명',category:'개발',type:'VENDOR',organization:'기관',difficulty:'BEGINNER',preparationWeeks:4,sampleData:false,
  examMethod:'실기 평가',passingCriteria:'75점 이상',eligibility:'경력 2년',cost:'123000원',validity:'3년',officialUrl:'https://example.com/official',lastVerifiedAt:'2026-10-13',
  subjects:['네트워크 설계','보안 운영'],recommendedJobs:[{jobRoleId:7,jobRole:'백엔드 개발',score:83,reason:'서버 운영 역량 검증',level:'BEGINNER'}],
  schedules:[{id:8,title:'실제 시험',startsAt:'2099-01-01T00:00:00Z'}],resources:[]
},recommendations:[{score:73,reason:'API 추천 사유',certification:{id:99,name:'검증 자격증',category:'개발',difficulty:'BEGINNER',preparationWeeks:4}}],
  schedules:Array.from({length:4},(_,i)=>({id:i+1,certificationId:i+1,certificationName:`동일 날짜 자격${i+1}`,title:'시험',type:'EXAM',startsAt:'2026-10-16T00:00:00Z'})),
  resources:[{id:99,name:'공식 자료 자격',category:'개발',organization:'기관',officialUrl:'https://example.com/exam',lastVerifiedAt:'2026-10-13',resources:[{id:5,title:'공식 학습 가이드',url:'https://example.com/guide',free:true},{id:6,title:'잘못된 링크',url:'javascript:alert(1)'}]}]
}));
vi.mock('@tanstack/react-query',()=>({useQueryClient:()=>({invalidateQueries:vi.fn()}),useQuery:({queryKey}:{queryKey:string[]})=>({data:queryKey[0]==='certification'?fixtures.detail:queryKey[0]==='recommendations'?fixtures.recommendations:queryKey[0]==='schedules'?fixtures.schedules:queryKey[0]==='public-resource-directory'?fixtures.resources:[]})}));
vi.mock('./auth',()=>({useSession:()=>null,clearSession:vi.fn(),saveSession:vi.fn(),getToken:()=>null}));
vi.mock('./saved',()=>({useSaved:()=>({saved:{},update:vi.fn()})}));

beforeEach(()=>{
  vi.useFakeTimers();vi.setSystemTime(new Date('2026-10-14T00:00:00Z'));
  vi.stubGlobal('localStorage',{getItem:()=>null});
  vi.stubGlobal('window',{matchMedia:()=>({matches:false})});
  vi.stubGlobal('location',{search:''});
});
afterEach(()=>vi.useRealTimers());
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
  it('renders all same-day events with an expandable overflow and month controls',()=>{
    const html=render('/schedule');
    for(const value of ['동일 날짜 자격1','동일 날짜 자격2','동일 날짜 자격3','동일 날짜 자격4','+1 더보기','이전 달','다음 달','D-2'])expect(html).toContain(value);
    expect(html).toContain('<details');expect(html).not.toContain('D--');
  });
  it('distinguishes official exam portals from guides and hides unsafe URLs',()=>{
    const html=render('/resources');
    for(const value of ['공식 자료 자격','공식 시험 안내','공식 학습 가이드','https://example.com/exam','https://example.com/guide'])expect(html).toContain(value);
    expect(html).not.toContain('javascript:');expect(html).not.toContain('잘못된 링크');
  });
  it('offers a home link from the standalone login page',()=>{
    expect(render('/login')).toContain('class="auth-home-link" href="/"');
  });
});
