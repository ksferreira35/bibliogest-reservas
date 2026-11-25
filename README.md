# BiblioGest — Reservas

## Rodar no computador

Use Java 21 e Docker. Copie `.env.example` para `.env` e coloque uma senha de desenvolvimento em `DB_PASSWORD`. O arquivo local é ignorado pelo Git.

Na raiz do repositório, suba o PostgreSQL de Reservas uma vez. O comando usa a mesma senha do `.env`.

```bash
POSTGRES_PASSWORD="$(sed -n 's/^DB_PASSWORD=//p' .env)" docker run -d \
  --name bibliogest-reservas-db \
  -p 127.0.0.1:15433:5432 \
  -v bibliogest-reservas-docker-data:/var/lib/postgresql/data \
  -e POSTGRES_USER=bibliogest_reservas \
  -e POSTGRES_DB=bibliogest_reservas \
  -e POSTGRES_PASSWORD \
  docker.io/library/postgres:17.12-bookworm
```

Se o contêiner já existir e estiver parado, use `docker start bibliogest-reservas-db`.

No IntelliJ, abra o projeto pelo `pom.xml` com o JDK 21. Na configuração de execução de `ReservasApiApplication`, selecione `.env` no campo **Environment variables** e inicie a API.

- [Saúde da API e do banco](http://localhost:8081/reservas/actuator/health/readiness)
- [Swagger de Reservas](http://localhost:8081/reservas/swagger-ui/index.html)

No Swagger, use **Authorize** com o `access_token` da sua própria conta de teste do BiblioGest. Para rodar os testes automatizados, use `mvn verify`. Os testes com PostgreSQL real (`ReservaBancoTest`) sobem um contêiner pelo Testcontainers, então o Docker precisa estar rodando.

Para consultar o Acervo publicado durante o desenvolvimento, o `.env.example` define `ACERVO_BASE_URL=https://bibliogest.ksferreira.org/acervo`. No RKE2, o Deployment usa o Service interno `http://acervo:8080/acervo`.

O CI executa os testes antes do empacotamento. O Dockerfile gera o pacote com `-DskipTests`, pois o teste de banco depende do Docker disponível no ambiente de testes.

## Endpoints de reservas

Prefixo público: `/reservas/api/v1`. O leitor vem da claim `sub` do token. O token do leitor é repassado ao Acervo na criação e na consulta de disponibilidade, definido por `ACERVO_BASE_URL`.

| Método e caminho | Perfil | O que faz |
| --- | --- | --- |
| `POST /reservas` (`obraId`, `exemplarId`) | LEITOR | Cria a reserva. 404 se o exemplar não existe na obra, 409 se já está bloqueado, 503 se o Acervo não responde. |
| `GET /reservas/minhas` | LEITOR, BIBLIOTECARIO | Reservas do usuário autenticado (`pagina`, `tamanho`). |
| `GET /reservas?estado=` | BIBLIOTECARIO | Todas as reservas. |
| `GET /reservas/{id}` | dono ou BIBLIOTECARIO | Detalhe. Reserva de outro leitor devolve 404. |
| `POST /reservas/{id}/cancelar` | dono ou BIBLIOTECARIO | Só em `AGUARDANDO_RETIRADA`. |
| `POST /reservas/{id}/retirada` | BIBLIOTECARIO | `AGUARDANDO_RETIRADA` → `RETIRADA`. |
| `POST /reservas/{id}/devolucao` | BIBLIOTECARIO | `RETIRADA` → `FINALIZADA`. |
| `GET /obras/{obraId}/disponibilidade` | LEITOR, BIBLIOTECARIO | Somente as cópias da obra no Acervo que estão livres: `{"obraId":"uuid-da-obra","exemplares":[{"exemplarId":"uuid-do-exemplar","disponivel":true}]}`. 404 se a obra não existe, 503 se o Acervo não responde. |


## Qualidade e cobertura obrigatória

A configuração de cobertura fica no `pom.xml`, com **JaCoCo 0.8.16**. Para rodar a mesma verificação usada no CI, use Java 21:

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
```

A fase `validate` verifica Checkstyle; a fase `test` executa JUnit; `verify` gera o relatório JaCoCo e reprova o build se a cobertura global ficar abaixo de **80% de linhas ou 80% de branches**. Todos os testes também precisam passar. Não basta executar `mvn test` para validar o limite, porque a regra é cobrada em `verify`.

| Arquivo gerado | Uso |
| --- | --- |
| `target/site/jacoco/index.html` | Relatório navegável por pacote, classe e linha |
| `target/site/jacoco/jacoco.xml` | Totais e integração com analisadores |
| `target/jacoco.exec` | Dados de execução coletados pelo agente |
| `target/surefire-reports/` | Resultados de cada teste JUnit |

O JaCoCo mede todas as classes compiladas da aplicação; não há exclusão de services, controllers, DTOs ou configuração de segurança para aumentar a cobertura. `target/` é ignorado pelo Git e não vai para o repositório. A cobertura mede execução de código, não garante ausência de bugs; os testes verificam resultados e erros esperados.

### GitHub Actions

O workflow `.github/workflows/ci.yml` executa:

```text
lint (Checkstyle) → testes + cobertura (clean verify) → empacotamento
```

Os jobs usam `needs`, portanto o empacotamento não começa quando um teste, lint ou limite de cobertura falhar. O último job usa `-DskipTests package`: ele empacota o mesmo código já verificado pelo job anterior, sem tentar validar cobertura em uma máquina sem os dados da execução anterior.

O resumo da execução mostra as porcentagens de linhas e branches. Em **Actions → execução → Artifacts → coverage-and-tests**, baixe o relatório HTML e os resultados JUnit. Os arquivos são publicados também em caso de reprovação e ficam disponíveis por 14 dias. O workflow roda em push para `main` e esta branch de implementação, além de pull requests para `main`.

Os testes não usam credenciais de produção e não fazem login em contas reais. Os testes de segurança fornecem JWTs simulados ao decoder de teste e verificam roles válidas, ausência de roles e claims malformadas. Somente `LEITOR` e `BIBLIOTECARIO`, com os nomes exatos, recebem permissões da aplicação. O marcador de autenticação Bearer do Spring não equivale a uma role administrativa.

O Cypress fica no [repositório do frontend](https://github.com/ksferreira35/bibliogest-frontend), onde testa os fluxos em navegador e acessibilidade. Nesta API, os testes usam JUnit, MockMvc e testes de integração; não é necessário outro repositório nem um navegador para medir a cobertura Java. Referência: [regra check do JaCoCo](https://www.jacoco.org/jacoco/trunk/doc/check-mojo.html).

### Segurança e integração com o navegador

O backend valida o token Bearer e aplica as permissões do Spring Security; as regras do frontend não substituem essa validação. Os dados privados continuam em variáveis locais ou Secrets do Kubernetes. `.env` e credenciais reais não devem ser incluídos em commits nem imagens; `.env.example` documenta somente as configurações de desenvolvimento.

O frontend e esta API usam a mesma origem em produção, com caminhos separados no Ingress. Não foi adicionada uma liberação genérica de CORS. Para desenvolvimento, use o proxy do Vite no frontend. Caso a API seja servida em outra origem no futuro, configure apenas as origens autorizadas, mantendo a validação de tokens.

A CSP é entregue pelo Nginx do frontend, junto do documento HTML. Esta API retorna JSON e não injeta a interface nem os controles de acessibilidade. Os READMEs do frontend detalham a política e a auditoria WCAG com Cypress/axe.

### Cenários de teste de Reservas

As suítes verificam criação, cancelamento pelo dono/bibliotecário, proibição de acessar reservas de outra pessoa, retirada/devolução, estados inválidos e disponibilidade. Os testes do cliente HTTP simulam o Acervo em um servidor local e conferem o repasse do Bearer e o tratamento de respostas inválidas ou indisponíveis.

`ReservaBancoTest` usa PostgreSQL real em contêiner via Testcontainers para verificar as restrições de reservas ativas e concorrência. O Docker precisa estar disponível para a suíte completa, inclusive no CI. Não use a base de produção para esses testes. Os demais testes usam mocks ou configurações isoladas.
