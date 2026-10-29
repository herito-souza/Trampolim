# Fundação SaaS da Trampolim

## Arquitetura atual

A aplicação é um monólito modular: React 19, Vite, TypeScript e Tailwind CSS v4; Java 21, Spring Boot, Spring Security/JWT, Spring Data/JDBC, PostgreSQL e Flyway. Oportunidades permanece no banco legado principal; Empresas e Capacita possuem bancos, conexões e históricos Flyway independentes. Integrações entre trilhas devem usar endpoints/DTOs controlados, sem SQL entre bancos.

## Organizações e associação

Organizações institucionais, membros, convites, vínculos com empresas e auditoria residem no banco Empresas, criados por `V8__independent_organizations_and_associations.sql`. A administração global autentica pela identidade de Oportunidades, mas as rotas `/api/admin/saas/organizations/**` consultam e alteram a base Empresas. A associação usa identidade/JWT Empresas em `/api/empresas/association/organizations/**`; convites para novos membros criam contas Empresas independentes em `/api/empresas/auth/organization-invite/register`, enquanto contas Empresas existentes aceitam convite em `/api/empresas/organizations/invites/accept`. Convites expirados, reutilizados ou de organização inativa são rejeitados.

O dashboard global reúne métricas por conexões separadas. Os painéis institucionais fornecem contagens agregadas e vagas públicas, sem expor currículos, contatos de candidatos, candidaturas individuais nem dados comerciais privados. O portal pode direcionar um usuário autenticado Empresas a uma empresa, associação ou a ambas conforme os vínculos concedidos.

As rotas institucionais antigas em `/api/legacy/**` são compatibilidade temporária, desativadas por padrão com `LEGACY_ORGANIZATIONS_ENABLED=false`. A migração administrativa `GET/POST /api/admin/empresas/legacy-organizations/{preview,execute}` copia metadados sem transferir senha, token ou sessão. Ela depende da V8 e da migração anterior das empresas para resolver seus vínculos. A origem não é apagada.

## Autorização

- Administração global: bootstrap compatível via `ADMIN_EMAILS` e papéis de plataforma no banco Oportunidades.
- Administração de empresa e associação: papéis e vínculos no banco Empresas, conferidos no backend.
- Capacita e Oportunidades mantêm autenticação independente.
- Logout invalida o `token_version` na base da trilha corrente; portanto, revoga as sessões existentes daquela conta apenas nessa trilha. A interface também remove localmente somente o token da área encerrada.
- A visibilidade no frontend não substitui autorização no servidor.

## Pagamentos e planos

A divulgação de estágio custa R$ 50,00 por vaga. O backend define o valor, associa pedido, empresa e vaga, previne reutilização e confirma apenas após validar evento e consultar o provedor. Sem `MERCADOPAGO_ACCESS_TOKEN` e `MERCADOPAGO_WEBHOOK_SECRET`, checkout não fica disponível e vaga não é marcada como paga por ação do navegador. Tabelas de planos/assinaturas são apenas preparação: não há preço recorrente nem cobrança automática.

## Pendências e operação

- Migrações V8 e anteriores, migrações de dados legados e testes PostgreSQL ainda não foram executados nesta máquina. O Docker Engine não está acessível; não foram alterados bancos ou dados reais.
- Antes de migrar, gerar backup restaurável, executar inventário e preview, testar em cópia e reconciliar os resultados. Preservar tabelas de origem até aprovação e validação pós-migração.
- Dados educacionais legados, contas com múltiplas funções, candidaturas e pagamentos históricos precisam de procedimentos próprios; não criar automaticamente contas Capacita a partir de perfis antigos.
- Conteúdo Capacita ainda é administrado globalmente; gestão por organização educacional/parceira ainda não existe.
- Backup/restore, retenção configurável e exportação/exclusão por organização exigem procedimento operacional de produção. A exportação/exclusão de conta pessoal existente é independente.
- Testes de integração PostgreSQL, isolamento de endpoints, migrações e indisponibilidade de uma base devem passar antes de produção.

## Configuração de produção

Armazenar segredos em gerenciador seguro; não os versionar. Configurar URLs/credenciais e chaves JWT independentes, `ADMIN_EMAILS` somente como bootstrap, CORS com origens aprovadas, SMTP se e-mail for habilitado, credenciais/webhook do Mercado Pago e webhook HTTPS. Aplicar migrações primeiro em homologação e validar backup/restauração. Métricas de demonstração não devem ser apresentadas como dados operacionais.
