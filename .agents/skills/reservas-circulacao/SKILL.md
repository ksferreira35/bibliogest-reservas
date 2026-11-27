---
name: reservas-circulacao
description: "Implementar regras de criação, cancelamento, retirada, devolução e consulta de reservas."
---

# Reservas Circulacao

- Consulte ReservaController, ReservaService, Reserva, EstadoReserva. Estados válidos: AGUARDANDO_RETIRADA, RETIRADA, FINALIZADA, CANCELADA.
- Cancelamento permitido ao dono ou bibliotecário enquanto aguardando retirada; retirada passa para RETIRADA, devolução passa para FINALIZADA, ambos pelo bibliotecário. Preserve 404 para detalhe de outro leitor sem autorização.
- Identidade vem da claim sub e nome das claims name/given_name/family_name; leitorNome é snapshot no momento da criação, opcional para dados antigos. Não confunda UUID ou e-mail com nome.
- Criação e concorrência respeitam índice único parcial de reservas ativas por exemplar. Teste estados inválidos e erros sem dados de produção.

## Fluxo de trabalho e critérios de entrega

- Antes de alterar uma transição, relacione estado anterior, estado final, role e propriedade; preserve os códigos HTTP do contrato.
- Confira concorrência ao criar reserva e mantenha o snapshot de leitorNome; nomes de registros históricos não devem ser inventados.
- Teste transições válidas e inválidas, dono e bibliotecário, estados terminais e concorrência em base de testes.

## Referência do projeto

Consulte [contexto técnico](references/contexto.md) para Endpoints de reservas, Ciclo de circulação, Nome do solicitante. Use o [README do repositório](../../../README.md) e o código da branch atual como fontes de verdade.
