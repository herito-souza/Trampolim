# Trampolim — referência técnica

Atualizada em 12/11/2026 a partir da implementação local. Esta é a entrada principal da documentação. Os relatórios em [histórico](historico/README.md) registram decisões e testes de versões específicas; não devem ser interpretados como comprovação permanente do estado atual.

## Produto e limites

Ecossistema de empregabilidade e educação com implantação inicial planejada em Irecê-BA e região e expansão por configuração territorial. A página institucional encaminha para Oportunidades, Empresas e Capacita. Não há empresas, oportunidades, clientes ou estatísticas fictícias intencionalmente cadastradas pelas novas migrações. Fixtures de teste devem ficar isoladas.

Diagnósticos e pontuações são orientação explicável, sem garantia de contratação. Conquistas internas não equivalem a certificado oficial. Indicadores representam somente registros da plataforma, não todo o mercado de trabalho regional. A implementação não certifica conformidade jurídica integral, acessibilidade integral ou capacidade para milhares de usuários.

## Arquitetura e isolamento

Monorepo: [frontend](../frontend/) React, TypeScript, Vite e Tailwind CSS v4; [backend](../backend/) Java 21, Spring Boot, Spring Security/JWT, JPA/JDBC, PostgreSQL e Flyway. A stack concreta está nos arquivos de dependências, que prevalecem sobre versões citadas em relatórios antigos.

As três trilhas possuem contas, sessões, JWT, conexões e históricos Flyway independentes. Uma conta não cria outra automaticamente. Oportunidades utiliza o banco principal; Empresas e Capacita usam datasources/transaction managers próprios. Comunicação autorizada ocorre por APIs/DTOs, com minimização de dados e segredo interno quando necessário. Não existe acesso livre entre bancos privados.

| Trilha | Responsabilidades | Histórico Flyway |
| --- | --- | --- |
| Oportunidades | Candidatos, currículo, diagnóstico, Radar, plano, interesses, portfólio e consentimentos | [migration](../backend/src/main/resources/db/migration/) — até V23 nesta versão |
| Empresas | Organizações, membros, vagas, pagamentos, recrutamento, demandas futuras, Hub, feirão, desafios e agregados | [migration-empresas](../backend/src/main/resources/db/migration-empresas/) — até V21 |
| Capacita | Estudantes/professores, aprendizagem, mídia, documentos, avaliações, competências, Passaporte e agregados | [migration-capacita](../backend/src/main/resources/db/migration-capacita/) — até V24 |

A autorização empresarial é por organização e recurso; links ocultos no frontend não substituem validação no backend. PlatformAccess e LearningAccess preservam os mecanismos administrativos existentes. Concessão de papéis não deve tornar credenciais de outra trilha válidas.

## Funcionalidades e integração

| Área | Recursos implementados | Referência detalhada |
| --- | --- | --- |
| Navegação e experiência | Sidebar/Topbar, perfis autorizados, foto privada, ações prioritárias, formulários progressivos e responsividade | [navegação](historico/NAVIGACAO_REVISAO_2026-11-06.md), [dashboards](historico/DASHBOARDS_REVISAO_2026-11-06.md), [formulários](historico/FORMULARIOS_REVISAO_2026-11-06.md) |
| Busca territorial | Município IBGE, UF/região configurável, filtros combinados, lista e mapa sob demanda com coordenadas existentes | [busca](historico/BUSCA_REGIONAL.md), [mapa](historico/MAPA_REGIONAL.md) |
| Radar e preparação | Competências declaradas/comprovadas, lacunas e pontuação explicável, interesses futuros e recomendações reais | [Radar](historico/RADAR_COMPETENCIAS_PRIORIDADE1.md), [demandas futuras](historico/BANCO_TALENTOS_FUTUROS_PRIORIDADE2.md) |
| Plano de Empregabilidade | Objetivos revisáveis, etapas, progresso e próxima ação, reutilizando diagnóstico, Radar, cursos e vagas | [backend](historico/PLANO_EMPREGABILIDADE_2026-11-07.md), [interface](historico/MEU_PLANO_INTERFACE_2026-11-07.md) |
| Recrutamento | Etapas, histórico, notas internas protegidas, filtros, paginação e concorrência | [recrutamento](historico/RECRUTAMENTO_EMPRESAS_2026-11-06.md) |
| Capacita | Cursos → disciplinas → módulos → aulas, PDFs, avaliações, progresso e competências | [aulas](historico/CAPACITA_VIDEOAULAS_PARTE1.md), [biblioteca](historico/CAPACITA_BIBLIOTECA_PARTE3.md), [avaliações](historico/CAPACITA_AVALIACOES_PARTE4.md) |
| Comunicação | Mural privado, respostas individuais/em lote, notificações persistidas, preferências e entrega controlada | [mural](historico/CAPACITA_MURAL_PARTE2.md), [notificações](historico/NOTIFICACOES_BACKEND_2026-11-07.md) |
| Passaporte | Aprendizagem adaptativa, evidências verificáveis, compartilhamento explícito e revogação | [Passaporte](historico/CAPACITACAO_ADAPTATIVA_PASSAPORTE_PRIORIDADE4.md) |
| Institucional | Associações, vínculos empresariais, permissões, programas, campanhas e limites comerciais | [Hub](historico/TRAMPOLIM_HUB_PRIORIDADE3.md), [fundação SaaS](historico/SAAS_ARCHITECTURE.md) |
| Feirão | Backend de eventos, agenda, empresas confirmadas, referências a vagas/cursos e inscrições independentes | [feirão](historico/FEIRAO_BACKEND_2026-11-11.md) |
| Desafios profissionais | Backend de propostas, revisão independente, rubrica, limite de participação, resultados e vínculo consentido com o Passaporte | [desafios](historico/DESAFIOS_PROFISSIONAIS_2026-11-11.md) |
| Observatório v2 | Capturas transacionais em segundo plano, filtros mensais/geográficos, indicadores definidos e supressão de grupos pequenos | [métricas/endpoints](historico/OBSERVATORIO_INDICADORES_2026-11-11.md) |

