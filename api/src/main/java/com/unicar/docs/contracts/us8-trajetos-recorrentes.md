# US8 - Trajetos Recorrentes

## Visão Geral

Este documento define os contratos da API relacionados à consulta e reutilização de trajetos recorrentes.

### Regras Gerais

* Todas as rotas exigem autenticação.
* Trajetos recorrentes são calculados a partir do histórico de caronas do motorista.
* Um trajeto é considerado recorrente quando existir pelo menos duas viagens com a mesma origem e destino.
* Não existe entidade própria de trajeto recorrente no banco de dados.
* Os dados são obtidos dinamicamente a partir do histórico de caronas.
* Não existe funcionalidade de favoritar trajetos recorrentes.
* O conceito de trajeto recorrente considera apenas origem e destino.
* Informações como veículo, horário, vagas, contribuição e ponto de encontro podem variar entre viagens e não fazem parte do trajeto recorrente.

---

# GET /trajetos-recorrentes

## Objetivo

Listar trajetos recorrentes do motorista autenticado.

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

    "quantidadeViagens": 8,

    "ultimaUtilizacao": "2026-06-20T08:00:00"
  }
]
```

## Regras de Negócio

* RN-TRJ-01: Retornar apenas trajetos do motorista autenticado.
* RN-TRJ-02: Considerar recorrente apenas trajetos utilizados duas ou mais vezes.
* RN-TRJ-03: Agrupar viagens pela combinação origem e destino.
* RN-TRJ-04: Ordenar trajetos pela quantidade de utilizações em ordem decrescente.

---

# GET /trajetos-recorrentes/{id}

## Objetivo

Consultar detalhes de um trajeto recorrente.

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

  "quantidadeViagens": 8,

  "primeiraUtilizacao": "2026-01-15T07:00:00",

  "ultimaUtilizacao": "2026-06-20T08:00:00"
}
```

## Response 404

```json
{
  "message": "Trajeto recorrente não encontrado"
}
```

## Regras de Negócio

* RN-TRJ-05: Apenas o proprietário pode consultar os detalhes do trajeto.
* RN-TRJ-06: O trajeto deve possuir pelo menos duas ocorrências para ser considerado recorrente.

---

# POST /trajetos-recorrentes/{id}/recriar

## Objetivo

Criar uma nova carona utilizando um trajeto recorrente como modelo.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "veiculoId": 1,

  "dataHoraSaida": "2026-06-25T07:00:00",

  "quantidadeVagas": 4,

  "valorContribuicao": 5.00,

  "pontoEncontro": "Portão principal"
}
```

## Response 201

```json
{
  "caronaId": 50,
  "status": "CRIADA"
}
```

## Regras de Negócio

* RN-TRJ-07: A nova carona deve copiar origem e destino do trajeto recorrente.
* RN-TRJ-08: Veículo, horário, vagas, contribuição e ponto de encontro devem ser informados novamente pelo motorista.
* RN-TRJ-09: A carona criada deve seguir todas as regras da US7.
* RN-TRJ-10: Apenas o proprietário do trajeto recorrente pode recriar a viagem.
* RN-TRJ-11: O valor de contribuição informado na recriação representa o valor do percurso completo da carona.
* RN-TRJ-12: Os valores de contribuição das futuras reservas serão calculados individualmente conforme as regras da US10.
