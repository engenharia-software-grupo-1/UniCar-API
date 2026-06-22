# US3 - Autenticação e Perfil do Usuário

## Visão Geral

Este documento define os contratos da API relacionados à autenticação e gerenciamento do perfil do usuário.

### Regras Gerais

* A autenticação é realizada através do Eureca.
* Após autenticação bem-sucedida, o UniCar cria ou sincroniza o usuário local.
* Nome, matrícula, CPF e e-mail são dados institucionais e não podem ser alterados.
* Apenas os campos `genero` e `receberEmail` podem ser atualizados pelo usuário.
* Todas as rotas protegidas exigem token JWT.
* O logout é realizado apenas no frontend através da remoção do token.

---

# POST /auth/login

## Objetivo

Autenticar o usuário através do Eureca.

## Request

```json
{
  "usuario": "jennifer.medeiros",
  "senha": "123456"
}
```

## Response 200

```json
{
  "token": "jwt-token",
  "usuario": {
    "id": 1,
    "matricula": "123456789",
    "nome": "Jennifer Medeiros",
    "email": "jennifer@ccc.ufcg.edu.br",
    "cpf": "12345678900",
    "curso": "Ciência da Computação",
    "genero": "FEMININO",
    "receberEmail": true,
    "dataCriacao": "2026-06-22T10:00:00",
    "dataAtualizacao": "2026-06-22T10:00:00"
  }
}
```

## Response 401

```json
{
  "message": "Credenciais inválidas"
}
```

## Regras de Negócio

* RN-AUTH-01: Apenas usuários autenticados pelo Eureca podem acessar o sistema.
* RN-AUTH-02: Credenciais inválidas impedem o acesso.
* RN-AUTH-03: Após autenticação bem-sucedida, o sistema deve sincronizar os dados institucionais do usuário.

---

# GET /usuarios/me

## Objetivo

Consultar os dados do usuário autenticado.

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
  "matricula": "123456789",
  "nome": "Jennifer Medeiros",
  "email": "jennifer@ccc.ufcg.edu.br",
  "cpf": "12345678900",
  "curso": "Ciência da Computação",
  "genero": "FEMININO",
  "receberEmail": true,
  "dataCriacao": "2026-06-22T10:00:00",
  "dataAtualizacao": "2026-06-22T10:00:00"
}
```

## Response 401

```json
{
  "message": "Usuário não autenticado"
}
```

## Regras de Negócio

* RN-AUTH-04: Apenas usuários autenticados podem consultar o próprio perfil.

---

# PATCH /usuarios/me

## Objetivo

Atualizar informações editáveis do perfil.

## Headers

```http
Authorization: Bearer <token>
```

## Request

```json
{
  "genero": "FEMININO",
  "receberEmail": false
}
```

## Response 200

```json
{
  "id": 1,
  "matricula": "123456789",
  "nome": "Jennifer Medeiros",
  "email": "jennifer@ccc.ufcg.edu.br",
  "cpf": "12345678900",
  "curso": "Ciência da Computação",
  "genero": "FEMININO",
  "receberEmail": false,
  "dataCriacao": "2026-06-22T10:00:00",
  "dataAtualizacao": "2026-06-22T12:30:00"
}
```

## Response 400

```json
{
  "message": "Dados inválidos"
}
```

## Response 401

```json
{
  "message": "Usuário não autenticado"
}
```

## Regras de Negócio

* RN-AUTH-05: Apenas o próprio usuário pode alterar seus dados.
* RN-AUTH-06: Apenas os campos `genero` e `receberEmail` podem ser alterados.
* RN-AUTH-07: Nome, CPF, matrícula e e-mail não podem ser alterados.

---

# DELETE /usuarios/me

## Objetivo

Desativar o cadastro local do usuário no UniCar.

## Headers

```http
Authorization: Bearer <token>
```

## Request

Sem corpo.

## Response 204

Sem conteúdo.

## Response 401

```json
{
  "message": "Usuário não autenticado"
}
```

## Regras de Negócio

* RN-AUTH-08: A exclusão afeta apenas os dados do UniCar.
* RN-AUTH-09: A conta institucional permanece ativa no Eureca.
* RN-AUTH-10: O histórico de caronas, reservas, mensagens, avaliações, notificações e denúncias deve ser preservado.
* RN-AUTH-11: O usuário só pode excluir a própria conta.
* RN-AUTH-12: A exclusão da conta deve desativar o usuário localmente, sem remover registros históricos.
* RN-AUTH-13: Usuários desativados não podem acessar funcionalidades do sistema.
* RN-AUTH-14: Um novo login via Eureca deve reativar automaticamente o cadastro local.

## Critérios de Aceite

* Usuário desativado com sucesso.
* Histórico do usuário preservado.
* Usuário não aparece em buscas após a desativação.
* Usuário não consegue utilizar funcionalidades do sistema após a desativação.
* Um novo login reativa automaticamente o cadastro.
