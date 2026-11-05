import type { SearchRefinements } from './territory';
import { searchField } from './searchOptions';
export function SearchRefinementFields({value,onChange,additionalOnly=false}:{additionalOnly?:boolean;value:SearchRefinements;onChange:(next:SearchRefinements)=>void}) {
 const change=(key:keyof SearchRefinements,v:string)=>onChange({...value,[key]:v});
 return <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
  {!additionalOnly && <label>Contratação<select className={searchField} value={value.contractType||''} onChange={e=>change('contractType',e.target.value)}><option value="">Todos os contratos</option>{['CLT','PJ','Temporário','Estágio','Aprendiz','Autônomo'].map(v=><option key={v}>{v}</option>)}</select></label>}
  <label>Escolaridade concluída<select className={searchField} value={value.educationLevel||''} onChange={e=>change('educationLevel',e.target.value)}><option value="">Sem filtro</option>{['Fundamental','Médio','Técnico','Superior','Pós-graduação'].map((v,i)=><option key={v} value={i+1}>{v}</option>)}</select></label>
  <label>Experiência disponível<select className={searchField} value={value.maxExperienceMonths||''} onChange={e=>change('maxExperienceMonths',e.target.value)}><option value="">Sem filtro</option><option value="0">Sem experiência</option><option value="12">Até 1 ano</option><option value="24">Até 2 anos</option><option value="60">Até 5 anos</option></select></label>
  {!additionalOnly && <label>Remuneração mínima (R$)<input className={searchField} type="number" min="0" max="100000000" step="0.01" value={value.minimumSalary||''} onChange={e=>change('minimumSalary',e.target.value)}/></label>}
  {!additionalOnly && <label>Remuneração máxima (R$)<input className={searchField} type="number" min="0" max="100000000" step="0.01" value={value.maximumSalary||''} onChange={e=>change('maximumSalary',e.target.value)}/></label>}
  <label>Data de publicação<select className={searchField} value={value.publishedDays||''} onChange={e=>change('publishedDays',e.target.value)}><option value="">Qualquer data</option><option value="1">Últimas 24 horas</option><option value="7">Últimos 7 dias</option><option value="30">Últimos 30 dias</option></select></label>
  <p className="text-sm text-ink/80 sm:col-span-2 lg:col-span-3">Estágio e Jovem Aprendiz dependem do tipo informado pela empresa. Escolaridade e experiência em meses consideram requisitos estruturados: anúncios sem esses dados ficam de fora. A faixa salarial inclui valores e intervalos em reais, sem estimar remuneração “a combinar”.</p>
 </div>;
}
