# UniCar API

Backend da plataforma **UniCar**, um sistema de compartilhamento de caronas voltado para estudantes universitários. A aplicação fornece uma API REST responsável pela autenticação dos usuários, gerenciamento de perfis, veículos, caronas, reservas, avaliações, notificações, histórico de viagens e demais funcionalidades da plataforma.

Este projeto foi desenvolvido como parte da disciplina de **Engenharia de Software** da **Universidade Federal de Campina Grande (UFCG)**.

---

# Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Funcionalidades](#funcionalidades)
- [Requisitos](#requisitos)
- [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Executando o Projeto](#executando-o-projeto)
- [Executando com Docker](#executando-com-docker)
- [Documentação da API](#documentação-da-api)
- [Autenticação](#autenticação)
- [Banco de Dados](#banco-de-dados)
- [Testes](#testes)
- [Licença](#licença)
- [Universidade](#universidade)

---

## Tecnologias

- Java 21
- Spring Boot 3
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT (JSON Web Token)
- Gradle
- Swagger / OpenAPI
- Docker
- JUnit 5
- Mockito

---

## Arquitetura

A API segue uma arquitetura em camadas, separando responsabilidades entre controladores, serviços e acesso aos dados.

```text
Cliente (Web / Mobile)
          │
          ▼
     REST API
          │
          ▼
 Controllers
          │
          ▼
   Service Layer
          │
          ▼
Repositories (JPA)
          │
          ▼
     PostgreSQL
```

---

## Estrutura do Projeto

```text
src
├── main
│   ├── java
│   │   └── com.unicar
│   │       ├── config
│   │       ├── controller
│   │       ├── domain
│   │       ├── dto
│   │       ├── enums
│   │       ├── exception
│   │       ├── repository
│   │       ├── security
│   │       ├── service
│   │       └── util
│   └── resources
│       ├── db
│       │   └── migration
│       └── application.yml
│
└── test
    └── java
```

Cada pacote possui uma responsabilidade específica:

| Pacote | Responsabilidade |
|---------|------------------|
| `config` | Configurações da aplicação |
| `controller` | Endpoints REST |
| `domain` | Entidades JPA |
| `dto` | Objetos de transferência de dados |
| `repository` | Acesso ao banco de dados |
| `service` | Regras de negócio |
| `security` | Autenticação e autorização |
| `exception` | Tratamento global de exceções |
| `util` | Classes utilitárias |

---

## Funcionalidades

- Autenticação via JWT
- Gerenciamento de usuários
- Gerenciamento de veículos
- Cadastro e gerenciamento de caronas
- Reserva de vagas
- Trajetos recorrentes
- Busca de caronas
- Avaliações entre usuários
- Histórico de viagens
- Sistema de notificações
- Chat entre motorista e passageiros
- Bloqueio de usuários

---

## Requisitos

- Java 21
- PostgreSQL
- Git

---

## Variáveis de Ambiente

Configure as seguintes variáveis antes de iniciar a aplicação.

| Variável | Descrição |
|----------|-----------|
| `DB_URL` | URL de conexão com o PostgreSQL |
| `DB_USER` | Usuário do banco |
| `DB_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave utilizada para assinatura dos tokens JWT |
| `MAIL_USERNAME` | Conta utilizada para envio de e-mails |
| `MAIL_PASSWORD` | Senha ou App Password da conta de e-mail |

Exemplo:

```text
DB_URL=
DB_USER=
DB_PASSWORD=

JWT_SECRET=

MAIL_USERNAME=
MAIL_PASSWORD=
```

---

## Executando o Projeto

Clone o repositório:

```bash
git clone <url-do-repositorio>
cd unicar-api
```

Execute a aplicação:

```bash
./gradlew bootRun
```

A API estará disponível em:

```
http://localhost:8080
```

---

## Executando com Docker

Construindo a imagem:

```bash
docker build -t unicar-api .
```

Executando o container:

```bash
docker run \
    -p 8080:8080 \
    -e DB_URL=... \
    -e DB_USER=... \
    -e DB_PASSWORD=... \
    -e JWT_SECRET=... \
    -e MAIL_USERNAME=... \
    -e MAIL_PASSWORD=... \
    unicar-api
```

---

## Documentação da API

### Produção

Swagger UI

```
https://unicar-api.onrender.com/swagger-ui/index.html
```

OpenAPI

```
https://unicar-api.onrender.com/v3/api-docs
```

### Ambiente Local

Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

OpenAPI

```
http://localhost:8080/v3/api-docs
```

---

## Autenticação

A API utiliza autenticação baseada em **JWT**.

Após realizar o login, inclua o token no cabeçalho das requisições protegidas.

```http
Authorization: Bearer <token>
```

---

## Banco de Dados

As migrações do banco são gerenciadas pelo **Flyway**.

Todas as migrations presentes em `src/main/resources/db/migration` são executadas automaticamente durante a inicialização da aplicação.

---

## Testes

Para executar toda a suíte de testes:

```bash
./gradlew test
```

O projeto utiliza:

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc

---

## Licença

Este projeto está licenciado sob os termos da **MIT License**.

---

## Universidade

Projeto desenvolvido para a disciplina de **Engenharia de Software** da **Universidade Federal de Campina Grande (UFCG)**.
