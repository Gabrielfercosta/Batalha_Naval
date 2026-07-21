package com.batalha.Batalha_Naval.quiz;

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
    private final ScheduledExecutorService agenda = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, Set<String>> chegaram = new ConcurrentHashMap<>();
    private final Set<String> iniciados = ConcurrentHashMap.newKeySet();
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public QuizService(TriviaService triviaService, SimpMessagingTemplate messaging) {
        this.triviaService = triviaService;
        this.messaging = messaging;
    }

    @Override
    protected PartidaQuiz novaPartida(String jogador, String nome, String senha) {
        return new PartidaQuiz(jogador, nome, senha);
    }

    public String criarPartidaQuiz(String jogador, String nome, String senha, List<String> categorias, String dificuldade, boolean modoRapido) {
        String gameId = criarPartida(jogador, nome, senha);
        buscarPartida(gameId).configurar(categorias, dificuldade, modoRapido);
        return gameId;
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
        synchronized (partida) {
            if (partida.isResolvida()) {
                return;
            }
            partida.resolverPergunta();
        }

        messaging.convertAndSend("/topic/quiz/" + gameId, new ResultadoRodadaResponse(
                partida.getPerguntaAtual().getRespostaCorreta(),
                new HashMap<>(partida.getAcertouPergunta()),
                partida.getPerguntaIndice() + 1));

        if (partida.ultimaPergunta()) {
            synchronized (partida) {
                partida.iniciarFaseTiros();
            }
            messaging.convertAndSend("/topic/quiz/" + gameId, new PlacarResponse(
                    new HashMap<>(partida.getAcertosRodada()), partida.proximoAtirador()));

            if (partida.faseTirosAcabou()) {
                agenda.schedule(() -> iniciarRodadaComContagem(gameId), DELAY_ENTRE_RODADAS_MS, TimeUnit.MILLISECONDS);
            }
        } else {
            synchronized (partida) {
                partida.avancarPergunta();
            }
            agenda.schedule(() -> iniciarPergunta(gameId), DELAY_ENTRE_PERGUNTAS_MS, TimeUnit.MILLISECONDS);
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
