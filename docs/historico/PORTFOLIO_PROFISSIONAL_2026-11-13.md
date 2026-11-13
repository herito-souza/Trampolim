# Portfólio profissional compartilhável — 13/11/2026

## Auditoria e implementação

O backend e o painel do módulo Carreira já possuíam itens de portfólio, evidências de competências, compartilhamentos com destinatário, expiração e revogação. A tela foi complementada para reutilizar experiências do perfil Oportunidades: o titular pode preencher um item a partir de uma experiência existente, revisar a cópia e salvar. Essa ação não publica nem compartilha os dados automaticamente.

Foi adicionada uma prévia dos itens selecionados antes da geração do link. A interface diferencia itens declarados, avaliações de desafios sujeitas à revalidação e competências verificadas no Capacita. Link, validade, destinatário e consentimento são controlados pelo usuário.

## Privacidade e acesso

- Nenhum link é emitido até o titular selecionar itens, informar o e-mail do recrutador, escolher prazo e marcar consentimento explícito.
- O compartilhamento é privado e restrito à conta empresarial cujo e-mail coincide com o destinatário. Não é um perfil público indexável.
- O token contém 256 bits aleatórios; somente seu hash é persistido. O link expira em 1 a 30 dias e pode ser revogado pelo titular.
- Apenas UUIDs de itens e comprovantes pertencentes ao próprio usuário são aceitos; tentativas de selecionar registros alheios retornam não encontrado.
- Currículo, foto, contatos, endereço, candidaturas e outros documentos privados não fazem parte do payload compartilhado.
- Evidências educacionais são revalidadas pelo Capacita no momento da consulta. Itens declarados permanecem identificados como declarações; avaliação empresarial de desafio não é apresentada como certificado educacional.
- Existe também uma configuração independente de perfil público em `/profile/public`; ela não é necessária para o portfólio e permanece desativada por padrão no modelo atual.

## APIs e persistência existentes

- `GET/POST /api/career/portfolio/items` e `DELETE /api/career/portfolio/items/{id}`: gestão de projetos, experiências, cursos declarados e referências de desafio.
- `GET /api/career/portfolio/evidence`: competências existentes elegíveis para compartilhamento.
- `GET/POST /api/career/portfolio/shares` e `DELETE /api/career/portfolio/shares/{id}`: criação com consentimento, consulta e revogação.
- `POST /api/integrations/career/portfolio/verify`: verificação interna entre trilhas, com autorização do recrutador e revalidação das competências no Capacita.
- Tabelas existentes desde `V17__career_mentor_portfolio_and_programs.sql`; não foi necessária migração nesta etapa.

## Validação

- `npm run build`: aprovado após a inclusão da prévia e integração de experiências existentes.
- `npm test`: 30 testes passaram.
- `npm run lint`: aprovado.
- `.\mvnw.cmd -DskipTests compile`: aprovado.
- O teste Maven focado não executou: `testCompile` compila 92 classes antigas com referências ausentes (por exemplo, `TrailTokenService` e `com.trampolim.api.user`). Não foi feita validação integrada contra PostgreSQL nesta etapa.

## Limites

O recrutador precisa entrar em uma conta Empresas com o mesmo e-mail destinatário; não existe link público anônimo para dados do portfólio. Experiências do perfil são copiadas como snapshot revisável para o item do portfólio e não se sincronizam após alterações posteriores no perfil. O usuário deve revisar a cópia antes de compartilhar.
