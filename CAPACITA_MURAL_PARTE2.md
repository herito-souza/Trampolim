# Capacita — mural privado e notificações (Parte 2)

## Entrega e limites de escopo

Implementado sobre os cursos, módulos e videoaulas da Parte 1, sem substituir a autenticação educacional. Não inclui a Parte 3. O mural é privado: aluno autor, professor responsável pelo curso com permissão vigente e administrador educacional podem acessá-lo. Não existe publicação pública de perguntas nesta etapa.

O aluno abre uma dúvida a partir do curso, módulo ou aula, escreve texto e pode anexar áudio ou vídeo. Também consulta o histórico, envia acompanhamentos e exclui sua dúvida. O professor usa a caixa de atendimento, filtra por curso, módulo, aula, data e situação, responde individualmente e encerra ou reabre atendimentos. Os estados são aguardando resposta, em atendimento, respondida e encerrada; uma resposta efetiva é necessária para marcar como respondida.

Respostas em lote exigem seleção, revisão dos destinatários e confirmação explícita. Cada aluno recebe uma mensagem em sua própria conversa. O servidor valida todos os destinatários antes de gravar e executa a operação atomicamente. Chaves de idempotência impedem duplicação em novas tentativas com o mesmo conteúdo; a reutilização da chave com conteúdo diferente é rejeitada. Não há envio automático após falha.

## Comunicação e notificações

As notificações são persistidas no PostgreSQL do Capacita e entregues por Server-Sent Events (SSE), com autenticação no cabeçalho da requisição. O token de login não é colocado na URL. Há contador de não lidas, histórico paginado, marcação como lida e abertura direta da conversa.

O servidor consulta os eventos persistidos a cada dois segundos, envia heartbeat e encerra a conexão após dois minutos para renovação. O navegador reconecta com espera progressiva de até 30 segundos e recupera eventos posteriores ao último identificador. O histórico permanece disponível após períodos offline. Sessão expirada, revogada ou conta bloqueada encerram o canal. Existem limites de dois canais por usuário e 500 por instância; não representam promessa de capacidade em produção.

## Mídia e acessibilidade

Gravação usa MediaRecorder/getUserMedia quando disponíveis, mediante autorização do navegador, com alternativa de arquivo e texto sempre disponível. Câmera e microfone normalmente exigem HTTPS ou localhost. A interface oferece progresso do upload e pré-visualização. Os testes de captura usam dispositivos sintéticos do Chromium; aparelhos físicos não foram homologados.

O backend inspeciona e normaliza arquivos com FFprobe/FFmpeg. Áudio é convertido para AAC/M4A e vídeo para H.264/AAC em MP4. Formatos de entrada incluem MP4, MOV, WebM, M4A, MP3, WAV, OGG, AAC e FLAC, conforme o tipo de mídia e os streams efetivamente encontrados. Arquivos inválidos, formatos inadequados, duração ou tamanho excessivos são rejeitados. A duração enviada pelo navegador não é confiada.

Armazenamento reutiliza o diretório privado persistente da Parte 1, em `discussion/`, fora do banco e da pasta pública. O banco guarda UUIDs e metadados. A reprodução usa autorização de participante e tickets temporários, limitados à validade e versão da sessão, com suporte a requisições Range. Logout invalida os tickets da sessão. Um professor não pode encaminhar gravações privadas de um aluno para outros destinatários.

Transcrição e legendas são campos opcionais fornecidos pelo autor e identificados dessa forma; nenhuma transcrição automática é apresentada como verificada. Formulários, seleção, confirmação e navegação possuem rótulos e foco por teclado.

## Configuração

Manter `CAPACITA_FFMPEG`, `CAPACITA_FFPROBE` e o volume privado `CAPACITA_MEDIA_ROOT` da Parte 1. Novas variáveis, documentadas também em `backend/.env.example`:

| Variável | Padrão |
| --- | --- |
| CAPACITA_QUESTION_MEDIA_MAX_BYTES | 26214400 (25 MiB) |
| CAPACITA_QUESTION_AUDIO_MAX_SECONDS | 120 |
| CAPACITA_QUESTION_VIDEO_MAX_SECONDS | 120 |
| CAPACITA_QUESTION_MEDIA_QUOTA_BYTES | 268435456 (256 MiB por conta) |
| CAPACITA_QUESTION_UPLOADS_HOUR | 12 |
| CAPACITA_QUESTION_CLOSED_RETENTION_DAYS | 0 |

Há limites adicionais de processamento e de mensagens no servidor: dez novas dúvidas por hora, 60 mensagens por hora para aluno e 300 para professor. Uploads sem tamanho conhecido são rejeitados; clientes devem enviar Content-Length. Configurar o proxy para suportar os limites de corpo e o tempo de processamento, desabilitar buffering de SSE e permitir conexões longas.

