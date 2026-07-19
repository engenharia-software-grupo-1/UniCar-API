# UniCar API

Backend da plataforma **UniCar**, um sistema de compartilhamento de caronas voltado para estudantes universitários. A aplicação fornece uma API REST responsável pela autenticação dos usuários, gerenciamento de perfis, veículos, caronas, reservas, avaliações, notificações, histórico de viagens e demais funcionalidades da plataforma.

Este projeto foi desenvolvido como parte da disciplina de **Engenharia de Software** da **Universidade Federal de Campina Grande (UFCG)**.

---

## Destaques

- API REST desenvolvida com Java 21 e Spring Boot 3
- Arquitetura em camadas (Controller → Service → Repository)
- Autenticação e autorização utilizando JWT
- Persistência com PostgreSQL e Spring Data JPA
- Versionamento do banco de dados com Flyway
- Documentação automática utilizando OpenAPI/Swagger
- Especificação formal em Alloy para validação das regras de negócio
- Testes automatizados com JUnit 5, Mockito e MockMvc
- Containerização com Docker

---

# Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Especificação Formal](#especificação-formal)
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
- [Contribuidores](#contribuidores)

---

# Tecnologias

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

# Arquitetura

A API segue uma arquitetura em camadas, promovendo separação de responsabilidades, facilidade de manutenção e alta coesão entre os componentes da aplicação.

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

# Estrutura do Projeto

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
│   │
│   └── resources
│       ├── db
│       │   └── migration
│       └── application.yml
│
└── test
    └── java
```

### Organização dos pacotes

| Pacote | Responsabilidade |
|---------|------------------|
| `config` | Configurações da aplicação |
| `controller` | Endpoints REST |
| `domain` | Entidades JPA |
| `dto` | Objetos de transferência de dados |
| `repository` | Persistência de dados |
| `service` | Regras de negócio |
| `security` | Autenticação e autorização |
| `exception` | Tratamento global de exceções |
| `util` | Classes utilitárias |

---

# Especificação Formal

Além da implementação da API, o projeto possui uma **especificação formal desenvolvida em Alloy**, utilizada para modelar e validar regras de negócio antes da implementação.

A especificação foi organizada de forma modular, permitindo a verificação independente dos principais domínios da aplicação.

```text
especificacao-formal/
├── main.als
└── modules
    ├── avaliacao.als
    ├── carona.als
    ├── comunicacao.als
    ├── interesse_trajeto.als
    ├── reserva.als
    ├── usuario.als
    └── veiculo.als
```

### Organização

| Arquivo | Descrição |
|----------|-----------|
| `main.als` | Arquivo principal responsável por importar todos os módulos e executar as verificações. |
| `usuario.als` | Modelo formal dos usuários e suas restrições. |
| `veiculo.als` | Especificação dos veículos cadastrados. |
| `carona.als` | Modelagem das caronas e suas regras de negócio. |
| `reserva.als` | Especificação das reservas realizadas pelos passageiros. |
| `avaliacao.als` | Modelo das avaliações entre usuários. |
| `comunicacao.als` | Regras relacionadas à comunicação entre motorista e passageiros. |
| `interesse_trajeto.als` | Modelagem dos interesses em trajetos. |

A especificação foi utilizada para validar propriedades do sistema, consistência das regras de negócio e possíveis cenários antes da implementação da API.

---

# Funcionalidades

- Autenticação utilizando JWT
- Gerenciamento de usuários
- Gerenciamento de veículos
- Cadastro e gerenciamento de caronas
- Busca de caronas
- Reserva de vagas
- Trajetos recorrentes
- Avaliações entre usuários
- Chat entre motorista e passageiros
- Histórico de viagens
- Sistema de notificações
- Bloqueio de usuários

---

# Requisitos

- Java 21
- PostgreSQL
- Git

---

# Variáveis de Ambiente

Configure as seguintes variáveis antes de iniciar a aplicação.

| Variável | Descrição |
|----------|-----------|
| `DB_URL` | URL de conexão com o banco PostgreSQL |
| `DB_USER` | Usuário do banco de dados |
| `DB_PASSWORD` | Senha do banco de dados |
| `JWT_SECRET` | Chave utilizada para geração e validação dos tokens JWT |
| `MAIL_USERNAME` | Conta utilizada para envio de e-mails |
| `MAIL_PASSWORD` | Senha (ou App Password) da conta de e-mail |

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

# Executando o Projeto

Clone o repositório.

```bash
git clone https://github.com/engenharia-software-grupo-1/UniCar-API.git
```

Acesse o diretório.

```bash
cd unicar-api
```

Execute a aplicação.

```bash
./gradlew bootRun
```

A API estará disponível em:

```
http://localhost:8080
```

---

# Executando com Docker

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

# Documentação da API

## Ambiente de Produção

### Swagger UI

```
https://unicar-api.onrender.com/swagger-ui/index.html
```

### OpenAPI

```
https://unicar-api.onrender.com/v3/api-docs
```

---

## Ambiente Local

### Swagger UI

```
http://localhost:8080/swagger-ui/index.html
```

### OpenAPI

```
http://localhost:8080/v3/api-docs
```

---

# Autenticação

A API utiliza autenticação baseada em **JSON Web Token (JWT)**.

Após realizar o login, todas as requisições autenticadas devem enviar o token no cabeçalho HTTP.

```http
Authorization: Bearer <token>
```

---

# Banco de Dados

O projeto utiliza **Flyway** para versionamento do banco de dados.

Todas as migrações localizadas em:

```text
src/main/resources/db/migration
```

são executadas automaticamente durante a inicialização da aplicação.

---

# Testes

Para executar todos os testes automatizados:

```bash
./gradlew test
```

O projeto utiliza:

- JUnit 5
- Mockito
- Spring Boot Test
- MockMvc

---

# Licença

Este projeto está licenciado sob os termos da **MIT License**. Consulte o arquivo `LICENSE` para mais informações.

---

# Universidade

Projeto desenvolvido para a disciplina de **Engenharia de Software**, ofertada pela **Universidade Federal de Campina Grande (UFCG)**.

# Contribuidores

<table>
  <tr>
    <td align="center">
      <a href="https://github.com/isadoralucena">
        <img src="https://github.com/isadoralucena.png" width="120px;" alt="Isadora Lucena"/>
        <br />
        <sub><b>Isadora Lucena</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/jennifermedeiross">
        <img src="https://github.com/jennifermedeiross.png" width="120px;" alt="Jennifer Medeiros"/>
        <br />
        <sub><b>Jennifer Medeiros</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/OscarRodrigues-83">
        <img src="https://github.com/OscarRodrigues-83.png" width="120px;" alt="Oscar Rodrigues"/>
        <br />
        <sub><b>Oscar Rodrigues</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/MarceloLuisDantas">
        <img src="https://github.com/MarceloLuisDantas.png" width="120px;" alt="Marcelo Luis Dantas"/>
        <br />
        <sub><b>Marcelo Luis Dantas</b></sub>
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/Eduarda-Cabral">
        <img src="https://github.com/Eduarda-Cabral.png" width="120px;" alt="Eduarda Cabral"/>
        <br />
        <sub><b>Eduarda Cabral</b></sub>
      </a>
    </td>
  </tr>
</table>
