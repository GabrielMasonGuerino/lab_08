package br.mackenzie.biblioteca.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Ponto de entrada do servidor gRPC da Biblioteca Digital.
 */
public class ServidorBiblioteca {

    private static final int PORTA = 50051;
    private Server server;

    public void iniciar() throws IOException {
        server = ServerBuilder.forPort(PORTA)
                // Interceptor de log automático (bônus)
                .addService(ServerInterceptors.intercept(
                        new BibliotecaServiceImpl(),
                        new LogInterceptor()
                ))
                .build()
                .start();

        System.out.println("══════════════════════════════════════════════");
        System.out.println("  Servidor Biblioteca gRPC iniciado na porta " + PORTA);
        System.out.println("══════════════════════════════════════════════");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[SERVIDOR] Encerrando servidor...");
            try {
                ServidorBiblioteca.this.encerrar();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
    }

    public void encerrar() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    public void aguardarTermino() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ServidorBiblioteca servidor = new ServidorBiblioteca();
        servidor.iniciar();
        servidor.aguardarTermino();
    }
}
