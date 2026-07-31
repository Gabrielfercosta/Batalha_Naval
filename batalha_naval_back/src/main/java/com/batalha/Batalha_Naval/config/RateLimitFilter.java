package com.batalha.Batalha_Naval.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting por IP, com limites separados por tipo de rota.
 *
 * Um limite único e baixo não funciona aqui: o lobby faz polling de três
 * endpoints a cada 5 segundos, o que dá 36 req/min por jogador. Dois jogadores
 * na mesma rede (ou duas abas no mesmo computador) compartilham o IP e somam
 * 72 req/min apenas de polling. Com o limite antigo de 100 req/min para tudo,
 * as chamadas do jogo passavam a receber 429 e a partida travava.
 *
 * Por isso as rotas de leitura do jogo têm um teto alto, enquanto autenticação
 * e criação de partidas — onde o abuso realmente importa — seguem restritas.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Autenticação: protege contra tentativa de adivinhar senha. */
    private static final int LIMITE_AUTH = 20;
    /** Criação de partidas: evita alguém inundar o lobby de salas. */
    private static final int LIMITE_ESCRITA = 60;
    /** Leituras do jogo: precisa acomodar o polling de vários jogadores por IP. */
    private static final int LIMITE_LEITURA = 600;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final MeterRegistry registry;
    /**
     * Criado no primeiro bloqueio, e não no construtor: filtros são inicializados
     * antes do registry do Prometheus ser anexado, e um contador criado cedo demais
     * não chega a ser exportado em /actuator/prometheus.
     */
    private volatile Counter bloqueios;

    public RateLimitFilter(MeterRegistry registry) {
        this.registry = registry;
    }

    private Counter contadorDeBloqueios() {
        Counter atual = bloqueios;
        if (atual == null) {
            synchronized (this) {
                atual = bloqueios;
                if (atual == null) {
                    atual = Counter.builder("rate.limit.blocked")
                            .description("Requisições rejeitadas pelo rate limiting")
                            .register(registry);
                    bloqueios = atual;
                }
            }
        }
        return atual;
    }

    private enum Categoria {
        AUTH(LIMITE_AUTH), ESCRITA(LIMITE_ESCRITA), LEITURA(LIMITE_LEITURA);

        final int limitePorMinuto;
        Categoria(int limitePorMinuto) { this.limitePorMinuto = limitePorMinuto; }
    }

    private Categoria categoriaDe(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth")) {
            return Categoria.AUTH;
        }
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return Categoria.LEITURA;
        }
        return Categoria.ESCRITA;
    }

    private Bucket bucketPara(String ip, Categoria categoria) {
        return buckets.computeIfAbsent(categoria.name() + ':' + ip, chave -> {
            Bandwidth limite = Bandwidth.classic(
                    categoria.limitePorMinuto,
                    Refill.greedy(categoria.limitePorMinuto, Duration.ofMinutes(1)));
            return Bucket.builder().addLimit(limite).build();
        });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = ipDoCliente(request);
        Categoria categoria = categoriaDe(request);
        Bucket bucket = bucketPara(ip, categoria);

        if (bucket.tryConsume(1)) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(bucket.getAvailableTokens()));
            filterChain.doFilter(request, response);
            return;
        }

        contadorDeBloqueios().increment();
        log.warn("Rate limit excedido: ip={} categoria={} uri={}", ip, categoria, request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"mensagem\": \"Muitas requisições em pouco tempo. Aguarde alguns segundos.\"}");
    }

    private String ipDoCliente(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        // WebSocket é conexão persistente; actuator é consumido pelo Prometheus.
        return uri.startsWith("/ws") || uri.startsWith("/actuator");
    }
}
