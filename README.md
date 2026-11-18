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
