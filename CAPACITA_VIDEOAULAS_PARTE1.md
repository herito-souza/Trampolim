# Capacita — cursos e videoaulas (Parte 1)

## Entrega

Esta etapa acrescenta Curso → Disciplina → Módulo → Videoaula ao Capacita existente. Não substitui o catálogo de materiais por etapa, o diagnóstico, os estágios ou a autenticação anterior. Nenhuma funcionalidade da Parte 2 foi implementada.

O aluno usa sua própria conta Capacita para consultar cursos publicados, navegar pelos módulos, assistir às aulas e retomar o progresso. O professor usa a mesma autenticação, com papel TEACHER concedido por administrador educacional, para criar e editar seus cursos, disciplinas, módulos e aulas. O administrador gerencia permissões e pode ocultar cursos; um bloqueio de moderação não pode ser removido pelo professor. A consulta de participação mostra somente nome, identificador e contagens de aulas iniciadas/concluídas no curso do professor, sem expor contatos ou outros perfis.

## Banco e autorização

São usadas exclusivamente conexões e transações `capacitaJdbcTemplate` / `capacitaTransactionManager` nas novas funcionalidades.

- `V9__courses_and_video_lessons.sql`: papéis, cursos, disciplinas, módulos, aulas, progresso, sessões de reprodução e auditoria de papéis.
- `V10__course_moderation_lock.sql`: bloqueio de moderação e controle persistente de envios.

As duas migrações foram aplicadas ao Capacita local e testadas em schemas vazios. Não há migração nova de Empresas ou Oportunidades nesta entrega. As aulas novas não reaproveitam IDs de contas dessas outras áreas. As credenciais e as sessões continuam independentes.

O acesso de aluno é implícito para conta ativa e confirmada. Os papéis TEACHER e ADMIN são consultados no servidor a cada operação; não são confiados a parâmetros do navegador. Professores só podem alterar seus próprios recursos; administradores educacionais podem gerenciá-los para moderação. Contas responsáveis por cursos não podem ser excluídas inadvertidamente pelo fluxo de exclusão pessoal.

## Administração inicial

`CAPACITA_ADMIN_EMAILS` define contas Capacita existentes e confirmadas que podem iniciar a gestão educacional. No ambiente local foi configurado `herito.fs01@gmail.com`, mediante autorização do usuário, em `backend/.env`, ignorado pelo Git. A configuração não cria uma conta nem copia credenciais de outra trilha. É necessário cadastrar/confirmar esse e-mail no Capacita e entrar por seu login próprio.

Após reiniciar o backend e o frontend, abra Capacita → Administração educacional. Cadastre primeiro cada professor pelo fluxo normal do Capacita e conceda o papel Professor a seu e-mail. O professor verá Gerenciar cursos. A revogação é efetiva no backend mesmo para tokens emitidos anteriormente. Alterações de papel são registradas em `learning_role_audit`.

## Vídeo: armazenamento e validação

Os arquivos ficam em diretório privado persistente, fora do PostgreSQL e da pasta pública do frontend. O banco contém somente a chave UUID e os metadados. Nomes originais não são usados como caminhos.

Formatos de entrada: MP4, MOV e WebM, até 128 MiB e 3840 × 2160. FFprobe inspeciona o arquivo; FFmpeg decodifica e converte para MP4 H.264/AAC, removendo metadados e faixas não usadas. A publicação depende da duração medida novamente no arquivo convertido: **180 a 300 segundos, inclusive**. A duração enviada pelo cliente não é aceita. WebM sem índice de duração é normalizado antes da decisão. Vídeos inválidos, fora do intervalo ou cujo processamento falha não recebem estado READY.

Restrições: um envio ativo por conta, duas transferências/processamentos simultâneos por instância, até 12 tentativas por hora por conta e cota de 512 MiB de vídeos por professor. O bloqueio inicial ocorre antes da leitura do corpo multipart; os limites por conta e a cota persistem no banco. O processamento tem tempo máximo, usa argumentos diretos de processo sem shell e protocolos/demuxers restritos. O proxy de produção também deve limitar o tamanho e o tempo de requisição.

O arquivo convertido recebe somente acesso por endpoint autorizado ou ticket aleatório de reprodução, limitado à aula e com validade de 30 minutos. Tickets não são JWT de login. A cada leitura são verificados conta ativa, versão da sessão, visibilidade e permissões. Logout, bloqueio da conta e ocultação do curso impedem novas leituras. Leitura parcial HTTP (Range/206) permite buscar trechos sem baixar o vídeo inteiro.

Trocar o vídeo devolve a aula a rascunho e reinicia o progresso dessa aula; o formulário informa essa consequência. Rascunhos e vídeos existentes são preservados quando o novo arquivo não é validado. Arquivos órfãos por falha do armazenamento devem ser reconciliados por operação administrativa comparando os UUIDs do diretório com `learning_lessons.media_key`; não há exclusão automática em lote nesta etapa.

## Reprodução e conclusão

