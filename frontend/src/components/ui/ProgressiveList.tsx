export function ListMore({visible,total,onMore,label}:{visible:number;total:number;onMore:()=>void;label:string}){
 if(total<=20)return null;
 return <div className="space-y-2 py-3"><p role="status" className="text-sm">Exibindo {visible} de {total} {label} filtrados.</p>{visible<total&&<button type="button" onClick={onMore} className="min-h-11 rounded-full border border-white/30 px-5 py-3 font-bold">Mostrar mais {label}</button>}</div>;
}
