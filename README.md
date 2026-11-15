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

No Swagger, use **Authorize** com o `access_token` da sua própria conta de teste do BiblioGest. Para rodar os testes automatizados, use `mvn verify`.
