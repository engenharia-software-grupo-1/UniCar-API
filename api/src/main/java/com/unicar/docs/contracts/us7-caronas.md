# US7 - Oferta e Gerenciamento de Caronas

## Visão Geral

Este documento define os contratos da API relacionados à criação, gerenciamento e execução de caronas.

### Regras Gerais

* Todas as rotas exigem autenticação.
* Apenas o proprietário da carona pode gerenciá-la.
* Apenas caronas futuras podem ser editadas ou canceladas.
* Usuários bloqueados não devem aparecer nas funcionalidades relacionadas à carona.
* A quantidade de vagas disponíveis deve considerar as reservas aceitas.
* Origem e destino devem possuir descrição, latitude e longitude.
* O ponto de encontro é apenas textual.
* O valor de contribuição é informado pelo motorista.

---

# POST /caronas

## Objetivo

Criar uma nova carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "veiculoId": 1,

  "origem": {
    "descricao": "Bodocongó",
    "latitude": -7.21456,
    "longitude": -35.90872
  },

  "destino": {
    "descricao": "UFCG",
    "latitude": -7.21590,
    "longitude": -35.90950
  },

  "pontoEncontro": "Portão principal",

  "dataHoraSaida": "2026-06-25T07:00:00",

  "quantidadeVagas": 4,

  "valorContribuicao": 5.00
}
```

## Response 201

```json
{
  "id": 10,
  "status": "CRIADA"
}
```

## Regras de Negócio

* RN-CAR-01: O veículo deve pertencer ao usuário autenticado.
* RN-CAR-02: A data da viagem deve ser futura.
* RN-CAR-03: A quantidade de vagas deve ser maior que zero.
* RN-CAR-04: O motorista não pode possuir outra carona em andamento.
* RN-CAR-05: Origem deve possuir descrição, latitude e longitude.
* RN-CAR-06: Destino deve possuir descrição, latitude e longitude.
* RN-CAR-07: O ponto de encontro é apenas textual.
* RN-CAR-08: O valor de contribuição é informado pelo motorista, mas não pode ultrapassar limite de `Distancia * fator (Ex. R$ 1,00 / km)`.

---

# GET /caronas/minhas

## Objetivo

Listar caronas criadas pelo motorista autenticado.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
[
  {
    "id": 10,
    "origem": {
      "descricao": "Bodocongó",
      "latitude": -7.21456,
      "longitude": -35.90872
    },
    "destino": {
      "descricao": "UFCG",
      "latitude": -7.21590,
      "longitude": -35.90950
    },
    "status": "CRIADA",
    "dataHoraSaida": "2026-06-25T07:00:00"
  }
]
```

## Regras de Negócio

* RN-CAR-09: Retornar apenas caronas do motorista autenticado.

---

# GET /caronas/{id}

## Objetivo

Consultar detalhes de uma carona.

## Headers

```http
Authorization: Bearer <token>
```

## Response 200

```json
{
  "id": 10,

  "origem": {
    "descricao": "Bodocongó",
    "latitude": -7.21456,
    "longitude": -35.90872
  },

  "destino": {
    "descricao": "UFCG",
    "latitude": -7.21590,
    "longitude": -35.90950
  },

  "pontoEncontro": "Portão principal",

  "dataHoraSaida": "2026-06-25T07:00:00",

  "quantidadeVagas": 4,

  "vagasDisponiveis": 3,

  "valorContribuicao": 5.00,

  "status": "CRIADA",

  "motorista": {
    "id": 1,
    "nome": "João Silva"
  },

  "veiculo": {
    "id": 1,
    "modelo": "Onix",
    "cor": "Prata"
  }
}
```

## Response 404

```json
{
  "message": "Carona não encontrada"
}
```

---

# PUT /caronas/{id}

## Objetivo

Atualizar informações de uma carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "origem": {
    "descricao": "Bodocongó",
    "latitude": -7.21456,
    "longitude": -35.90872
  },

  "destino": {
    "descricao": "UFCG",
    "latitude": -7.21590,
    "longitude": -35.90950
  },

  "pontoEncontro": "Biblioteca Central",

  "dataHoraSaida": "2026-06-25T07:30:00",

  "quantidadeVagas": 4,

  "valorContribuicao": 5.00
}
```

## Response 200

```json
{
  "id": 10,
  "status": "CRIADA"
}
```

## Regras de Negócio

* RN-CAR-10: Apenas o motorista proprietário pode editar a carona.
* RN-CAR-11: Não permitir edição após o início da viagem.
* RN-CAR-12: Não permitir reduzir vagas abaixo da quantidade de passageiros já aceitos.

---

# PATCH /caronas/{id}/cancelar

## Objetivo

Cancelar uma carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 10,
  "status": "CANCELADA"
}
```

## Regras de Negócio

* RN-CAR-13: Apenas o motorista proprietário pode cancelar a carona.
* RN-CAR-14: Todas as reservas associadas devem ser encerradas.
* RN-CAR-15: Notificações devem ser geradas para os participantes.

---

# GET /caronas/{id}/passageiros

## Objetivo

Listar passageiros de uma carona.

## Headers

```http
Authorization: Bearer <token>
```

## Response 200

```json
[
  {
    "reservaId": 20,
    "usuarioId": 5,
    "nome": "Maria Oliveira",
    "quantidadePassageiros": 2,
    "status": "ACEITA"
  }
]
```

## Regras de Negócio

* RN-CAR-16: Apenas o motorista proprietário pode consultar os passageiros.

---

# PATCH /reservas/{id}/remover

## Objetivo

Remover um passageiro da carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 20,
  "status": "CANCELADA"
}
```

## Regras de Negócio

* RN-CAR-17: Apenas o motorista proprietário pode remover passageiros.
* RN-CAR-18: Apenas reservas ACEITAS podem ser removidas.
* RN-CAR-19: O passageiro removido deve ser notificado.

---

# PATCH /caronas/{id}/iniciar

## Objetivo

Iniciar uma carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 10,
  "status": "EM_ANDAMENTO"
}
```

## Regras de Negócio

* RN-CAR-20: Apenas o motorista proprietário pode iniciar a carona.
* RN-CAR-21: Apenas caronas com status CRIADA podem ser iniciadas.

---

# PATCH /caronas/{id}/finalizar

## Objetivo

Finalizar uma carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 10,
  "status": "FINALIZADA"
}
```

## Regras de Negócio

* RN-CAR-22: Apenas o motorista proprietário pode finalizar a carona.
* RN-CAR-23: Apenas caronas EM_ANDAMENTO podem ser finalizadas.
* RN-CAR-24: Todas as reservas ACEITAS devem ser marcadas como FINALIZADAS.
* RN-CAR-25: Os participantes tornam-se elegíveis para avaliação.

---

# Atualização Automática de Vagas

## Regras de Negócio

* RN-CAR-26: Ao aceitar uma reserva, a quantidade de vagas disponíveis deve ser reduzida pela quantidade de passageiros da reserva.
* RN-CAR-27: Ao cancelar, remover, recusar ou expirar uma reserva, as vagas devem ser devolvidas à carona.
* RN-CAR-28: Não é permitido aceitar reservas que excedam a quantidade de vagas disponíveis.
