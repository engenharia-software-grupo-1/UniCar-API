# US6 - Cadastro de Veículos

## Visão Geral

Este documento define os contratos da API relacionados ao gerenciamento de veículos utilizados para oferta de caronas.

### Regras Gerais

* Todas as rotas exigem autenticação.
* Todo veículo pertence a um único usuário.
* A placa deve ser única no sistema.
* Usuários só podem gerenciar seus próprios veículos.

---

# POST /veiculos

## Objetivo

Cadastrar um novo veículo.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "modelo": "Onix",
  "placa": "ABC1D23",
  "cor": "Prata"
}
```

## Response 201

```json
{
  "id": 1,
  "modelo": "Onix",
  "placa": "ABC1D23",
  "cor": "Prata"
}
```

## Response 400

```json
{
  "message": "Placa já cadastrada"
}
```

## Regras de Negócio

* RN-VEI-01: O veículo deve ser associado ao usuário autenticado.
* RN-VEI-02: A placa deve ser única no sistema.
* RN-VEI-03: Modelo é obrigatório.
* RN-VEI-04: Placa é obrigatória.

---

# GET /veiculos

## Objetivo

Listar veículos cadastrados pelo usuário autenticado.

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
    "modelo": "Onix",
    "placa": "ABC1D23",
    "cor": "Prata"
  },
  {
    "id": 2,
    "modelo": "HB20",
    "placa": "XYZ9A87",
    "cor": "Branco"
  }
]
```

## Regras de Negócio

* RN-VEI-05: Retornar apenas veículos pertencentes ao usuário autenticado.

---

# GET /veiculos/{id}

## Objetivo

Consultar detalhes de um veículo.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "id": 1,
  "modelo": "Onix",
  "placa": "ABC1D23",
  "cor": "Prata"
}
```

## Response 404

```json
{
  "message": "Veículo não encontrado"
}
```

## Response 403

```json
{
  "message": "Acesso negado"
}
```

## Regras de Negócio

* RN-VEI-06: Usuário só pode visualizar veículos próprios.

---

# PUT /veiculos/{id}

## Objetivo

Atualizar informações de um veículo.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "modelo": "Onix Plus",
  "placa": "ABC1D23",
  "cor": "Preto"
}
```

## Response 200

```json
{
  "id": 1,
  "modelo": "Onix Plus",
  "placa": "ABC1D23",
  "cor": "Preto"
}
```

## Response 400

```json
{
  "message": "Placa já cadastrada"
}
```

## Response 403

```json
{
  "message": "Acesso negado"
}
```

## Regras de Negócio

* RN-VEI-07: Usuário só pode editar veículos próprios.
* RN-VEI-08: Não permitir alteração para uma placa já existente.

---

# DELETE /veiculos/{id}

## Objetivo

Remover um veículo.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 204

Sem conteúdo.

## Response 400

```json
{
  "message": "Veículo possui caronas ativas associadas"
}
```

## Response 403

```json
{
  "message": "Acesso negado"
}
```

## Regras de Negócio

* RN-VEI-09: Usuário só pode remover veículos próprios.
* RN-VEI-10: Não permitir exclusão de veículo associado a uma carona ativa.
* RN-VEI-11: Consideram-se ativas as caronas CRIADA, ACEITA ou EM_ANDAMENTO.
