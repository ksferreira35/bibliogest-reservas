# Migrations de Reservas

Não havia migrations SQL neste serviço durante a revisão de integração com o PostgreSQL. O banco `bibliogest_reservas` já é provisionado pela infraestrutura; migrations deste serviço não devem criar bancos ou usuários.

Adicione aqui alterações reais do schema em arquivos `V1__descricao.sql`, `V2__descricao.sql` etc. Não copie dumps de produção para este diretório. O Flyway usa o mesmo DataSource e a mesma credencial restrita ao banco deste serviço. Mantenha o histórico aplicado e `spring.jpa.hibernate.ddl-auto: validate`; não altere migrations já executadas.

Este diretório é incluído no Git e no contexto de build, mesmo com a exclusão de dumps SQL nas regras de ignore.
