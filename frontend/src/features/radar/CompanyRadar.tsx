import { useState } from 'react';
import { RequirementsEditor } from './RequirementsEditor';
import { radarField } from './radarUi';
export function CompanyRadar({token,jobs,canEdit}:{token:string;jobs:{id:number;title:string;kind:string}[];canEdit:boolean}){
 const [open,setOpen]=useState(false);const [id,setId]=useState('');
 return <details className="my-8 rounded-2xl border border-brand-blue/30 p-5" onToggle={event=>setOpen(event.currentTarget.open)}><summary className="cursor-pointer text-xl font-bold">Competências e critérios das vagas</summary><p className="mt-3 text-sm text-ink/75">Requisitos estruturados permitem explicar a compatibilidade aos candidatos. Nenhum currículo privado é exposto por esta ferramenta.</p>{open&&<><label className="my-4 block font-bold">Vaga para revisar<select className={radarField} value={id} onChange={event=>setId(event.target.value)}><option value="">Selecione uma vaga da sua empresa</option>{jobs.filter(job=>job.kind==='job').map(job=><option key={job.id} value={job.id}>{job.title}</option>)}</select></label>{id&&<RequirementsEditor key={id} token={token} path={`/company/listings/${id}/radar`} canEdit={canEdit}/>}</>}</details>;
}
