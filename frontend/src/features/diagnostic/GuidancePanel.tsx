import type { DiagnosticResult, Skill } from './recommendations';

const guidance: Record<Skill, { title: string; action: string; practice: string }> = {
  service: { title: 'Atendimento e relacionamento', action: 'Pratique uma apresentação curta e exemplos de como ajudou alguém a resolver uma dúvida.', practice: 'Explore comunicação, escuta ativa e atendimento digital.' },
  sales: { title: 'Vendas e negociação', action: 'Prepare um exemplo de como identificaria a necessidade de uma pessoa antes de recomendar uma solução.', practice: 'Explore técnicas de vendas, negociação e relacionamento com clientes.' },
  admin: { title: 'Organização administrativa', action: 'Destaque situações em que organizou informações, documentos ou prazos.', practice: 'Explore ferramentas digitais, rotinas de escritório e gestão do tempo.' },
  numbers: { title: 'Finanças e análise', action: 'Inclua no currículo ferramentas de planilha e exemplos de atenção a valores e registros.', practice: 'Explore Excel, controles financeiros e conferência de dados.' },
  logistics: { title: 'Logística e estoque', action: 'Mostre experiências com organização, cumprimento de prazos e movimentação de materiais.', practice: 'Explore estoque, armazenagem, distribuição e segurança.' },
  maintenance: { title: 'Manutenção e resolução de problemas', action: 'Registre ferramentas ou equipamentos que conhece e cursos de segurança que concluiu.', practice: 'Explore manutenção preventiva, ferramentas e fundamentos técnicos.' },
  production: { title: 'Produção e qualidade', action: 'Destaque atenção a procedimentos, segurança e consistência em tarefas repetitivas.', practice: 'Explore operação, qualidade e segurança no ambiente de trabalho.' },
  technology: { title: 'Tecnologia e informática', action: 'Registre ferramentas, sistemas ou projetos digitais que já utilizou ou estudou.', practice: 'Explore fundamentos de TI, suporte técnico, programação e análise de dados.' },
  health: { title: 'Saúde e bem-estar', action: 'Destaque experiências de acolhimento, organização e cuidado com informações.', practice: 'Explore cursos introdutórios e confira os requisitos de cada formação na área da saúde.' },
  marketing: { title: 'Marketing e comunicação', action: 'Registre conteúdos, campanhas, publicações ou resultados que ajudou a produzir.', practice: 'Explore marketing digital, produção de conteúdo, redes sociais e análise de campanhas.' },
};

export function GuidancePanel({ result }: { result: DiagnosticResult }) {
  const topSkills = (Object.entries(result.scores) as [Skill, number][]).sort((a, b) => b[1] - a[1]).filter(([, score]) => score > 0).slice(0, 2);
  return <section aria-labelledby="guidance-title" className="mt-8 rounded-3xl border border-brand-green/25 bg-brand-green/10 p-6 md:p-8">
    <p className="text-xs font-extrabold uppercase tracking-widest text-brand-green">Orientação para os próximos passos</p><h2 id="guidance-title" className="mt-2 text-2xl font-black">Transforme seus interesses em ações</h2><p className="mt-2 max-w-3xl text-sm text-ink/70">O diagnóstico organiza interesses que você mesmo informou. Ele não mede capacidade nem prevê sucesso; use as sugestões como ponto de partida e escolha o que faz sentido para você.</p>
    <div className="mt-5 grid gap-4 md:grid-cols-2">{topSkills.map(([skill, score]) => <article key={skill} className="rounded-2xl bg-brand-white p-5"><h3 className="text-lg font-black">{guidance[skill].title}</h3><p className="mt-2 text-sm leading-6"><strong>Para currículo ou entrevista:</strong> {guidance[skill].action}</p><p className="mt-2 text-sm leading-6"><strong>Para aprender:</strong> {guidance[skill].practice}</p><p className="mt-3 text-xs font-semibold text-ink/55">Esse tema teve destaque nas suas respostas (pontuação {score}).</p></article>)}</div>
  </section>;
}
