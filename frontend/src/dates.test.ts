import {describe,expect,it} from 'vitest';
import {calendarDays,daysUntil,formatDday,primaryRound,seoulDateKey} from './dates';
import type {Schedule} from './types';
const now=new Date('2026-10-14T14:50:00Z');
const event=(id:number,type:string,startsAt:string,examRound='1회',endsAt?:string):Schedule=>({id,type,startsAt,endsAt,examRound,title:type,certificationId:1,certificationName:'테스트',sampleData:false});
describe('Korean calendar dates',()=>{
  it('uses Seoul midnight, not elapsed 24-hour intervals',()=>{
    expect(seoulDateKey('2026-10-14T15:01:00Z')).toBe('2026-10-15');
    expect(daysUntil('2026-10-14T00:00:00Z',now)).toBe(0);
    expect(formatDday('2026-10-14T00:00:00Z',now)).toBe('D-DAY');
    expect(formatDday('2026-10-14T15:01:00Z',now)).toBe('D-1');
    expect(formatDday('2026-10-12T00:00:00Z',now)).toBe('종료');
    expect(formatDday('invalid',now)).toBe('일정 확인 필요');
  });
  it('includes six-week months and year boundary cells',()=>{
    expect(calendarDays(2026,8)).toHaveLength(42);
    expect(calendarDays(2026,8)).toContain('2026-08-31');
    expect(calendarDays(2026,1)[0]).toBe('2025-12-28');
    expect(calendarDays(2028,2)).toContain('2028-02-29');
  });
  it('never combines registration dates from different rounds',()=>{
    const round=primaryRound([event(1,'REGISTRATION_CLOSE','2026-10-16T00:00:00Z'),event(2,'REGISTRATION_OPEN','2026-10-26T00:00:00Z','2회'),event(3,'EXAM','2026-11-01T00:00:00Z')],now);
    expect(round.registrationStart).toBeUndefined();
    expect(round.registrationEnd).toBe('2026-10-16T00:00:00Z');
    expect(round.events.every(s=>s.examRound==='1회')).toBe(true);
  });
  it('keeps an already-open registration interval and ignores ended rounds',()=>{
    const round=primaryRound([event(1,'EXAM','2026-09-01T00:00:00Z','옛회차'),event(2,'REGISTRATION_OPEN','2026-10-10T00:00:00Z','현재','2026-10-20T00:00:00Z')],now);
    expect(round.registrationStart).toBe('2026-10-10T00:00:00Z');
    expect(round.registrationEnd).toBe('2026-10-20T00:00:00Z');
  });
  it('does not display inverted date ranges or fabricate corrected dates',()=>{
    const round=primaryRound([event(1,'REGISTRATION_OPEN','2026-10-26T00:00:00Z'),event(2,'REGISTRATION_CLOSE','2026-10-16T00:00:00Z')],now);
    expect(round.registrationStart).toBeUndefined();expect(round.registrationEnd).toBe('2026-10-16T00:00:00Z');
    expect(primaryRound([event(1,'EXAM','2025-01-01T00:00:00Z')],now).events).toHaveLength(0);
  });
});
