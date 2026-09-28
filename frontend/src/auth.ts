import {useSyncExternalStore} from 'react';

type User={email:string;nickname:string;role:string};
export type Session={accessToken:string;user:User;expiresAt:number};
type AuthResponse={accessToken:string;expiresIn:number;nickname:string;role:string};

const key='ddayaji-session';
const listeners=new Set<()=>void>();

function readSession():Session|null{
  try{
    const value=JSON.parse(localStorage.getItem(key)||'null') as Session|null;
    if(value&&typeof value.accessToken==='string'&&value.accessToken&&value.user?.email&&Number.isFinite(value.expiresAt)&&value.expiresAt>Date.now())return value;
  }catch{/* 잘못된 저장 값은 로그인 상태로 취급하지 않습니다. */}
  return null;
}

let session=readSession();
localStorage.removeItem('ddayaji-token');
localStorage.removeItem('ddayaji-user');
localStorage.removeItem('ddayaji-saved');
let expiryTimer:ReturnType<typeof setTimeout>|undefined;
function publish(next:Session|null){
  session=next;
  if(expiryTimer)clearTimeout(expiryTimer);
  if(next)expiryTimer=setTimeout(()=>clearSession(),Math.min(next.expiresAt-Date.now(),2147483647));
  listeners.forEach(listener=>listener());
}
if(session)publish(session);
else localStorage.removeItem(key);

export function getSession(){return session;}
export function getToken(){
  if(session&&session.expiresAt<=Date.now())clearSession();
  return session?.accessToken??null;
}
export function subscribeSession(listener:()=>void){listeners.add(listener);return()=>{listeners.delete(listener);};}
export function useSession(){return useSyncExternalStore(subscribeSession,getSession,()=>null);}

export function saveSession(auth:AuthResponse,email:string){
  const next={accessToken:auth.accessToken,user:{email,nickname:auth.nickname,role:auth.role},expiresAt:Date.now()+auth.expiresIn*1000};
  localStorage.setItem(key,JSON.stringify(next));
  localStorage.removeItem('ddayaji-token');
  localStorage.removeItem('ddayaji-user');
  localStorage.removeItem('ddayaji-saved');
  publish(next);
}

export function clearSession(){
  localStorage.removeItem(key);
  localStorage.removeItem('ddayaji-token');
  localStorage.removeItem('ddayaji-user');
  localStorage.removeItem('ddayaji-saved');
  if(session)publish(null);
}

window.addEventListener('storage',event=>{if(event.key===key||event.key===null)publish(readSession());});
