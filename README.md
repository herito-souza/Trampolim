# Trampolim

Plataforma de empregabilidade e capacitação com atuação inicial em Irecê-BA e região, preparada para outros territórios.

- **Oportunidades:** perfil profissional, diagnóstico, Radar, vagas, candidaturas e Plano de Empregabilidade.
- **Empresas:** vagas, recrutamento, demandas futuras, Hub e propostas de desafios profissionais.
- **Capacita:** cursos, videoaulas, materiais, avaliações, dúvidas e Passaporte Digital.

As três trilhas possuem autenticação e bancos independentes, com integrações explícitas.

## Documentação

A referência principal é **[docs/TRAMPOLIM.md](docs/TRAMPOLIM.md)**: arquitetura, funcionalidades, execução, testes, segurança e pendências.

[Relatórios históricos](docs/historico/README.md) preservam diagnósticos e resultados das versões anteriores. O histórico não substitui a documentação atual.

## Execução local

Requisitos: Java 21, Node.js compatível com o Vite instalado (20.19+ ou 22.12+), Docker e PowerShell para os scripts operacionais.

No diretório backend:

~~~powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
~~~

Em outro terminal, no diretório frontend:

~~~powershell
npm ci
npm run dev
~~~

Frontend: http://localhost:5173. API: http://localhost:8080. Os bancos locais usam as portas 5432, 5433 e 5434. As credenciais de desenvolvimento do Compose não devem ser usadas em produção. Configure os exemplos de ambiente conforme [o guia](docs/TRAMPOLIM.md).

## Estado e limites

Existem funcionalidades exclusivamente backend que ainda exigem telas: feirão, novos fluxos de revisão de desafios e Observatório v2. SMTP e pagamentos dependem de configuração externa. Não há garantia de contratação, certificação oficial, capacidade para milhares de usuários ou prontidão automática para produção.
