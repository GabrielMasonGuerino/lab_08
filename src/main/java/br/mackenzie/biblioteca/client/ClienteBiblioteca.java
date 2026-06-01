package br.mackenzie.biblioteca.client;

import br.mackenzie.biblioteca.grpc.*;
import io.grpc.*;
import io.grpc.stub.StreamObserver;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Cliente gRPC da Biblioteca Digital.
 *
 * Demonstra os 6 cenários de teste exigidos:
 *  1. Cadastrar 3 livros diferentes (Unary)
 *  2. Listar livros de um autor cadastrado (Server Streaming)
 *  3. Listar livros de autor inexistente — NOT_FOUND (Server Streaming)
 *  4. Registrar 5 empréstimos consecutivos (Client Streaming)
 *  5. Chat com pelo menos 3 mensagens (Bidirectional Streaming)
 *  6. Cadastrar livro com ISBN duplicado — ALREADY_EXISTS (Unary)
 */
public class ClienteBiblioteca {

    private static final String HOST  = "localhost";
    private static final int    PORTA = 50051;

    // ── Deadline global para chamadas (bônus) ──────────────────────────────
    private static final int DEADLINE_SEGUNDOS = 10;

    private final BibliotecaServiceGrpc.BibliotecaServiceBlockingStub  blocoStub;
    private final BibliotecaServiceGrpc.BibliotecaServiceStub          asyncStub;

    public ClienteBiblioteca(ManagedChannel canal) {
        // Adicionar token de autenticação via metadata (bônus)
        ClientInterceptor authInterceptor = new ClientInterceptor() {
            @Override
            public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
                    MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
                return new ForwardingClientCall.SimpleForwardingClientCall<>(
                        next.newCall(method, callOptions)) {
                    @Override
                    public void start(Listener<RespT> responseListener, Metadata headers) {
                        headers.put(
                            Metadata.Key.of("auth-token", Metadata.ASCII_STRING_MARSHALLER),
                            "biblioteca-token-2024"
                        );
                        super.start(responseListener, headers);
                    }
                };
            }
        };

        Channel canalInterceptado = ClientInterceptors.intercept(canal, authInterceptor);
        this.blocoStub = BibliotecaServiceGrpc.newBlockingStub(canalInterceptado);
        this.asyncStub = BibliotecaServiceGrpc.newStub(canalInterceptado);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helpers
    // ══════════════════════════════════════════════════════════════════════════

    private void separador(String titulo) {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║  " + titulo);
        System.out.println("╚══════════════════════════════════════════════════════╝");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1) Unary RPC — Cadastrar Livros
    // ══════════════════════════════════════════════════════════════════════════

    private String cadastrarLivro(String titulo, String autor, int ano, String isbn) {
        try {
            CadastrarLivroResponse resp = blocoStub
                    .withDeadlineAfter(DEADLINE_SEGUNDOS, TimeUnit.SECONDS)
                    .cadastrarLivro(CadastrarLivroRequest.newBuilder()
                            .setTitulo(titulo)
                            .setAutor(autor)
                            .setAno(ano)
                            .setIsbn(isbn)
                            .build());

            System.out.printf("  ✔ [%s] '%s' cadastrado — ID: %s%n",
                    resp.getSucesso() ? "OK" : "FALHA", titulo, resp.getId());
            return resp.getId();

        } catch (StatusRuntimeException e) {
            System.out.printf("  ✘ Erro ao cadastrar '%s': %s — %s%n",
                    titulo, e.getStatus().getCode(), e.getStatus().getDescription());
            return null;
        }
    }

    private void testeCadastrarLivros() {
        separador("TESTE 1 — Cadastrar 3 livros (Unary RPC)");
        cadastrarLivro("Clean Code",                 "Robert C. Martin",  2008, "ISBN-001");
        cadastrarLivro("The Pragmatic Programmer",   "Hunt & Thomas",     1999, "ISBN-002");
        cadastrarLivro("Designing Data-Intensive Applications", "Martin Kleppmann", 2017, "ISBN-003");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2 & 3) Server Streaming RPC — Listar Livros por Autor
    // ══════════════════════════════════════════════════════════════════════════

    private void listarLivrosPorAutor(String autor) {
        System.out.printf("%n  Buscando livros do autor: '%s'%n", autor);
        try {
            blocoStub
                    .withDeadlineAfter(DEADLINE_SEGUNDOS, TimeUnit.SECONDS)
                    .listarLivrosPorAutor(
                        ListarLivrosPorAutorRequest.newBuilder().setAutor(autor).build()
                    )
                    .forEachRemaining(livro ->
                        System.out.printf("  → [%s] '%s' (%d) — ISBN: %s%n",
                                livro.getId(), livro.getTitulo(), livro.getAno(), livro.getIsbn())
                    );
        } catch (StatusRuntimeException e) {
            System.out.printf("  ✘ Erro: %s — %s%n",
                    e.getStatus().getCode(), e.getStatus().getDescription());
        }
    }

