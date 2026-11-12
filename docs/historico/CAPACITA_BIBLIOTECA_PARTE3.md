# Capacita — biblioteca virtual e materiais didáticos (Parte 3)

## Resultado

Biblioteca integrada a Curso → Disciplina → Módulo → Aula, utilizando exclusivamente a conta, as permissões e o PostgreSQL do Capacita. As Partes 1 e 2 foram preservadas. Nenhuma funcionalidade da Parte 4 foi iniciada e nenhum documento fictício foi cadastrado como conteúdo real.

O projeto local na branch `main` já continha alterações não commitadas das etapas anteriores; elas foram mantidas. A implementação continua em React, TypeScript, Vite e Tailwind no frontend, e Java 21/Spring Boot, JDBC transacional e Flyway no backend. Não foi criado outro login, banco ou serviço de aplicação.

## Funcionalidades

- Catálogo com pesquisa por título/assunto e filtros de curso, disciplina, módulo, categoria e professor; paginação de 50 registros.
- Materiais com título, descrição, assunto, categoria, professor do curso, data de publicação, situação, acesso e alternativa textual.
- Professor responsável e administrador educacional podem criar rascunhos, editar, enviar PDFs, publicar, retirar de publicação e excluir materiais. Professores de outros cursos não podem gerenciá-los.
- Um mesmo registro pode ser associado a várias aulas do mesmo curso; o arquivo não é copiado para cada aula. Sem aulas específicas, o material é disponibilizado às aulas do curso, disciplina ou módulo escolhido.
- Atualizações de arquivo criam versões. O histórico fica disponível somente ao responsável e à administração. Uma versão nova volta a rascunho e exige confirmação de publicação. Reenvio do mesmo arquivo não cria outra versão idêntica no mesmo material.
- Controle de revisão rejeita gravações desatualizadas, inclusive quando o material muda durante um upload. O banco impede referências a versões de outro material.
- Editor integrado de texto simples gera PDFs com paginação automática e caracteres latinos, incluindo português. Não depende de conversor externo. Para imagens, fórmulas ou diagramação avançada, enviar um PDF preparado externamente. Caracteres não suportados são rejeitados, sem substituição silenciosa.
- Visualizador PDF.js carregado sob demanda: páginas anterior/próxima, número da página, zoom, texto selecionável, abertura em visualizador compatível, download autorizado, carregamento, erros e nova tentativa.
- Alternativa textual fornecida pelo professor e texto extraído automaticamente, identificado como não verificado. A presença de texto e de marcações estruturais é informada, sem declarar conformidade de acessibilidade automática. PDFs sem texto ou estrutura detectada exibem a limitação.
- Biblioteca geral e atalhos de curso, módulo e aula. Abrir a biblioteca e o mural mantém o reprodutor montado, preservando o contexto de estudo.

## Permissões e arquivos privados

`STUDENTS`: alunos com conta Capacita ativa e confirmada consultam somente materiais publicados de cursos publicados, ativos e não bloqueados pela moderação. Não existe matrícula restritiva adicional na arquitetura atual; portanto esse escopo significa alunos autenticados do Capacita, conforme explicitado na interface.

`TEACHERS`: somente o professor responsável pelo curso e a administração educacional. Não concede acesso a todos os professores. Rascunhos, materiais ocultos/excluídos e versões anteriores não são liberados ao aluno.

Upload e geração validam a permissão antes do processamento e novamente antes de vincular o arquivo. O filtro limita o corpo e a concorrência antes da leitura multipart. Há limite de 12 tentativas por hora por conta, duas operações simultâneas por instância e uma por conta. Corpos sem Content-Length são recusados; o formulário utiliza upload multipart do navegador.

Arquivos são armazenados em `<CAPACITA_MEDIA_ROOT>/library`, com nomes UUID privados. O PostgreSQL contém referências e metadados, não os bytes dos PDFs. Acesso ao arquivo e à alternativa textual exige autenticação Capacita e autorização atual no backend. Respostas não são armazenadas em cache; requisições Range são suportadas. Não há links públicos ou JWT na URL.

O download é validado pelo servidor e seu botão aparece apenas quando permitido. A visualização necessariamente entrega os bytes ao navegador: a preferência de download não é DRM e não impede cópias por ferramentas do navegador ou de um visualizador compatível.

