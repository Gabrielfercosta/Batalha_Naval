package com.batalha.Batalha_Naval.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Métricas de gameplay: tiros, acertos, navios afundados, minas, perguntas respondidas.
 * Visíveis no Grafana via Prometheus.
 */
@Component
public class GameplayMetrics {

    // Tiros no modo clássico
    private final Counter tirosAgua;
    private final Counter tirosAcerto;
    private final Counter tirosAfundado;

    // Tiros no modo minado
    private final Counter minadoAgua;
    private final Counter minadoNavio;
    private final Counter minadoMina;

    // Quiz
    private final Counter quizRespostasCorretas;
    private final Counter quizRespostasErradas;
    private final Counter quizTirosDisparados;
    private final Counter quizCorretaFacil;
    private final Counter quizErradaFacil;
    private final Counter quizCorretaMedia;
    private final Counter quizErradaMedia;
    private final Counter quizCorretaDificil;
    private final Counter quizErradaDificil;

    public GameplayMetrics(MeterRegistry registry) {
        // Clássico
        tirosAgua = Counter.builder("gameplay.tiros")
                .tag("modo", "classico").tag("resultado", "agua")
                .description("Tiros na água (clássico)")
                .register(registry);
        tirosAcerto = Counter.builder("gameplay.tiros")
                .tag("modo", "classico").tag("resultado", "acerto")
                .description("Tiros que acertaram navio (clássico)")
                .register(registry);
        tirosAfundado = Counter.builder("gameplay.tiros")
                .tag("modo", "classico").tag("resultado", "afundado")
                .description("Tiros que afundaram navio (clássico)")
                .register(registry);

        // Minado
        minadoAgua = Counter.builder("gameplay.tiros")
                .tag("modo", "minado").tag("resultado", "agua")
                .description("Tiros na água (minado)")
                .register(registry);
        minadoNavio = Counter.builder("gameplay.tiros")
                .tag("modo", "minado").tag("resultado", "navio")
                .description("Tiros que acertaram navio (minado)")
                .register(registry);
        minadoMina = Counter.builder("gameplay.tiros")
                .tag("modo", "minado").tag("resultado", "mina")
                .description("Tiros que atingiram mina (minado)")
                .register(registry);

        // Quiz
        quizRespostasCorretas = Counter.builder("gameplay.quiz.respostas")
                .tag("resultado", "correta")
                .description("Respostas corretas no quiz")
                .register(registry);
        quizRespostasErradas = Counter.builder("gameplay.quiz.respostas")
                .tag("resultado", "errada")
                .description("Respostas erradas no quiz")
                .register(registry);
        quizTirosDisparados = Counter.builder("gameplay.quiz.tiros")
                .description("Tiros disparados no quiz (conquistados por acertos)")
                .register(registry);

        // Por dificuldade
        quizCorretaFacil = Counter.builder("gameplay.quiz.por_dificuldade")
                .tag("dificuldade", "easy").tag("resultado", "correta").register(registry);
        quizErradaFacil = Counter.builder("gameplay.quiz.por_dificuldade")
                .tag("dificuldade", "easy").tag("resultado", "errada").register(registry);
        quizCorretaMedia = Counter.builder("gameplay.quiz.por_dificuldade")
                .tag("dificuldade", "medium").tag("resultado", "correta").register(registry);
        quizErradaMedia = Counter.builder("gameplay.quiz.por_dificuldade")
                .tag("dificuldade", "medium").tag("resultado", "errada").register(registry);
        quizCorretaDificil = Counter.builder("gameplay.quiz.por_dificuldade")
                .tag("dificuldade", "hard").tag("resultado", "correta").register(registry);
        quizErradaDificil = Counter.builder("gameplay.quiz.por_dificuldade")
                .tag("dificuldade", "hard").tag("resultado", "errada").register(registry);
    }

    // Clássico
    public void registrarTiroClassico(String resultado) {
        switch (resultado) {
            case "AGUA" -> tirosAgua.increment();
            case "ACERTO" -> tirosAcerto.increment();
            case "AFUNDADO" -> tirosAfundado.increment();
        }
    }

    // Minado
    public void registrarTiroMinado(String resultado) {
        switch (resultado) {
            case "AGUA" -> minadoAgua.increment();
            case "NAVIO" -> minadoNavio.increment();
            case "MINA" -> minadoMina.increment();
        }
    }

    // Quiz
    public void registrarRespostaQuiz(boolean correta) {
        if (correta) quizRespostasCorretas.increment();
        else quizRespostasErradas.increment();
    }

    public void registrarRespostaPorDificuldade(String dificuldade, boolean correta) {
        switch (dificuldade) {
            case "easy" -> { if (correta) quizCorretaFacil.increment(); else quizErradaFacil.increment(); }
            case "medium" -> { if (correta) quizCorretaMedia.increment(); else quizErradaMedia.increment(); }
            case "hard" -> { if (correta) quizCorretaDificil.increment(); else quizErradaDificil.increment(); }
        }
    }

    public void registrarTiroQuiz() {
        quizTirosDisparados.increment();
    }
}
