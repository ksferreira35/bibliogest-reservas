# Implantação de Reservas

O Dockerfile deste repositório gera uma imagem independente. A infraestrutura Kubernetes compartilhada e o guia operacional ficam em `bibliogest-acervo/deploy`, no repositório Acervo.

Na estrutura local desta organização: `microsservices/acervo-api/deploy/README.md`. O guia contém configuração de PostgreSQL/Keycloak, variáveis de ambiente, build/push, publicação HTTPS, implantação ordenada e atualização individual desta aplicação.

Execute o build com as dependências instaladas antes de publicar. Nenhum deploy foi executado durante a preparação: o ambiente não permitiu acesso ao cluster nem aos registries.

## Banco já provisionado

Reservas usa `jdbc:postgresql://postgres:5432/bibliogest_reservas`, com as chaves `username` e `password` do Secret `reservas-db` no namespace `bibliogest`. O deploy individual confere esse Secret sem criar/rotacionar credenciais ou depender do Job histórico de criação dos bancos.

Na estrutura local, o procedimento de revisão, build e verificação sem escrita está em [`../acervo-api/deploy/POSTGRESQL.md`](../acervo-api/deploy/POSTGRESQL.md). A API conserva Flyway/JPA, sua audience do Keycloak e suas permissões. Suas migrations ficam em `src/main/resources/db/migration`; não havia migrations SQL nesta revisão.
