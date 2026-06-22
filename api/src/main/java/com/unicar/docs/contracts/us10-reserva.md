# US10 - Participação em Caronas

## Visão Geral

Este documento define os contratos da API relacionados à solicitação, gerenciamento e participação em caronas.

### Regras Gerais

* Todas as rotas exigem autenticação.
* O motorista não pode reservar sua própria carona.
* Usuários bloqueados não podem criar reservas entre si.
* Apenas caronas com status CRIADA podem receber novas reservas.
* O valor da contribuição da reserva é calculado automaticamente pelo sistema.
* O valor da contribuição da reserva pode ser diferente do valor da contribuição da carona.
* O valor da contribuição da reserva é calculado com base no trecho percorrido pelo passageiro e na quantidade de passageiros informada na solicitação.

---

# POST /reservas

## Objetivo

Solicitar participação em uma carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "caronaId": 10,

  "quantidadePassageiros": 2,

  "origemEmbarque": {
    "descricao": "Rua Aprígio Veloso",
    "latitude": -7.22000,
    "longitude": -35.91000
  }
}
```

## Response 201

```json
{
  "id": 50,

  "status": "PENDENTE",

  "quantidadePassageiros": 2,

  "valorContribuicao": 8.00
}
```

## Response 400

```json
{
  "message": "Quantidade de vagas indisponível"
}
```

## Regras de Negócio

* RN-RES-01: O passageiro não pode reservar sua própria carona.
* RN-RES-02: O local de embarque deve ser informado.
* RN-RES-03: O local de embarque deve estar compatível com o trajeto da carona.
* RN-RES-04: A quantidade de passageiros deve ser maior que zero.
* RN-RES-05: A quantidade de passageiros não pode ultrapassar as vagas disponíveis.
* RN-RES-06: Não permitir reservas duplicadas para a mesma carona.
* RN-RES-07: O valor da contribuição deve ser calculado automaticamente pelo sistema.
* RN-RES-08: O valor calculado deve considerar:

    * valor da carona;
    * distância total da carona;
    * distância percorrida pelo passageiro;
    * quantidade de passageiros da reserva.
* RN-RES-09: O valor calculado deve ser armazenado na reserva.
* RN-RES-10: A criação da reserva deve gerar notificação ao motorista.

---

# POST /reservas/simular

## Objetivo

Calcular o valor estimado da contribuição antes da criação da reserva.

## Regras de Negócio

RN-RES-34
O endpoint não cria reservas.

RN-RES-35
O valor retornado possui caráter informativo.

RN-RES-36
O cálculo utilizado deve ser o mesmo aplicado na criação da reserva.

RN-RES-37
O valor da reserva deve ser recalculado durante a criação para evitar manipulação de dados pelo cliente.
---

# GET /reservas/enviadas

## Objetivo

Listar solicitações enviadas pelo usuário autenticado.

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
    "id": 50,

    "carona": {
      "id": 10,
      "origem": "Bodocongó",
      "destino": "UFCG"
    },

    "status": "PENDENTE",

    "quantidadePassageiros": 2,

    "valorContribuicao": 8.00
  }
]
```

## Regras de Negócio

* RN-RES-11: Retornar apenas reservas criadas pelo usuário autenticado.

---

# GET /reservas/recebidas

## Objetivo

Listar solicitações recebidas pelo motorista autenticado.

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
    "id": 50,

    "usuario": {
      "id": 5,
      "nome": "Maria Oliveira"
    },

    "origemEmbarque": {
      "descricao": "Rua Aprígio Veloso",
      "latitude": -7.22000,
      "longitude": -35.91000
    },

    "quantidadePassageiros": 2,

    "valorContribuicao": 8.00,

    "status": "PENDENTE"
  }
]
```

## Regras de Negócio

* RN-RES-12: Retornar apenas reservas das caronas do motorista autenticado.
* RN-RES-13: O motorista deve visualizar o valor calculado da reserva antes da decisão.

---

# GET /reservas/{id}

## Objetivo

Consultar detalhes de uma reserva.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 50,

  "status": "PENDENTE",

  "quantidadePassageiros": 2,

  "valorContribuicao": 8.00,

  "origemEmbarque": {
    "descricao": "Rua Aprígio Veloso",
    "latitude": -7.22000,
    "longitude": -35.91000
  },

  "carona": {
    "id": 10,
    "origem": "Bodocongó",
    "destino": "UFCG"
  }
}
```

## Regras de Negócio

* RN-RES-14: Apenas o passageiro responsável ou o motorista da carona podem consultar a reserva.

---

# PATCH /reservas/{id}/aceitar

## Objetivo

Aceitar uma solicitação de reserva.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 50,
  "status": "ACEITA"
}
```

## Regras de Negócio

* RN-RES-15: Apenas o motorista da carona pode aceitar reservas.
* RN-RES-16: Apenas reservas com status PENDENTE podem ser aceitas.
* RN-RES-17: Deve existir quantidade de vagas suficiente.
* RN-RES-18: Ao aceitar uma reserva, as vagas disponíveis da carona devem ser atualizadas.
* RN-RES-19: O passageiro deve ser notificado.

---

# PATCH /reservas/{id}/recusar

## Objetivo

Recusar uma solicitação de reserva.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 50,
  "status": "RECUSADA"
}
```

## Regras de Negócio

* RN-RES-20: Apenas o motorista da carona pode recusar reservas.
* RN-RES-21: Apenas reservas PENDENTES podem ser recusadas.
* RN-RES-22: O passageiro deve ser notificado.

---

# PATCH /reservas/{id}/cancelar

## Objetivo

Cancelar uma reserva.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 50,
  "status": "CANCELADA"
}
```

## Regras de Negócio

* RN-RES-23: O passageiro pode cancelar sua própria reserva.
* RN-RES-24: O motorista pode cancelar reservas ACEITAS de sua carona.
* RN-RES-25: Reservas FINALIZADAS não podem ser canceladas.
* RN-RES-26: Caso a reserva esteja ACEITA, as vagas devem ser devolvidas à carona.
* RN-RES-27: O usuário afetado deve ser notificado.

---

# Status Possíveis

```text
PENDENTE
ACEITA
RECUSADA
CANCELADA
EXPIRADA
FINALIZADA
```

## Regras de Negócio

* RN-RES-28: Reservas aceitas devem ser marcadas como FINALIZADAS quando a carona for finalizada.
* RN-RES-29: Reservas pendentes podem expirar automaticamente quando a data da carona for atingida.
* RN-RES-30: Reservas EXPIRADAS permitem apenas consulta histórica.
* RN-RES-31: Reservas RECUSADAS permitem apenas consulta histórica.
* RN-RES-32: Reservas CANCELADAS permitem apenas consulta histórica.
* RN-RES-33: Reservas FINALIZADAS permitem participação no processo de avaliação da US11.
