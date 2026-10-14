import type {Schedule} from './types';

const dayMs=86400000;
export const roundLabel=(round?:string)=>!round||round==='UNSPECIFIED'?'회차 미정':round;
export function seoulDateKey(value:string|Date=new Date()):string{
  const date=new Date(value);
  if(Number.isNaN(date.getTime()))return '';
  const parts=new Intl.DateTimeFormat('en-US',{timeZone:'Asia/Seoul',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(date);
  const part=(type:string)=>parts.find(p=>p.type===type)!.value;
  return `${part('year')}-${part('month')}-${part('day')}`;
}
export function daysUntil(value:string,now:Date=new Date()){
  const target=seoulDateKey(value),today=seoulDateKey(now);
  return target?Math.round((Date.parse(target)-Date.parse(today))/dayMs):NaN;
}
export function formatDday(value:string,now:Date=new Date()){
  const days=daysUntil(value,now);
  return Number.isNaN(days)?'일정 확인 필요':days===0?'D-DAY':days>0?`D-${days}`:'종료';
}
export function calendarDays(year:number,month:number){
  const first=new Date(Date.UTC(year,month-1,1));
  const count=Math.ceil((first.getUTCDay()+new Date(Date.UTC(year,month,0)).getUTCDate())/7)*7;
  return Array.from({length:count},(_,i)=>new Date(Date.UTC(year,month-1,1-first.getUTCDay()+i)).toISOString().slice(0,10));
}
export function monthStart(year:number,month:number){
  return new Date(Date.UTC(year,month-1,1)).toISOString().slice(0,10)+'T00:00:00+09:00';
}
export function primaryRound(schedules:Schedule[],now:Date=new Date()){
  const groups=new Map<string,Schedule[]>();
  for(const s of schedules){const round=s.examRound||'UNSPECIFIED';groups.set(round,[...(groups.get(round)||[]),s]);}
  const candidates=[...groups.values()].map(events=>({events,upcoming:events.flatMap(s=>{
    const times=[s.startsAt,...(s.endsAt?[s.endsAt]:[])].filter(t=>daysUntil(t,now)>=0).map(t=>Date.parse(t));
    return times;
  })})).filter(g=>g.upcoming.length).sort((a,b)=>Math.min(...a.upcoming)-Math.min(...b.upcoming));
  const events=candidates[0]?.events.slice().sort((a,b)=>a.startsAt.localeCompare(b.startsAt))||[];
  const open=events.find(s=>s.type==='REGISTRATION_OPEN');
  const close=events.find(s=>s.type==='REGISTRATION_CLOSE');
  const end=close?.startsAt||open?.endsAt;
  // Contradictory dates are shown as unknown rather than inventing a valid range.
  const start=open?.startsAt&&(!end||Date.parse(open.startsAt)<=Date.parse(end))?open.startsAt:undefined;
  return {events,open,close,registrationStart:start,registrationEnd:end,
    exam:events.find(s=>s.type==='EXAM'),result:events.find(s=>s.type==='RESULT')};
}