As novas interfaces do feirão, da revisão/importação de desafios profissionais e do Observatório v2 ainda dependem de etapa frontend. O endpoint antigo do Observatório permanece compatível e dinâmico; o benefício da camada preparada exige migrar seus consumidores. Não assumir que uma funcionalidade backend já possui tela.

## Regras importantes

- Divulgação de estágio: R$ 50 por anúncio, validado pelo servidor. Publicação exige pagamento confirmado pelo provedor e aprovações aplicáveis; clique no navegador não confirma pagamento. O meio de pagamento precisa de configuração externa.
- Videoaulas: duração real validada no backend entre 180 e 300 segundos. Arquivos fora do PostgreSQL; processamento depende de FFmpeg/FFprobe. Não presumir legendas/transcrição sem conteúdo fornecido.
- Avaliações: correção no backend, tentativas preservadas e critérios de aprovação. Evidência empresarial não deve ser convertida em tentativa acadêmica fictícia.
- Desafios: participação voluntária, finalidade educacional, direitos do autor preservados e proibição de uso produtivo/comercial disfarçado. Moderação humana necessária. Desafios antigos precisam de revisão; entregas legadas sem revisão identificável não recebem aprovação automática.
- Feirão: inscrição não reserva emprego ou assento; contagens representam contas, não pessoas físicas únicas. Vagas são referências aos registros originais.
- Observatório: publicações registradas, estoque ativo, relatos de entrevista e confirmações são métricas diferentes. Métricas pessoais usam meses/UF e limiares/faixas; não se infere município educacional a partir da residência ou do curso.

## Configuração e execução

Pré-requisitos: Java 21; Node.js compatível com a versão do Vite do projeto; Docker Compose; executáveis FFmpeg/FFprobe para validar/processar mídia. As configurações completas estão em [backend/.env.example](../backend/.env.example), [frontend/.env.example](../frontend/.env.example) e [application.properties](../backend/src/main/resources/application.properties). Spring Boot não carrega automaticamente qualquer arquivo .env: disponibilize as variáveis no ambiente/processo conforme o comando de inicialização utilizado.

No PowerShell, a partir da raiz do checkout:

~~~powershell
Set-Location backend
docker compose up -d
.\mvnw.cmd spring-boot:run
~~~

Em outro terminal, na raiz:

~~~powershell
Set-Location frontend
npm ci
npm run dev
~~~

A interface local usa http://localhost:5173 e a API http://localhost:8080. Os bancos locais ficam em 5432, 5433 e 5434 conforme [Compose](../backend/docker-compose.yml). Credenciais de demonstração do ambiente local não são credenciais de produção. Nunca publicar .env real, tokens, backups, currículos, mídia privada ou logs com dados de usuários.

Variáveis por família:

