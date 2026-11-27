---
name: reservas-deploy-docker
description: "Preparar imagem de Reservas e integração de deploy com manifests compartilhados do Acervo."
---

# Reservas Deploy Docker

- Build Maven em Docker; os testes de PostgreSQL/Testcontainers rodam antes no CI. Use Java 21, banco de desenvolvimento e variáveis de .env.example.
- Manifests de Reservas ficam no repositório Acervo em deploy/k8s/31-reservas.yaml e políticas compartilhadas. Preserve conexão interna ao Acervo e ao banco de Reservas.
- Revise configuração de prontidão, credenciais referenciadas e versões antes de publicação autorizada. Não exponha Swagger em produção ou abra CORS genérico.
- Preserve banco e migrações, prepare retorno à versão anterior e verifique rollout/saúde. Não copie segredos e imagens de dados para repositório.

## Fluxo de trabalho e critérios de entrega

- Confirme os manifests compartilhados no Acervo e a configuração de banco de Reservas antes de alterar o empacotamento.
- Valide construção e testes antes de qualquer publicação; não altere configurações compartilhadas apenas para contornar falha de um serviço.
- Após atualização autorizada, confira rollout, prontidão e integração com Acervo sem criar registros reais de teste.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Arquitetura do BiblioGest, GitHub Actions. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
