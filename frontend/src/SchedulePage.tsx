import {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {Link} from 'react-router-dom';
import {ChevronLeft,ChevronRight} from 'lucide-react';
import {api} from './api';
import {useSaved} from './saved';
import {calendarDays,daysUntil,formatDday,monthStart,seoulDateKey} from './dates';
import {typeLabel} from './data';
import type {Schedule} from './types';

const tone:Record<string,string>={REGISTRATION_OPEN:'open',REGISTRATION_CLOSE:'close',EXAM:'exam',RESULT:'result',RENEWAL:'renewal'};
function EventLink({event}:{event:Schedule}){return <Link className={`event ${tone[event.type]||'open'}`} to={`/certifications/${event.certificationId}`} title={`${event.certificationName} · ${typeLabel[event.type]||event.title}`}>{event.certificationName} · {typeLabel[event.type]||event.title}</Link>;}
export default function SchedulePage(){
  const today=seoulDateKey();
  const [viewDate,setViewDate]=useState(()=>today.slice(0,7));
  const [onlySaved,setOnlySaved]=useState(false),[type,setType]=useState('');
  const {saved}=useSaved();
  const [year,month]=viewDate.split('-').map(Number);
  const rangeStart=monthStart(year,month-1),rangeEnd=new Date(Date.parse(monthStart(year,month+7))-1).toISOString();
  const query=useQuery({queryKey:['schedules',rangeStart,rangeEnd],queryFn:()=>api.schedules(rangeStart,rangeEnd),staleTime:5*60*1000});
  const list=(query.data||[]).filter(s=>(!onlySaved||saved[s.certificationId])&&(!type||s.type===type)).slice().sort((a,b)=>a.startsAt.localeCompare(b.startsAt));
  const byDay=new Map<string,Schedule[]>();
  for(const event of list){const key=seoulDateKey(event.startsAt);byDay.set(key,[...(byDay.get(key)||[]),event]);}
  const upcoming=list.filter(s=>daysUntil(s.startsAt)>=0);
  const move=(amount:number)=>setViewDate(new Date(Date.UTC(year,month-1+amount,1)).toISOString().slice(0,7));
  return <div className="page"><div className="page-title"><p className="eyebrow">SCHEDULE</p><h1>시험 일정</h1><p>접수부터 발표일까지 준비 중인 자격증 일정을 놓치지 마세요.</p></div>
    <div className="schedule-tools"><div>{['','REGISTRATION_OPEN','REGISTRATION_CLOSE','EXAM','RESULT'].map(value=><button key={value} className={type===value?'active':''} onClick={()=>setType(value)}>{value?typeLabel[value]:'전체'}</button>)}</div><label className="switch"><input type="checkbox" checked={onlySaved} onChange={e=>setOnlySaved(e.target.checked)}/><span/>관심 자격증만</label></div>
    {query.isLoading&&<p role="status">시험 일정을 불러오는 중입니다.</p>}{query.isError&&<div role="alert" className="empty"><p>시험 일정을 불러오지 못했습니다.</p><button onClick={()=>query.refetch()}>다시 시도</button></div>}
    <div className="calendar-layout"><section className="calendar"><div className="cal-head"><h2>{year}년 {month}월</h2><div className="calendar-nav"><button aria-label="이전 달" onClick={()=>move(-1)}><ChevronLeft size={18}/></button><button onClick={()=>setViewDate(today.slice(0,7))}>오늘</button><button aria-label="다음 달" onClick={()=>move(1)}><ChevronRight size={18}/></button></div></div>
      <div className="weekdays">{['일','월','화','수','목','금','토'].map(d=><span key={d}>{d}</span>)}</div><div className="days">{calendarDays(year,month).map(key=>{
        const events=byDay.get(key)||[];
        return <div key={key} className={`${key.slice(0,7)!==viewDate?'muted':''} ${key===today?'today':''}`} aria-label={key}><time dateTime={key}>{Number(key.slice(8))}</time>{events.slice(0,3).map(event=><EventLink event={event} key={event.id}/>)}{events.length>3&&<details className="more-events"><summary>+{events.length-3} 더보기</summary>{events.slice(3).map(event=><EventLink key={event.id} event={event}/>)}</details>}</div>;
      })}</div><div className="calendar-legend">{['REGISTRATION_OPEN','REGISTRATION_CLOSE','EXAM','RESULT'].map(value=><span className={`event ${tone[value]}`} key={value}>{typeLabel[value]}</span>)}</div></section>
      <aside className="upcoming"><h2>다가오는 일정</h2><p className="schedule-range">표시 월 기준 향후 6개월 범위</p>{upcoming.length?upcoming.map(s=>{const key=seoulDateKey(s.startsAt);return <Link to={`/certifications/${s.certificationId}`} key={s.id}><div className="date-box"><strong>{Number(key.slice(8))}</strong><small>{Number(key.slice(5,7))}월</small></div><div><span className={`badge ${tone[s.type]}`}>{typeLabel[s.type]||s.title}</span><strong>{s.certificationName}</strong><small>{formatDday(s.startsAt)}</small></div></Link>}):!query.isLoading&&!query.isError&&<p>이 범위에 다가오는 일정이 없습니다.</p>}</aside>
    </div></div>;
}