Retenção zero preserva conversas encerradas até exclusão explícita. Valor positivo habilita exclusão das conversas encerradas após esse número de dias. A limpeza executa a cada hora, remove tickets vencidos e mídias sem referência há mais de 24 horas. Definir a política operacional de retenção antes da produção; não foi inventado um prazo legal. Backups devem abranger o banco Capacita e o volume privado de arquivos de forma consistente.

## Banco e segurança

Nova migração: `backend/src/main/resources/db/migration-capacita/V11__private_questions_and_notifications.sql`. Cria tabelas `learning_questions`, `learning_question_messages`, `learning_reply_batches`, `learning_notifications`, `learning_question_media`, `learning_question_media_tickets`, `learning_communication_uploads` e `learning_question_audit`, com restrições e índices. A migração é exclusiva do Capacita e mantém os dados anteriores. Não há nova migração de Oportunidades ou Empresas.

Todas as consultas funcionais usam `capacitaJdbcTemplate` e transações `capacitaTransactionManager`. A autorização é verificada no backend, inclusive para mídias, notificações e lotes; IDs enviados pelo navegador não concedem acesso. Logs de auditoria registram atores e eventos sem conteúdo das conversas. A exportação pessoal inclui as dúvidas e mensagens autorizadas da própria conta.

Endpoints principais ficam sob `/api/capacita/learning/discussion`: perguntas, mensagens, respostas, status, lotes e mídias. Notificações usam `/api/capacita/learning/notifications`, incluindo `/stream`. O único endpoint de mídia acessível por ticket é o de reprodução; cadastro, upload e emissão de ticket exigem sessão Capacita válida.

## Arquivos da Parte 2

Novos no backend: `QuestionService.java`, `QuestionController.java`, `LearningNotifications.java`, `DiscussionMediaStore.java`, `DiscussionMediaController.java`, V11 e testes `DiscussionPostgresTest.java` / `DiscussionHttpPostgresTest.java`, todos nos diretórios de educação/migrações correspondentes.

Adaptados no backend: `SecurityConfig.java`, `LearningUploadFilter.java`, `LessonMediaStore.java`, `CapacitaController.java`, `application.properties` e `.env.example`.

Novos no frontend em `src/features/learning/`: `discussionTypes.ts`, `MediaRecorderInput.tsx`, `QuestionComposer.tsx`, `QuestionNotifications.tsx`, `QuestionBoard.tsx`. Adaptados `LearningPortal.tsx` e `src/pages/CapacitaPage.tsx`. Novos testes `e2e/discussion-journey.spec.ts` e fixtures `question-audio.wav` / `question-video.mp4` (dados sintéticos).

Os demais arquivos locais da Parte 1 foram preservados. Esta entrega não implica commit ou publicação no GitHub.

## Validação e pendências operacionais

Validação final em 29/10/2026: 75 testes do backend passaram, sem falhas, erros ou testes ignorados; 26 testes Playwright passaram; sete testes unitários do frontend passaram; lint e compilação de produção passaram. O build informa um aviso de bundle JavaScript acima de 500 kB, sem impedir a compilação. A V11 foi confirmada como aplicada com sucesso no Capacita local. `git diff --check` não apontou erros de whitespace.

Testes cobrem texto, áudio, vídeo, duração real, privacidade, respostas individuais e em lote, atomicidade, idempotência concorrente, envio simultâneo aluno/professor, SSE real autenticado, reconexão, recuperação offline, marcação como lida, reprodução parcial e invalidação após logout. PostgreSQL real é usado com schemas temporários removidos ao final. Playwright verifica também telas estreitas e gravação com câmera/microfone sintéticos.

Para utilização real: reiniciar backend/frontend, manter FFmpeg disponível, volume persistente e HTTPS; cadastrar e confirmar a conta Capacita `herito.fs01@gmail.com` configurada como administrador inicial, se ainda não existir, e conceder papéis aos professores. Isso não cria contas automaticamente. SMTP continua dependendo da configuração externa já documentada. Notificações dentro da plataforma não dependem de SMTP; não foram implementados e-mails ou push do sistema operacional.

A solução usa armazenamento privado local e SSE com leitura do PostgreSQL, sem novo serviço de nuvem. Em múltiplas instâncias, o volume de mídia deve ser compartilhado e protegido ou substituído por armazenamento de objetos adequado. Homologação em dispositivos físicos, capacidade sob carga e configuração de infraestrutura de produção permanecem atividades operacionais; não foram simuladas como concluídas.
