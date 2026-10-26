export type Skill = 'service' | 'sales' | 'admin' | 'numbers' | 'logistics' | 'maintenance' | 'production' | 'technology' | 'health' | 'marketing';
type Weights = Partial<Record<Skill, number>>;
export interface AnswerOption { label: string; weights: Weights }
export interface Question { prompt: string; options: AnswerOption[] }
export interface DiagnosticResult { trackId: 'people' | 'organization' | 'handsOn' | 'technology' | 'health' | 'marketing'; trackName: string; answers: number[]; scores: Record<Skill, number> }
export interface Opportunity { id: string; kind: 'course' | 'job'; title: string; provider: string; description: string; tags: Skill[]; url: string }

export const firstQuestion: Question = {
  prompt: 'O que você mais gosta de fazer no dia a dia?',
  options: [
    { label: 'Conversar e ajudar pessoas', weights: { service: 3, sales: 1 } },
    { label: 'Organizar tarefas e mexer com números', weights: { admin: 2, numbers: 2 } },
    { label: 'Trabalhar com as mãos e consertar coisas', weights: { maintenance: 2, logistics: 1 } },
    { label: 'Usar computadores e resolver desafios digitais', weights: { technology: 3 } },
    { label: 'Cuidar do bem-estar e apoiar pessoas', weights: { health: 3, service: 1 } },
    { label: 'Criar conteúdo e divulgar ideias ou produtos', weights: { marketing: 3, sales: 1, technology: 1 } },
  ],
};

const q = (prompt: string, choices: [string, Weights][]): Question => ({ prompt, options: choices.map(([label, weights]) => ({ label, weights })) });

