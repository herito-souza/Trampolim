import {useState} from 'react';
import {NavIcon} from './NavIcon';
export function ProfileAvatar({src,className='size-9',alt=''}:{src?:string|null;className?:string;alt?:string}){
 const [failed,setFailed]=useState<string|null>(null);
 return <span className={'grid shrink-0 place-items-center overflow-hidden rounded-full '+className}>{src&&src!==failed?<img src={src} alt={alt} className="h-full w-full object-cover" onError={()=>setFailed(src)}/>:<NavIcon name="user"/>}</span>;
}
