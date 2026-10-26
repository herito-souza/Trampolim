export function HowItWorks() {
  return (
    <section className="mx-auto max-w-7xl px-[6vw] py-20 md:py-28" id="como-funciona">
      <div className="mb-3 text-xs font-extrabold uppercase tracking-[0.2em] text-brand-yellow">Como funciona</div>
      <div className="mb-11 flex flex-wrap items-end justify-between gap-5"><h2 className="max-w-3xl text-4xl font-bold leading-[0.95] md:text-6xl">Construa seu caminho.<br /><span className="text-white/45">Uma etapa por vez.</span></h2><p className="max-w-sm text-sm leading-6 text-ink/60">Do primeiro diagnóstico a novas possibilidades de trabalho, você decide o ritmo da sua jornada.</p></div>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        {[["01", "Descubra seu ponto de partida", "Responda ao diagnóstico e reconheça os interesses e experiências que já fazem parte da sua história."], ["02", "Amplie suas possibilidades", "Explore cursos e formações que podem ajudar você a desenvolver novas habilidades."], ["03", "Encontre novas oportunidades", "Consulte vagas e áreas profissionais alinhadas aos seus interesses e à sua região."]].map(([number, title, description]) => <article key={number} className="group rounded-2xl border border-white/10 bg-brand-white p-6 transition-colors hover:border-brand-yellow/45 md:p-8"><span className="font-serif text-5xl font-bold text-brand-yellow">{number}</span><h3 className="mt-9 text-2xl font-bold leading-tight">{title}</h3><p className="mt-3 text-sm leading-6 text-ink/65">{description}</p><span className="mt-7 block h-px w-full bg-white/10 transition-colors group-hover:bg-brand-yellow/50" /></article>)}
      </div>
    </section>
  );
}
