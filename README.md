# Trampolim 🚀

O Trampolim é um projeto inovador focado em conectar jovens a cursos de capacitação gratuitos (Sebrae, Senai, etc.) e cruzar o perfil com vagas de empresas locais usando "Match". 

O projeto adota uma arquitetura em **monólito de repositório (monorepo)**, sendo dividido em:
- **`frontend/`**: React + Vite + Tailwind CSS v4 + TypeScript.
- **`backend/`**: Java 21 + Spring Boot + Spring Security (JWT) + PostgreSQL.

---

## 🛠 Pré-requisitos
Para rodar este projeto na sua máquina, você precisa ter instalado:
- **Node.js** (versão 20.19+ ou 22.12+) e **NPM**.
- **Java JDK 21**.
- **Docker** e **Docker Compose** (para subir o banco de dados facilmente).

---

## ⚙️ Como executar o projeto localmente

Siga o passo a passo abaixo estritamente nessa ordem para garantir que a aplicação inicie sem erros:

### 1. Subir o Banco de Dados (PostgreSQL via Docker)
O backend exige um banco de dados rodando para iniciar corretamente. Usamos o Docker para facilitar isso.

1. Abra um terminal na raiz do projeto.
2. Navegue até a pasta do backend:
   ```bash
   cd backend
   ```
3. Suba o container do banco de dados em background:
   ```bash
   docker compose up -d
   ```
   *Dica: Se quiser acompanhar os logs do banco em tempo real, use `docker compose logs -f`.*
   
   *Nota: O banco subirá na porta `5432` com as credenciais padrões (`root` / `root`) definidas no arquivo `docker-compose.yml` e esperadas pelo `application.properties`.*

### 2. Rodar o Backend (API Spring Boot)
Com o banco rodando, podemos iniciar a API. 
*(Ainda no terminal, dentro da pasta `backend/`)*

- Se você estiver no **Linux/macOS**:
  ```bash
  ./mvnw spring-boot:run
  ```
- Se você estiver no **Windows**:
  ```bash
  mvnw.cmd spring-boot:run
  ```
*O backend estará rodando em `http://localhost:8080`. As rotas de autenticação e de visualização de perfis compartilhados são públicas; as demais exigem um token JWT.*

### 3. Rodar o Frontend (React / Vite)
Por fim, abra **uma nova aba/janela no terminal** e inicie a interface do usuário.

1. Navegue até a pasta do frontend:
   ```bash
   cd frontend
   ```
2. Instale as dependências na primeira vez:
   ```bash
   npm install
   ```
3. Inicie o servidor de desenvolvimento:
   ```bash
   npm run dev
   ```
*O frontend abrirá automaticamente no seu navegador em `http://localhost:5173`. O CORS já está configurado no backend para aceitar requisições nativas vindas dessa porta.*

### Compartilhar uma sessão de teste pela internet (sem deploy)

Para outra pessoa testar o sistema, mantenha o PostgreSQL e o backend rodando na sua máquina e abra um segundo terminal para o frontend:

```powershell
cd C:\Users\Windows\trampolim-main\backend
$env:CORS_ALLOWED_ORIGIN_PATTERNS="https://*.trycloudflare.com"
.\mvnw.cmd spring-boot:run
```

```powershell
cd C:\Users\Windows\trampolim-main\frontend
npm run dev:tunnel
```

Em um terceiro terminal, crie o endereço temporário do Cloudflare Tunnel:

```powershell
cloudflared tunnel --url http://localhost:5173
```

Envie à pessoa o endereço `https://...trycloudflare.com` exibido no terminal. As chamadas `/api` serão encaminhadas pelo frontend ao backend local; não é necessário expor a porta `8080`. O link e o acesso ficam disponíveis apenas enquanto os três serviços estiverem ativos. Use dados fictícios durante o teste e encerre o túnel quando terminar.

O comando `cloudflared` precisa estar instalado. Se o PowerShell não o reconhecer, instale o Cloudflare Tunnel e repita o comando. O tunnel rápido é temporário e o endereço pode mudar quando for iniciado novamente.

O backend recebe a permissão CORS para domínios `trycloudflare.com` somente quando essa variável de ambiente é definida na sessão local do PowerShell para o teste. Para o modo local comum, não defina a variável.

