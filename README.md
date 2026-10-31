# 🎂 API da Confeitaria

API REST desenvolvida em **Spring Boot** para gerenciar uma confeitaria: cadastro de **clientes**, **bolos** (cardápio/estoque) e **pedidos**, com regras de negócio, tratamento de erros, documentação Swagger e uma tela web simples conectada ao backend.

> Trabalho da disciplina de Programação Backend — UNIFSA.

## Sumário
1. [Tecnologias](#tecnologias)
2. [Modelo de dados](#modelo-de-dados)
3. [Regras de negócio](#regras-de-negócio)
4. [Endpoints](#endpoints)
5. [Tratamento de erros e status HTTP](#tratamento-de-erros-e-status-http)
6. [Como executar](#como-executar)
7. [Documentação Swagger](#documentação-swagger)
8. [Tela web (ponto extra)](#tela-web-ponto-extra)
9. [Exemplos de requisições](#exemplos-de-requisições)
10. [Estrutura do projeto](#estrutura-do-projeto)

## Tecnologias
- Java 17
- Spring Boot 3.3.5 (Web, Data JPA, Validation)
- PostgreSQL
- Springdoc OpenAPI (Swagger UI)
- DTOs (records) com Bean Validation e `ResponseEntity` em todos os endpoints
- Maven
- HTML + JavaScript puro (tela web)

## Modelo de dados

```
Cliente (1) ───────< Pedido >─────── (1) Bolo
```

| Tabela | Campos principais |
|---|---|
| `clientes` | id, nome, email (único), telefone |
| `bolos` | id, nome (único), descricao, sabor, preco, estoque, ativo |
| `pedidos` | id, cliente_id (FK), bolo_id (FK), quantidade, preco_unitario, desconto_percentual, valor_total, data_entrega, data_criacao, status |

As tabelas são criadas automaticamente pelo Hibernate (`ddl-auto=update`).

## Regras de negócio

**Clientes**
- E-mail único (409 se duplicado).
- Não é possível excluir um cliente que possui pedidos (409).

**Bolos**
- Nome único; preço maior que zero; estoque não pode ser negativo.
- Não é possível excluir um bolo que já foi pedido (409). Para tirá-lo do cardápio, marque `ativo = false`.

**Pedidos**
1. Só é possível pedir bolos **ativos**.
2. A entrega deve ser agendada com **no mínimo 2 dias de antecedência**.
3. Cada cliente pode ter no máximo **3 pedidos PENDENTES** ao mesmo tempo.
4. O pedido **baixa o estoque** do bolo; se não houver estoque suficiente, é recusado.
5. **Desconto por quantidade:** 5 a 9 unidades = 5%; 10 ou mais = 10%.
6. O preço unitário é **gravado no pedido** (mudanças futuras de preço não alteram pedidos antigos).
7. Só é possível **editar** (quantidade e data) pedidos PENDENTES; o estoque é reajustado automaticamente.
8. Fluxo de status: `PENDENTE → CONFIRMADO → EM_PRODUCAO → ENTREGUE`.
9. Só é possível **cancelar** pedidos PENDENTES ou CONFIRMADOS; o estoque é devolvido ao bolo.
10. Só é possível **excluir** pedidos CANCELADOS.

## Endpoints

### Clientes — `/clientes`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/clientes` | Cadastrar |
| GET | `/clientes?nome=&email=` | Listar (com filtros) |
| GET | `/clientes/{id}` | Buscar por id |
| PUT | `/clientes/{id}` | Atualizar |
| DELETE | `/clientes/{id}` | Excluir |

### Bolos — `/bolos`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/bolos` | Cadastrar |
| GET | `/bolos?nome=&sabor=&precoMin=&precoMax=&ativo=` | Listar (com filtros) |
| GET | `/bolos/{id}` | Buscar por id |
| PUT | `/bolos/{id}` | Atualizar |
| DELETE | `/bolos/{id}` | Excluir |

### Pedidos — `/pedidos`
| Método | Rota | Descrição |
|---|---|---|
| POST | `/pedidos` | Criar pedido |
| GET | `/pedidos?status=&clienteId=&boloId=&entregaDe=&entregaAte=` | Listar (com filtros) |
| GET | `/pedidos/{id}` | Buscar por id |
| PUT | `/pedidos/{id}` | Atualizar quantidade e data (só PENDENTE) |
| PATCH | `/pedidos/{id}/avancar` | Avançar para a próxima etapa |
| PATCH | `/pedidos/{id}/cancelar` | Cancelar e devolver estoque |
| DELETE | `/pedidos/{id}` | Excluir (só CANCELADO) |

Datas no formato `AAAA-MM-DD`.

## Tratamento de erros e status HTTP

| Status | Quando acontece |
|---|---|
| 200 OK | Consulta ou atualização bem-sucedida |
| 201 Created | Recurso criado (com header `Location`) |
| 204 No Content | Exclusão bem-sucedida |
| 400 Bad Request | Dados inválidos (Bean Validation), JSON malformado, parâmetro com tipo errado |
| 404 Not Found | Cliente, bolo ou pedido não encontrado |
| 409 Conflict | E-mail/nome duplicado ou exclusão bloqueada por vínculo |
| 422 Unprocessable Entity | Regra de negócio violada (sem estoque, prazo curto, status inválido...) |
| 500 Internal Server Error | Erro inesperado |

Todos os erros seguem o mesmo formato JSON:

```json
{
  "timestamp": "2026-10-26T14:30:00",
  "status": 422,
  "erro": "Unprocessable Entity",
  "mensagem": "Estoque insuficiente para 'Bolo de Chocolate': disponível 2, solicitado 5.",
  "caminho": "/pedidos"
}
```

Erros de validação (400) incluem o objeto `campos` com a mensagem de cada campo inválido.

## Como executar

### Pré-requisitos
- JDK 17 ou superior
- PostgreSQL + pgAdmin
- VS Code com o *Extension Pack for Java* (ou outra IDE)

### Passo a passo
1. **Clone** o repositório:
   ```bash
   git clone <URL-DO-SEU-REPOSITORIO>
   ```
2. **Crie o banco** no pgAdmin (ou no `psql`):
   ```sql
   CREATE DATABASE confeitaria;
   ```
3. **Configure a senha** em `src/main/resources/application.properties`:
   ```properties
   spring.datasource.username=postgres
   spring.datasource.password=SUA_SENHA_AQUI
   ```
4. **Execute** a aplicação:
   - No VS Code: abra `ConfeitariaApplication.java` e clique em **Run** acima do método `main`.
   - Com Maven instalado: `mvn spring-boot:run`
5. Aguarde a mensagem `Started ConfeitariaApplication` no terminal. A API estará em `http://localhost:8080`.

## Documentação Swagger
Com a aplicação rodando, acesse: **http://localhost:8080/swagger-ui.html**

Todos os endpoints estão documentados e podem ser testados direto pelo navegador. O JSON da especificação OpenAPI fica em `http://localhost:8080/v3/api-docs`.

## Tela web (ponto extra)
Com a aplicação rodando, acesse **http://localhost:8080/** para abrir o painel, que permite cadastrar clientes e bolos, fazer pedidos, filtrar, avançar status, cancelar e excluir — tudo consumindo a API.

## Exemplos de requisições

**Cadastrar cliente** — `POST /clientes`
```json
{ "nome": "Maria Silva", "email": "maria@email.com", "telefone": "86999998888" }
```

**Cadastrar bolo** — `POST /bolos`
```json
{ "nome": "Bolo de Chocolate", "sabor": "Chocolate", "descricao": "Recheio de brigadeiro", "preco": 45.90, "estoque": 20 }
```

**Criar pedido** — `POST /pedidos` (use uma data com pelo menos 2 dias à frente)
```json
{ "clienteId": 1, "boloId": 1, "quantidade": 6, "dataEntrega": "2026-11-10" }
```
Resposta (6 unidades → 5% de desconto):
```json
{ "id": 1, "clienteNome": "Maria Silva", "boloNome": "Bolo de Chocolate", "quantidade": 6,
  "precoUnitario": 45.90, "descontoPercentual": 5.00, "valorTotal": 261.63, "status": "PENDENTE" }
```

## Estrutura do projeto
```
src/main/java/com/unifsa/confeitaria
├── ConfeitariaApplication.java
├── config/        -> configuração do Swagger
├── controller/    -> endpoints REST
├── dto/           -> objetos de entrada e saída (Request/Response) de clientes, bolos e pedidos
├── entity/        -> entidades JPA e enum de status
├── exception/     -> exceções e tratamento global de erros
├── repository/    -> acesso ao banco (Spring Data JPA)
└── service/       -> regras de negócio
src/main/resources
├── application.properties
└── static/index.html   -> tela web
```

## Autor
**Marcos Andre Soares** — Curso de Engenharia de Software, UNIFSA
