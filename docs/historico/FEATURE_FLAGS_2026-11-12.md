# Feature Flags — 12/11/2026

## Auditoria e decisão

O Spring Boot já usa `@ConditionalOnProperty` para habilitar módulos, tarefas e conexões por ambiente. Essas configurações são estáticas durante a inicialização, não possuem segmentação por perfil e não mantêm histórico de alterações administrativas. Foram preservadas para configuração operacional. As flags dinâmicas usam uma tabela e auditoria independentes em cada banco/trilha.

## Comportamento seguro

- Flag ausente, desabilitada, sem ambiente correspondente ou sem perfil correspondente resulta em **desativada**.
- Ambientes e perfis precisam ser definidos explicitamente; não há wildcard implícito.
- O ambiente é obtido dos perfis ativos do Spring (`local`, `dev`, `test`, `staging`, `prod`); `development` e `production` são normalizados para `dev` e `prod`. Perfis desconhecidos falham fechados para flags.
- `companyIds` e códigos IBGE de município apenas reduzem o grupo que recebe uma liberação. Eles nunca autorizam uma operação ou substituem permissões por recurso.
- O administrador altera apenas o registro da trilha selecionada; não existe leitura cruzada entre bancos.
- As flags são lidas diretamente do banco, sem cache local. Uma desativação administrativa passa a valer para as próximas avaliações em todas as instâncias conectadas.
- O frontend recebe apenas as flags efetivamente aplicáveis à sessão autenticada. O token define a trilha; perfil e vínculo empresarial são consultados no banco da própria trilha, nunca aceitos do navegador.

## Interfaces

- `GET /api/admin/features/{OPORTUNIDADES|EMPRESAS|CAPACITA}`: lista para superadministrador.
- `PUT /api/admin/features/{trilha}/{key}`: cria/atualiza com controle otimista (`expectedRevision`), ambientes, perfis, empresas, municípios e justificativa de 10–500 caracteres.
- `GET /api/admin/features/{trilha}/{key}/audit`: histórico dos últimos 100 registros, incluindo autor, justificativa e estados anterior/novo.
- `GET /api/features`: retorna chaves aplicáveis à trilha e à identidade derivadas do JWT e banco correspondente; não é autorização de API. Sem contexto territorial confiável nessa resposta, flags limitadas por município não são expostas ali; serviços backend podem avaliá-las usando código validado no próprio domínio.

Exemplo de corpo de atualização:

```json
{
  "enabled": true,
  "expectedRevision": 0,
  "environments": ["staging"],
  "profiles": ["ADMIN"],
  "companyIds": [],
  "municipalityCodes": [],
  "reason": "Piloto controlado da nova visão"
}
```

## Uso por funcionalidades

O serviço interno `TrailFeatureFlags.enabled(key, profile, companyId, municipalityCode)` é o ponto de decisão para rotas e serviços. Cada operação deve chamar a flag no backend depois de autenticar e autorizar o recurso; para ações empresariais, o `companyId` deve vir da associação verificada do usuário, não do corpo da requisição. Para segmentação municipal, usar o código IBGE obtido de dados confiáveis já validados pelo backend.

Não foram colocadas flags em fluxos existentes de negócio nesta mudança para não desativar funcionalidades já publicadas sem decisão de rollout específica. Novos módulos ou versões devem ser conectados ao avaliador antes de serem liberados; a API de elegibilidade é apenas uma ajuda de interface.

## Ciclo de vida

1. Criar a flag desativada, com `expectedRevision=0` e justificativa.
2. Implantar código cujo endpoint verifica a flag no backend.
3. Habilitar primeiro em `dev`/`test`, depois `staging`, e por fim em coorte explícita de `prod`.
4. Ampliar perfil/empresa/município em revisões auditadas, monitorando erros e métricas.
5. Em incidente, desabilitar a flag (`enabled=false`); leitura sem cache torna a interrupção imediata para chamadas subsequentes.
6. Após rollout completo, remover a flag e seu código de controle em uma alteração planejada.

## Migrações

- Oportunidades: `V27__feature_flags.sql`.
- Empresas: `V26__feature_flags.sql`.
- Capacita: `V28__feature_flags.sql`.

As tabelas e auditoria são isoladas por banco. Nenhuma chave/segredo é armazenado nas flags.
