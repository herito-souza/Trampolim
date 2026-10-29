export function Footer({ showDiagnosticCta = false }: { showDiagnosticCta?: boolean }) {
  return (
    <>
      {showDiagnosticCta && <section className="px-[6vw] py-14 md:py-20">
        <div className="relative mx-auto max-w-7xl overflow-hidden rounded-3xl border border-brand-yellow/25 bg-[#101a29] p-7 md:p-12">
          <div aria-hidden="true" className="absolute -right-16 -top-28 h-80 w-80 rounded-full border border-brand-yellow/20 shadow-[0_0_100px_rgba(214,255,56,0.08)]" />
          <p className="text-xs font-extrabold uppercase tracking-[0.2em] text-brand-yellow">O futuro começa no próximo passo</p>
          <h3 className="relative mt-4 max-w-3xl text-4xl font-bold leading-[0.95] text-white md:text-6xl">Você não precisa ter tudo planejado. Só precisa começar.</h3>
          <p className="relative mt-5 max-w-xl text-sm leading-6 text-white/60">Faça o diagnóstico gratuito e descubra cursos e áreas profissionais que podem combinar com seus interesses.</p>
          <a href="#diagnostico" className="relative mt-7 inline-flex rounded-full bg-brand-yellow px-6 py-3 font-extrabold text-[#101725] no-underline transition-transform hover:-translate-y-0.5">Começar meu diagnóstico ↗</a>
        </div>
      </section>}
      <footer className="flex flex-wrap items-center justify-between gap-3 border-t border-white/10 px-[6vw] py-7 text-xs text-white/45">
        <span className="font-serif text-xl font-bold tracking-wide text-white">TRAMPOL<span className="text-brand-yellow">I</span>M</span>
        <span>Capacitação e oportunidades para o seu próximo passo.</span>
        <span>© {new Date().getFullYear()} Trampolim</span>
      </footer>
    </>
  );
}
