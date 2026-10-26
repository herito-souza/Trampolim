export function Stats() {
  const features = [
    ["10", "perguntas para conhecer seu perfil"],
    ["25", "opções de cursos para explorar"],
    ["25", "vagas para conhecer no catálogo"],
    ["R$ 0", "para começar sua jornada"],
  ];

  return (
    <section aria-label="O que você encontra no Trampolim" className="border-y border-white/10 bg-[#0b1421] px-[6vw] py-9 text-white">
      <div className="mx-auto grid max-w-7xl grid-cols-2 gap-7 md:grid-cols-4 md:gap-4">
        {features.map(([value, label]) => <div key={label} className="border-l border-brand-yellow/50 pl-4 md:pl-6"><p className="font-serif text-4xl font-bold text-brand-yellow md:text-5xl">{value}</p><p className="mt-1 max-w-44 text-xs leading-5 text-white/60 md:text-sm">{label}</p></div>)}
      </div>
    </section>
  );
}