## 👤 Conta e perfil profissional

Responda ao questionário na página inicial. Ao concluir o diagnóstico, escolha **Criar conta** para se cadastrar ou **Já tenho uma conta** para entrar. Após a autenticação, a sessão fica salva no navegador por meio de um token JWT, válido por duas horas. O perfil, experiências e formações são armazenados no PostgreSQL. Cada usuário só consegue acessar os próprios dados.

Após entrar, a pessoa pode adicionar uma foto JPG, PNG ou WEBP de até 5 MB. Em seguida, acessa o perfil detalhado, onde pode atualizar nome, telefone, cidade e apresentação; adicionar e remover experiências e formações; e enviar, baixar ou remover um currículo PDF de até 10 MB. Também é possível continuar sem foto. A senha precisa ter pelo menos 8 caracteres.

Depois de confirmar os dados do perfil, a pessoa é encaminhada à área de oportunidades, com links para explorar cursos dos parceiros e vagas externas. O envio do currículo é opcional e pode ser feito depois.

### Questionário e recomendações

Antes do login ou cadastro, a página inicial apresenta um questionário vocacional de 10 perguntas. A primeira resposta define uma de três trilhas — atendimento e relacionamento, organização e números ou atividades práticas e operação — e seleciona as nove perguntas seguintes. As respostas somam pontos por área de interesse; cursos e áreas de trabalho só aparecem quando têm pontuação compatível com as respostas. Depois da autenticação, o resultado é salvo na conta e carregado nos próximos acessos. Ao refazer o questionário, as respostas anteriores são substituídas quando a nova trilha é concluída.

O questionário serve para explorar interesses profissionais e orientar buscas. Ele não é uma avaliação psicológica clínica nem substitui orientação profissional. As oportunidades listadas são áreas para pesquisar em plataformas parceiras; a disponibilidade de vagas depende dos anúncios ativos nesses serviços.

### Recursos adicionais

