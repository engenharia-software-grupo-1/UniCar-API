# US5 - Sistema de Comunicação e Histórico

## Visão Geral

Este documento define os contratos da API relacionados ao sistema de notificações, histórico de caronas e comunicação entre usuários.

### Regras Gerais

* Todas as rotas exigem autenticação.
* Usuários bloqueados não podem trocar mensagens entre si.
* O chat é criado automaticamente quando uma reserva é criada.
* Cada reserva possui exatamente um chat.
* Apenas o motorista e o responsável pela reserva possuem acesso ao chat.
* Reservas RECUSADAS, CANCELADAS, EXPIRADAS e FINALIZADAS permitem apenas consulta ao histórico de mensagens.

---

# GET /notificacoes

## Objetivo

Listar notificações do usuário autenticado.

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
    "id": 1,
    "titulo": "Reserva aceita",
    "mensagem": "Sua reserva foi aceita.",
    "tipo": "RESERVA_ACEITA",
    "visualizada": false,
    "dataEnvio": "2026-06-22T10:00:00"
  }
]
```

## Regras de Negócio

* RN-NOT-01: Retornar apenas notificações do usuário autenticado.
* RN-NOT-02: Ordenar notificações por data de envio decrescente.

---

# PATCH /notificacoes/{id}/visualizar

## Objetivo

Marcar uma notificação como visualizada.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 204

Sem conteúdo.

## Response 404

```json
{
  "message": "Notificação não encontrada"
}
```

## Regras de Negócio

* RN-NOT-03: Apenas o proprietário pode alterar a notificação.

---

# GET /historico/passageiro

## Objetivo

Consultar histórico de caronas como passageiro.

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
    "reservaId": 10,
    "caronaId": 5,
    "origem": "Bodocongó",
    "destino": "UFCG",
    "motorista": "João Silva",
    "status": "FINALIZADA",
    "dataViagem": "2026-06-20T08:00:00",
    "quantidadePassageiros": 2
  }
]
```

## Regras de Negócio

* RN-HIS-01: Retornar apenas reservas do usuário autenticado.

---

# GET /historico/motorista

## Objetivo

Consultar histórico de caronas como motorista.

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
    "caronaId": 5,
    "origem": "Bodocongó",
    "destino": "UFCG",
    "status": "FINALIZADA",
    "dataViagem": "2026-06-20T08:00:00",
    "totalPassageiros": 3
  }
]
```

## Regras de Negócio

* RN-HIS-02: Retornar apenas caronas criadas pelo motorista autenticado.

---

# GET /historico/{caronaId}

## Objetivo

Consultar detalhes de uma viagem do histórico.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "caronaId": 5,
  "origem": "Bodocongó",
  "destino": "UFCG",
  "motorista": {
    "id": 1,
    "nome": "João Silva"
  },
  "status": "FINALIZADA",
  "dataViagem": "2026-06-20T08:00:00",
  "passageiros": [
    {
      "id": 10,
      "nome": "Maria Oliveira"
    }
  ]
}
```

## Regras de Negócio

* RN-HIS-03: Apenas participantes da viagem podem consultar os detalhes.

---

# GET /chats

## Objetivo

Listar chats do usuário autenticado.

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
    "id": 1,
    "reservaId": 10,
    "nomeParticipante": "João Silva",
    "ultimaMensagem": "Tudo certo para amanhã.",
    "dataUltimaMensagem": "2026-06-22T10:00:00",
    "mensagensNaoLidas": 2
  }
]
```

## Regras de Negócio

* RN-COM-01: Retornar apenas chats acessíveis ao usuário autenticado.

---

# GET /chats/{id}/mensagens

## Objetivo

Consultar mensagens de um chat.

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
    "id": 1,
    "remetenteId": 1,
    "conteudo": "Olá!",
    "lida": true,
    "dataEnvio": "2026-06-22T09:00:00"
  },
  {
    "id": 2,
    "remetenteId": 2,
    "conteudo": "Tudo bem?",
    "lida": false,
    "dataEnvio": "2026-06-22T09:05:00"
  }
]
```

## Regras de Negócio

* RN-COM-02: Apenas participantes do chat podem consultar mensagens.

---

# POST /chats/{id}/mensagens

## Objetivo

Enviar mensagem para um chat.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "conteudo": "Olá!"
}
```

## Response 201

```json
{
  "id": 15,
  "remetenteId": 1,
  "conteudo": "Olá!",
  "lida": false,
  "dataEnvio": "2026-06-22T10:00:00"
}
```

## Response 403

```json
{
  "message": "Usuário não possui acesso ao chat"
}
```

## Regras de Negócio

* RN-COM-03: Apenas participantes do chat podem enviar mensagens.
* RN-COM-04: Usuários bloqueados não podem trocar mensagens.
* RN-COM-05: Chats de reservas RECUSADAS, CANCELADAS, EXPIRADAS ou FINALIZADAS não permitem envio de mensagens.

---

# PATCH /chats/{id}/lidas

## Objetivo

Marcar mensagens do chat como lidas.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 204

Sem conteúdo.

## Regras de Negócio

* RN-COM-06: Apenas participantes do chat podem executar a operação.

---

# Eventos Automáticos

## Notificações de Reserva

Devem ser geradas automaticamente quando ocorrer:

* RESERVA_CRIADA
* RESERVA_ACEITA
* RESERVA_RECUSADA
* RESERVA_CANCELADA
* RESERVA_EXPIRADA

## Notificações de Carona

Devem ser geradas automaticamente quando ocorrer:

* CARONA_CANCELADA
* CARONA_INICIADA
* CARONA_FINALIZADA

## Notificações de Interesse

Devem ser geradas automaticamente quando uma nova carona compatível for criada para um trajeto registrado em interesse_trajeto.

## Envio de E-mail

Se `usuario.receberEmail = true`, a notificação também deverá ser enviada por e-mail.
