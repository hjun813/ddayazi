import {afterEach,beforeEach,describe,expect,it,vi} from 'vitest';

describe('session lifecycle',()=>{
  const values=new Map<string,string>();

  beforeEach(()=>{
    values.clear();
    vi.resetModules();
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'));
    vi.stubGlobal('localStorage',{
      getItem:(key:string)=>values.get(key)??null,
      setItem:(key:string,value:string)=>{values.set(key,value);},
      removeItem:(key:string)=>{values.delete(key);},
    });
    vi.stubGlobal('window',{addEventListener:vi.fn()});
  });

  afterEach(()=>{
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('expires a saved session and removes legacy shared data',async()=>{
    values.set('ddayaji-saved','{"1":"PASSED"}');
    const auth=await import('./auth');
    auth.saveSession({accessToken:'token',expiresIn:60,nickname:'user',role:'USER'},'user@example.com');
    expect(auth.getSession()?.user.email).toBe('user@example.com');
    expect(values.has('ddayaji-saved')).toBe(false);
    await vi.advanceTimersByTimeAsync(60_000);
    expect(auth.getToken()).toBeNull();
    expect(values.has('ddayaji-session')).toBe(false);
  });

  it('clears authentication when the server rejects the token',async()=>{
    const auth=await import('./auth');
    auth.saveSession({accessToken:'token',expiresIn:60,nickname:'user',role:'USER'},'user@example.com');
    vi.stubGlobal('fetch',vi.fn().mockResolvedValue({status:401,ok:false,json:async()=>({message:'Unauthorized'})}));
    const {api}=await import('./api');
    await expect(api.profile()).rejects.toThrow('Unauthorized');
    expect(auth.getSession()).toBeNull();
  });

  it('updates this tab when another tab logs out',async()=>{
    const auth=await import('./auth');
    auth.saveSession({accessToken:'token',expiresIn:60,nickname:'user',role:'USER'},'user@example.com');
    const listener=vi.mocked(window.addEventListener).mock.calls.find(([name])=>name==='storage')?.[1] as EventListener;
    values.delete('ddayaji-session');
    listener({key:'ddayaji-session'} as StorageEvent);
    expect(auth.getSession()).toBeNull();
  });
});
