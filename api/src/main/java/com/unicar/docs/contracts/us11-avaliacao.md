# US11 - Sistema de Avaliações

## Visão Geral

Este documento define os contratos da API relacionados à avaliação de motoristas e passageiros após a conclusão de uma carona.

### Regras Gerais

* Todas as rotas exigem autenticação.
* Apenas usuários que participaram da mesma carona podem se avaliar.
* Avaliações só podem ser realizadas após a finalização da carona.
* Cada participante pode avaliar outro participante apenas uma vez por carona.
* A reputação do usuário é calculada automaticamente a partir das avaliações recebidas.
* Avaliações permanecem disponíveis para consulta histórica.
* Avaliações não podem ser editadas após o envio.
* Avaliações não podem ser removidas após o envio.

---

# POST /avaliacoes

## Objetivo

Registrar uma avaliação para um participante da carona.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "caronaId": 10,
  "avaliadoId": 5,
  "nota": 5,
  "comentario": "Motorista pontual e educado."
}
```

## Response 201

```json
{
  "id": 100
}
```

## Regras de Negócio

* RN-AVA-01: Apenas participantes da mesma carona podem realizar avaliações.
* RN-AVA-02: A carona deve possuir status FINALIZADA.
* RN-AVA-03: A nota deve estar entre 1 e 5.
* RN-AVA-04: O usuário não pode avaliar a si próprio.
* RN-AVA-05: Não permitir mais de uma avaliação para o mesmo participante na mesma carona.
* RN-AVA-06: A avaliação deve ficar vinculada à carona que a originou.
* RN-AVA-07: Após registrar uma avaliação, a reputação do usuário avaliado deve ser recalculada.

---

# GET /usuarios/me/avaliacoes

## Objetivo

Consultar avaliações recebidas pelo usuário autenticado.

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
    "id": 100,
    "nota": 5,
    "comentario": "Motorista pontual e educado.",
    "dataAvaliacao": "2026-06-25T18:30:00",
    "avaliador": {
      "id": 10,
      "nome": "João Silva"
    },
    "carona": {
      "id": 25
    }
  }
]
```

## Regras de Negócio

* RN-AVA-08: Retornar apenas avaliações recebidas pelo usuário autenticado.
* RN-AVA-09: Ordenar avaliações da mais recente para a mais antiga.

---

# GET /usuarios/{id}/reputacao

## Objetivo

Consultar a reputação pública de um usuário.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 200

```json
{
  "usuarioId": 5,
  "media": 4.8,
  "quantidadeAvaliacoes": 35
}
```

## Regras de Negócio

* RN-AVA-10: A reputação deve ser pública.
* RN-AVA-11: A média deve considerar todas as avaliações válidas recebidas.
* RN-AVA-12: A quantidade de avaliações deve corresponder ao total de avaliações recebidas.

---

# GET /caronas/{id}/avaliacoes-pendentes

## Objetivo

Listar participantes que ainda podem ser avaliados após a finalização da carona.

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
    "usuarioId": 5,
    "nome": "Maria Oliveira",
    "tipo": "PASSAGEIRO"
  },
  {
    "usuarioId": 8,
    "nome": "Pedro Santos",
    "tipo": "MOTORISTA"
  }
]
```

## Regras de Negócio

* RN-AVA-13: Apenas participantes da carona podem consultar avaliações pendentes.
* RN-AVA-14: Apenas caronas FINALIZADAS possuem avaliações pendentes.
* RN-AVA-15: Usuários já avaliados não devem ser retornados.

---

# Cálculo de Reputação

## Regras de Negócio

* RN-AVA-16: A reputação é calculada pela média aritmética das notas recebidas.
* RN-AVA-17: A reputação deve ser atualizada após cada nova avaliação.
* RN-AVA-18: A reputação deve ser exibida com uma casa decimal.
* RN-AVA-19: Usuários sem avaliações devem possuir reputação nula.

---

# Notificações Automáticas

## Regras de Negócio

* RN-AVA-20: Quando uma carona for finalizada, os participantes devem ser notificados sobre a possibilidade de avaliação.
* RN-AVA-21: A notificação deve permanecer disponível até que a avaliação seja realizada ou ignorada pelo usuário.

---

# Integridade das Avaliações

## Regras de Negócio

* RN-AVA-22: Avaliações não podem ser editadas após serem registradas.
* RN-AVA-23: Avaliações não podem ser excluídas pelos usuários.
* RN-AVA-24: Avaliações fazem parte do histórico permanente do sistema.

### Endpoints do Módulo

```http
POST /avaliacoes

GET /usuarios/me/avaliacoes

GET /usuarios/{id}/reputacao

GET /caronas/{id}/avaliacoes-pendentes
```

