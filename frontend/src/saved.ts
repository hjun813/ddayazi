import {useQuery,useQueryClient} from '@tanstack/react-query';
import {useNavigate} from 'react-router-dom';
import {api} from './api';
import {useSession} from './auth';

export function useSaved(){
  const session=useSession();
  const navigate=useNavigate();
  const queryClient=useQueryClient();
  const queryKey=['my-certifications',session?.user.email];
  const {data=[],isPending}=useQuery({queryKey,queryFn:api.mine,enabled:!!session});
  const saved=Object.fromEntries(data.map(item=>[item.certificationId,item.status])) as Record<number,string>;

  const update=async(id:number,status?:string)=>{
    if(!session){navigate('/login');return;}
    try{
      const current=await queryClient.fetchQuery({queryKey,queryFn:api.mine,staleTime:0});
      if(status)await api.status(id,status);
      else if(saved[id]){
        if(current.some(item=>item.certificationId===id))await api.remove(id);
      }else if(!current.some(item=>item.certificationId===id))await api.add(id);
      await queryClient.invalidateQueries({queryKey});
    }catch(error){
      window.alert(error instanceof Error?error.message:'자격증 상태를 변경하지 못했습니다.');
    }
  };
  return {saved,update,isPending:isPending&&!!session};
}
