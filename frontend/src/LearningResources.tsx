import {useState} from 'react';
import {useQuery} from '@tanstack/react-query';
import {Link} from 'react-router-dom';
import {BookOpen,ExternalLink} from 'lucide-react';
import {api} from './api';

export function externalUrl(url:string|null|undefined){try{const parsed=new URL(url||'');return ['http:','https:'].includes(parsed.protocol)?url:undefined;}catch{return undefined;}}
export default function LearningResources(){
  const [category,setCategory]=useState('');
  const query=useQuery({queryKey:['public-resource-directory'],queryFn:api.resourceDirectory,staleTime:5*60*1000});
  const groups=(query.data||[]).filter(c=>externalUrl(c.officialUrl)||c.resources.some(r=>externalUrl(r.url)));
  const categories=[...new Set(groups.map(c=>c.category))].sort((a,b)=>a.localeCompare(b,'ko'));
  return <div className="page"><div className="page-title"><p className="eyebrow">LEARNING RESOURCES</p><h1>공식 안내와 학습 자료</h1><p>시험 안내는 공식 홈페이지에서, 학습 자료는 등록된 공식 링크에서 확인하세요.</p></div>
    <div className="resource-filters" aria-label="자료 분야">{['',...categories].map(value=><button key={value} aria-pressed={category===value} className={category===value?'active':''} onClick={()=>setCategory(value)}>{value||'전체'}</button>)}</div>
    {query.isLoading?<p role="status">공식 자료를 불러오는 중입니다.</p>:query.isError?<div className="empty" role="alert"><p>공식 자료를 불러오지 못했습니다.</p><button onClick={()=>query.refetch()}>다시 시도</button></div>:groups.length?<div className="cards cards-3">{groups.filter(c=>!category||c.category===category).map(c=><article className="cert-card resource-card" key={c.id}><span className="badge soft">{c.category}</span><Link to={`/certifications/${c.id}`}><h2>{c.name}</h2></Link><p>{c.organization}</p>{externalUrl(c.officialUrl)&&<a className="secondary" href={c.officialUrl!} target="_blank" rel="noopener noreferrer">공식 시험 안내 <ExternalLink size={15}/></a>}{c.resources.filter(r=>externalUrl(r.url)).map(r=><a className="resource" href={r.url} target="_blank" rel="noopener noreferrer" key={r.id}><BookOpen size={18}/><span><strong>{r.title}</strong><small>{r.free?'무료':'유료'} · 공식 자료</small></span><ExternalLink size={15}/></a>)}<small>정보 확인일: {c.lastVerifiedAt||'확인일 미등록'}</small></article>)}</div>:<div className="empty"><BookOpen/><h3>등록된 공식 링크가 아직 없습니다.</h3><p>공식 출처가 등록되면 이곳에서 확인할 수 있습니다.</p><Link className="primary" to="/certifications">자격증 탐색</Link></div>}
  </div>;
}
