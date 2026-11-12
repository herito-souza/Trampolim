# Auditoria técnica — diagnóstico inicial

Fonte de verdade: checkout local `C:\Users\Windows\trampolim-main`. Branch main, HEAD sem diferença com a referência local origin/main (não foi feito fetch), com alterações anteriores preservadas. React 19, Vite 8, TypeScript 6, Tailwind 4; Java 21, Spring Boot 4.1, JPA/JDBC, Flyway e PostgreSQL. Empresas e Capacita possuem DataSources e migrações próprias; Oportunidades mantém JPA primário.

| Prioridade | Evidência inicial | Impacto / ação |
|---|---|---|
| Alta | App importa todas as páginas estaticamente; pacote inicial 611,27 kB / gzip 155,58 kB | Separar por carregamento sob demanda, preservando navegação |
| Alta | LearningController.course consulta módulos por disciplina e aulas por módulo | Remover N+1 com três consultas de estrutura |
| Alta | PublicCompanyController e catálogo retornam coleções sem paginação | Adicionar contratos paginados compatíveis e atualizar consumidores de maior uso |
| Alta | HikariDataSource criado manualmente sem binding de pool por trilha | Configurar limites, aquisição e nomes por banco |
| Alta | Sem Actuator/Prometheus e correlação de requisições | Adicionar métricas protegidas, probes, logs sem identificadores privados |
| Alta | Limitadores de autenticação e cotas concorrentes locais | Não certificar multi-instância irrestrita; preparar proxy/worker e registrar requisito de distribuição |
| Média | SSE usa cursor e PostgreSQL persistido; polling em lote de canais, sem broker | Compatível com recuperação entre nós; medir carga de polling e limitar conexões por instância |
| Média | Processamento de arquivos síncrono, limitado por semáforos por instância | Preservar validação, armazenamento compartilhado privado e isolamento do processamento; não introduzir fila sem integração completa |
| Média | Jobs agendados de catálogo/e-mail executariam em cada réplica | Configurar um worker designado para esses jobs |
| Média | Módulos Capacita montados mesmo ocultos iniciam consultas | Adiar primeira montagem até acesso e manter contexto após uso |

Restrições: sem exclusão de dados reais, sem mudanças de chaves/sessões, sem alteração de produção, sem infraestrutura paga. Metas 5.000 usuários/1.000 RPS não são capacidade comprovada. Testes pesados serão entregues reproduzíveis, executando apenas cargas locais limitadas em schemas temporários.

Sequência: referência inicial → code splitting/N+1/configurações → observabilidade/consultas → operação e cenários de carga → regressão e relatório com medições e limitações. Endpoints existentes serão preservados; não haverá remoção baseada apenas em ausência de referência.