O player usa controles nativos de reprodução, pausa, volume e busca, além de velocidade de 0,75× a 2×, legendas WEBVTT e transcrição quando fornecidas. Nenhuma legenda ou transcrição automática é inventada.

O navegador envia eventos a cada 5 segundos. O servidor verifica sessão, conta e aula, bloqueia a linha de progresso e credita somente avanços compatíveis com o tempo decorrido e velocidade de até 2×. Saltos, reprodução duplicada de trechos já creditados e intervalos inativos acima de 30 segundos não acumulam conclusão. É preciso atingir **90% de duração assistida validada**. Abrir a página ou ir diretamente ao final não conclui a aula. Isso registra reprodução compatível com as regras; não comprova atenção ou aprendizagem do aluno.

O progresso fica no banco Capacita e é incluído na exportação pessoal. Pré-visualizações do professor não contam como progresso estudantil. O aluno pode retomar a última posição registrada e navegar à próxima aula.

## Configuração

Instale FFmpeg e FFprobe atualizados no servidor e configure:

```properties
CAPACITA_ADMIN_EMAILS=seu-email-confirmado-no-capacita
CAPACITA_MEDIA_ROOT=/diretorio-privado-persistente/capacita/videos
CAPACITA_FFMPEG=/caminho/ffmpeg
CAPACITA_FFPROBE=/caminho/ffprobe
```

No ambiente Windows local foi baixada uma distribuição portátil de FFmpeg, verificada por SHA-256, fora do repositório, e os caminhos foram configurados em `backend/.env`. Os binários não fazem parte do código. Referências: [FFprobe](https://ffmpeg.org/ffprobe.html), [distribuição Windows usada](https://www.gyan.dev/ffmpeg/builds/), [Range no Spring MVC](https://docs.spring.io/spring/reference/web/webmvc/mvc-range.html).

O processo de vídeo deve executar com usuário sem privilégios, ferramentas atualizadas, recursos limitados e acesso somente ao armazenamento necessário. O adaptador entregue usa filesystem: mantenha volume persistente e backup conjunto dos arquivos e do banco. Para múltiplas réplicas, todas precisam acessar o mesmo volume privado; armazenamento de objetos/CDN e fila de transcodificação não foram implantados nesta etapa.

## Testes

- PostgreSQL real: estrutura, propriedade, permissão de aluno/professor/administrador, moderação, publicação sem vídeo e progresso.
- FFmpeg real: MP4 de 179/180/300/301 segundos, aceitação dos limites inclusivos, rejeição de duração inválida/arquivo falso e conversão MOV/WebM.
- HTTP real: cadastro Capacita, concessão de papel, hierarquia, upload multipart, publicação, Range/206, CORS e ticket invalidado após logout.
- Proteção anterior à leitura multipart: acesso anônimo e envio paralelo da mesma conta rejeitados.
- Playwright: upload/publicação em viewport de computador e celular; navegação do aluno, reprodução, pausa, volume, velocidade, retomada e transcrição. A API de negócio é controlada nos testes de interface; os testes HTTP/PostgreSQL verificam o servidor real separadamente.

A regressão completa executada passou com 69 testes backend, sem falhas/erros/skips, e 21 testes Playwright. Os sete testes unitários frontend, lint e build também passaram. O aviso de bundle acima de 500 kB continua sendo uma limitação de desempenho conhecida, não uma falha de compilação.

Para repetir os testes reais, defina CAPACITA_FFMPEG e CAPACITA_FFPROBE como variáveis de ambiente do processo Maven. Sem essas ferramentas, os testes que dependem delas são explicitamente marcados como não executados, não como validação real. O CI foi preparado para instalar FFmpeg e executá-los; o CI remoto ainda não foi rodado para esta alteração.

## Limites da entrega

Não foi implantado serviço cloud de vídeo, CDN, antivírus externo, fila distribuída, legendagem automática ou transferência de responsabilidade de curso pela interface. O professor responsável precisa de conta Capacita existente e papel concedido. A conta administrativa real ainda precisa estar cadastrada/confirmada para usar o painel; nenhum usuário foi criado automaticamente. A validação de celular foi feita em navegador com viewport móvel, não em dispositivos físicos iOS/Android.

## Arquivos principais

Novos: `LearningAccess`, `LearningController`, `LearningPlaybackController`, `LessonMediaController`, `LessonMediaStore`, `LearningUploadFilter`; migrações Capacita V9/V10; `LearningPostgresTest`, `LearningHttpPostgresTest`, `LearningUploadFilterTest`; frontend `LearningPortal`, `LessonEditor`, `LessonPlayer`, `types`; teste `learning-journey.spec.ts` e vídeo sintético `fixtures/lesson-180s.mp4` (gerado para testes, não é curso real).

Adaptados: `CapacitaController` (exportação), `StudentAccountController` (proteção de professor responsável), `SecurityConfig`, `CorsConfig`, `application.properties`, `backend/.env.example`, `backend/.gitignore`, `CapacitaPage` e pipeline CI. `backend/.env` tem somente configuração local e continua fora do Git.