- Confirmação de e-mail no cadastro, recuperação de senha, alertas e lembretes usam SMTP. Para ativá-los, defina `MAIL_ENABLED=true`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH=true` e `SMTP_STARTTLS=true` antes de iniciar o backend. Em PowerShell, use `$env:SMTP_HOST = "smtp.seuprovedor.com"` (repita para as demais variáveis) na mesma janela em que inicia o Maven. Com o e-mail desativado, contas locais são confirmadas automaticamente para desenvolvimento.
- Para habilitar o painel administrativo, configure `ADMIN_EMAILS` no backend como uma lista de e-mails separada por vírgulas. Exemplo no PowerShell: `$env:ADMIN_EMAILS = "admin@exemplo.com"`. No frontend, crie `frontend/.env.local` com `VITE_ADMIN_EMAILS=admin@exemplo.com` e reinicie o Vite. O servidor verifica o e-mail da conta independentemente da visibilidade do botão.
- No painel **Admin**, cadastre cursos e vagas confirmados. Novas publicações e edições ficam pendentes até a revisão. Revise o link, empresa, requisitos, acessibilidade e data limite antes de aprovar; anúncios vencidos deixam de aparecer no catálogo. O painel registra autoria e revisão e avisa quando uma publicação está perto do prazo.
- Pessoas autenticadas podem sinalizar anúncios expirados, links quebrados, informações incorretas ou duplicadas. A equipe administra os avisos na seção **Avisos da comunidade** do painel Admin; o sistema evita repetir o mesmo aviso em 24 horas.
- Currículos e fotos são privados. O backend valida assinatura e tipo real do arquivo, limita tamanho, sanitiza o nome e envia os arquivos com `no-store` e `nosniff`.
- O catálogo permite filtrar vagas por cidade, modalidade, nível de experiência, faixa salarial e recursos de acessibilidade informados. A candidatura é feita no site indicado no anúncio.
- O perfil reúne etapas da candidatura, notas privadas e lembretes. Se SMTP estiver configurado, os lembretes por e-mail são enviados às 9h da data escolhida (horário de Brasília); atrasos de até dois dias também são processados.
- Na página **Meu perfil**, a pessoa pode baixar uma cópia JSON de seus dados ou excluir a conta confirmando a senha. A exclusão remove o perfil, currículo, foto, diagnóstico, experiências, formações e candidaturas.
- O perfil público pode ser ativado, revogado e configurado por campo. E-mail, telefone, foto e currículo não são compartilhados.
- Preferências de alertas e áreas ficam no perfil. Com SMTP configurado, avisos imediatos são enviados ao publicar uma vaga compatível; resumos diários são processados às 9h e os semanais às 9h de segunda-feira (horário de Brasília). Alertas pendentes ficam guardados no banco até o envio ser bem-sucedido.
- Alertas podem considerar área, cidade/localidade, modalidade, tipo de contrato, jornada, palavras-chave e, mediante opção explícita, raio de distância. A pessoa pode revogar essa opção no perfil; as coordenadas não aparecem no perfil público. No catálogo, a busca calcula o raio no navegador e não envia a localização. Para a distância funcionar, a administração precisa registrar as coordenadas confirmadas do local de trabalho.
- Os anúncios têm campos para contrato e jornada. Um processo diário sinaliza vagas que vencem em até sete dias e oculta as vencidas. A conferência de links é feita no painel Admin: abra o destino, confira e registre se continua ativo; links cadastrados são conteúdo externo e não são acessados automaticamente pelo servidor.
- O painel Admin apresenta métricas agregadas de aberturas de cursos/vagas e acompanhamentos. Eventos não incluem identificador da pessoa; registros são removidos após cerca de 400 dias.
- `GET /api/health` verifica a conexão com o banco. O backend registra erros 5xx e requisições lentas com um identificador de solicitação; erros do navegador são registrados sem enviar mensagem, stack trace ou dados pessoais.
- O diagnóstico oferece sugestões de próximos passos para currículo, entrevista e aprendizagem conforme as áreas de maior pontuação. São dicas de exploração profissional, não avaliação psicológica nem previsão de desempenho.

### CI e testes

O GitHub Actions executa auditoria de dependências, lint, testes unitários e compilação do frontend, testes ponta a ponta com Chromium e testes do backend com PostgreSQL em cada push e pull request. Para rodar localmente, use `npm run lint`, `npm test`, `npm run build` e `npm run test:e2e` em `frontend/`; instale o navegador com `npm run playwright:install`. Com Maven e PostgreSQL disponíveis, rode `mvn test` em `backend/`.

### Configuração para produção

Use o perfil `prod` e injete os valores de produção por um gerenciador de segredos. Veja `backend/.env.example` e `frontend/.env.example` para os nomes das variáveis; esses arquivos contêm apenas exemplos e não devem ser usados como segredos reais. `JWT_SECRET` precisa ter pelo menos 32 bytes. O perfil prod usa migrações Flyway e valida o esquema em vez de alterá-lo automaticamente. Configure CORS com a origem HTTPS exata do frontend e publique a API atrás de HTTPS. Login e cadastro também recebem limite por IP; em ambientes atrás de proxy, configure a identificação do IP apenas para proxies confiáveis.

### Acessibilidade

O frontend inclui foco visível, atalhos para pular conteúdo, movimento reduzido e ajustes para telas pequenas. Antes de publicar, siga o roteiro em [`ACCESSIBILITY.md`](ACCESSIBILITY.md), incluindo avaliação voluntária com pessoas com deficiência e leitor de tela.

---

## 🔒 Regras de Segurança (O Porteiro)
O backend possui um filtro rigoroso (`JwtAuthenticationFilter`).
- As rotas REST privadas exigem token JWT. Login, cadastro, confirmação de e-mail, solicitação de redefinição e leitura de perfil público são exceções; o painel administrativo também valida a lista `ADMIN_EMAILS` no backend.
- Rota livre para testes (Autenticação): Faça um `POST` para `http://localhost:8080/api/auth/login` enviando os dados de entrada para extrair seu token gerado pelo `JwtUtil`.

## 🤝 Como contribuir
1. Lembre-se que as dependências não se misturam. Qualquer pacote Node é restrito ao `/frontend`, enquanto pacotes Maven ficam restritos ao `pom.xml` do `/backend`.
2. O CSS deve seguir os padrões já definidos no Tailwind v4. Não adicione arquivos de estilo avulsos a não ser que estritamente necessário.