export const trackQuestions: Question[][] = [
  [
    q('Em uma equipe, qual atividade mais combina com você?', [['Ouvir e orientar quem precisa de ajuda', { service: 2 }], ['Apresentar produtos e soluções', { sales: 2 }], ['Organizar pedidos e informações', { admin: 1, service: 1 }]]),
    q('Qual habilidade você quer desenvolver?', [['Comunicação e escuta', { service: 2 }], ['Negociação e persuasão', { sales: 2 }], ['Organização de rotinas', { admin: 2 }]]),
    q('Quando alguém tem um problema, você prefere...', [['Entender com calma o que aconteceu', { service: 2 }], ['Explicar opções e ajudar a decidir', { sales: 1, service: 1 }], ['Registrar as informações e encaminhar', { admin: 2 }]]),
    q('Qual rotina parece mais interessante?', [['Atender pessoas diferentes', { service: 2 }], ['Trabalhar com metas e resultados', { sales: 2 }], ['Conferir documentos e processos', { admin: 2 }]]),
    q('O que seria um bom dia de trabalho?', [['Ajudar alguém a resolver uma dúvida', { service: 2 }], ['Alcançar uma meta', { sales: 2 }], ['Deixar tudo organizado', { admin: 2 }]]),
    q('Em qual ambiente você se imagina?', [['Recepção ou central de atendimento', { service: 2 }], ['Loja ou equipe comercial', { sales: 2 }], ['Escritório administrativo', { admin: 2 }]]),
    q('Que desafio gostaria de aprender a resolver?', [['Atender reclamações com clareza', { service: 2 }], ['Entender o que o cliente procura', { sales: 2 }], ['Manter cadastros corretos', { admin: 2 }]]),
    q('Qual tarefa escolheria primeiro?', [['Orientar o público', { service: 2 }], ['Ajudar a escolher um produto', { sales: 2 }], ['Atualizar planilhas e arquivos', { admin: 1, numbers: 1 }]]),
    q('Qual função quer conhecer melhor?', [['Atendente ou recepcionista', { service: 2 }], ['Vendedor de loja', { sales: 2 }], ['Assistente administrativo', { admin: 2 }]]),
  ],
  [
    q('Que tipo de tarefa prefere organizar?', [['Documentos e agendas', { admin: 2 }], ['Contas e valores', { numbers: 2 }], ['Produtos e entregas', { logistics: 2 }]]),
    q('Qual é um ponto forte seu?', [['Perceber detalhes', { admin: 2 }], ['Fazer cálculos', { numbers: 2 }], ['Planejar etapas e prazos', { logistics: 2 }]]),
    q('Qual atividade parece mais interessante?', [['Preparar documentos', { admin: 2 }], ['Acompanhar pagamentos', { numbers: 2 }], ['Controlar estoque e pedidos', { logistics: 2 }]]),
    q('Com qual ferramenta gostaria de aprender?', [['Sistemas de cadastro', { admin: 2 }], ['Planilhas e controles', { numbers: 2 }], ['Sistemas de estoque', { logistics: 2 }]]),
    q('O que mais importa em uma tarefa?', [['Informações organizadas', { admin: 2 }], ['Precisão nos cálculos', { numbers: 2 }], ['Cumprir prazos', { logistics: 2 }]]),
    q('Qual curso escolheria primeiro?', [['Rotinas de escritório', { admin: 2 }], ['Excel e controles financeiros', { numbers: 2 }], ['Logística e estoque', { logistics: 2 }]]),
    q('Você encontra uma informação divergente. O que faz?', [['Confere o cadastro', { admin: 2 }], ['Refaz a conta', { numbers: 2 }], ['Acompanha o pedido', { logistics: 2 }]]),
    q('Qual resultado lhe daria satisfação?', [['Arquivos em dia', { admin: 2 }], ['Valores conferidos', { numbers: 2 }], ['Pedidos entregues corretamente', { logistics: 2 }]]),
    q('Qual função quer conhecer melhor?', [['Auxiliar de escritório', { admin: 2 }], ['Auxiliar financeiro ou caixa', { numbers: 2 }], ['Auxiliar de estoque', { logistics: 2 }]]),
  ],
  [
    q('Em qual ambiente gostaria de trabalhar?', [['Oficina ou manutenção', { maintenance: 2 }], ['Estoque ou depósito', { logistics: 2 }], ['Fábrica ou produção', { production: 2 }]]),
    q('Qual atividade prática chama sua atenção?', [['Identificar defeitos e reparar', { maintenance: 2 }], ['Separar e movimentar materiais', { logistics: 2 }], ['Montar ou embalar produtos', { production: 2 }]]),
    q('Que habilidade gostaria de aprender?', [['Usar ferramentas', { maintenance: 2 }], ['Conferir mercadorias', { logistics: 2 }], ['Operar máquinas com segurança', { production: 2 }]]),
    q('Quando algo para de funcionar, prefere...', [['Investigar e consertar', { maintenance: 2 }], ['Conferir o fluxo de materiais', { logistics: 2 }], ['Ajudar a retomar a produção', { production: 2 }]]),
    q('Qual rotina parece mais adequada?', [['Cuidar de equipamentos', { maintenance: 2 }], ['Separar pedidos e organizar o espaço', { logistics: 2 }], ['Executar etapas de fabricação', { production: 2 }]]),
    q('O que valoriza em um trabalho prático?', [['Resolver problemas concretos', { maintenance: 2 }], ['Manter materiais disponíveis', { logistics: 2 }], ['Ver um produto pronto', { production: 2 }]]),
    q('Qual curso escolheria primeiro?', [['Manutenção e ferramentas', { maintenance: 2 }], ['Logística e estoque', { logistics: 2 }], ['Operação e segurança', { production: 2 }]]),
    q('Como prefere aprender uma tarefa nova?', [['Observando um reparo', { maintenance: 2 }], ['Praticando a separação de itens', { logistics: 2 }], ['Repetindo etapas de operação', { production: 2 }]]),
    q('Qual resultado lhe daria satisfação?', [['Equipamento funcionando', { maintenance: 2 }], ['Estoque organizado', { logistics: 2 }], ['Produto finalizado com qualidade', { production: 2 }]]),
  ],
  [
    q('Qual atividade digital mais desperta sua curiosidade?', [['Montar ou configurar computadores', { technology: 2 }], ['Criar sites ou aplicativos', { technology: 2 }], ['Organizar e analisar dados', { technology: 2, numbers: 1 }]]),
    q('Que problema gostaria de resolver?', [['Ajudar alguém com computador ou celular', { technology: 2, service: 1 }], ['Encontrar um erro em um sistema', { technology: 2 }], ['Automatizar uma tarefa repetitiva', { technology: 2, admin: 1 }]]),
    q('Qual ferramenta gostaria de aprender?', [['Suporte técnico e redes', { technology: 2 }], ['Programação', { technology: 2 }], ['Planilhas e análise de dados', { technology: 2, numbers: 1 }]]),
    q('Como prefere trabalhar?', [['Ajudando usuários com tecnologia', { technology: 2, service: 1 }], ['Criando soluções digitais', { technology: 2 }], ['Testando sistemas e procurando falhas', { technology: 2 }]]),
    q('Qual tarefa parece mais interessante?', [['Instalar programas e equipamentos', { technology: 2 }], ['Escrever instruções para um computador', { technology: 2 }], ['Conferir informações em sistemas', { technology: 2, admin: 1 }]]),
    q('O que gostaria de aprender primeiro?', [['Fundamentos de informática', { technology: 2 }], ['Lógica de programação', { technology: 2 }], ['Segurança e boas práticas digitais', { technology: 2 }]]),
    q('Quando um aplicativo apresenta um problema, você prefere...', [['Orientar a pessoa a usar o recurso', { technology: 2, service: 1 }], ['Investigar o que causou o erro', { technology: 2 }], ['Registrar o problema para a equipe', { technology: 2, admin: 1 }]]),
    q('Qual resultado daria mais satisfação?', [['Computador funcionando corretamente', { technology: 2 }], ['Uma página ou programa criado por você', { technology: 2 }], ['Dados organizados e fáceis de entender', { technology: 2, numbers: 1 }]]),
    q('Qual função gostaria de conhecer melhor?', [['Auxiliar de suporte de TI', { technology: 2 }], ['Desenvolvedor iniciante', { technology: 2 }], ['Auxiliar de dados ou testes', { technology: 2, numbers: 1 }]]),
  ],
  [
    q('Em qual ambiente de saúde se imagina trabalhando?', [['Recepção de clínica ou unidade de saúde', { health: 2, service: 1 }], ['Farmácia ou laboratório', { health: 2 }], ['Apoio administrativo em hospital', { health: 2, admin: 1 }]]),
    q('Como gostaria de apoiar as pessoas?', [['Orientando sobre horários e atendimento', { health: 2, service: 1 }], ['Organizando informações e cadastros', { health: 2, admin: 1 }], ['Apoiando equipes com materiais e rotinas', { health: 2, logistics: 1 }]]),
    q('Qual habilidade quer desenvolver?', [['Comunicação acolhedora', { health: 2, service: 1 }], ['Organização de registros', { health: 2, admin: 1 }], ['Higiene e segurança no ambiente', { health: 2 }]]),
    q('Qual rotina parece mais interessante?', [['Receber e orientar pacientes', { health: 2, service: 1 }], ['Agendar consultas e atualizar cadastros', { health: 2, admin: 1 }], ['Controlar materiais e estoque', { health: 2, logistics: 1 }]]),
    q('O que considera importante em um serviço de saúde?', [['Acolhimento e respeito', { health: 2, service: 1 }], ['Atenção aos detalhes e registros', { health: 2, admin: 1 }], ['Organização e cuidado com os materiais', { health: 2, logistics: 1 }]]),
    q('Qual curso introdutório gostaria de explorar?', [['Atendimento em serviços de saúde', { health: 2, service: 1 }], ['Rotinas administrativas em clínicas', { health: 2, admin: 1 }], ['Noções de saúde e prevenção', { health: 2 }]]),
    q('Se uma pessoa estiver com dúvida, você prefere...', [['Ouvir e explicar onde buscar atendimento', { health: 2, service: 1 }], ['Conferir o agendamento e os dados', { health: 2, admin: 1 }], ['Chamar a equipe responsável quando necessário', { health: 2, service: 1 }]]),
    q('Qual tarefa escolheria?', [['Organizar prontuários e documentos', { health: 2, admin: 1 }], ['Receber pessoas na unidade', { health: 2, service: 1 }], ['Conferir materiais de uso da equipe', { health: 2, logistics: 1 }]]),
    q('Qual função gostaria de conhecer melhor?', [['Recepcionista de clínica', { health: 2, service: 1 }], ['Auxiliar administrativo de saúde', { health: 2, admin: 1 }], ['Auxiliar de estoque hospitalar', { health: 2, logistics: 1 }]]),
  ],
  [
    q('Que parte do marketing mais desperta sua curiosidade?', [['Criar textos, imagens e vídeos', { marketing: 2 }], ['Planejar campanhas e ações', { marketing: 2, admin: 1 }], ['Entender resultados e métricas', { marketing: 2, numbers: 1 }]]),
    q('Qual atividade gostaria de experimentar?', [['Criar publicações para redes sociais', { marketing: 2, technology: 1 }], ['Pesquisar o público e suas necessidades', { marketing: 2, sales: 1 }], ['Organizar um calendário de conteúdo', { marketing: 2, admin: 1 }]]),
    q('Qual habilidade quer desenvolver primeiro?', [['Escrita e comunicação', { marketing: 2, service: 1 }], ['Design e edição de conteúdo', { marketing: 2, technology: 1 }], ['Análise de campanhas e dados', { marketing: 2, numbers: 1 }]]),
    q('Uma pequena empresa quer divulgar um produto. O que você faria?', [['Prepararia uma publicação atraente', { marketing: 2 }], ['Identificaria quem pode se interessar', { marketing: 2, sales: 1 }], ['Acompanharia o alcance da divulgação', { marketing: 2, numbers: 1 }]]),
    q('Que ferramenta teria vontade de aprender?', [['Redes sociais e agendamento de posts', { marketing: 2, technology: 1 }], ['Edição de imagens e vídeos', { marketing: 2, technology: 1 }], ['Planilhas e relatórios de campanha', { marketing: 2, numbers: 1 }]]),
    q('Como prefere trabalhar em um projeto de divulgação?', [['Criando ideias e conteúdos', { marketing: 2 }], ['Conversando com clientes e parceiros', { marketing: 2, sales: 1 }], ['Organizando prazos e tarefas da equipe', { marketing: 2, admin: 1 }]]),
    q('O que faria ao perceber que uma campanha teve pouco alcance?', [['Testaria outro formato de conteúdo', { marketing: 2, technology: 1 }], ['Reveria o público e a mensagem', { marketing: 2, sales: 1 }], ['Compararia os dados para encontrar ajustes', { marketing: 2, numbers: 1 }]]),
    q('Qual resultado lhe daria mais satisfação?', [['Ver pessoas interagindo com o conteúdo', { marketing: 2, service: 1 }], ['Ajudar uma marca a ser reconhecida', { marketing: 2 }], ['Perceber melhoria nos resultados da campanha', { marketing: 2, numbers: 1 }]]),
    q('Qual função gostaria de conhecer melhor?', [['Assistente de marketing digital', { marketing: 2 }], ['Assistente de conteúdo e redes sociais', { marketing: 2, technology: 1 }], ['Assistente de comunicação e campanhas', { marketing: 2, sales: 1 }]]),
  ],
];

