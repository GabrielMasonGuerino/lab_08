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

<img width="1090" height="724" alt="image" src="https://github.com/user-attachments/assets/b8570cae-439f-4c2a-bb7f-66a453d3222e" />


### Cliente

<img width="947" height="815" alt="image" src="https://github.com/user-attachments/assets/885d0020-06ef-4b0b-ae11-895d8cb4764b" />

## Estrutura do projeto

```
biblioteca-grpc/
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
