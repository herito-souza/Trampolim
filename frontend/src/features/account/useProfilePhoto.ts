import {useEffect,useState} from 'react';
import {API_URL} from './api';
/** Authenticated, memory-only image: no token in URL, no sharing between accounts. */
export function useProfilePhoto(token:string|null,fileName:string|null|undefined,revision:number){
 const key=token&&fileName?token+'|'+fileName+'|'+revision:'';
 const [photo,setPhoto]=useState<{key:string;url:string}|null>(null);
 useEffect(()=>{if(!key||!token)return;const abort=new AbortController();let url:string|undefined;
 void fetch(API_URL+'/profile/photo',{headers:{Authorization:'Bearer '+token},signal:abort.signal,cache:'no-store'}).then(async response=>{if(!response.ok)throw new Error('Photo unavailable');const blob=await response.blob();if(!['image/jpeg','image/png','image/webp'].includes(blob.type)||abort.signal.aborted)return;url=URL.createObjectURL(blob);setPhoto({key,url});}).catch(()=>{/* The generic avatar remains available on network failure. */});
 return()=>{abort.abort();if(url)URL.revokeObjectURL(url)};
 },[key,token]);
 return photo?.key===key?photo.url:null;
}
