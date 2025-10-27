# 🏗️ Arquitetura de Microsserviços com Spring Boot e RabbitMQ

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5+-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3.13-orange.svg)](https://www.rabbitmq.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue.svg)](https://www.postgresql.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Visão Geral

Sistema distribuído implementando arquitetura de microsserviços utilizando **Spring Boot 3** e **RabbitMQ** para comunicação entre serviços. O projeto demonstra padrões de mensageria síncronos (request-reply) e assíncronos (fire-and-forget), circuit breaker, e práticas recomendadas de arquitetura de software.

## 🏛️ Diagrama de Arquitetura

```mermaid
graph TB
    subgraph "Cliente"
        CLIENT[Cliente HTTP]
    end

    subgraph "API Gateway / Load Balancer"
        GATEWAY[API Gateway<br/>Port: 8090]
    end

    subgraph "Microsserviços"
        PESSOA[Microsserviço Pessoa<br/>Spring Boot 3.5.3<br/>Port: 8090<br/>PostgreSQL: 5432]
        SERASA[Microsserviço Serasa<br/>Spring Boot 3.5.3<br/>Port: 8070]
        LOG[Microsserviço Log<br/>Spring Boot 3.5.0<br/>Port: 8060<br/>PostgreSQL: 5432]
    end

    subgraph "Message Broker"
        RABBITMQ[RabbitMQ 3.13<br/>Port: 5672<br/>Management: 15672]
        
        subgraph "Filas Síncronas"
            QUEUE_REQ[verificar-serasa-request]
            QUEUE_RES[verificar-serasa-response]
        end
        
        subgraph "Fila Assíncrona"
            QUEUE_LOG[enviar-log]
        end
    end

    subgraph "Bancos de Dados"
        DB_PESSOA[(PostgreSQL<br/>pessoa_db<br/>Port: 5432)]
        DB_LOG[(PostgreSQL<br/>logs_db<br/>Port: 5432)]
    end

    subgraph "Resiliência"
        CB[Circuit Breaker<br/>Resilience4j]
    end

    CLIENT --> GATEWAY
    GATEWAY --> PESSOA
    
    PESSOA -->|1. Consulta Síncrona| QUEUE_REQ
    QUEUE_REQ --> SERASA
    SERASA -->|2. Resposta| QUEUE_RES
    QUEUE_RES --> PESSOA
    
    PESSOA -->|3. Envio Assíncrono| QUEUE_LOG
    QUEUE_LOG --> LOG
    
    PESSOA --> DB_PESSOA
    LOG --> DB_LOG
    
    PESSOA -.->|Fallback| CB
    CB -.->|Timeout 3s| SERASA

    style CLIENT fill:#e1f5ff
    style GATEWAY fill:#fff4e6
    style PESSOA fill:#e8f5e9
    style SERASA fill:#fff3e0
    style LOG fill:#f3e5f5
    style RABBITMQ fill:#ffebee
    style DB_PESSOA fill:#e0f2f1
    style DB_LOG fill:#e0f2f1
    style CB fill:#fce4ec
```

## 🔄 Diagrama de Componentes Detalhado

```mermaid
C4Component
    title Diagrama de Componentes - Sistema de Microsserviços

    Container_Boundary(pessoa, "Microsserviço Pessoa") {
        Component(pessoa_controller, "PessoaController", "Spring MVC", "Endpoints REST para CRUD")
        Component(pessoa_service, "PessoaService", "Spring Service", "Lógica de negócio")
        Component(serasa_service, "SerasaService", "Spring Service", "Integração com Serasa")
        Component(log_service, "LogService", "Spring Service", "Envio de logs")
        Component(rabbitmq_sync, "RabbitMQSincronoService", "Spring Service", "Comunicação síncrona")
        Component(rabbitmq_async, "RabbitMQAssincronoService", "Spring Service", "Comunicação assíncrona")
        ComponentDb(pessoa_repo, "PessoaRepository", "Spring Data JPA", "Acesso ao banco")
    }

    Container_Boundary(serasa, "Microsserviço Serasa") {
        Component(serasa_consumer, "SerasaConsumerService", "RabbitMQ Listener", "Consumidor de mensagens")
        Component(serasa_logic, "SerasaService", "Spring Service", "Verificação de negativação")
    }

    Container_Boundary(log, "Microsserviço Log") {
        Component(log_consumer, "LogConsumerService", "RabbitMQ Listener", "Consumidor de logs")
        Component(log_logic, "LogService", "Spring Service", "Persistência de logs")
        ComponentDb(log_repo, "LogRepository", "Spring Data JPA", "Acesso ao banco")
    }

    Container_Boundary(broker, "RabbitMQ Message Broker") {
        Component(exchange, "Direct Exchange", "AMQP", "Roteamento de mensagens")
        Component(queue_req, "Queue Request", "AMQP", "Fila de requisição")
        Component(queue_res, "Queue Response", "AMQP", "Fila de resposta")
        Component(queue_log, "Queue Log", "AMQP", "Fila de logs")
    }

    ContainerDb(db_pessoa, "PostgreSQL", "pessoa_db", "Armazena dados de pessoas")
    ContainerDb(db_log, "PostgreSQL", "logs_db", "Armazena auditoria")

    Rel(pessoa_controller, pessoa_service, "Usa")
    Rel(pessoa_service, serasa_service, "Consulta situação")
    Rel(pessoa_service, log_service, "Envia eventos")
    Rel(pessoa_service, pessoa_repo, "Persiste dados")
    
    Rel(serasa_service, rabbitmq_sync, "Envia e aguarda")
    Rel(log_service, rabbitmq_async, "Envia e esquece")
    
    Rel(rabbitmq_sync, queue_req, "Publica em", "AMQP")
    Rel(queue_req, serasa_consumer, "Consome de", "AMQP")
    Rel(serasa_consumer, serasa_logic, "Processa")
    Rel(serasa_consumer, queue_res, "Responde em", "AMQP")
    Rel(queue_res, rabbitmq_sync, "Recebe de", "AMQP")
    
    Rel(rabbitmq_async, queue_log, "Publica em", "AMQP")
    Rel(queue_log, log_consumer, "Consome de", "AMQP")
    Rel(log_consumer, log_logic, "Processa")
    Rel(log_logic, log_repo, "Persiste")
    
    Rel(pessoa_repo, db_pessoa, "Lê/Escreve", "JDBC")
    Rel(log_repo, db_log, "Escreve", "JDBC")
```

## 🎯 Características Principais

### ✅ Padrões Arquiteturais Implementados

- **Microsserviços Independentes**: Cada serviço possui seu próprio ciclo de vida e banco de dados
- **Event-Driven Architecture**: Comunicação baseada em eventos via mensageria
- **Request-Reply Pattern**: Comunicação síncrona com garantia de resposta
- **Fire-and-Forget Pattern**: Comunicação assíncrona para logs de auditoria
- **Circuit Breaker**: Resilience4j para tolerância a falhas
- **Database per Service**: Cada microsserviço tem seu próprio schema PostgreSQL
- **API-First Design**: Documentação OpenAPI/Swagger
- **Domain-Driven Design**: Organização por domínios de negócio

### 🔧 Tecnologias e Frameworks

| Tecnologia | Versão | Propósito |
|------------|--------|-----------|
| Java | 21 | Linguagem de programação |
| Spring Boot | 3.5+ | Framework principal |
| Spring Data JPA | 3.5+ | Persistência de dados |
| Spring AMQP | 3.5+ | Integração com RabbitMQ |
| RabbitMQ | 3.13 | Message broker |
| PostgreSQL | 15+ | Banco de dados relacional |
| Flyway | Latest | Versionamento de banco |
| Resilience4j | 2.2.0 | Circuit breaker e resiliência |
| Lombok | 1.18.32 | Redução de boilerplate |
| SpringDoc OpenAPI | 2.7.0 | Documentação API |
| Jackson | Latest | Serialização JSON |
| Maven | 3.9+ | Gerenciamento de dependências |

## 📦 Estrutura do Projeto

```
microsservicos-spring-rabbitmq/
├── microsservico-pessoa/          # Serviço de gerenciamento de pessoas
│   ├── src/main/java/
│   │   └── com/example/pessoa/
│   │       ├── config/            # Configurações (RabbitMQ, Swagger, CORS)
│   │       ├── controller/        # Controllers REST
│   │       ├── dto/               # Data Transfer Objects
│   │       ├── model/             # Entidades JPA
│   │       ├── repository/        # Repositórios Spring Data
│   │       ├── service/           # Serviços de negócio
│   │       └── constants/         # Constantes e mensagens
│   └── src/main/resources/
│       ├── application.yml        # Configurações da aplicação
│       └── db/migration/          # Scripts Flyway
│
├── microsservico-serasa/          # Serviço de consulta Serasa
│   ├── src/main/java/
│   │   └── com/example/serasa/
│   │       ├── config/            # Configurações RabbitMQ
│   │       ├── service/           # Lógica de consulta
│   │       └── constants/         # Constantes de tópicos
│   └── src/main/resources/
│       └── application.yml
│
├── microsservico-log/             # Serviço de auditoria
│   ├── src/main/java/
│   │   └── com/example/log/
│   │       ├── config/            # Configurações
│   │       ├── dto/               # DTOs de eventos
│   │       ├── model/             # Entidade Log
│   │       ├── repository/        # Repositório
│   │       └── service/           # Consumidor e persistência
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/
│
├── rabbitmq/                      # Configuração RabbitMQ
│   └── docker-compose.yml
│
├── redis/                         # Configuração Redis (futuro)
│   └── docker-compose.yml
│
└── README.md                      # Este arquivo
```

## 🚀 Guia de Instalação e Execução

### 📋 Pré-requisitos

- **Java 21** (JDK) - [Download](https://www.oracle.com/java/technologies/downloads/#java21)
- **Maven 3.9+** - [Download](https://maven.apache.org/download.cgi)
- **Docker & Docker Compose** - [Download](https://www.docker.com/products/docker-desktop/)
- **PostgreSQL 15+** - [Download](https://www.postgresql.org/download/)
- **Git** - [Download](https://git-scm.com/downloads)

### 🔧 Configuração da Infraestrutura

#### 1. RabbitMQ (Message Broker)

```bash
cd rabbitmq
docker-compose up -d
```

**Acesso ao Management Console:**
- URL: http://localhost:15672
- Usuário: `admin`
- Senha: `admin`

#### 2. PostgreSQL (Bancos de Dados)

**Criar databases e schemas:**

```sql
-- Database do Microsserviço Pessoa
CREATE DATABASE pessoa_db;
\c pessoa_db
CREATE SCHEMA pessoa_db;

-- Database do Microsserviço Log
CREATE DATABASE logs_db;
\c logs_db
CREATE SCHEMA logs_db;
```

**Configurar credenciais:**
- Host: `localhost`
- Port: `5432`
- Username: `postgres`
- Password: `postgresql`

### 🏃 Executando os Microsserviços

#### Opção 1: Executar via Maven (Desenvolvimento)

**Terminal 1 - Microsserviço Log:**
```bash
cd microsservico-log
./mvnw clean spring-boot:run
```

**Terminal 2 - Microsserviço Serasa:**
```bash
cd microsservico-serasa
./mvnw clean spring-boot:run
```

**Terminal 3 - Microsserviço Pessoa:**
```bash
cd microsservico-pessoa
./mvnw clean spring-boot:run
```

#### Opção 2: Build e Executar JARs (Produção)

```bash
# Build de todos os serviços
cd microsservico-log && ./mvnw clean package -DskipTests && cd ..
cd microsservico-serasa && ./mvnw clean package -DskipTests && cd ..
cd microsservico-pessoa && ./mvnw clean package -DskipTests && cd ..

# Executar
java -jar microsservico-log/target/microsservico-log-0.0.1-SNAPSHOT.jar &
java -jar microsservico-serasa/target/microsservico-serasa-0.0.1-SNAPSHOT.jar &
java -jar microsservico-pessoa/target/microsservico-pessoa-0.0.1-SNAPSHOT.jar &
```

### ✅ Verificação da Instalação

| Serviço | URL | Status |
|---------|-----|--------|
| Microsserviço Pessoa | http://localhost:8090/api | ✅ Running |
| Microsserviço Serasa | http://localhost:8070 | ✅ Running |
| Microsserviço Log | http://localhost:8060 | ✅ Running |
| Swagger UI (Pessoa) | http://localhost:8090/swagger-ui.html | 📄 Docs |
| RabbitMQ Management | http://localhost:15672 | 🐰 Admin |

## 📚 Detalhamento dos Microsserviços

### 🟢 Microsserviço Pessoa

**Responsabilidade:** Gerenciamento completo do cadastro de pessoas

**Tecnologias Específicas:**
- Spring Boot 3.5.3
- Spring Data JPA
- Spring Validation
- Resilience4j Circuit Breaker
- SpringDoc OpenAPI

**Endpoints REST:**

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/pessoa` | Cadastrar nova pessoa |
| PUT | `/api/pessoa/{id}` | Atualizar dados de pessoa |
| DELETE | `/api/pessoa/{id}` | Remover pessoa |

**Modelo de Dados:**
```json
{
  "nome": "João Silva",
  "cpf": "12345678901",
  "dataNascimento": "1990-01-15",
  "negativado": false,
  "dataHoraCriacao": "2025-10-27T10:30:00"
}
```

**Validações:**
- Nome: obrigatório, 2-150 caracteres
- CPF: obrigatório, formato válido, único
- Data Nascimento: obrigatória, no passado

**Integrações:**
- **Síncrona:** Consulta Serasa durante cadastro (timeout 3s)
- **Assíncrona:** Envia logs de auditoria para todas operações

**Circuit Breaker:**
```yaml
minimum-number-of-calls: 1
failure-rate-threshold: 100
wait-duration-in-open-state: 3s
```

### 🟡 Microsserviço Serasa

**Responsabilidade:** Simulação de consulta de negativação financeira

**Tecnologias Específicas:**
- Spring Boot 3.5.3
- Spring AMQP

**Comunicação:**
- Padrão: Request-Reply (síncrono)
- Tópico Request: `verificar-serasa-request`
- Tópico Response: `verificar-serasa-response`

**Base de Dados Mockada:**
```java
CPFs Negativados:
- 18142226006
- 16470435068
```

**Lógica de Negócio:**
- CPF na lista → retorna `true` (negativado)
- CPF fora da lista → retorna `false` (regular)

**Características:**
- Resposta automática via `@SendTo`
- Acknowledgment manual (MANUAL_IMMEDIATE)
- Retry: 3 tentativas

### 🔵 Microsserviço Log

**Responsabilidade:** Auditoria e rastreabilidade de operações

**Tecnologias Específicas:**
- Spring Boot 3.5.0
- Spring Data JPA
- Flyway

**Comunicação:**
- Padrão: Fire-and-Forget (assíncrono)
- Tópico: `enviar-log`

**Modelo de Log:**
```sql
CREATE TABLE log (
    id BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    nome_usuario VARCHAR(100) NOT NULL,
    operacao VARCHAR(30) NOT NULL,
    dados TEXT NOT NULL,
    nome_microsservico VARCHAR(60) NOT NULL,
    data_hora_criacao TIMESTAMP NOT NULL
);
```

**Operações Auditadas:**
- CADASTRO
- ATUALIZAÇÃO
- EXCLUSÃO

**Formato de Evento:**
```json
{
  "pessoaDto": { ... },
  "operacao": "CADASTRO",
  "microservico": "microservico-pessoa",
  "idUsuario": 1,
  "nomeUsuario": "Jhon Doe"
}
```

## 🔄 Fluxos de Comunicação

### Fluxo 1: Cadastro de Pessoa (Comunicação Síncrona + Assíncrona)

```mermaid
sequenceDiagram
    autonumber
    participant C as Cliente
    participant P as Pessoa Service
    participant CB as Circuit Breaker
    participant RQ as RabbitMQ
    participant S as Serasa Service
    participant L as Log Service
    participant DB as PostgreSQL

    C->>P: POST /api/pessoa
    activate P
    
    P->>P: Validar dados
    
    P->>CB: Consultar Serasa
    activate CB
    CB->>RQ: Publish (verificar-serasa-request)
    RQ->>S: Consume request
    activate S
    S->>S: Verificar CPF
    S->>RQ: Publish (verificar-serasa-response)
    deactivate S
    RQ->>CB: Consume response
    CB->>P: Boolean negativado
    deactivate CB
    
    P->>DB: INSERT pessoa
    DB-->>P: Pessoa salva
    
    P->>RQ: Publish (enviar-log) ASYNC
    RQ->>L: Consume log event
    activate L
    L->>DB: INSERT log
    deactivate L
    
    P-->>C: 201 Created
    deactivate P
```

### Fluxo 2: Circuit Breaker em Ação (Fallback)

```mermaid
sequenceDiagram
    participant P as Pessoa Service
    participant CB as Circuit Breaker
    participant RQ as RabbitMQ
    participant S as Serasa Service

    Note over CB: Estado: CLOSED

    P->>CB: Consultar Serasa
    CB->>RQ: Request
    Note over S: Serasa indisponível
    RQ--xCB: Timeout (3s)
    
    Note over CB: Estado: OPEN
    
    CB->>P: Fallback: null
    
    Note over CB: Aguarda 3s
    Note over CB: Estado: HALF_OPEN
    
    P->>CB: Nova consulta
    CB->>RQ: Request teste
    RQ->>S: Consume
    S->>RQ: Response
    RQ->>CB: Success
    
    Note over CB: Estado: CLOSED
    CB->>P: Boolean negativado
```

## 🛠️ Configurações Principais

### RabbitMQ Configuration

**application.yml (Pessoa):**
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: admin
    password: admin

resilience4j:
  circuitbreaker:
    instances:
      microsservico-serasa:
        minimum-number-of-calls: 1
        failure-rate-threshold: 100
        wait-duration-in-open-state: 3s
```

### Flyway Migrations

**Pessoa - V1__create_table_pessoa.sql:**
```sql
CREATE TABLE pessoa (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    data_nascimento DATE NOT NULL,
    negativado BOOLEAN,
    data_hora_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Log - V1__create_table_log.sql:**
```sql
CREATE TABLE log (
    id BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL,
    nome_usuario VARCHAR(100) NOT NULL,
    operacao VARCHAR(30) NOT NULL,
    dados TEXT NOT NULL,
    nome_microsservico VARCHAR(60) NOT NULL,
    data_hora_criacao TIMESTAMP NOT NULL
);
```

## 🧪 Testando a Aplicação

### Cenário 1: Cadastro com CPF Regular

**Request:**
```bash
curl -X POST http://localhost:8090/api/pessoa \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Maria Santos",
    "cpf": "12345678901",
    "dataNascimento": "1995-05-20"
  }'
```

**Response:**
```json
{
  "id": 1,
  "nome": "Maria Santos",
  "cpf": "12345678901",
  "dataNascimento": "1995-05-20",
  "negativado": false,
  "dataHoraCriacao": "2025-10-27T10:30:00"
}
```

### Cenário 2: Cadastro com CPF Negativado

**Request:**
```bash
curl -X POST http://localhost:8090/api/pessoa \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "João Silva",
    "cpf": "18142226006",
    "dataNascimento": "1988-03-15"
  }'
```

**Response:**
```json
{
  "id": 2,
  "nome": "João Silva",
  "cpf": "18142226006",
  "dataNascimento": "1988-03-15",
  "negativado": true,
  "dataHoraCriacao": "2025-10-27T10:32:00"
}
```

### Cenário 3: Atualização de Pessoa

**Request:**
```bash
curl -X PUT http://localhost:8090/api/pessoa/1 \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Maria Santos Silva",
    "cpf": "12345678901",
    "dataNascimento": "1995-05-20"
  }'
```

### Cenário 4: Exclusão de Pessoa

**Request:**
```bash
curl -X DELETE http://localhost:8090/api/pessoa/1
```

**Response:**
```json
{
  "status": 200,
  "message": "Operação realizada com sucesso."
}
```

### Verificando Logs no RabbitMQ

1. Acesse: http://localhost:15672
2. Login: `admin` / `admin`
3. Vá em **Queues** → `enviar-log`
4. Clique em **Get messages**
5. Visualize os eventos de auditoria

## 📊 Monitoramento e Observabilidade

### RabbitMQ Management Console

**Métricas disponíveis:**
- Taxa de publicação de mensagens
- Taxa de consumo de mensagens
- Tamanho das filas
- Conexões ativas
- Canais abertos

### Logs da Aplicação

**Níveis de log configurados:**
```yaml
logging:
  level:
    org.springframework.amqp: INFO
    org.hibernate.SQL: DEBUG
    org.apache.kafka: INFO
```

### Health Checks

**Verificar saúde dos serviços:**
```bash
# Pessoa
curl http://localhost:8090/actuator/health

# Serasa
curl http://localhost:8070/actuator/health

# Log
curl http://localhost:8060/actuator/health
```

## 🔒 Segurança

### Recomendações para Produção

1. **Autenticação e Autorização:**
   - Implementar Spring Security
   - Utilizar JWT para autenticação
   - Configurar OAuth2/OIDC

2. **Comunicação Segura:**
   - Habilitar TLS/SSL no RabbitMQ
   - Usar certificados para comunicação entre serviços
   - Implementar mTLS (mutual TLS)

3. **Gestão de Segredos:**
   - Utilizar Spring Cloud Config
   - Integrar com HashiCorp Vault
   - Nunca commitar credenciais

4. **Rate Limiting:**
   - Implementar Bucket4j
   - Configurar limites por IP/usuário

## 🚀 Melhorias Futuras

### Roadmap Técnico

- [ ] **Service Discovery:** Implementar Eureka ou Consul
- [ ] **API Gateway:** Adicionar Spring Cloud Gateway
- [ ] **Distributed Tracing:** Integrar Zipkin/Jaeger
- [ ] **Caching:** Implementar Redis para cache distribuído
- [ ] **Containerização:** Criar Dockerfiles e docker-compose completo
- [ ] **Kubernetes:** Preparar manifestos K8s (Deployments, Services, Ingress)
- [ ] **CI/CD:** Pipeline com GitHub Actions ou GitLab CI
- [ ] **Testes:** Aumentar cobertura (JUnit, Testcontainers, WireMock)
- [ ] **Métricas:** Prometheus + Grafana
- [ ] **ELK Stack:** Centralizar logs (Elasticsearch, Logstash, Kibana)
- [ ] **Config Server:** Externalizar configurações
- [ ] **Dead Letter Queue:** Tratamento de mensagens com falha
- [ ] **Saga Pattern:** Transações distribuídas
- [ ] **CQRS:** Separação de comandos e consultas

### Novas Funcionalidades

- [ ] Consulta de pessoas (GET endpoints)
- [ ] Paginação e ordenação
- [ ] Filtros avançados
- [ ] Export para CSV/Excel
- [ ] Notificações por email
- [ ] Dashboard de auditoria
- [ ] Relatórios gerenciais

## 🤝 Contribuindo

Contribuições são bem-vindas! Siga as etapas:

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

### Padrões de Código

- **Java:** Google Java Style Guide
- **Commits:** Conventional Commits
- **Branches:** GitFlow

## 📝 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 👨‍💻 Autor

**Senior Java Architect**
- 20+ anos de experiência em Java
- Especialista em Arquitetura de Microsserviços
- Spring Framework Expert

## 📞 Suporte

Para dúvidas ou suporte:
- Abra uma [Issue](https://github.com/seu-usuario/microsservicos-spring-rabbitmq/issues)
- Entre em contato via email

## 🙏 Agradecimentos

- Spring Team pela excelente documentação
- RabbitMQ Community
- Comunidade Java Brasil

---

**⭐ Se este projeto foi útil, considere dar uma estrela no GitHub!**

**📚 Documentação Adicional:**
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [Microservices Patterns](https://microservices.io/patterns/index.html)