## Validação de PDF

Usa [Apache PDFBox 3.0.8](https://pdfbox.apache.org/download.cgi), com inspeção, extração de texto e regravação do documento. Arquivos com senha/criptografia, JavaScript, ações, formulários, anexos e recursos multimídia não permitidos são rejeitados. Extensão e assinatura também são verificadas. Texto extraído tem limite de um milhão de caracteres.

A inspeção e a geração executam em processo Java separado, com heap de 192 MiB e prazo padrão de 45 segundos; o processo é encerrado se exceder o prazo. O backend não executa scripts de PDF. Isso reduz o risco de conteúdo ativo e processamento excessivo; não equivale a uma certificação antivírus. Não foi contratado nem simulado um serviço externo de análise.

O frontend usa [Mozilla PDF.js](https://mozilla.github.io/pdf.js/examples/) 6.4.299 e seu worker distribuído no build local, sem CDN ou transferência do documento para um terceiro. O renderizador visual e a camada de texto são carregados ao abrir o primeiro material.

## Banco e migração

Nova migração: `backend/src/main/resources/db/migration-capacita/V12__private_pdf_library.sql`.

Tabelas novas:

| Tabela | Responsabilidade |
| --- | --- |
| learning_materials | Metadados, escopo, publicação, versão atual e revisão |
| learning_material_versions | Histórico, UUID privado, hash, tamanho, páginas e indicadores de texto/estrutura |
| learning_material_lessons | Associação de um material a várias aulas |
| learning_library_uploads | Limitação persistente de tentativas |
| learning_library_audit | Eventos de criação, edição, versão e exclusão, sem conteúdo do PDF |

Todas as transações novas usam `capacitaTransactionManager` e `capacitaJdbcTemplate`. A V12 foi aplicada com sucesso no Capacita local e testada em schemas vazios. As migrações V1–V11 não foram alteradas. Não há migração ou nova tabela em Empresas/Oportunidades.

Excluir um material retira o acesso imediatamente nas próximas requisições, preservando as versões no armazenamento durante a retenção configurada. A limpeza remove registros excluídos após 30 dias por padrão; arquivos sem referência são removidos após 24 horas, com execução periódica. Valor zero na retenção desativa a remoção automática dos registros excluídos. Definir a política institucional antes da produção; esses valores são parâmetros operacionais, não prazos legais presumidos.

## Configuração e execução

Variáveis documentadas em `backend/.env.example`:

| Variável | Padrão |
| --- | --- |
| CAPACITA_LIBRARY_MAX_BYTES | 20971520 — 20 MiB |
| CAPACITA_LIBRARY_MAX_PAGES | 300 |
| CAPACITA_LIBRARY_QUOTA_BYTES | 536870912 — 512 MiB por professor responsável |
| CAPACITA_LIBRARY_PROCESSING_SECONDS | 45 |
| CAPACITA_LIBRARY_DELETED_RETENTION_DAYS | 30 |

As versões anteriores e os arquivos de materiais excluídos ainda retidos contam na quota. O armazenamento reutiliza `CAPACITA_MEDIA_ROOT` das etapas anteriores. Java deve permitir iniciar um processo filho; o executor encontra o binário na instalação Java usada pelo backend. Não é necessário instalar outro conversor de PDF. A validação foi conferida tanto no desenvolvimento quanto no JAR executável empacotado.

Após atualizar as dependências, reiniciar os dois processos:

```powershell
Set-Location C:\Users\Windows\trampolim-main\backend
.\mvnw.cmd spring-boot:run
```

Em outra janela:

```powershell
Set-Location C:\Users\Windows\trampolim-main\frontend
npm ci
npm run dev
```

Manter as configurações de banco e autenticação existentes. O Flyway executa a V12 na conexão Capacita ao iniciar. A conta administradora educacional inicial continua sendo a configurada na Parte 1; ela precisa existir e estar confirmada no próprio Capacita. Não são criadas contas ou permissões automaticamente nesta etapa.

Professor: Capacita → Biblioteca virtual → Gerenciar biblioteca → Novo material PDF. Escolher curso, organização e aulas; preencher os metadados, enviar arquivo ou usar o editor e salvar como rascunho/publicado. Para atualizar ou retirar: Editar material → substituir arquivo ou alterar Situação do material → Salvar. Para ler versões antigas: Ler PDF → Versão do PDF.

Aluno: Biblioteca virtual, ou Materiais deste curso/módulo/aula → Ler PDF. O mural continua disponível para solicitar uma alternativa acessível ao professor.

## Arquivos desta etapa

Novos no backend:

- `backend/src/main/java/com/trampolim/api/education/LibraryService.java`
- `backend/src/main/java/com/trampolim/api/education/LibraryController.java`
- `backend/src/main/java/com/trampolim/api/education/LibraryPdfStore.java`
- `backend/src/main/java/com/trampolim/api/education/LibraryUploadFilter.java`
- `backend/src/main/java/com/trampolim/api/education/PdfDocumentWorker.java`
- `backend/src/main/resources/db/migration-capacita/V12__private_pdf_library.sql`
- `backend/src/test/java/com/trampolim/api/education/LibraryPostgresTest.java`
- `backend/src/test/java/com/trampolim/api/education/LibraryHttpPostgresTest.java`
- `backend/src/test/java/com/trampolim/api/education/LibraryUploadFilterTest.java`

Novos no frontend:

- `frontend/src/features/learning/libraryTypes.ts`
- `frontend/src/features/learning/LibraryPanel.tsx`
- `frontend/src/features/learning/PdfReader.tsx`
- `frontend/e2e/library-journey.spec.ts`

Modificados nesta etapa:

- `backend/pom.xml`: PDFBox.
- `backend/src/main/java/com/trampolim/api/config/SecurityConfig.java`: filtro de upload da biblioteca.
- `backend/src/main/java/com/trampolim/api/education/LearningController.java`: reutilização da verificação de visibilidade de curso.
- `backend/src/main/resources/application.properties` e `backend/.env.example`: limites e retenção.
- `frontend/package.json` e `frontend/package-lock.json`: PDF.js.
- `frontend/src/features/learning/LearningPortal.tsx`: biblioteca e atalhos contextuais.
- `README.md` e este relatório: documentação.

## Testes e resultados — 29/10/2026

- Backend completo: **82 testes, zero falhas, zero erros, zero ignorados**.
- Playwright completo: **31 testes passaram**, incluindo as etapas anteriores.
- Frontend unitário: **7 testes passaram**.
- Lint: aprovado.
- TypeScript/build Vite: aprovado; permanece aviso de bundle inicial acima de 500 kB. O visualizador é um chunk separado carregado sob demanda.
- Empacotamento Maven: aprovado. Geração e inspeção usando o worker do JAR de implantação: aprovadas.
- V12 confirmada no banco local; instalação completa em schema vazio testada.
- `git diff --check`: sem erros de whitespace.
- Conferência visual do leitor em largura de 390 px, sem transbordamento horizontal da página.

Cobertura nova: PDFs reais e texto selecionável, arquivo inválido, scripts, páginas/tamanho excessivos, geração em português, rascunho/publicação, filtros, vínculo a duas aulas sem duplicação, acesso indevido, download negado, versões privadas, revisão desatualizada, exclusão, pré-validação do corpo, captura de erros, upload em computador/celular e preservação do reprodutor ao abrir biblioteca/mural. A integração HTTP usa PostgreSQL real com schema temporário removido ao final; os PDFs sintéticos são apenas fixtures de teste.

## Pendências reais de produção

Nenhuma credencial de serviço externo é necessária para esta etapa. A infraestrutura de produção precisa disponibilizar armazenamento privado persistente e backups consistentes do banco Capacita e desse volume; se houver múltiplas instâncias, o armazenamento precisa ser compartilhado/protegido ou substituído por um adaptador de objetos. Configurar o proxy para os limites de corpo e o tempo de processamento. Homologação em aparelhos físicos e teste de capacidade sob carga não foram realizados; os testes móveis usam o navegador em viewport reduzida.

O editor entrega texto simples, não diagramação avançada. Não há OCR, revisão automática de acessibilidade ou garantia de que texto extraído tenha ordem de leitura correta. Tais limitações são identificadas ao aluno. Nenhum plano de Parte 4 foi executado. As alterações permanecem locais, sem commit/push automático.