| Configuração | Finalidade |
| --- | --- |
| OPORTUNIDADES_DB_*, EMPRESAS_DB_*, CAPACITA_DB_* | URLs e credenciais independentes; conferir precedência com DATABASE_* nos exemplos |
| Segredos JWT por trilha | Assinatura, emissor/audiência e sessões independentes; usar nomes concretos dos exemplos atuais |
| RADAR_INTERNAL_API_URL e RADAR_BRIDGE_SECRET | APIs controladas entre trilhas; rede privada/HTTPS e segredo forte |
| CAPACITA_FFMPEG e CAPACITA_FFPROBE | Processamento confiável de mídia |
| Configurações de armazenamento Capacita | Caminhos/serviço de arquivos privados; não tornar a pasta pública |
| SMTP e Mercado Pago | Integrações externas pendentes de credenciais/homologação; não simular entrega ou pagamento |
| OBSERVATORY_CAPTURE_ENABLED e OBSERVATORY_V2_REFRESH_MS | Captura periódica, padrão de quinze minutos para v2 |

Irecê é referência inicial por configuração territorial e catálogo IBGE; novos municípios/UFs não devem exigir duplicar a aplicação. Ver [configuração geográfica](../backend/src/main/resources/geography/).

## Testes e evidências

Frontend, dentro de frontend:

~~~powershell
npm run lint
npm test
npm run build
npm run test:e2e
~~~

Playwright exige navegadores instalados e as condições descritas em sua configuração. Não interpretar build como teste de todos os fluxos de navegador.

Backend: [ops/scripts/Test-BackendSecurity.ps1](../ops/scripts/Test-BackendSecurity.ps1) prepara schemas aleatórios nos três bancos Docker locais, desativa envios reais e remove os schemas ao terminar. Execute da raiz:

~~~powershell
.\ops\scripts\Test-BackendSecurity.ps1 -EvidenceName validacao-local
~~~

Configure FFmpeg/FFprobe para incluir processamento real de mídia nos testes. O modo de carga é opcional e não deve usar dados pessoais reais. Para testes unitários convencionais e build, utilize o Maven Wrapper do backend.

Última regressão completa de backend desta consolidação: **11/11/2026**, 266 testes registrados, **265 aprovados**, zero falhas/erros e um ensaio opcional de carga não habilitado. [Resumo versionado da evidência](evidencias/backend-2026-11-11.json). Estes resultados não comprovam infraestrutura de produção nem capacidade de carga. Logs brutos e capturas locais não acompanham esta publicação; os scripts permitem reprodução. Em **12/11/2026**, lint, 23 testes unitários e build/typecheck do frontend passaram. O build mantém avisos de tamanho dos módulos de mapa/PDF; não houve novo ensaio de desempenho ou execução de Playwright nesta organização documental. [Evidência frontend](evidencias/frontend-2026-11-12.json).

## Segurança e implantação

Preservar autorização no servidor, isolamento por organização, JWT por trilha, CORS restrito, proteção de arquivos, validações de upload, limites de requisições, idempotência, tratamento seguro de erros e logs sem segredos. Os documentos de [segurança](historico/SEGURANCA_BACKEND_2026-11-06.md) e [qualidade](historico/QA_IMPLANTACAO_IRECE.md) são registros de auditorias, não certificado permanente de segurança.

Antes de produção: backups independentes e ensaio de restauração; homologação das migrações novas em cópia isolada; segredos exclusivos; HTTPS/proxy; armazenamento privado; SMTP/pagamentos em ambiente de testes; monitoramento; política de retenção e atendimento; revisão de acessibilidade e testes de carga representativos. Não ativar funcionalidades pagas, planos ou serviços externos sem decisão/configuração apropriadas.

## Pendências conhecidas

1. Interfaces dos módulos exclusivamente backend descritos acima e migração do dashboard para o Observatório v2.
2. Credenciais/homologação de SMTP e pagamentos; infraestrutura operacional de mídia, backups e monitoramento.
3. Revisão de desafios e entregas legadas, preservando consentimentos e critérios aceitos.
4. Indicadores educacionais municipais: fonte atual só possui UF autorizada; nenhuma atribuição municipal foi inventada.
5. Medições representativas de carga, disponibilidade e acessibilidade em produção. Resultados locais não autorizam afirmar suporte a milhares de pessoas.
6. Revisão contínua da documentação quando código, contratos, dependências ou configuração mudarem.

## Como manter a documentação

Atualizar este documento para o estado vigente. Relatórios datados pertencem a [historico/](historico/README.md), preservando contexto, data, testes e limitações. Manter evidências pequenas e sanitizadas em evidencias/; guardar logs brutos apenas localmente. Evitar novos diagnósticos soltos na raiz. Links relativos devem continuar funcionando ao mover um arquivo.
