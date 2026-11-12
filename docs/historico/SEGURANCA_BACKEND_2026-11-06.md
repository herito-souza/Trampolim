# Auditoria e fortalecimento do backend — 06/11/2026

## Resultado e escopo

Revisão sobre o checkout local da Trampolim, base 96e8c48, preservando as alterações não commitadas da revisão de navegação. Java 21, Spring Boot, JPA/JDBC, PostgreSQL e a separação das três trilhas foram mantidos. Nenhuma implantação, chamada real de SMTP/pagamento, alteração de registros operacionais ou nova migração foi realizada.

Conclusão operacional: o código fortalecido foi validado nos cenários abaixo e pode seguir para homologação controlada. A liberação em produção depende dos itens externos e das decisões de operação abaixo; testes de software não constituem certificação jurídica de LGPD nem garantia de ausência de vulnerabilidades.

## Diagnóstico e correções

| Área | Evidência encontrada | Tratamento |
| --- | --- | --- |
| Dependências | Inventário inicial: 155 coordenadas/versionamentos; OSV retornou 31 correspondências pacote/advisório, distribuídas por 22 avisos únicos. | Spring Boot 4.1.0 → 4.1.2, atualização de manutenção da mesma linha e sem troca de arquitetura. Nova consulta: 156 dependências, zero correspondências conhecidas na data da consulta. |
| Logs e correlação | Dois filtros criavam IDs diferentes para a mesma requisição; o filtro redundante registrava URI, que pode conter identificadores de compartilhamento privados. | Removido RequestMonitoringFilter; OperationalRequestFilter concentra correlação e erros sem URI, query, corpo, token ou texto da exceção. |
| Telemetria pública | O mapa de limitação só removia entradas expiradas após saturar; clientes ativos novos podiam fazê-lo crescer. O caminho enviado pelo cliente era registrado. | Substituído pelo BoundedRateLimiter existente, com capacidade limitada e rejeição de novas chaves na saturação. Log guarda somente o tipo de evento e a correlação do servidor. |
| JWT HTTP | O parser removia a palavra Bearer em vez de exigir o esquema, aceitando um JWT sem prefixo. | Authorization deve conter uma única credencial Bearer, sem espaços internos e com tamanho limitado; esquema sem diferenciação de maiúsculas. Cookies, Basic e formulário Spring não autenticam a API. |
| CORS/CSRF | CORS permitia credenciais de cookie, embora o cliente use Authorization explicitamente. | Cookies desautorizados por CORS; origens são aparadas. Em produção, somente origens HTTPS exatas; padrões de origem são rejeitados. |
| Cadastro | O cadastro de Oportunidades não limitava nome/e-mail ao tamanho das colunas antes da persistência. | Nome de até 120 e e-mail de até 180 caracteres, rejeitados antes de consultar/gravar ou calcular hash. |
| Produção | Configuração permitia baseline implícito e não validava vários requisitos em conjunto. | ProductionSecurityGuard bloqueia configurações inseguras antes de criar DataSources/migrar. Produção usa validate, Flyway ativo, clean desabilitado e baseline automático desabilitado. |
| Proxy e força bruta | Sem tratamento explícito, todo o público atrás do proxy podia compartilhar o mesmo limite de autenticação. | TrustedProxyFilter permite um IP encaminhado somente se o peer imediato estiver em IP/CIDR explicitamente confiável. Padrão: confiar em ninguém. Nginx sobrescreve X-Forwarded-For. |

### Dependências

O BOM 4.1.2 do Maven Central foi conferido antes da alteração. Versões gerenciadas relevantes: Tomcat 11.0.27, Spring Framework 7.0.10, Jackson 2.21.7 e 3.1.7, PostgreSQL JDBC 42.7.14 e Log4j API 2.25.5. Auth0 JWT e PDFBox foram preservados. Um aviso em uma dependência não prova exploração no produto; a atualização reduz a superfície conhecida sem depender dessa suposição.

