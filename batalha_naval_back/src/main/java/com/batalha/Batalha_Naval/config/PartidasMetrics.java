package com.batalha.Batalha_Naval.config;

import com.batalha.Batalha_Naval.minado.MinadoService;
import com.batalha.Batalha_Naval.quiz.QuizService;
import com.batalha.Batalha_Naval.service.GameService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Expõe métricas sobre as partidas ativas no Prometheus/Grafana.
 * Como as partidas ficam em memória, os gauges leem diretamente do mapa.
 */
@Component
public class PartidasMetrics {

    private final Counter partidasCriadasClassico;
    private final Counter partidasCriadasMinado;
    private final Counter partidasCriadasQuiz;

    public PartidasMetrics(MeterRegistry registry,
                           GameService gameService,
                           MinadoService minadoService,
                           QuizService quizService) {

        // Partidas ativas agora (gauge - sobe e desce)
        Gauge.builder("partidas.ativas", gameService, s -> s.totalPartidas())
                .description("Partidas clássicas ativas em memória")
                .tag("modo", "classico")
                .register(registry);

        Gauge.builder("partidas.ativas", minadoService, s -> s.totalPartidas())
                .description("Partidas minadas ativas em memória")
                .tag("modo", "minado")
                .register(registry);

        Gauge.builder("partidas.ativas", quizService, s -> s.totalPartidas())
                .description("Partidas quiz ativas em memória")
                .tag("modo", "quiz")
                .register(registry);

        // Total de partidas criadas desde o boot (counter - só sobe)
        partidasCriadasClassico = Counter.builder("partidas.criadas")
                .description("Total de partidas criadas")
                .tag("modo", "classico")
                .register(registry);

        partidasCriadasMinado = Counter.builder("partidas.criadas")
                .description("Total de partidas criadas")
                .tag("modo", "minado")
                .register(registry);

        partidasCriadasQuiz = Counter.builder("partidas.criadas")
                .description("Total de partidas criadas")
                .tag("modo", "quiz")
                .register(registry);
    }

    public void partidaCriada(String modo) {
        switch (modo) {
            case "classico" -> partidasCriadasClassico.increment();
            case "minado" -> partidasCriadasMinado.increment();
            case "quiz" -> partidasCriadasQuiz.increment();
        }
    }
}
