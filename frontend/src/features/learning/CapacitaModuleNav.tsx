import {WorkspaceNavigation} from '../../components/workspace/WorkspaceNavigation';
import {useWorkspace} from '../../components/workspace/workspaceContext';
import { useRef } from 'react';

type Item = { id: string; label: string };

export function CapacitaModuleNav({ label, value, items, onChange, current=true }: { current?:boolean; label: string; value: string; items: Item[]; onChange: (id: string) => void }) {
  const shell=useWorkspace();
  const controls = useRef<(HTMLButtonElement | null)[]>([]);
  if(shell){
    const communication=items.filter(item=>['questions','inbox'].includes(item.id));
    const account=items.filter(item=>['support','account'].includes(item.id));
    const main=items.filter(item=>!communication.includes(item)&&!account.includes(item));
    const group=(id:string,name:string,entries:Item[])=>({id,label:name,items:entries.map(item=>({...item,onSelect:()=>onChange(item.id)}))});
    return <WorkspaceNavigation label={label} active={current?value:''} groups={[group(label==='Módulos do Capacita'?'areas':'aprendizagem',label==='Módulos do Capacita'?'Capacita':'Aprendizagem',main),...(communication.length?[group('comunicacao','Comunicação',communication)]:[]),...(account.length?[group('apoio','Conta e apoio',account)]:[])]}/>;
  }
  return <nav aria-label={label} className="border-b border-white/10 bg-[#0b1421] py-3">
    <div className="mx-auto flex w-full max-w-7xl flex-wrap gap-2 px-4 md:px-7">
      {items.map((item, index) => <button key={item.id} ref={element => { controls.current[index] = element; }} type="button"
        aria-current={value === item.id ? 'page' : undefined}
        onClick={() => onChange(item.id)}
        onKeyDown={event => {
          const target = event.key === 'ArrowRight' ? (index + 1) % items.length : event.key === 'ArrowLeft' ? (index - 1 + items.length) % items.length : event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : -1;
          if (target >= 0) { event.preventDefault(); controls.current[target]?.focus(); }
        }}
        className={`min-h-11 rounded-full border px-4 py-2 text-sm font-bold transition focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand-blue ${value === item.id ? 'border-brand-blue bg-brand-blue/10 text-brand-blue' : 'border-white/15 text-ink hover:border-brand-blue/60'}`}>
        {item.label}
      </button>)}
    </div>
  </nav>;
}
