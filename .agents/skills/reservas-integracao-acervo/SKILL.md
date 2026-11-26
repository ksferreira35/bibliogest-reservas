---
name: reservas-integracao-acervo
description: "Manter DisponibilidadeController, AcervoClient e HttpAcervoClient, integração HTTP e cálculo de exemplares livres."
---

# Reservas Integracao Acervo

- Acervo é dono do catálogo; Reservas possui disponibilidade e circulação. Consulte interfaces, DTOs e README. A URL de Acervo é configurada por ACERVO_BASE_URL; produção usa Service interno.
- Preserve repasse do Bearer da requisição válida apenas ao Acervo configurado. Não aceite destino HTTP arbitrário. Valide obra e exemplar e trate ausência como 404, indisponibilidade do Acervo como 503.
- GET /obras/{obraId}/disponibilidade retorna exemplares livres com exemplarId e disponivel. Consulte reservas ativas próprias para filtrar as cópias do Acervo.
- Teste com servidor simulado, respostas inválidas e timeouts; não execute reservas reais em testes. Mantenha contrato compatível com frontend e Acervo.
