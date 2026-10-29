import type { AuthTrail } from './api';

const areas = {
  oportunidades: { name: 'Oportunidades', data: 'Nome, e-mail, credenciais e os dados profissionais que você preencher, como currículo, experiências, preferências e respostas ao diagnóstico.', purpose: 'Manter seu perfil profissional, diagnóstico, recomendações, preferências e acompanhamento de oportunidades.' },
  empresas: { name: 'Empresas', data: 'Nome, e-mail, credenciais, dados da organização, vínculos e permissões corporativas, vagas e registros de publicação e pagamento.', purpose: 'Administrar seu acesso corporativo e os recursos das organizações para as quais você possui permissão.' },
  capacita: { name: 'Capacita', data: 'Nome, e-mail, credenciais, faixa etária autodeclarada, etapa de ensino, instituição quando informada e progresso nos conteúdos.', purpose: 'Organizar os conteúdos da sua etapa de ensino e acompanhar sua aprendizagem.' },
} as const;

export function TrailUsageNotice({ trail }: { trail: AuthTrail }) {
  const area = areas[trail];
  const responsible = String(import.meta.env.VITE_PRIVACY_RESPONSIBLE || '').trim();
  const contact = String(import.meta.env.VITE_PRIVACY_CONTACT_EMAIL || '').trim();
  const validContact = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(contact);
  return <details className="rounded-xl border border-ink/20 p-4 text-sm">
    <summary className="cursor-pointer font-bold text-brand-blue">Aviso de uso e privacidade — {area.name}</summary>
    <div className="mt-3 space-y-3 text-ink/80">
      <p><strong>Dados desta área.</strong> {area.data} {area.purpose}</p>
      <p><strong>Contas independentes.</strong> Esta conta pertence somente ao Trampolim {area.name}. O cadastro não cria contas nas outras áreas, nem sincroniza senhas ou sessões. Proteja sua senha e forneça informações corretas.</p>
      <p><strong>Compartilhamento.</strong> As vagas públicas podem aparecer em Oportunidades e Capacita. Para enviar uma candidatura, você preenche um formulário e autoriza o envio daqueles dados à empresa. Esse envio não concede acesso automático ao seu perfil de outra área.</p>
      {trail === 'empresas' ? <p><strong>Publicações.</strong> Criar a conta não verifica a empresa. A divulgação de estágio custa R$ 50,00 por vaga e depende da confirmação do pagamento e das validações de publicação. Senhas e dados de cartão não são apresentados a outros usuários.</p> : <p><strong>Seu controle.</strong> Na área autenticada você pode baixar seus dados e solicitar a exclusão da sua conta desta trilha. A exclusão não encerra suas contas das outras áreas.</p>}
      {trail === 'empresas' && <p><strong>Seu controle.</strong> No portal você pode baixar seus dados e excluir seu acesso pessoal. Responsáveis por organizações precisam tratar a transferência de responsabilidade antes da exclusão. Registros da organização são preservados.</p>}
      {trail === 'capacita' && <p><strong>Faixa etária.</strong> O cadastro é destinado a pessoas a partir de 14 anos. A faixa informada é autodeclarada; não envie documentos de identidade, senhas ou informações sensíveis nos campos de estudo.</p>}
      <p><strong>Comunicações de acesso.</strong> O e-mail informado é usado para confirmação da conta e recuperação de senha quando o serviço de envio está disponível. Links de acesso têm validade e pertencem somente a esta área.</p>
      {responsible && <p><strong>Responsável pela plataforma.</strong> {responsible}</p>}
      {validContact && <p><strong>Privacidade e atendimento.</strong> Para dúvidas ou solicitações sobre seus dados, escreva para <a href={`mailto:${contact}`} className="font-semibold text-brand-blue underline">{contact}</a>.</p>}
    </div>
  </details>;
}
