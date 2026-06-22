# US4 - Segurança e Gestão de Usuários

## Visão Geral

Este documento define os contratos da API relacionados ao bloqueio de usuários, denúncias e consulta de avaliações.

### Regras Gerais

* Todas as rotas exigem autenticação.
* Usuários bloqueados não podem interagir entre si.
* Usuários bloqueados não aparecem em resultados de busca.
* Usuários bloqueados não podem criar reservas entre si.
* Usuários bloqueados não podem trocar mensagens entre si.

---

# POST /usuarios/{id}/bloquear

## Objetivo

Bloquear um usuário.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 201

```json
{
  "id": 10,
  "usuarioId": 1,
  "usuarioBloqueadoId": 5,
  "dataBloqueio": "2026-06-22T10:00:00"
}
```

## Response 400

```json
{
  "message": "Usuário já bloqueado"
}
```

## Response 404

```json
{
  "message": "Usuário não encontrado"
}
```

## Regras de Negócio

* RN-BLOQ-01: Usuário não pode bloquear a si mesmo.
* RN-BLOQ-02: Não pode existir bloqueio duplicado.
* RN-BLOQ-03: Usuários bloqueados não podem interagir entre si.

---

# DELETE /usuarios/{id}/bloquear

## Objetivo

Remover um bloqueio existente.

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
  "message": "Bloqueio não encontrado"
}
```

## Regras de Negócio

* RN-BLOQ-04: Apenas o criador do bloqueio pode removê-lo.

---

# GET /usuarios/bloqueados

## Objetivo

Listar usuários bloqueados pelo usuário autenticado.

## Headers

```http
Authorization: Bearer <token>
```

## Response 200

```json
[
  {
    "id": 5,
    "nome": "João Silva",
    "curso": "Engenharia Elétrica",
    "dataBloqueio": "2026-06-22T10:00:00"
  }
]
```

## Regras de Negócio

* RN-BLOQ-05: Retornar apenas bloqueios realizados pelo usuário autenticado.

---

## Regras de Negócio

* RN-DEN-01: Usuário não pode denunciar a si mesmo.
* RN-DEN-02: Motivo é obrigatório.

---

# GET /usuarios/me/avaliacoes

## Objetivo

Consultar avaliações recebidas pelo usuário autenticado.

## Headers

```http
Authorization: Bearer <token>
```

## Response 200

```json
{
  "media": 4.8,
  "quantidade": 15,
  "avaliacoes": [
    {
      "id": 1,
      "nota": 5,
      "comentario": "Motorista muito pontual.",
      "dataAvaliacao": "2026-06-20T18:00:00"
    },
    {
      "id": 2,
      "nota": 4,
      "comentario": "Viagem tranquila.",
      "dataAvaliacao": "2026-06-15T18:00:00"
    }
  ]
}
```

## Regras de Negócio

* RN-AVA-01: Retornar apenas avaliações recebidas.
* RN-AVA-02: Média deve ser calculada automaticamente.
* RN-AVA-03: Quantidade deve refletir o total de avaliações recebidas.