    private void testeListarLivros() {
        separador("TESTE 2 — Listar livros de autor cadastrado (Server Streaming)");
        listarLivrosPorAutor("Robert C. Martin");

        separador("TESTE 3 — Listar livros de autor inexistente (deve retornar NOT_FOUND)");
        listarLivrosPorAutor("Autor Inexistente");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4) Client Streaming RPC — Registrar Empréstimos
    // ══════════════════════════════════════════════════════════════════════════

    private void testeRegistrarEmprestimos() throws InterruptedException {
        separador("TESTE 4 — Registrar 5 empréstimos (Client Streaming)");

        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<Emprestimo> requestObserver = asyncStub
                .withDeadlineAfter(DEADLINE_SEGUNDOS, TimeUnit.SECONDS)
                .registrarEmprestimos(new StreamObserver<>() {
                    @Override
                    public void onNext(ResumoEmprestimos resumo) {
                        System.out.printf("  ✔ Resumo recebido: %d empréstimo(s) em %d ms — %s%n",
                                resumo.getTotalRegistrado(),
                                resumo.getTempoProcessamentoMs(),
                                resumo.getMensagem());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("  ✘ Erro: " + t.getMessage());
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                });

        List<String[]> emprestimos = Arrays.asList(
                new String[]{"Alice",   "LIV-1"},
                new String[]{"Bob",     "LIV-2"},
                new String[]{"Carlos",  "LIV-3"},
                new String[]{"Diana",   "LIV-1"},
                new String[]{"Eduardo", "LIV-2"}
        );

        for (String[] e : emprestimos) {
            System.out.printf("  → Enviando empréstimo: usuário='%s' livro='%s'%n", e[0], e[1]);
            requestObserver.onNext(
                Emprestimo.newBuilder()
                    .setUsuario(e[0])
                    .setLivroId(e[1])
                    .build()
            );
            Thread.sleep(100); // simular chegada gradual
        }
        requestObserver.onCompleted();

        latch.await(DEADLINE_SEGUNDOS + 2L, TimeUnit.SECONDS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5) Bidirectional Streaming RPC — Chat Bibliotecário
    // ══════════════════════════════════════════════════════════════════════════

    private void testeChatBibliotecario() throws InterruptedException {
        separador("TESTE 5 — Chat com bibliotecário (Bidirectional Streaming)");

        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<MensagemChat> requestObserver = asyncStub
                .withDeadlineAfter(DEADLINE_SEGUNDOS, TimeUnit.SECONDS)
                .chatBibliotecario(new StreamObserver<>() {
                    @Override
                    public void onNext(SugestaoLivro sugestao) {
                        System.out.printf("  📚 Sugestão: '%s' — %s | %s%n",
                                sugestao.getTitulo(), sugestao.getAutor(), sugestao.getMotivo());
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.err.println("  ✘ Erro no chat: " + t.getMessage());
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("  Chat encerrado pelo servidor.");
                        latch.countDown();
                    }
                });

        List<String[]> mensagens = Arrays.asList(
                new String[]{"Alice",  "java"},
                new String[]{"Alice",  "distribuido"},
                new String[]{"Alice",  "seguranca"}
        );

        for (String[] m : mensagens) {
            System.out.printf("  💬 Usuário '%s' pergunta sobre: '%s'%n", m[0], m[1]);
            requestObserver.onNext(
                MensagemChat.newBuilder()
                    .setUsuario(m[0])
                    .setPalavraChave(m[1])
                    .build()
            );
            Thread.sleep(300);
        }
        requestObserver.onCompleted();

        latch.await(DEADLINE_SEGUNDOS + 2L, TimeUnit.SECONDS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6) Unary RPC — ISBN duplicado (deve retornar ALREADY_EXISTS)
    // ══════════════════════════════════════════════════════════════════════════

    private void testeIsbnDuplicado() {
        separador("TESTE 6 — Cadastrar livro com ISBN duplicado (deve retornar ALREADY_EXISTS)");
        cadastrarLivro("Livro Duplicado", "Autor Qualquer", 2020, "ISBN-001");
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Main
    // ══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Conectando ao servidor em " + HOST + ":" + PORTA + " ...");

        ManagedChannel canal = ManagedChannelBuilder
                .forAddress(HOST, PORTA)
                .usePlaintext()
                .build();

        try {
            ClienteBiblioteca cliente = new ClienteBiblioteca(canal);

            // ── Executar todos os testes ──
            cliente.testeCadastrarLivros();
            cliente.testeListarLivros();
            cliente.testeRegistrarEmprestimos();
            cliente.testeChatBibliotecario();
            cliente.testeIsbnDuplicado();

            System.out.println("\n✅ Todos os testes concluídos.");

        } finally {
            canal.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
