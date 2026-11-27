---
name: reservas-integracao-acervo
description: "Manter DisponibilidadeController, AcervoClient e HttpAcervoClient, integração HTTP e cálculo de exemplares livres."
---

# Reservas Integracao Acervo

- Acervo é dono do catálogo; Reservas possui disponibilidade e circulação. Consulte interfaces, DTOs e README. A URL de Acervo é configurada por ACERVO_BASE_URL; produção usa Service interno.
- Preserve repasse do Bearer da requisição válida apenas ao Acervo configurado. Não aceite destino HTTP arbitrário. Valide obra e exemplar e trate ausência como 404, indisponibilidade do Acervo como 503.
- GET /obras/{obraId}/disponibilidade retorna exemplares livres com exemplarId e disponivel. Consulte reservas ativas próprias para filtrar as cópias do Acervo.
- Teste com servidor simulado, respostas inválidas e timeouts; não execute reservas reais em testes. Mantenha contrato compatível com frontend e Acervo.

## Fluxo de trabalho e critérios de entrega

- Consulte o contrato do Acervo e preserve o identificador de exemplar; a circulação não duplica dados de catálogo.
- Mantenha chamadas HTTP limitadas à origem configurada e preserve tradução de indisponibilidade para falha de serviço.
- Use servidor simulado para validar respostas válidas, ausência de recurso, erros e timeout; teste a filtragem por reservas ativas.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Endpoints de reservas, SQL Injection e integração, Arquitetura do BiblioGest. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
