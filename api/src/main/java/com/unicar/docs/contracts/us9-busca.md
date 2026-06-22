# US9 - Busca e Descoberta de Caronas

## Visão Geral

Este documento define os contratos da API relacionados à busca de caronas, visualização de perfis públicos e gerenciamento de interesses em trajetos.

### Regras Gerais

* Todas as rotas exigem autenticação.
* Usuários bloqueados não devem visualizar caronas uns dos outros.
* Usuários bloqueados não podem visualizar perfis públicos uns dos outros.
* Apenas caronas futuras podem ser exibidas na busca.
* Apenas caronas com vagas disponíveis podem ser exibidas na busca.
* O valor de contribuição exibido na busca representa o valor máximo da contribuição para o trajeto completo da carona.
* O valor efetivamente pago pelo passageiro será calculado posteriormente durante o processo de reserva.

---

# GET /caronas

## Objetivo

Buscar caronas disponíveis.

## Headers

```http
Authorization: Bearer <token>
```

## Query Params

```http
GET /caronas?
origemLatitude=-7.21456&
origemLongitude=-35.90872&
destinoLatitude=-7.21590&
destinoLongitude=-35.90950&
generoMotorista=FEMININO&
cursoMotorista=CIENCIA_DA_COMPUTACAO
```

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

    "motorista": {
      "id": 1,
      "nome": "Maria Oliveira",
      "genero": "FEMININO",
      "curso": "Ciência da Computação",
      "reputacao": 4.8
    },

    "dataHoraSaida": "2026-06-25T07:00:00",

    "vagasDisponiveis": 3,

    "valorContribuicao": 5.00
  }
]
```

## Regras de Negócio

* RN-BUS-01: Retornar apenas caronas com status CRIADA.
* RN-BUS-02: Não retornar caronas criadas pelo próprio usuário.
* RN-BUS-03: Respeitar bloqueios entre usuários.
* RN-BUS-04: Retornar apenas caronas futuras.
* RN-BUS-05: Retornar apenas caronas com vagas disponíveis.
* RN-BUS-06: Permitir filtros por origem, destino, gênero do motorista e curso do motorista.

---

# GET /caronas/{id}

## Objetivo

Consultar detalhes de uma carona disponível.

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

  "pontoEncontro": "Portão Principal",

  "dataHoraSaida": "2026-06-25T07:00:00",

  "vagasDisponiveis": 3,

  "valorContribuicao": 5.00,

  "motorista": {
    "id": 1,
    "nome": "Maria Oliveira",
    "reputacao": 4.8
  }
}
```

## Response 404

```json
{
  "message": "Carona não encontrada"
}
```

## Regras de Negócio

* RN-BUS-07: Apenas caronas disponíveis podem ser consultadas.
* RN-BUS-08: Respeitar bloqueios entre usuários.

---

# GET /usuarios/{id}/perfil-publico

## Objetivo

Consultar informações públicas do motorista.

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
  "nome": "Maria Oliveira",
  "curso": "Ciência da Computação",
  "genero": "FEMININO",
  "reputacao": 4.8,
  "quantidadeAvaliacoes": 15
}
```

## Regras de Negócio

* RN-BUS-09: Não retornar informações privadas do usuário.
* RN-BUS-10: Exibir apenas informações públicas do perfil.
* RN-BUS-11: Respeitar bloqueios entre usuários.

---

# POST /interesses-trajeto

## Objetivo

Cadastrar interesse em um trajeto futuro.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "origem": {
    "latitude": -7.21456,
    "longitude": -35.90872
  },

  "destino": {
    "latitude": -7.21590,
    "longitude": -35.90950
  }
}
```

## Response 201

```json
{
  "id": 5
}
```

## Regras de Negócio

* RN-BUS-12: O interesse deve pertencer ao usuário autenticado.
* RN-BUS-13: Não permitir interesses duplicados para o mesmo trajeto.
* RN-BUS-14: Origem e destino são obrigatórios.

---

# GET /interesses-trajeto

## Objetivo

Listar trajetos de interesse cadastrados pelo usuário.

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
    "id": 5,

    "origem": {
      "latitude": -7.21456,
      "longitude": -35.90872
    },

    "destino": {
      "latitude": -7.21590,
      "longitude": -35.90950
    }
  }
]
```

## Regras de Negócio

* RN-BUS-15: Retornar apenas interesses pertencentes ao usuário autenticado.

---

# DELETE /interesses-trajeto/{id}

## Objetivo

Remover um trajeto de interesse.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 204

Sem conteúdo.

## Regras de Negócio

* RN-BUS-16: Apenas o proprietário pode remover um interesse.
* RN-BUS-17: O interesse deve existir.

---

# Notificações Automáticas

## Nova Carona Compatível

Quando uma nova carona for criada, o sistema deverá verificar os interesses cadastrados.

### Regras de Negócio

* RN-BUS-18: O sistema deve identificar usuários com interesse em trajetos compatíveis.
* RN-BUS-19: Usuários compatíveis devem receber uma notificação.
* RN-BUS-20: Caso o usuário possua a preferência `receberEmail = true`, a notificação também deverá ser enviada por e-mail.
