# Homologação e liberação regional

Este procedimento prepara uma homologação. Não autoriza implantação de produção nem substitui a revisão das pendências em [QA_IMPLANTACAO_IRECE.md](../QA_IMPLANTACAO_IRECE.md).

## Ambiente e dependências

Java 21, Node compatível com o lockfile/CI (Node 22 no pipeline), Docker com os três PostgreSQL de desenvolvimento, FFmpeg e FFprobe. Dependências estão em frontend/package-lock.json e backend/pom.xml; não instalar pacotes do frontend no backend. Vídeos e PDFs usam armazenamento privado externo ao PostgreSQL. Os bancos possuem históricos Flyway independentes (Oportunidades V18, Empresas V15, Capacita V19 no checkout auditado).

Use somente ambiente de desenvolvimento/homologação. Confirme que OPORTUNIDADES_DB_*, EMPRESAS_DB_* e CAPACITA_DB_* do processo de testes apontam para os bancos locais previstos. Não executar testes com conexões de produção. Não exibir valores de segredos em relatórios ou logs.

## Verificações locais — PowerShell

Na raiz do projeto:

```powershell
Set-Location C:\Users\Windows\trampolim-main
docker compose -f backend/docker-compose.yml up -d
docker compose -f backend/docker-compose.yml ps
```

No frontend, executar cada comando e interromper se falhar:

```powershell
Set-Location C:\Users\Windows\trampolim-main\frontend
npm ci
npm run lint
node node_modules/typescript/bin/tsc -b
npm test
npm run build
npm run test:e2e -- --workers=2
npm audit --audit-level=high
```

No backend, disponibilizar FFmpeg/FFprobe no PATH ou configurar CAPACITA_FFMPEG e CAPACITA_FFPROBE do processo com os caminhos instalados, sem alterar credenciais. Depois:

```powershell
Set-Location C:\Users\Windows\trampolim-main\backend
.\mvnw.cmd --batch-mode "-Dspring.jpa.hibernate.ddl-auto=validate" verify
```

Os testes criam schemas temporários identificados e usam fixtures; alguns testam recusa de conexão em porta local fechada sem derrubar os bancos. Relatórios Maven ficam em target/surefire-reports; planos e orçamento de consultas em target/audit. Testes reais de mídia não devem ser considerados aprovados se aparecerem como ignorados por falta de FFmpeg.

Para medição de frontend, após build e sem outros testes/carga concorrentes:

```powershell
Set-Location C:\Users\Windows\trampolim-main\frontend
node scripts/measure-performance.mjs homologacao
```

O instrumento usa porta local 4180, gzip, três amostras por cenário, respostas sintéticas de API e rede/CPU controladas. Não representa p75 de usuários reais. Rede instável na busca e falhas de progresso também são testadas em regional-resilience e learning-journey. Não há garantia de abrir/recarregar toda a aplicação completamente offline.

## Configuração regional e arquitetura

A referência inicial vem de `GEOGRAPHY_REGIONS` (recurso aceito pelo Spring: por exemplo, arquivo externo com prefixo file:). Copiar o formato de backend/src/main/resources/geography/regions.json, definir defaultRegion, referenceMunicipality e regiões com códigos oficiais válidos. Validar em homologação antes de substituir a configuração em execução. Não é necessário alterar usuários ou autenticação. Atualizar o dataset oficial é uma operação separada de manutenção dos dados de referência.

Preservar as três URLs, credenciais, JWT e permissões independentes. Referências: [SAAS_ARCHITECTURE.md](../SAAS_ARCHITECTURE.md), [BUSCA_REGIONAL.md](../BUSCA_REGIONAL.md), [MAPA_REGIONAL.md](../MAPA_REGIONAL.md), [PERFORMANCE_ACESSIBILIDADE.md](../PERFORMANCE_ACESSIBILIDADE.md).

## Antes da liberação pública

1. Preparar homologação com cópia minimizada/anonimizada ou fixtures; não usar dados reais para demonstração sem autorização.
2. Configurar domínio HTTPS, perfil prod, chaves exclusivas fortes por trilha, segredos de integração, origens CORS exatas e contas de banco de menor privilégio. Os valores de exemplo não são credenciais de produção.
3. Instalar armazenamento privado e ferramentas de mídia; configurar limites e testar Range, autorização, expiração e erros. A distribuição atual usa o backend e não oferece bitrate adaptativo/CDN automaticamente.
4. Homologar SMTP e recuperação das três trilhas. Homologar Mercado Pago em sandbox para estágios de R$ 50,00; testar falhas, assinatura, conciliação, repetição e vaga encerrada. Não conceder PAID pelo frontend.
5. Fazer backup independente dos três bancos e mídia e ensaiar restauração em novos destinos isolados. Seguir backend/migration/README.md e OPERACAO_E_ESCALA.md. Não restaurar sobre produção durante o ensaio.
6. Ensaiar instalação vazia e atualização de uma cópia do ambiente existente; Flyway deve validar os históricos. Não editar migrações aplicadas ou apagar dados para fazer o deploy passar.
7. Testar a compilação real e o conjunto final de contêineres na infraestrutura de homologação. A auditoria local valida Java/React, Compose e sintaxe do proxy; não certifica o ambiente final completo.
8. Executar smoke tests autenticados por perfil, publicação/expiração, busca em Irecê/Lapão/região/remoto, professor/aluno, notificações, mídia e competência. Repetir no celular e com leitor de tela.
9. Configurar métricas, alertas, capacidade, retenção, responsável por incidentes e janela de manutenção. Realizar carga gradual no ambiente isolado; não inferir milhares de usuários a partir de poucos tokens ou testes de consultas.
10. Registrar autorização de liberação, versões dos artefatos e plano de retorno antes de publicar. Esta auditoria não realizou essa publicação.

## Retorno e incidentes

Interromper a liberação se houver falha de autenticação, vazamento entre trilhas/organizações, pagamentos inconsistentes ou perda de dados. Preservar evidências sanitizadas e bloquear o recurso afetado. Reverter o artefato de aplicação somente após avaliar compatibilidade das migrações; não executar rollback destrutivo de banco. Restaurar dados apenas com procedimento autorizado, destino conferido e evidência do backup. Definir previamente RPO/RTO e quem pode decidir.

## Expansão para outros territórios

Cadastrar configuração e códigos oficiais, validar nomes ambíguos, revisar filtros com usuários do território e medir o volume real. Manter o catálogo nacional e a opção todas as localidades. Rever índices, paginação, caches públicos e limite de conexões conforme os dados cresçam. Não compartilhar localização residencial, credenciais ou perfis privados para produzir indicadores regionais.

Na auditoria de 05/11, ops/.env.local estava ausente. O Compose dos bancos e o nginx -t foram validados; o conjunto completo de duas APIs/proxy ainda exige preparar esse arquivo privado a partir do exemplo, configurar credenciais reais de homologação e executar config --quiet antes de iniciá-lo. Não foi criado um arquivo com credenciais inventadas para declarar essa validação aprovada.
