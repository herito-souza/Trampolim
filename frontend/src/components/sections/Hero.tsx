import { Button } from "../ui/Button";

export function Hero() {
  const scrollTo = (id: string) => {
    document.getElementById(id)?.scrollIntoView();
  };

  return (
    <section className="relative isolate overflow-hidden px-[6vw] pb-20 pt-16 md:pb-28 md:pt-24">
      <div aria-hidden="true" className="pointer-events-none absolute -right-36 top-6 -z-10 h-[34rem] w-[34rem] rounded-full border border-brand-yellow/20 shadow-[0_0_120px_rgba(214,255,56,0.08)]" />
      <div className="mx-auto grid max-w-7xl items-center gap-14 lg:grid-cols-[1.08fr_0.92fr]">
        <div>
          <p className="mb-5 flex items-center gap-3 text-xs font-extrabold uppercase tracking-[0.22em] text-brand-yellow"><span className="h-px w-9 bg-brand-yellow" />Capacitação · carreira · oportunidade</p>
          <h1 className="max-w-4xl text-[clamp(4rem,10vw,8.4rem)] font-extrabold uppercase leading-[0.82] tracking-[-0.04em]">
            Seu próximo<br />passo <span className="text-brand-yellow">começa</span><br />aqui.
          </h1>
          <p className="mt-8 max-w-xl text-base leading-7 text-ink/70 md:text-lg">Descubra seus pontos fortes, encontre cursos para crescer e explore oportunidades de trabalho que combinam com o seu momento.</p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Button onClick={() => scrollTo('diagnostico')}>Encontrar meu caminho <span aria-hidden="true">↗</span></Button>
            <Button variant="ghost" onClick={() => scrollTo('vagas')}>Explorar oportunidades</Button>
          </div>
          <div className="mt-10 flex flex-wrap items-center gap-x-6 gap-y-2 text-xs font-semibold text-ink/55"><span>✓ Cursos gratuitos</span><span>✓ Perfil profissional</span><span>✓ Recomendações personalizadas</span></div>
        </div>
        <div className="relative mx-auto w-full max-w-xl">
          <div className="absolute -inset-5 -rotate-3 rounded-[2rem] border border-brand-yellow/20" aria-hidden="true" />
          <article className="relative overflow-hidden rounded-[1.8rem] border border-white/10 bg-[#101a29] p-6 shadow-2xl md:p-8">
            <div className="flex items-start justify-between gap-4"><div><p className="text-xs font-extrabold uppercase tracking-[0.2em] text-brand-yellow">Seu mapa de carreira</p><h2 className="mt-2 text-3xl font-bold text-white md:text-4xl">Do que você sabe<br />ao que pode ser.</h2></div><span className="grid h-12 w-12 shrink-0 place-items-center rounded-full bg-brand-yellow text-2xl font-black text-[#101725]" aria-hidden="true">↗</span></div>
            <div className="my-7 h-px bg-white/10" />
            <div className="space-y-5">
              {[['01', 'Reconheça seus talentos', 'Comece por uma conversa rápida sobre suas experiências.'], ['02', 'Aprenda no seu ritmo', 'Encontre cursos que ajudam a dar o próximo passo.'], ['03', 'Chegue mais perto da vaga', 'Explore oportunidades alinhadas aos seus interesses.']].map(([number, title, description], index) => <div key={number} className="flex gap-4"><span className={`font-serif text-2xl font-bold ${index === 0 ? 'text-brand-yellow' : 'text-white/35'}`}>{number}</span><div><h3 className="font-bold text-white">{title}</h3><p className="mt-1 text-sm leading-6 text-white/55">{description}</p></div></div>)}
            </div>
            <div className="mt-8 flex items-center justify-between rounded-2xl border border-brand-yellow/25 bg-brand-yellow/5 px-4 py-3"><span className="text-xs font-bold uppercase tracking-wider text-white/65">Um passo de cada vez</span><span className="text-sm font-extrabold text-brand-yellow">Seu futuro, seu ritmo</span></div>
          </article>
        </div>
      </div>
    </section>
  );
}
