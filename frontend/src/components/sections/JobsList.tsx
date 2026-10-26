export function JobsList() {
  return (
    <section className="mx-auto max-w-7xl px-[6vw] py-20 md:py-28" id="vagas">
      <div className="mb-3 text-xs font-extrabold uppercase tracking-[0.2em] text-brand-yellow">Oportunidades em destaque</div>
      <div className="mb-10 flex flex-wrap items-end justify-between gap-4"><h2 className="max-w-2xl text-4xl font-bold leading-[0.95] md:text-6xl">Seu talento pode<br />ir mais longe.</h2><p className="max-w-sm text-sm leading-6 text-ink/60">Conheça algumas das áreas que você pode explorar no catálogo de oportunidades.</p></div>
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2 lg:grid-cols-3">
        {[["01", "Atendimento e serviços", "Comunicação · Relacionamento · Varejo"], ["02", "Administração", "Organização · Rotinas · Apoio operacional"], ["03", "Logística e produção", "Planejamento · Estoque · Processos"], ["04", "Tecnologia e informática", "Suporte · Sistemas · Dados"], ["05", "Saúde e bem-estar", "Recepção · Apoio administrativo · Serviços de saúde"], ["06", "Marketing e comunicação", "Conteúdo · Redes sociais · Campanhas"]].map(([number, title, skills]) => <article key={number} className="rounded-2xl border border-white/10 bg-brand-white p-6 md:p-8"><span className="font-serif text-3xl font-bold text-brand-yellow">{number}</span><h3 className="mt-8 text-2xl font-bold">{title}</h3><p className="mt-2 text-sm text-ink/55">{skills}</p></article>)}
      </div>
    </section>
  );
}
