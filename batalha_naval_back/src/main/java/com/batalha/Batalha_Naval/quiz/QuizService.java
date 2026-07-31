package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.config.GameplayMetrics;
import com.batalha.Batalha_Naval.dominio.Coordenada;
import com.batalha.Batalha_Naval.dominio.Direcao;
import com.batalha.Batalha_Naval.dominio.FabricaNavio;
import com.batalha.Batalha_Naval.dominio.Navio;
import com.batalha.Batalha_Naval.dominio.ResultadoTiro;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dominio.Tabuleiro;
import com.batalha.Batalha_Naval.dominio.TipoNavio;
import com.batalha.Batalha_Naval.dto.NavioRevelado;
import com.batalha.Batalha_Naval.service.ServicoPartidaBase;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class QuizService extends ServicoPartidaBase<PartidaQuiz> {
    private static final int SEGUNDOS_CONTAGEM = 3;
    private static final int SEGUNDOS_RESPOSTA = 12;
    private static final long DELAY_ENTRE_PERGUNTAS_MS = 2500;
    private static final long DELAY_ENTRE_RODADAS_MS = 3000;
    private final TriviaService triviaService;
    private final SimpMessagingTemplate messaging;
    private final GameplayMetrics gameplayMetrics;
    private final ScheduledExecutorService agenda = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Set<String>> chegaram = new ConcurrentHashMap<>();
    private final Set<String> iniciados = ConcurrentHashMap.newKeySet();
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public QuizService(TriviaService triviaService, SimpMessagingTemplate messaging, GameplayMetrics gameplayMetrics) {
        this.triviaService = triviaService;
        this.messaging = messaging;
        this.gameplayMetrics = gameplayMetrics;
    }

    @Override
    protected PartidaQuiz novaPartida(String jogador, String nome, String senha) {
        return new PartidaQuiz(jogador, nome, senha);
    }

    /**
     * O @CacheEvict precisa estar aqui, e não apenas no criarPartida() herdado:
     * a chamada interna abaixo não passa pelo proxy do Spring, então o evict da
     * classe base não dispararia e a sala nova não apareceria na listagem.
     */
    @org.springframework.cache.annotation.CacheEvict(value = "salas-abertas", key = "#root.targetClass.simpleName")
    public String criarPartidaQuiz(String jogador, String nome, String senha, List<String> categorias, String dificuldade, boolean modoRapido) {
        String gameId = criarPartida(jogador, nome, senha);
        buscarPartida(gameId).configurar(categorias, dificuldade, modoRapido);
        return gameId;
    }

    /**
     * Assim como em criarPartidaQuiz, o evict precisa estar aqui: a chamada a
     * super.sairDaPartida() não passa pelo proxy do Spring, então a anotação da
     * classe base não dispara e a sala continuaria listada após o jogador sair.
     */
    @Override
    @org.springframework.cache.annotation.CacheEvict(value = "salas-abertas", key = "#root.targetClass.simpleName")
    public PartidaQuiz sairDaPartida(String gameId, String jogador) {
        PartidaQuiz partida = super.sairDaPartida(gameId, jogador);
        if (partida != null && partida.getStatus() == StatusPartida.FINALIZADA) {
            limpar(gameId);
        }
        return partida;
    }

    public void posicionarNavio(String gameId, String jogador, TipoNavio tipo, int linha, int coluna, Direcao direcao) {
        PartidaQuiz partida = buscarPartida(gameId);
        Navio navio = FabricaNavio.criar(tipo, linha, coluna, direcao);
        Tabuleiro tabuleiro = jogador.equals(partida.getJogador1()) ? partida.getTabuleiro1() : partida.getTabuleiro2();
        tabuleiro.posicionarNavio(navio);
    }

    public void jogadorChegou(String gameId, String jogador) {
        PartidaQuiz partida = buscarPartida(gameId);
        Set<String> presentes = chegaram.computeIfAbsent(gameId, k -> ConcurrentHashMap.newKeySet());
        presentes.add(jogador);

        boolean ambos = partida.getJogador2() != null
                && presentes.contains(partida.getJogador1())
                && presentes.contains(partida.getJogador2());

        if (ambos && partida.getStatus() == StatusPartida.EM_ANDAMENTO && iniciados.add(gameId)) {
            iniciarRodadaComContagem(gameId);
        }
    }

    public void responder(String gameId, String jogador, String resposta) {
        PartidaQuiz partida = buscarPartida(gameId);
        boolean todos;
        synchronized (partida) {
            todos = partida.responder(jogador, resposta);
        }
        if (todos) {
            cancelarTimer(gameId);
            resolverPergunta(gameId);
        }
    }

    public void atirar(String gameId, String jogador, int linha, int coluna) {
        PartidaQuiz partida = buscarPartida(gameId);
        Coordenada tiro = new Coordenada(linha, coluna);

        ResultadoTiro resultado;
        NavioRevelado navioAfundado = null;
        synchronized (partida) {
            resultado = partida.atirar(jogador, tiro);
            gameplayMetrics.registrarTiroQuiz();
            resultado = partida.atirar(jogador, tiro);
            if (resultado == ResultadoTiro.AFUNDADO) {
                Navio navio = partida.navioAfundadoEm(jogador, tiro);
                if (navio != null) {
                    navioAfundado = new NavioRevelado(navio);
                }
            }
        }

        TiroQuizResponse resposta = new TiroQuizResponse(
                jogador, linha, coluna, resultado, navioAfundado,
                partida.proximoAtirador(), partida.getStatus(), partida.getVencedor());
        messaging.convertAndSend("/topic/quiz/" + gameId, resposta);

        if (partida.getStatus() == StatusPartida.FINALIZADA) {
            limpar(gameId);
        } else if (partida.faseTirosAcabou()) {
            agenda.schedule(() -> iniciarRodadaComContagem(gameId), DELAY_ENTRE_RODADAS_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void iniciarRodadaComContagem(String gameId) {
        PartidaQuiz partida = partidas.get(gameId);
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return;
        }
        synchronized (partida) {
            partida.novaRodada();
        }
        messaging.convertAndSend("/topic/quiz/" + gameId, new ContagemResponse(SEGUNDOS_CONTAGEM));
        agenda.schedule(() -> iniciarPergunta(gameId), SEGUNDOS_CONTAGEM, TimeUnit.SECONDS);
    }

    private void iniciarPergunta(String gameId) {
        PartidaQuiz partida = partidas.get(gameId);
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return;
        }

        PerguntaTrivia pergunta = null;
        try {
            for (int i = 0; i < 8; i++) {
                pergunta = triviaService.sortearPergunta(partida.getCategorias(), partida.getDificuldade());
                if (partida.getPerguntasUsadas().add(pergunta.getPergunta())) {
                    break;
                }
                if (i == 7) {
                    partida.getPerguntasUsadas().clear();
                    partida.getPerguntasUsadas().add(pergunta.getPergunta());
                }
            }
        } catch (Exception e) {
            messaging.convertAndSend("/topic/quiz/" + gameId, Map.of("tipo", "ERRO", "mensagem", "Não consegui buscar uma pergunta. Tentando de novo..."));
            agenda.schedule(() -> iniciarPergunta(gameId), DELAY_ENTRE_PERGUNTAS_MS, TimeUnit.MILLISECONDS);
            return;
        }

        synchronized (partida) {
            partida.iniciarPergunta(pergunta);
        }

        PerguntaResponse resposta = new PerguntaResponse(
                pergunta.getPergunta(), pergunta.getOpcoes(), SEGUNDOS_RESPOSTA,
                partida.getPerguntaIndice() + 1, PartidaQuiz.PERGUNTAS_POR_RODADA,
                pergunta.getDificuldade(), partida.isModoRapido());
        messaging.convertAndSend("/topic/quiz/" + gameId, resposta);

        agendarTimer(gameId);
    }

    private void resolverPergunta(String gameId) {
        PartidaQuiz partida = partidas.get(gameId);
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return;
        }

        // Os dados usados depois são copiados dentro do lock: acertouPergunta é o mapa
        // real da partida e responder() escreve nele a qualquer momento. Iterar sobre
        // ele fora do lock quebra com ConcurrentModificationException e interrompe
        // este método antes de emitir o RESULTADO, travando a partida.
        Map<String, Boolean> acertos;
        String dificuldade;
        synchronized (partida) {
            if (partida.isResolvida()) {
                return;
            }
            partida.resolverPergunta();
            acertos = new HashMap<>(partida.getAcertouPergunta());
            dificuldade = partida.getPerguntaAtual().getDificuldade();
        }

        for (Map.Entry<String, Boolean> entry : partida.getAcertouPergunta().entrySet()) {
            gameplayMetrics.registrarRespostaQuiz(entry.getValue());
            gameplayMetrics.registrarRespostaPorDificuldade(
                    partida.getPerguntaAtual().getDificuldade(), entry.getValue());
        }

        // Emitir RESULTADO sempre (rodada normal e desempate) para parar timer no frontend
        messaging.convertAndSend("/topic/quiz/" + gameId, new ResultadoRodadaResponse(
                partida.getPerguntaAtual().getRespostaCorreta(),
                acertos,
                partida.isEmDesempate() ? partida.getPerguntaDesempate() : partida.getPerguntaIndice() + 1));

        if (partida.isEmDesempate()) {
            // Morte súbita — mostrar resultado e depois resolver desempate com delay
            agenda.schedule(() -> resolverDesempate(gameId), DELAY_ENTRE_PERGUNTAS_MS, TimeUnit.MILLISECONDS);
        } else {
            if (partida.ultimaPergunta()) {
                // Fim das perguntas normais — verificar se empatou
                agenda.schedule(() -> verificarEmpate(gameId), DELAY_ENTRE_PERGUNTAS_MS, TimeUnit.MILLISECONDS);
            } else {
                synchronized (partida) {
                    partida.avancarPergunta();
                }
                agenda.schedule(() -> iniciarPergunta(gameId), DELAY_ENTRE_PERGUNTAS_MS, TimeUnit.MILLISECONDS);
            }
        }
    }

    private void verificarEmpate(String gameId) {
        PartidaQuiz partida = partidas.get(gameId);
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return;
        }
        int a1 = partida.getAcertosRodada().getOrDefault(partida.getJogador1(), 0);
        int a2 = partida.getAcertosRodada().getOrDefault(partida.getJogador2(), 0);

        if (a1 == a2 && a1 > 0) {
            // Empate com acertos! Iniciar morte súbita
            synchronized (partida) {
                partida.iniciarDesempate();
            }
            messaging.convertAndSend("/topic/quiz/" + gameId, Map.of(
                    "tipo", "DESEMPATE",
                    "mensagem", "Empate! Morte súbita — quem errar primeiro perde!",
                    "pontuacao", a1));
            agenda.schedule(() -> iniciarPerguntaDesempate(gameId), DELAY_ENTRE_RODADAS_MS, TimeUnit.MILLISECONDS);
        } else {
            // Ou não empatou, ou ambos com 0 (vai pra fase de tiros normal — ninguém atira)
            iniciarFaseTiros(gameId);
        }
    }

    private void iniciarPerguntaDesempate(String gameId) {
        PartidaQuiz partida = partidas.get(gameId);
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return;
        }

        synchronized (partida) {
            partida.prepararPerguntaDesempate();
        }

        PerguntaTrivia pergunta = null;
        try {
            for (int i = 0; i < 8; i++) {
                pergunta = triviaService.sortearPergunta(partida.getCategorias(), partida.getDificuldade());
                if (partida.getPerguntasUsadas().add(pergunta.getPergunta())) {
                    break;
                }
                if (i == 7) {
                    partida.getPerguntasUsadas().clear();
                    partida.getPerguntasUsadas().add(pergunta.getPergunta());
                }
            }
        } catch (Exception e) {
            messaging.convertAndSend("/topic/quiz/" + gameId, Map.of("tipo", "ERRO", "mensagem", "Não consegui buscar uma pergunta. Tentando de novo..."));
            agenda.schedule(() -> iniciarPerguntaDesempate(gameId), DELAY_ENTRE_PERGUNTAS_MS, TimeUnit.MILLISECONDS);
            return;
        }

        synchronized (partida) {
            partida.iniciarPergunta(pergunta);
        }

        PerguntaResponse resposta = new PerguntaResponse(
                pergunta.getPergunta(), pergunta.getOpcoes(), SEGUNDOS_RESPOSTA,
                partida.getPerguntaDesempate(), partida.getPerguntaDesempate(),
                pergunta.getDificuldade(), partida.isModoRapido(), true);
        messaging.convertAndSend("/topic/quiz/" + gameId, resposta);

        agendarTimer(gameId);
    }

    private void resolverDesempate(String gameId) {
        PartidaQuiz partida = partidas.get(gameId);
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return;
        }
        String vencedor;
        synchronized (partida) {
            vencedor = partida.resolverDesempate();
        }

        String respostaCorreta = partida.getPerguntaAtual().getRespostaCorreta();
        Map<String, Boolean> acertos = new HashMap<>(partida.getAcertouPergunta());

        if (vencedor != null) {
            // Alguém venceu o desempate
            synchronized (partida) {
                partida.finalizarDesempate();
            }
            int tirosVencedor = partida.getAcertosRodada().getOrDefault(vencedor, 0);
            Map<String, Object> evento = new HashMap<>();
            evento.put("tipo", "DESEMPATE_FIM");
            evento.put("vencedorDesempate", vencedor);
            evento.put("tiros", tirosVencedor);
            evento.put("respostaCorreta", respostaCorreta);
            evento.put("acertos", acertos);
            evento.put("mensagem", vencedor + " venceu a morte súbita e atira primeiro!");
            messaging.convertAndSend("/topic/quiz/" + gameId, evento);
            agenda.schedule(() -> iniciarFaseTiros(gameId), DELAY_ENTRE_RODADAS_MS, TimeUnit.MILLISECONDS);
        } else {
            // Ambos acertaram ou ambos erraram — continuar morte súbita
            // O RESULTADO já foi exibido antes com delay, então ir direto pra próxima pergunta
            iniciarPerguntaDesempate(gameId);
        }
    }

    private void iniciarFaseTiros(String gameId) {
        PartidaQuiz partida = partidas.get(gameId);
        if (partida == null || partida.getStatus() != StatusPartida.EM_ANDAMENTO) {
            return;
        }
        synchronized (partida) {
            partida.iniciarFaseTiros();
        }
        messaging.convertAndSend("/topic/quiz/" + gameId, new PlacarResponse(
                new HashMap<>(partida.getAcertosRodada()), partida.proximoAtirador()));

        if (partida.faseTirosAcabou()) {
            agenda.schedule(() -> iniciarRodadaComContagem(gameId), DELAY_ENTRE_RODADAS_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void agendarTimer(String gameId) {
        cancelarTimer(gameId);
        ScheduledFuture<?> futuro = agenda.schedule(() -> resolverPergunta(gameId), SEGUNDOS_RESPOSTA, TimeUnit.SECONDS);
        timers.put(gameId, futuro);
    }

    private void cancelarTimer(String gameId) {
        ScheduledFuture<?> futuro = timers.remove(gameId);
        if (futuro != null) {
            futuro.cancel(false);
        }
    }

    private void limpar(String gameId) {
        cancelarTimer(gameId);
        chegaram.remove(gameId);
        iniciados.remove(gameId);
    }
}
