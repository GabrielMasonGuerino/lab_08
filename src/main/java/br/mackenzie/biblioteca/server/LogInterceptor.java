package br.mackenzie.biblioteca.server;

import io.grpc.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Interceptor gRPC para log automático de todas as chamadas (bônus).
 * Registra: timestamp, método RPC, metadados relevantes e duração.
 */
public class LogInterceptor implements ServerInterceptor {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String metodo    = call.getMethodDescriptor().getFullMethodName();
        String timestamp = LocalDateTime.now().format(FMT);
        long   inicio    = System.currentTimeMillis();

        // Log de autenticação simples via metadata (bônus)
        String token = headers.get(Metadata.Key.of("auth-token", Metadata.ASCII_STRING_MARSHALLER));
        if (token != null) {
            System.out.printf("[INTERCEPTOR] %s | Token recebido: %s%n", timestamp, token);
        }

        System.out.printf("[INTERCEPTOR] %s | → Chamada recebida: %s%n", timestamp, metodo);

        ServerCall<ReqT, RespT> callComLog = new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void close(Status status, Metadata trailers) {
                long duracao = System.currentTimeMillis() - inicio;
                System.out.printf("[INTERCEPTOR] %s | ← Chamada encerrada: %s | status=%s | duração=%d ms%n",
                        LocalDateTime.now().format(FMT), metodo, status.getCode(), duracao);
                super.close(status, trailers);
            }
        };

        return next.startCall(callComLog, headers);
    }
}