Resultados brutos: [antes] (evidência local não versionada), [depois] (evidência local não versionada). A consulta é temporal, não uma garantia permanente. Inclui dependências de teste, além de compilação e runtime; não substitui scan da imagem, JDK, FFmpeg ou sistema operacional.

O script ops/scripts/audit-java-dependencies.mjs usa apenas coordenadas públicas e versões resolvidas; não envia código, bancos ou credenciais. Falha se a consulta for incompleta ou apontar avisos. A etapa foi adicionada ao CI do backend, junto do empacotamento verify. O workflow remoto não foi executado por esta revisão.

Referências primárias: [API OSV](https://google.github.io/osv.dev/api/), [BOM Spring Boot 4.1.2](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.2/spring-boot-dependencies-4.1.2.pom), [avisos Spring](https://spring.io/security/).

## Proteções existentes preservadas

- JWT HMAC com chaves, emissores e audiências distintos por trilha, expiração de duas horas, claims obrigatórias e versão de sessão conferida no banco. Chaves públicas de exemplo não são aceitas em produção.
- BCrypt, limite de 72 bytes de senha, bloqueios temporários após tentativas inválidas, limitadores com memória limitada e respostas genéricas de recuperação. Logout/redefinição revogam a versão da sessão correspondente.
- Três DataSources, usuários e bancos independentes; guard contra reutilização do mesmo banco; migrações Flyway separadas e transações nomeadas. O guard compara a identidade das URLs; aliases DNS e privilégios efetivos precisam ser conferidos na implantação. Administração global atravessa essas fronteiras somente por serviços administrativos explícitos e autorizados.
- Autorizações de empresa/associação, professor/curso, estudante, arquivos e resultados verificadas no backend. SQL parametrizado nos serviços auditados; tabelas dinâmicas são selecionadas por listas internas, não pelo usuário.
- Vídeos/áudios inspecionados e normalizados por FFprobe/FFmpeg, limites de duração/tamanho, semáforos e armazenamento privado. PDFs inspecionados em processo isolado com heap/tempo/páginas limitados; acesso autenticado e política sandbox. Fotos/currículos possuem validação de assinatura e tamanho. Isso não equivale a antivírus ou sandbox completa de sistema operacional.
- Pagamento de estágio calculado no servidor; idempotência e histórico; webhook com HMAC, consulta ao provedor, referência externa e valor verificados antes de pagar/publicar. Testes usam provedor sintético, não uma transação real.
- Actuator com detalhes ocultos e porta de gerenciamento privada; métricas exigem chave em cabeçalho. Health por trilha não expõe credenciais. Readiness geral e saúde de cada banco têm finalidades diferentes.
- Clientes HTTP com timeout e sem redirecionamento automático, chamadas de integração autorizadas por segredo de servidor e controles de concorrência.

## Tokens, XSS e CSRF

As sessões continuam independentes no localStorage do frontend, como na arquitetura existente. Não houve migração silenciosa para cookies. Requisições privadas usam Authorization manual; Basic/formulário e autenticação por cookies não estão ativos. Nesse modelo, manter CSRF desabilitado não equivale a proteger uma futura autenticação por cookie: se cookies forem introduzidos, a proteção CSRF deve ser redesenhada e testada. [Referência Spring Security](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html).

O localStorage permanece acessível a JavaScript e, portanto, a um eventual XSS. A renderização React auditada utiliza texto escapado; os arquivos privados possuem proteções específicas. Ainda é necessário homologar uma CSP compatível com mapas, workers, PDFs e mídias, reduzir scripts externos e manter auditoria do frontend. Não foi aplicada uma CSP genérica que quebrasse essas funções.

## Configuração de produção

Ative SPRING_PROFILES_ACTIVE=prod. O guard também reconhece production, mas o arquivo fornecido de propriedades é application-prod.properties; prefira prod e confira as propriedades efetivas.

O guard exige: origens CORS HTTPS exatas sem wildcard/padrões/userinfo/query; frontend HTTPS; Hibernate validate; Flyway habilitado; clean e baseline automático bloqueados; trilhas secundárias habilitadas; credenciais de banco não triviais e papéis distintos sem os nomes administrativos root/postgres; cinco segredos distintos com ao menos 32 bytes (JWTs, monitoramento, bridge); autenticação legada desativada; e-mail ativo com transporte TLS. Não detecta a entropia real dos segredos nem prova que um papel com outro nome não é superusuário: revise privilégios no PostgreSQL.

Não desative a verificação de identidade do certificado SMTP nem configure trust-all. A configuração real deve ser homologada com o provedor; a [documentação Angus Mail](https://eclipse-ee4j.github.io/angus-mail/docs/api/org.eclipse.angus.mail/org/eclipse/angus/mail/smtp/package-summary.html) descreve STARTTLS requerido e verificação de identidade.

SMTP desativado é permitido em desenvolvimento/testes, mas não em produção, porque o modo local aceita cadastro sem verificar o e-mail. Configure SMTP real antes de ativar prod. STARTTLS requerido é padrão de produção; SMTPS explícito também é aceito pelo guard.

As variáveis estão documentadas em backend/.env.example, sem valores reais. MONITORING_KEY e RADAR_BRIDGE_SECRET precisam ser preenchidos além das três chaves JWT. Prefira segredos aleatórios em ASCII/base64 com 64 ou mais caracteres, para atender também às validações existentes por caracteres do bridge. Use um gerenciador de segredos; não inclua valores em comandos compartilhados, imagens ou logs. Não use simultaneamente o alias APP_CORS_ALLOWED_ORIGINS com valores locais e CORS_ALLOWED_ORIGINS HTTPS.

### Proxy

TRUSTED_PROXY_CIDRS vazio ignora X-Forwarded-For. Configure apenas o IP/rede mínima do peer Nginx que conecta diretamente ao backend. /32 IPv4 e /128 IPv6 são preferíveis para peers estáveis; /0, nomes DNS e wildcard são rejeitados. Cabeçalhos com cadeias de múltiplos IPs ou nomes são ignorados, sem resolução DNS de dados do cliente. O filtro não passa a confiar em Host ou X-Forwarded-Proto; server.forward-headers-strategy permanece none.

Nginx sobrescreve o cabeçalho com $remote_addr. Se existir outro balanceador antes dele, configure real_ip somente para os peers explícitos desse balanceador antes da sobrescrita. Não repasse uma cadeia arbitrária. Os IPs efetivos e certificados da implantação não são conhecidos e não foram configurados automaticamente.

A sintaxe Nginx foi validada com nginx -t em contêiner sem portas públicas. A configuração local continua HTTP em loopback; TLS real, renovação e HSTS devem ser configurados no terminador externo ou no Nginx de produção, e ensaiados antes de liberar tráfego.

## Testes e backup/restauração

| Verificação executada | Resultado |
| --- | --- |
| Suíte completa Maven verify | 173 testes aprovados; zero falhas, erros ou ignorados; 6 min 11 s |
| Regressão final de segurança e package | 37 testes aprovados; zero falhas, erros ou ignorados |
| Compilação/empacotamento Java 21 | Aprovado, JAR executável gerado |
| Migrações | Flyway em schemas vazios separados; validação/reexecução idempotente e teste de instalação nova com Hibernate validate aprovados |
| Indisponibilidade | Recusa real de conexão simulada apenas em portas locais fechadas; rotas afetadas retornaram 503 sem detalhes de banco |
| Backup/restauração sintéticos | Aprovados nos três bancos, em alvos novos |
| Proxy | nginx -t aprovado; seis testes do filtro de confiança aprovados |
| Dependências | OSV completo, 156 dependências e zero avisos retornados na consulta final |
| Segredos em arquivos | 458 arquivos de texto versionados/novos examinados por padrões conhecidos; zero correspondências |
| Whitespace | git diff --check sem erros |

Após a suíte completa foi acrescentada a rejeição explícita das senhas públicas de desenvolvimento no guard de produção. Os 37 testes focados e o package foram executados novamente nesse estado final; não foi repetida a suíte completa após essa restrição adicional. Os 37 são parte da suíte e não devem ser somados aos 173 como casos distintos. Não houve remoção ou desativação de testes.

O restore comparou 58 tabelas/18 migrações em Oportunidades, 39/15 em Empresas e 48/19 em Capacita. Os bancos de destino foram criados apenas para o ensaio, sem --clean. Confirmada posteriormente, por leitura, a remoção dos três schemas raiz e dos três bancos temporários. As mídias dos testes foram direcionadas a diretórios temporários próprios.

Evidências: [validação] (evidência local não versionada), [173 testes] (evidência local não versionada), [37 testes finais] (evidência local não versionada), [restore] (evidência local não versionada), [identidade do JAR] (evidência local não versionada) e [scan de padrões de segredos] (evidência local não versionada). Logs publicados contêm apenas resumos; stack traces e logs completos locais não foram copiados para o repositório.

A imagem completa do backend não foi construída nem executada nesta revisão; o build validado é o JAR de produção. TLS público, entrega de e-mail e sandbox real do provedor de pagamento não foram exercitados.

O JAR final foi conferido: contém ProductionSecurityGuard e TrustedProxyFilter; não contém o filtro de logs removido. O scan de segredos não incluiu arquivos privados ignorados nem todo o histórico Git e não prova ausência de qualquer segredo possível.

O harness ops/scripts/Test-BackendSecurity.ps1 é local-only: exige socket Docker local, cria schemas com UUID nos três bancos, aponta os contextos Spring padrão para eles e usa diretórios temporários de mídia. Os demais testes criam seus próprios schemas únicos. Desativa SMTP e tarefas de produção. Com -TestRestore, usa pg_dump em formato custom, restaura somente os schemas sintéticos em bancos novos, compara tabelas e histórico Flyway e elimina apenas os alvos criados pelo próprio harness. Não usa --clean nem restaura sobre banco existente.

Exemplo no Windows, a partir da raiz, com os três PostgreSQL de desenvolvimento ativos e FFmpeg/FFprobe disponíveis:

~~~powershell
./ops/scripts/Test-BackendSecurity.ps1 -TestRestore
~~~

Para repetir a análise de dependências:

~~~powershell
cd backend
./mvnw.cmd --batch-mode dependency:tree -DoutputType=json -DoutputFile=target/dependency-audit-tree.json
node ../ops/scripts/audit-java-dependencies.mjs target/dependency-audit-tree.json target/audit/dependencies.json
~~~

## Checklist de produção e riscos remanescentes

- [ ] Configurar domínio, certificados HTTPS, renovação, TLS do proxy e HSTS após validar todos os domínios envolvidos.
- [ ] Ativar prod e conferir o fail-closed com a configuração efetiva; não usar a imagem no perfil local padrão em produção.
- [ ] Gerar/armazenar os cinco segredos exclusivos e estabelecer rotação; remover acessos de bootstrap desnecessários e revisar papéis administrativos.
- [ ] Confirmar que os três bancos/roles são independentes, sem SUPERUSER/CREATEDB/CREATEROLE, e com grants mínimos compatíveis com Flyway e com o runtime. Separar o migrador quando a operação permitir.
- [ ] Usar conexão PostgreSQL com verificação de certificado em redes não confiáveis e proteger as portas de API/banco/management por rede privada/firewall.
- [ ] Configurar CORS e proxies confiáveis com os IPs reais; validar múltiplos usuários atrás de NAT e cabeçalhos forjados na topologia final.
- [ ] Configurar SMTP com TLS e homologar confirmação/recuperação de cada trilha, entrega, remetente e DNS de e-mail.
- [ ] Configurar Mercado Pago em homologação e validar conta, webhook público HTTPS, eventos repetidos/falhas e conciliação; nenhum pagamento real foi feito nesta auditoria.
- [ ] Definir backup real criptografado/offsite dos três bancos E da mídia privada, retenção, acessos, RPO/RTO e ensaio periódico em alvos novos. O ensaio sintético não comprova recuperação da instalação de produção.
- [ ] Validar privilégios de arquivos, volume privado compartilhado e restauração da mídia; atualizar e analisar JDK, imagens e FFmpeg. Pin de imagens/actions por digest/commit precisa de política operacional.
- [ ] Habilitar monitoramento privado, alertas de indisponibilidade por trilha, discos, pools, falhas de login, SSE e pagamentos; testar entrega dos alertas.
- [ ] Revisar limites por rota/corpo JSON/conexões no proxy de produção, além dos limites existentes de arquivo e processamento. O limite global de 130 MB é necessário para mídia, mas não deve ser tratado como limite ideal para todos os endpoints JSON.
- [ ] Homologar CSP/XSS e decisões futuras sobre armazenamento de tokens; nunca habilitar cookies mantendo a política CSRF atual sem revisão.
- [ ] Manter gravações/autenticação no worker designado do proxy atual; cotas e semáforos ainda são locais por instância. Redis/fila distribuída não foram introduzidos sem necessidade e implementação completa.
- [ ] Rever retenção, consentimentos, tratamento de menores, incidentes e documentação de privacidade com os responsáveis institucionais/jurídicos. A auditoria técnica não atesta conformidade jurídica integral.

## Arquivos desta revisão

- .github/workflows/ci.yml
- README.md
- SEGURANCA_BACKEND_2026-11-06.md
- backend/.env.example
- backend/pom.xml
- backend/src/main/java/com/trampolim/api/auth/AuthController.java
- backend/src/main/java/com/trampolim/api/config/ClientErrorController.java
- backend/src/main/java/com/trampolim/api/config/CorsConfig.java
- backend/src/main/java/com/trampolim/api/config/OperationalRequestFilter.java
- backend/src/main/java/com/trampolim/api/config/ProductionSecurityGuard.java
- backend/src/main/java/com/trampolim/api/config/RequestMonitoringFilter.java
- backend/src/main/java/com/trampolim/api/config/SecurityConfig.java
- backend/src/main/java/com/trampolim/api/config/TrailDataSourceConfig.java
- backend/src/main/java/com/trampolim/api/config/TrustedProxyFilter.java
- backend/src/main/java/com/trampolim/api/security/JwtAuthenticationFilter.java
- backend/src/main/resources/application-prod.properties
- backend/src/main/resources/application.properties
- backend/src/test/java/com/trampolim/api/auth/RegistrationBoundsTest.java
- backend/src/test/java/com/trampolim/api/config/ClientErrorControllerSecurityTest.java
- backend/src/test/java/com/trampolim/api/config/CorsConfigSecurityTest.java
- backend/src/test/java/com/trampolim/api/config/OperationalRequestFilterTest.java
- backend/src/test/java/com/trampolim/api/config/ProductionSecurityGuardTest.java
- backend/src/test/java/com/trampolim/api/config/TrustedProxyFilterTest.java
- backend/src/test/java/com/trampolim/api/security/JwtAuthenticationFilterTrailIsolationTest.java
- frontend/ops/nginx.conf
- ops/scripts/Test-BackendSecurity.ps1
- ops/scripts/audit-java-dependencies.mjs

Inventário completo com evidências: [arquivos desta revisão] (evidência local não versionada). Alterações de navegação anteriores foram preservadas e não integram as correções de backend.
