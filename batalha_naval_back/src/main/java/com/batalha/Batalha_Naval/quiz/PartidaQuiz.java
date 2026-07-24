package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.dominio.Coordenada;
import com.batalha.Batalha_Naval.dominio.Navio;
import com.batalha.Batalha_Naval.dominio.PartidaBase;
import com.batalha.Batalha_Naval.dominio.ResultadoTiro;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dominio.Tabuleiro;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class PartidaQuiz extends PartidaBase {
    public static final int TOTAL_NAVIOS = 5;
    public static final int PERGUNTAS_POR_RODADA = 5;
    private final Tabuleiro tabuleiro1 = new Tabuleiro();
    private final Tabuleiro tabuleiro2 = new Tabuleiro();
    private boolean faseTiros = false;
    private int perguntaIndice = 0;
    private PerguntaTrivia perguntaAtual;
    private boolean resolvida = false;
    private final Set<String> responderam = new HashSet<>();
    private final Map<String, Boolean> acertouPergunta = new HashMap<>();
    private final Map<String, Integer> acertosRodada = new HashMap<>();
    private final Map<String, Integer> tirosRestantes = new HashMap<>();
    private final List<String> ordemTiro = new ArrayList<>();
    private List<String> categorias = new ArrayList<>();
    private String dificuldade = "";
    private boolean modoRapido = false;
    private final Set<String> perguntasUsadas = new HashSet<>();

    // Morte súbita (desempate)
    private boolean emDesempate = false;
    private int perguntaDesempate = 0;
    private String vencedorDesempate = null;
    private int pontuacaoEmpatada = 0;

    public PartidaQuiz(String jogador1, String nome, String senha) {
        super(jogador1, nome, senha);
    }

    public void configurar(List<String> categorias, String dificuldade, boolean modoRapido) {
        this.categorias = categorias != null ? categorias : new ArrayList<>();
        this.dificuldade = dificuldade != null ? dificuldade : "";
        this.modoRapido = modoRapido;
    }

    @Override
    public void marcarPronto(String jogador) {
        garantirNaoIniciada();
        Tabuleiro tab = ehJogador1(jogador) ? tabuleiro1 : tabuleiro2;
        if (tab.getNavios().size() < TOTAL_NAVIOS) {
            throw new IllegalStateException("Posicione todos os navios antes de ficar pronto.");
        }
        prontos.add(jogador);
        if (ambosProntos()) {
            status = StatusPartida.EM_ANDAMENTO;
        }
    }

    @Override
    public void removerJogador2() {
        this.jogador2 = null;
        this.prontos.clear();
        this.tabuleiro2.limpar();
        limparRodada();
        if (this.status == StatusPartida.POSICIONANDO) {
            this.status = StatusPartida.AGUARDANDO;
            this.turnoAtual = this.jogador1;
        }
    }

    public void novaRodada() {
        if (status != StatusPartida.EM_ANDAMENTO) {
            throw new IllegalStateException("A partida não está em andamento.");
        }
        limparRodada();
    }

    public void iniciarPergunta(PerguntaTrivia pergunta) {
        this.perguntaAtual = pergunta;
        this.resolvida = false;
        this.responderam.clear();
        this.acertouPergunta.clear();
    }

    public boolean responder(String jogador, String resposta) {
        if (status != StatusPartida.EM_ANDAMENTO || faseTiros || perguntaAtual == null || resolvida) {
            return false;
        }
        if (responderam.contains(jogador)) {
            return todosResponderam();
        }
        responderam.add(jogador);
        boolean acertou = perguntaAtual.estaCorreta(resposta);
        acertouPergunta.put(jogador, acertou);
        if (acertou && !emDesempate) {
            int base = switch (perguntaAtual.getDificuldade()) {
                case "hard" -> 3;
                case "medium" -> 2;
                default -> 1;
            };
            int valor = modoRapido ? base * 2 : base;
            acertosRodada.merge(jogador, valor, Integer::sum);
        }
        return todosResponderam();
    }

    public void resolverPergunta() {
        if (resolvida) {
            return;
        }
        resolvida = true;
        acertouPergunta.putIfAbsent(jogador1, false);
        if (jogador2 != null) {
            acertouPergunta.putIfAbsent(jogador2, false);
        }
    }

    public boolean ultimaPergunta() {
        return perguntaIndice >= PERGUNTAS_POR_RODADA - 1;
    }

    public void avancarPergunta() {
        perguntaIndice++;
    }

    public boolean rodadaEmpatou() {
        if (jogador2 == null) return false;
        int a1 = acertosRodada.getOrDefault(jogador1, 0);
        int a2 = acertosRodada.getOrDefault(jogador2, 0);
        return a1 == a2;
    }

    public void iniciarDesempate() {
        emDesempate = true;
        perguntaDesempate = 0;
        vencedorDesempate = null;
        pontuacaoEmpatada = acertosRodada.getOrDefault(jogador1, 0);
    }

    public void prepararPerguntaDesempate() {
        perguntaDesempate++;
        responderam.clear();
        acertouPergunta.clear();
        resolvida = false;
    }

    public String resolverDesempate() {
        if (jogador2 == null) return null;
        boolean j1Acertou = acertouPergunta.getOrDefault(jogador1, false);
        boolean j2Acertou = acertouPergunta.getOrDefault(jogador2, false);

        if (j1Acertou && !j2Acertou) {
            vencedorDesempate = jogador1;
        } else if (j2Acertou && !j1Acertou) {
            vencedorDesempate = jogador2;
        }
        // Se ambos acertaram ou ambos erraram, continua null (não houve desempate)
        return vencedorDesempate;
    }

    public void finalizarDesempate() {
        // Vencedor ganha a pontuação original empatada + 1 bônus, perdedor fica com 0
        emDesempate = false;
        if (vencedorDesempate != null) {
            acertosRodada.put(vencedorDesempate, pontuacaoEmpatada + 1);
            String perdedor = vencedorDesempate.equals(jogador1) ? jogador2 : jogador1;
            acertosRodada.put(perdedor, 0);
        }
    }

    public void iniciarFaseTiros() {
        faseTiros = true;
        tirosRestantes.clear();
        ordemTiro.clear();

        int a1 = acertosRodada.getOrDefault(jogador1, 0);
        int a2 = jogador2 != null ? acertosRodada.getOrDefault(jogador2, 0) : 0;

        if (a1 > 0) tirosRestantes.put(jogador1, a1);
        if (jogador2 != null && a2 > 0) tirosRestantes.put(jogador2, a2);

        if (a1 >= a2) {
            if (a1 > 0) ordemTiro.add(jogador1);
            if (a2 > 0) ordemTiro.add(jogador2);
        } else {
            ordemTiro.add(jogador2);
            if (a1 > 0) ordemTiro.add(jogador1);
        }
    }

    public ResultadoTiro atirar(String jogador, Coordenada tiro) {
        if (status != StatusPartida.EM_ANDAMENTO) {
            throw new IllegalStateException("A partida não está em andamento.");
        }
        if (!faseTiros) {
            throw new IllegalStateException("Ainda não é hora de atirar.");
        }
        if (!jogador.equals(proximoAtirador())) {
            throw new IllegalStateException("Não é a vez de " + jogador + " atirar.");
        }
        Tabuleiro alvo = ehJogador1(jogador) ? tabuleiro2 : tabuleiro1;
        ResultadoTiro resultado = alvo.receberTiro(tiro);

        int restam = tirosRestantes.getOrDefault(jogador, 0) - 1;
        if (restam <= 0) {
            tirosRestantes.remove(jogador);
            ordemTiro.remove(jogador);
        } else {
            tirosRestantes.put(jogador, restam);
        }

        if (alvo.todosAfundados()) {
            status = StatusPartida.FINALIZADA;
            vencedor = jogador;
        }
        return resultado;
    }

    public Navio navioAfundadoEm(String jogador, Coordenada tiro) {
        Tabuleiro alvo = ehJogador1(jogador) ? tabuleiro2 : tabuleiro1;
        return alvo.navioEm(tiro);
    }

    public String proximoAtirador() {
        for (String j : ordemTiro) {
            if (tirosRestantes.getOrDefault(j, 0) > 0) {
                return j;
            }
        }
        return null;
    }

    public boolean faseTirosAcabou() {
        return faseTiros && proximoAtirador() == null;
    }

    private boolean todosResponderam() {
        return jogador2 != null && responderam.contains(jogador1) && responderam.contains(jogador2);
    }

    private void limparRodada() {
        this.faseTiros = false;
        this.perguntaIndice = 0;
        this.perguntaAtual = null;
        this.resolvida = false;
        this.responderam.clear();
        this.acertouPergunta.clear();
        this.acertosRodada.clear();
        this.tirosRestantes.clear();
        this.ordemTiro.clear();
        this.emDesempate = false;
        this.perguntaDesempate = 0;
        this.vencedorDesempate = null;
        this.pontuacaoEmpatada = 0;
    }
}
