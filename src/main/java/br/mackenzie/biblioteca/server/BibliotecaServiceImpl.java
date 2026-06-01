package br.mackenzie.biblioteca.server;

import br.mackenzie.biblioteca.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação dos quatro tipos de RPC do BibliotecaService.
 * Persistência em memória (HashMap / List).
 */
public class BibliotecaServiceImpl extends BibliotecaServiceGrpc.BibliotecaServiceImplBase {

    // ── Acervo em memória ──────────────────────────────────────────────────────
    private final Map<String, Livro> acervo    = new ConcurrentHashMap<>();
    private final Set<String>        isbnIndex = ConcurrentHashMap.newKeySet();
    private final AtomicInteger      idSeq     = new AtomicInteger(1);

    // ── Mapa de sugestões para o chat bidirecional ─────────────────────────────
    private static final Map<String, String[]> SUGESTOES = new HashMap<>();
    static {
        SUGESTOES.put("programacao",   new String[]{"Clean Code",            "Robert C. Martin"});
        SUGESTOES.put("redes",         new String[]{"Computer Networks",     "Andrew Tanenbaum"});
        SUGESTOES.put("distribuido",   new String[]{"Designing Data-Intensive Applications", "Martin Kleppmann"});
        SUGESTOES.put("grpc",          new String[]{"gRPC: Up and Running",  "Kasun Indrasiri"});
        SUGESTOES.put("java",          new String[]{"Effective Java",        "Joshua Bloch"});
        SUGESTOES.put("banco",         new String[]{"Database System Concepts", "Silberschatz"});
        SUGESTOES.put("algoritmo",     new String[]{"Introduction to Algorithms", "Cormen et al."});
        SUGESTOES.put("seguranca",     new String[]{"The Web Application Hacker's Handbook", "Stuttard"});
        SUGESTOES.put("ia",            new String[]{"Artificial Intelligence: A Modern Approach", "Russell & Norvig"});
        SUGESTOES.put("default",       new String[]{"The Pragmatic Programmer", "Hunt & Thomas"});
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1) Unary RPC — CadastrarLivro
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void cadastrarLivro(CadastrarLivroRequest req,
                               StreamObserver<CadastrarLivroResponse> responseObserver) {

        System.out.printf("[SERVIDOR] CadastrarLivro | titulo='%s' autor='%s' isbn='%s'%n",
                req.getTitulo(), req.getAutor(), req.getIsbn());

        // Validação básica
        if (req.getTitulo().isBlank() || req.getAutor().isBlank() || req.getIsbn().isBlank()) {
            responseObserver.onError(
                Status.INVALID_ARGUMENT
                    .withDescription("Título, autor e ISBN são obrigatórios.")
                    .asRuntimeException()
            );
            return;
        }

        // ISBN duplicado
        if (isbnIndex.contains(req.getIsbn())) {
            responseObserver.onError(
                Status.ALREADY_EXISTS
                    .withDescription("Já existe um livro com ISBN: " + req.getIsbn())
                    .asRuntimeException()
            );
            return;
        }

        String novoId = "LIV-" + idSeq.getAndIncrement();
        Livro livro = Livro.newBuilder()
                .setId(novoId)
                .setTitulo(req.getTitulo())
                .setAutor(req.getAutor())
                .setAno(req.getAno())
                .setIsbn(req.getIsbn())
                .build();

        acervo.put(novoId, livro);
        isbnIndex.add(req.getIsbn());

        System.out.printf("[SERVIDOR] Livro cadastrado com ID=%s%n", novoId);

        responseObserver.onNext(
            CadastrarLivroResponse.newBuilder()
                .setId(novoId)
                .setSucesso(true)
                .setMensagem("Livro cadastrado com sucesso!")
                .build()
        );
        responseObserver.onCompleted();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2) Server Streaming RPC — ListarLivrosPorAutor
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void listarLivrosPorAutor(ListarLivrosPorAutorRequest req,
                                     StreamObserver<Livro> responseObserver) {

        System.out.printf("[SERVIDOR] ListarLivrosPorAutor | autor='%s'%n", req.getAutor());

        List<Livro> encontrados = acervo.values().stream()
                .filter(l -> l.getAutor().equalsIgnoreCase(req.getAutor()))
                .toList();

        if (encontrados.isEmpty()) {
            responseObserver.onError(
                Status.NOT_FOUND
                    .withDescription("Nenhum livro encontrado para o autor: " + req.getAutor())
                    .asRuntimeException()
            );
            return;
        }

        for (Livro livro : encontrados) {
            System.out.printf("[SERVIDOR]   → Enviando livro: %s%n", livro.getTitulo());
            responseObserver.onNext(livro);
        }
        responseObserver.onCompleted();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3) Client Streaming RPC — RegistrarEmprestimos
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public StreamObserver<Emprestimo> registrarEmprestimos(
            StreamObserver<ResumoEmprestimos> responseObserver) {

        System.out.println("[SERVIDOR] RegistrarEmprestimos | stream iniciado");
        long inicio = System.currentTimeMillis();

        return new StreamObserver<>() {
            int total = 0;

            @Override
            public void onNext(Emprestimo emp) {
                // Validar que o livro existe
                if (!acervo.containsKey(emp.getLivroId())) {
                    System.out.printf("[SERVIDOR]   ⚠ Livro ID=%s não encontrado para usuário=%s%n",
                            emp.getLivroId(), emp.getUsuario());
                } else {
                    total++;
                    System.out.printf("[SERVIDOR]   Empréstimo %d: usuário='%s' livro='%s'%n",
                            total, emp.getUsuario(), emp.getLivroId());
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[SERVIDOR] RegistrarEmprestimos | erro: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                long duracao = System.currentTimeMillis() - inicio;
                System.out.printf("[SERVIDOR] RegistrarEmprestimos | concluído: %d empréstimos em %d ms%n",
                        total, duracao);

                responseObserver.onNext(
                    ResumoEmprestimos.newBuilder()
                        .setTotalRegistrado(total)
                        .setTempoProcessamentoMs(duracao)
                        .setMensagem(total + " empréstimo(s) registrado(s) com sucesso.")
                        .build()
                );
                responseObserver.onCompleted();
            }
        };
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4) Bidirectional Streaming RPC — ChatBibliotecario
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public StreamObserver<MensagemChat> chatBibliotecario(
            StreamObserver<SugestaoLivro> responseObserver) {

        System.out.println("[SERVIDOR] ChatBibliotecario | sessão iniciada");

        return new StreamObserver<>() {
            @Override
            public void onNext(MensagemChat msg) {
                System.out.printf("[SERVIDOR]   Chat | usuário='%s' palavra_chave='%s'%n",
                        msg.getUsuario(), msg.getPalavraChave());

                String chave = msg.getPalavraChave().toLowerCase(java.util.Locale.ROOT);
                String[] sugestao = SUGESTOES.getOrDefault(chave, SUGESTOES.get("default"));

                responseObserver.onNext(
                    SugestaoLivro.newBuilder()
                        .setTitulo(sugestao[0])
                        .setAutor(sugestao[1])
                        .setMotivo("Sugestão baseada na palavra-chave: \"" + msg.getPalavraChave() + "\"")
                        .build()
                );
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("[SERVIDOR] ChatBibliotecario | erro: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("[SERVIDOR] ChatBibliotecario | sessão encerrada");
                responseObserver.onCompleted();
            }
        };
    }
}