export const trackNames = ['Atendimento e relacionamento', 'Organização e números', 'Atividades práticas e operação', 'Tecnologia e informática', 'Saúde e bem-estar', 'Marketing e comunicação'] as const;
export const diagnosticQuestionCount = 10;

export const opportunities: Opportunity[] = [
  { id: 'customer-service', kind: 'course', title: 'Atendimento ao cliente', provider: 'SEBRAE · Cursos online', description: 'Desenvolva comunicação, escuta e relacionamento com clientes.', tags: ['service'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'sales', kind: 'course', title: 'Vendas e relacionamento comercial', provider: 'SEBRAE · Cursos online', description: 'Explore cursos de vendas, negociação e experiência do cliente.', tags: ['sales'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'office-routines', kind: 'course', title: 'Rotinas administrativas', provider: 'SENAI · Futuro.Digital', description: 'Explore cursos de organização de documentos e processos de escritório.', tags: ['admin'], url: 'https://www.futuro.digital/' },
  { id: 'excel', kind: 'course', title: 'Excel e controles numéricos', provider: 'SENAI · Futuro.Digital', description: 'Explore formações para planilhas, cálculos e acompanhamento de dados.', tags: ['numbers'], url: 'https://www.futuro.digital/' },
  { id: 'logistics', kind: 'course', title: 'Logística e controle de estoque', provider: 'SENAI · Futuro.Digital', description: 'Conheça cursos de armazenagem, movimentação e distribuição.', tags: ['logistics'], url: 'https://www.futuro.digital/' },
  { id: 'maintenance', kind: 'course', title: 'Manutenção e ferramentas', provider: 'SENAI · Futuro.Digital', description: 'Explore formações ligadas a reparos, equipamentos e manutenção.', tags: ['maintenance'], url: 'https://www.futuro.digital/' },
  { id: 'production', kind: 'course', title: 'Produção e segurança no trabalho', provider: 'SENAI · Futuro.Digital', description: 'Conheça cursos de operação, qualidade e segurança na produção.', tags: ['production'], url: 'https://www.futuro.digital/' },
  { id: 'ti-fundamentals', kind: 'course', title: 'Fundamentos de TI: hardware e software', provider: 'Fundação Bradesco · Escola Virtual', description: 'Curso introdutório sobre computadores, sistemas, hardware, software e segurança da informação.', tags: ['technology'], url: 'https://www.ev.org.br/cursos/fundamentos-de-ti-hardware-e-software' },
  { id: 'programming', kind: 'course', title: 'Programação e desenvolvimento de sistemas', provider: 'Fundação Bradesco · Escola Virtual', description: 'Explore cursos introdutórios de lógica, programação e desenvolvimento no catálogo da Escola Virtual.', tags: ['technology'], url: 'https://www.ev.org.br/' },
  { id: 'digital-support', kind: 'course', title: 'Informática e suporte técnico', provider: 'SENAI · Tecnologia da Informação', description: 'Consulte formações de informática, operação de computadores e suporte; ofertas e requisitos variam por região.', tags: ['technology'], url: 'https://www.sp.senai.br/cursos/cursos-livres/tecnologia-da-informacao-e-informatica' },
  { id: 'data-basics', kind: 'course', title: 'Planilhas e análise de dados', provider: 'Fundação Bradesco · Escola Virtual', description: 'Explore cursos de produtividade, planilhas e análise de dados para o trabalho.', tags: ['technology', 'numbers'], url: 'https://www.ev.org.br/cursos?nivel=0' },
  { id: 'health-courses', kind: 'course', title: 'Cursos introdutórios na área da saúde', provider: 'UNA-SUS', description: 'Consulte cursos abertos e confira os pré-requisitos de cada oferta; alguns são destinados a profissionais da saúde.', tags: ['health'], url: 'https://www.unasus.gov.br/cursos/sobre' },
  { id: 'health-prevention', kind: 'course', title: 'Saúde, prevenção e atenção primária', provider: 'UNA-SUS', description: 'Explore cursos disponíveis sobre saúde e prevenção e verifique o público elegível em cada inscrição.', tags: ['health'], url: 'https://www.unasus.gov.br/programa_modular/busca' },
  { id: 'health-service', kind: 'course', title: 'Atendimento e acolhimento em serviços de saúde', provider: 'UNA-SUS · catálogo de cursos', description: 'Pesquise conteúdos de acolhimento, atendimento e organização em saúde; confirme os requisitos da oferta.', tags: ['health', 'service'], url: 'https://www.unasus.gov.br/cursos/sobre' },
  { id: 'excel-basico', kind: 'course', title: 'Excel básico para o trabalho', provider: 'SENAI · Futuro.Digital', description: 'Aprenda fórmulas, tabelas e organização de dados para rotinas profissionais.', tags: ['numbers', 'admin'], url: 'https://www.futuro.digital/' },
  { id: 'financas-pessoais', kind: 'course', title: 'Educação financeira e controles', provider: 'SEBRAE · Cursos online', description: 'Conheça conceitos de orçamento, fluxo de caixa e controle financeiro.', tags: ['numbers'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'comunicacao', kind: 'course', title: 'Comunicação no ambiente de trabalho', provider: 'SEBRAE · Cursos online', description: 'Desenvolva comunicação profissional, colaboração e escuta ativa.', tags: ['service', 'sales'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'marketing-digital', kind: 'course', title: 'Marketing digital para negócios', provider: 'SEBRAE · Cursos online', description: 'Explore divulgação digital, conteúdo e relacionamento com clientes.', tags: ['marketing', 'sales'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'marketing-content-course', kind: 'course', title: 'Conteúdo para redes sociais', provider: 'SEBRAE · Cursos online', description: 'Aprenda a planejar conteúdos e organizar a presença de pequenos negócios nas redes sociais.', tags: ['marketing', 'technology'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'marketing-campaign-course', kind: 'course', title: 'Planejamento de campanhas digitais', provider: 'SEBRAE · Cursos online', description: 'Conheça etapas para planejar campanhas, definir público e acompanhar resultados.', tags: ['marketing', 'numbers', 'admin'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'empreendedorismo', kind: 'course', title: 'Empreendedorismo e plano de negócios', provider: 'SEBRAE · Cursos online', description: 'Conheça ferramentas para planejar e organizar pequenos negócios.', tags: ['admin', 'sales'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'atendimento-digital', kind: 'course', title: 'Atendimento por canais digitais', provider: 'SEBRAE · Cursos online', description: 'Explore boas práticas de atendimento por chat, e-mail e redes sociais.', tags: ['service'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'gestao-tempo', kind: 'course', title: 'Organização e gestão do tempo', provider: 'SEBRAE · Cursos online', description: 'Aprenda a priorizar tarefas e planejar uma rotina de trabalho.', tags: ['admin'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'assistente-rh', kind: 'course', title: 'Introdução a recursos humanos', provider: 'SENAI · Futuro.Digital', description: 'Conheça rotinas de apoio a pessoas, documentos e processos de RH.', tags: ['admin', 'service'], url: 'https://www.futuro.digital/' },
  { id: 'logistica-distribuicao', kind: 'course', title: 'Armazenagem e distribuição', provider: 'SENAI · Futuro.Digital', description: 'Explore processos de recebimento, armazenamento e expedição.', tags: ['logistics'], url: 'https://www.futuro.digital/' },
  { id: 'qualidade', kind: 'course', title: 'Qualidade nos processos produtivos', provider: 'SENAI · Futuro.Digital', description: 'Conheça práticas de inspeção, padronização e melhoria da produção.', tags: ['production', 'maintenance'], url: 'https://www.futuro.digital/' },
  { id: 'eletrica-basica', kind: 'course', title: 'Fundamentos de eletricidade', provider: 'SENAI · Futuro.Digital', description: 'Consulte formações introdutórias sobre instalações e segurança elétrica.', tags: ['maintenance'], url: 'https://www.futuro.digital/' },
  { id: 'seguranca-trabalho', kind: 'course', title: 'Segurança no ambiente de trabalho', provider: 'SENAI · Futuro.Digital', description: 'Aprenda princípios de prevenção de riscos e práticas seguras.', tags: ['production', 'maintenance', 'logistics'], url: 'https://www.futuro.digital/' },
  { id: 'vendas-online', kind: 'course', title: 'Vendas online e comércio eletrônico', provider: 'SEBRAE · Cursos online', description: 'Conheça etapas de venda e relacionamento em canais digitais.', tags: ['sales', 'service'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'assistente-financeiro', kind: 'course', title: 'Rotinas de apoio financeiro', provider: 'SENAI · Futuro.Digital', description: 'Explore contas a pagar, recebimentos e organização de registros.', tags: ['numbers', 'admin'], url: 'https://www.futuro.digital/' },
  { id: 'manutencao-industrial', kind: 'course', title: 'Introdução à manutenção industrial', provider: 'SENAI · Futuro.Digital', description: 'Conheça inspeção, conservação de equipamentos e manutenção preventiva.', tags: ['maintenance', 'production'], url: 'https://www.futuro.digital/' },
  { id: 'planilhas-avancadas', kind: 'course', title: 'Planilhas para controle de negócios', provider: 'SEBRAE · Cursos online', description: 'Explore recursos de planilhas para organizar informações e acompanhar resultados.', tags: ['numbers', 'admin'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'logistica-suprimentos', kind: 'course', title: 'Fundamentos de logística e suprimentos', provider: 'SENAI · Futuro.Digital', description: 'Conheça o fluxo de materiais, compras, armazenagem e abastecimento.', tags: ['logistics', 'admin'], url: 'https://www.futuro.digital/' },
  { id: 'relacionamento-cliente', kind: 'course', title: 'Relacionamento e fidelização de clientes', provider: 'SEBRAE · Cursos online', description: 'Aprenda práticas para entender necessidades e construir boas relações com clientes.', tags: ['service', 'sales'], url: 'https://sebrae.com.br/sites/PortalSebrae/cursosonline' },
  { id: 'ferramentas-digitais-trabalho', kind: 'course', title: 'Ferramentas digitais para o trabalho', provider: 'Fundação Bradesco · Escola Virtual', description: 'Explore cursos de produtividade, tecnologia e organização digital para aplicar nas rotinas profissionais.', tags: ['technology', 'admin', 'numbers'], url: 'https://www.ev.org.br/cursos?nivel=0' },
  { id: 'seguranca-digital', kind: 'course', title: 'Segurança digital e proteção de dados', provider: 'Fundação Bradesco · Escola Virtual', description: 'Conheça práticas de segurança da informação e proteção de dados aplicadas ao uso de tecnologia.', tags: ['technology'], url: 'https://www.ev.org.br/cursos?nivel=0' },
  { id: 'service-role', kind: 'job', title: 'Atendente ou recepcionista', provider: 'Atendimento ao público', description: 'Pesquise oportunidades de atendimento e recepção.', tags: ['service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'sales-role', kind: 'job', title: 'Vendedor ou consultor de loja', provider: 'Vendas e comércio', description: 'Pesquise oportunidades de vendas e comércio.', tags: ['sales'], url: 'https://servicos.mte.gov.br/' },
  { id: 'admin-role', kind: 'job', title: 'Assistente administrativo', provider: 'Rotinas de escritório', description: 'Pesquise oportunidades administrativas.', tags: ['admin'], url: 'https://servicos.mte.gov.br/' },
  { id: 'cashier-role', kind: 'job', title: 'Operador de caixa', provider: 'Comércio e operações', description: 'Pesquise oportunidades de caixa e operações comerciais.', tags: ['numbers', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'stock-role', kind: 'job', title: 'Auxiliar de estoque ou logística', provider: 'Estoque e distribuição', description: 'Pesquise oportunidades de estoque e logística.', tags: ['logistics'], url: 'https://servicos.mte.gov.br/' },
  { id: 'maintenance-role', kind: 'job', title: 'Auxiliar de manutenção', provider: 'Manutenção e reparos', description: 'Pesquise oportunidades de manutenção.', tags: ['maintenance'], url: 'https://servicos.mte.gov.br/' },
  { id: 'production-role', kind: 'job', title: 'Auxiliar de produção', provider: 'Operação e fabricação', description: 'Pesquise oportunidades de produção.', tags: ['production'], url: 'https://servicos.mte.gov.br/' },
  { id: 'it-support-role', kind: 'job', title: 'Auxiliar de suporte de TI', provider: 'Tecnologia e informática', description: 'Pesquise vagas iniciais de suporte, instalação e orientação básica a usuários.', tags: ['technology', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'computer-operator-role', kind: 'job', title: 'Operador de computador', provider: 'Tecnologia e informática', description: 'Pesquise oportunidades de operação de computadores e apoio a rotinas digitais.', tags: ['technology', 'admin'], url: 'https://servicos.mte.gov.br/' },
  { id: 'data-assistant-role', kind: 'job', title: 'Auxiliar de dados e cadastros', provider: 'Tecnologia e administração', description: 'Pesquise posições de atualização de sistemas, cadastros e conferência de informações.', tags: ['technology', 'admin', 'numbers'], url: 'https://servicos.mte.gov.br/' },
  { id: 'digital-content-role', kind: 'job', title: 'Assistente de conteúdo digital', provider: 'Tecnologia e comunicação', description: 'Pesquise vagas de apoio a conteúdo, publicação e organização de canais digitais.', tags: ['technology', 'sales'], url: 'https://servicos.mte.gov.br/' },
  { id: 'clinic-reception-role', kind: 'job', title: 'Recepcionista de clínica ou laboratório', provider: 'Saúde e atendimento', description: 'Pesquise vagas de recepção, agendamento e orientação em clínicas e laboratórios.', tags: ['health', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'health-admin-role', kind: 'job', title: 'Auxiliar administrativo em saúde', provider: 'Saúde e administração', description: 'Pesquise posições de apoio a cadastros, documentos e rotinas administrativas em serviços de saúde.', tags: ['health', 'admin'], url: 'https://servicos.mte.gov.br/' },
  { id: 'hospital-stock-role', kind: 'job', title: 'Auxiliar de estoque hospitalar', provider: 'Saúde e logística', description: 'Pesquise vagas de recebimento, controle e organização de materiais em serviços de saúde.', tags: ['health', 'logistics'], url: 'https://servicos.mte.gov.br/' },
  { id: 'health-customer-role', kind: 'job', title: 'Atendente de serviços de saúde', provider: 'Saúde e atendimento', description: 'Pesquise vagas de atendimento e orientação ao público em unidades e serviços de saúde.', tags: ['health', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'sales-assistant-role', kind: 'job', title: 'Auxiliar de vendas', provider: 'Vendas e comércio', description: 'Pesquise vagas de apoio a equipes comerciais e atendimento em lojas.', tags: ['sales', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'call-center-role', kind: 'job', title: 'Operador de teleatendimento', provider: 'Atendimento ao público', description: 'Pesquise oportunidades em centrais de atendimento e relacionamento.', tags: ['service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'hotel-reception-role', kind: 'job', title: 'Recepcionista de hotel ou clínica', provider: 'Recepção e serviços', description: 'Pesquise vagas de recepção, cadastro e orientação ao público.', tags: ['service', 'admin'], url: 'https://servicos.mte.gov.br/' },
  { id: 'cashier-retail-role', kind: 'job', title: 'Atendente de comércio', provider: 'Varejo e atendimento', description: 'Pesquise oportunidades em lojas, mercados e outros comércios.', tags: ['service', 'sales'], url: 'https://servicos.mte.gov.br/' },
  { id: 'office-assistant-role', kind: 'job', title: 'Auxiliar de escritório', provider: 'Rotinas administrativas', description: 'Pesquise vagas de apoio a documentos, agendas e rotinas de escritório.', tags: ['admin'], url: 'https://servicos.mte.gov.br/' },
  { id: 'hr-assistant-role', kind: 'job', title: 'Auxiliar de recursos humanos', provider: 'Administração de pessoal', description: 'Pesquise posições de apoio a cadastros, atendimento interno e documentos.', tags: ['admin', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'finance-assistant-role', kind: 'job', title: 'Auxiliar financeiro', provider: 'Finanças e controles', description: 'Pesquise oportunidades de apoio a pagamentos, recebimentos e planilhas.', tags: ['numbers', 'admin'], url: 'https://servicos.mte.gov.br/' },
  { id: 'billing-role', kind: 'job', title: 'Auxiliar de cobrança', provider: 'Financeiro e relacionamento', description: 'Pesquise vagas de negociação, acompanhamento de pagamentos e atendimento.', tags: ['numbers', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'inventory-role', kind: 'job', title: 'Conferente de mercadorias', provider: 'Estoque e distribuição', description: 'Pesquise posições de recebimento e conferência de produtos.', tags: ['logistics', 'numbers'], url: 'https://servicos.mte.gov.br/' },
  { id: 'warehouse-role', kind: 'job', title: 'Separador de pedidos', provider: 'Estoque e distribuição', description: 'Pesquise vagas de separação, embalagem e preparação de pedidos.', tags: ['logistics'], url: 'https://servicos.mte.gov.br/' },
  { id: 'delivery-assistant-role', kind: 'job', title: 'Ajudante de carga e descarga', provider: 'Logística e transporte', description: 'Pesquise oportunidades de movimentação e organização de mercadorias.', tags: ['logistics', 'production'], url: 'https://servicos.mte.gov.br/' },
  { id: 'machine-operator-role', kind: 'job', title: 'Operador de máquinas', provider: 'Indústria e produção', description: 'Pesquise vagas de operação de equipamentos e acompanhamento de processos.', tags: ['production', 'maintenance'], url: 'https://servicos.mte.gov.br/' },
  { id: 'quality-inspector-role', kind: 'job', title: 'Inspetor de qualidade', provider: 'Indústria e qualidade', description: 'Pesquise posições de inspeção e conferência de produtos e processos.', tags: ['production', 'numbers'], url: 'https://servicos.mte.gov.br/' },
  { id: 'maintenance-assistant-role', kind: 'job', title: 'Ajudante de manutenção predial', provider: 'Manutenção e serviços', description: 'Pesquise vagas de apoio a reparos e conservação de instalações.', tags: ['maintenance'], url: 'https://servicos.mte.gov.br/' },
  { id: 'mechanic-assistant-role', kind: 'job', title: 'Auxiliar de mecânico', provider: 'Manutenção e reparos', description: 'Pesquise oportunidades de apoio à manutenção de veículos e equipamentos.', tags: ['maintenance'], url: 'https://servicos.mte.gov.br/' },
  { id: 'marketing-assistant-role', kind: 'job', title: 'Auxiliar de marketing', provider: 'Comunicação e negócios', description: 'Pesquise vagas de apoio a conteúdo, campanhas e atendimento digital.', tags: ['marketing', 'sales', 'admin'], url: 'https://servicos.mte.gov.br/' },
  { id: 'social-media-role', kind: 'job', title: 'Assistente de redes sociais', provider: 'Marketing e comunicação', description: 'Pesquise vagas de apoio a publicações, calendário editorial e interação com o público.', tags: ['marketing', 'technology', 'service'], url: 'https://servicos.mte.gov.br/' },
  { id: 'marketing-campaign-role', kind: 'job', title: 'Assistente de comunicação e campanhas', provider: 'Marketing e negócios', description: 'Pesquise oportunidades de apoio a campanhas, divulgação e acompanhamento de resultados.', tags: ['marketing', 'numbers', 'sales'], url: 'https://servicos.mte.gov.br/' },
  { id: 'purchasing-assistant-role', kind: 'job', title: 'Assistente de compras', provider: 'Administração e suprimentos', description: 'Pesquise oportunidades de cotação, cadastro de fornecedores e acompanhamento de pedidos.', tags: ['admin', 'numbers', 'logistics'], url: 'https://servicos.mte.gov.br/' },
  { id: 'production-packer-role', kind: 'job', title: 'Auxiliar de embalagem', provider: 'Indústria e produção', description: 'Pesquise vagas de embalagem, identificação e preparação de produtos para envio.', tags: ['production', 'logistics'], url: 'https://servicos.mte.gov.br/' },
  { id: 'internal-communications-role', kind: 'job', title: 'Assistente de comunicação interna', provider: 'Marketing e comunicação', description: 'Pesquise oportunidades de apoio a comunicados, campanhas internas e produção de conteúdo para equipes.', tags: ['marketing', 'service', 'admin'], url: 'https://servicos.mte.gov.br/' },
];

export function createDiagnosticResult(answers: number[]): DiagnosticResult {
  const firstAnswer = answers[0] ?? 0;
  const trackIndex = Number.isInteger(firstAnswer) && firstAnswer >= 0 && firstAnswer < trackQuestions.length ? firstAnswer : 0;
  const trackId = (['people', 'organization', 'handsOn', 'technology', 'health', 'marketing'] as const)[trackIndex];
  const scores: Record<Skill, number> = { service: 0, sales: 0, admin: 0, numbers: 0, logistics: 0, maintenance: 0, production: 0, technology: 0, health: 0, marketing: 0 };
  const options = [firstQuestion.options[trackIndex], ...trackQuestions[trackIndex].map((question, index) => question.options[answers[index + 1] ?? 0])];
  options.forEach(option => Object.entries(option?.weights ?? {}).forEach(([skill, points]) => { scores[skill as Skill] += points ?? 0; }));
  return { trackId, trackName: trackNames[trackIndex], answers, scores };
}

export function getRecommendations(result: DiagnosticResult | null, kind: Opportunity['kind']): Opportunity[] {
  const available = opportunities.filter(item => item.kind === kind);
  if (!result) return available;
  return available
    .map(item => ({ item, score: getRecommendationScore(result, item.tags) }))
    .filter(entry => entry.score > 0)
    .sort((a, b) => b.score - a.score)
    .map(entry => entry.item);
}

export function getRecommendationScore(result: DiagnosticResult, tags: Skill[]): number {
  return [...new Set(tags)].reduce((sum, tag) => sum + Math.max(0, result.scores[tag] ?? 0), 0);
}

export function getRecommendationReason(result: DiagnosticResult, tags: Skill[]): { skills: Skill[]; points: number } | null {
  const relevant = [...new Set(tags)]
    .map(skill => ({ skill, points: result.scores[skill] ?? 0 }))
    .filter(entry => entry.points > 0)
    .sort((a, b) => b.points - a.points);
  if (!relevant.length) return null;
  return { skills: relevant.slice(0, 2).map(entry => entry.skill), points: relevant.reduce((total, entry) => total + entry.points, 0) };
}

export function loadDiagnosticResult(): DiagnosticResult | null {
  try {
    const raw = sessionStorage.getItem('trampolim_diagnostic_result');
    if (!raw) return null;
    const result = JSON.parse(raw) as DiagnosticResult;
    return Array.isArray(result.answers) && result.answers.length === diagnosticQuestionCount && result.scores ? result : null;
  } catch { return null; }
}
