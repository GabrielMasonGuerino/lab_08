# Biblioteca Digital — gRPC com Java

**Disciplina:** Computação Distribuída  
**Tema:** Comunicação entre serviços com gRPC e Protocol Buffers

---

## Descrição

Sistema de Gerenciamento de Biblioteca Digital distribuído, implementado com Java 17, Maven e gRPC 1.68.x. O servidor central expõe quatro operações RPC que cobrem os quatro tipos de comunicação gRPC:

| # | RPC | Tipo |
|---|-----|------|
| 1 | `CadastrarLivro` | Unary |
| 2 | `ListarLivrosPorAutor` | Server Streaming |
| 3 | `RegistrarEmprestimos` | Client Streaming |
| 4 | `ChatBibliotecario` | Bidirectional Streaming |

**Bônus implementados:**
- ✅ Interceptor gRPC para log automático de todas as chamadas (`LogInterceptor`)
- ✅ Deadline/timeout nas chamadas do cliente (10 segundos)
- ✅ Autenticação simples via metadata (`auth-token` no header)

---

## Como compilar e executar

### Pré-requisitos
- Java 17+
- Maven 3.8+

### 1. Compilar

```bash
mvn clean package -DskipTests
```

### 2. Iniciar o Servidor

```bash
# Terminal 1
mvn exec:java -Dexec.mainClass="br.mackenzie.biblioteca.server.ServidorBiblioteca"
```

Ou usando o JAR gerado:
```bash
java -cp "target/biblioteca-grpc-1.0-SNAPSHOT-servidor.jar:target/libs/*" \
     br.mackenzie.biblioteca.server.ServidorBiblioteca
```

### 3. Executar o Cliente

```bash
# Terminal 2 (com o servidor rodando)
mvn exec:java -Dexec.mainClass="br.mackenzie.biblioteca.client.ClienteBiblioteca"
```

Ou usando o JAR:
```bash
java -cp "target/biblioteca-grpc-1.0-SNAPSHOT-cliente.jar:target/libs/*" \
     br.mackenzie.biblioteca.client.ClienteBiblioteca
```

---

## Saída esperada

### Servidor
```
══════════════════════════════════════════════
  Servidor Biblioteca gRPC iniciado na porta 50051
══════════════════════════════════════════════
[INTERCEPTOR] 2024-01-15 10:00:01 | Token recebido: biblioteca-token-2024
[INTERCEPTOR] 2024-01-15 10:00:01 | → Chamada recebida: biblioteca.BibliotecaService/CadastrarLivro
[SERVIDOR] CadastrarLivro | titulo='Clean Code' autor='Robert C. Martin' isbn='ISBN-001'
[SERVIDOR] Livro cadastrado com ID=LIV-1
[INTERCEPTOR] 2024-01-15 10:00:01 | ← Chamada encerrada: ... | status=OK | duração=12 ms
...
```

### Cliente
```
╔══════════════════════════════════════════════════════╗
║  TESTE 1 — Cadastrar 3 livros (Unary RPC)
╚══════════════════════════════════════════════════════╝
  ✔ [OK] 'Clean Code' cadastrado — ID: LIV-1
  ✔ [OK] 'The Pragmatic Programmer' cadastrado — ID: LIV-2
  ✔ [OK] 'Designing Data-Intensive Applications' cadastrado — ID: LIV-3

╔══════════════════════════════════════════════════════╗
║  TESTE 2 — Listar livros de autor cadastrado (Server Streaming)
╚══════════════════════════════════════════════════════╝
  Buscando livros do autor: 'Robert C. Martin'
  → [LIV-1] 'Clean Code' (2008) — ISBN: ISBN-001

╔══════════════════════════════════════════════════════╗
║  TESTE 3 — Listar livros de autor inexistente (deve retornar NOT_FOUND)
╚══════════════════════════════════════════════════════╝
  Buscando livros do autor: 'Autor Inexistente'
  ✘ Erro: NOT_FOUND — Nenhum livro encontrado para o autor: Autor Inexistente

╔══════════════════════════════════════════════════════╗
║  TESTE 4 — Registrar 5 empréstimos (Client Streaming)
╚══════════════════════════════════════════════════════╝
  → Enviando empréstimo: usuário='Alice' livro='LIV-1'
  ...
  ✔ Resumo recebido: 5 empréstimo(s) em 503 ms — 5 empréstimo(s) registrado(s) com sucesso.

╔══════════════════════════════════════════════════════╗
║  TESTE 5 — Chat com bibliotecário (Bidirectional Streaming)
╚══════════════════════════════════════════════════════╝
  💬 Usuário 'Alice' pergunta sobre: 'java'
  📚 Sugestão: 'Effective Java' — Joshua Bloch | Sugestão baseada na palavra-chave: "java"
  ...

╔══════════════════════════════════════════════════════╗
║  TESTE 6 — Cadastrar livro com ISBN duplicado (deve retornar ALREADY_EXISTS)
╚══════════════════════════════════════════════════════╝
  ✘ Erro ao cadastrar 'Livro Duplicado': ALREADY_EXISTS — Já existe um livro com ISBN: ISBN-001

✅ Todos os testes concluídos.
```

---

## Estrutura do projeto

```
lab_08/
├── pom.xml
└── src/
    └── main/
        ├── java/br/mackenzie/biblioteca/
        │   ├── server/
        │   │   ├── BibliotecaServiceImpl.java   ← implementação dos 4 RPCs
        │   │   ├── ServidorBiblioteca.java       ← servidor gRPC (main)
        │   │   └── LogInterceptor.java           ← interceptor de log (bônus)
        │   └── client/
        │       └── ClienteBiblioteca.java        ← cliente com 6 testes
        └── proto/
            └── biblioteca.proto                  ← contrato gRPC/Protobuf
```

---

## Alunos

| Nome | RA |
|------|----|
| *Gabriel Mason Guerino* | *(10409928)* |
| *Mauricio Vicentini* | *(10426074)* |
| *Victor Hong* | *(10425852)* |
