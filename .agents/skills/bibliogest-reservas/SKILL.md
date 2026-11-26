---
name: bibliogest-reservas
description: "Desenvolver e revisar a API de reservas do BiblioGest, disponibilidade, circulação e integração com o Acervo."
---

# BiblioGest — Reservas

## Antes de alterar

- Leia o README, o workflow de CI e os arquivos de configuração da branch atual. A main pode estar atrás da versão implantada; confirme a base antes de implementar ou integrar alterações.
- Mantenha os contratos REST, os códigos HTTP e a autorização existentes; alterações de contrato precisam ser coordenadas com os consumidores.
- Preserve segredos em variáveis de ambiente ou referências a Secrets. Use valores fictícios nos exemplos e testes; não registre tokens, senhas ou dumps.
- Deploy em RKE2: valide os manifests, use imagens com tags identificáveis e confira prontidão após publicação autorizada. Não modifique produção ao desenvolver ou testar localmente.

## Domínio de reservas

- Reservas é responsável pelos estados de circulação, disponibilidade, retirada, devolução e cancelamento. Acervo fornece dados das obras e exemplares.
- Use a identidade autenticada (claim sub) para propriedade da reserva. Autorize cancelamento pelo dono ou bibliotecário conforme contrato; retirada e devolução exigem bibliotecário.
- Preserve as transições de estado e a proteção contra reservas concorrentes de um exemplar. Valide disponibilidade e trate erros de integração com Acervo.
- Não dependa de IDs enviados pelo navegador para identificar o usuário autenticado. Quando presente, preserve o nome do solicitante obtido do token.
## Desenvolvimento e validação

- Use Java 21 e Maven, preserve as camadas controller, DTO, service e repository. Valide entradas e mantenha os erros padronizados.
- Use migrations Flyway incrementais, sem reescrever migrations já aplicadas. Utilize banco isolado para desenvolvimento e Testcontainers quando disponível.
- Mantenha autenticação JWT e autorização no backend. Valide emissor, audiência, expiração e roles conforme o contrato do serviço.
- Execute `./mvnw clean verify` (ou `mvn clean verify`). Quando a branch incluir JaCoCo e Checkstyle, preserve a cobertura mínima de 80% de linhas e branches e as verificações de lint. Swagger somente no perfil local.
