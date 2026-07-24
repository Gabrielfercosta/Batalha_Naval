package com.batalha.Batalha_Naval.quiz;

import java.util.List;

public class PerguntaResponse {
    private final String pergunta;
    private final List<String> opcoes;
    private final int segundos;
    private final int indice;
    private final int total;
    private final String dificuldade;
    private final boolean modoRapido;
    private final boolean desempate;

    public PerguntaResponse(String pergunta, List<String> opcoes, int segundos, int indice, int total, String dificuldade, boolean modoRapido) {
        this(pergunta, opcoes, segundos, indice, total, dificuldade, modoRapido, false);
    }

    public PerguntaResponse(String pergunta, List<String> opcoes, int segundos, int indice, int total, String dificuldade, boolean modoRapido, boolean desempate) {
        this.pergunta = pergunta;
        this.opcoes = opcoes;
        this.segundos = segundos;
        this.indice = indice;
        this.total = total;
        this.dificuldade = dificuldade;
        this.modoRapido = modoRapido;
        this.desempate = desempate;
    }

    public String getPergunta() { return pergunta; }
    public List<String> getOpcoes() { return opcoes; }
    public int getSegundos() { return segundos; }
    public int getIndice() { return indice; }
    public int getTotal() { return total; }
    public String getDificuldade() { return dificuldade; }
    public boolean isModoRapido() { return modoRapido; }
    public boolean isDesempate() { return desempate; }
    public String getTipo() { return "PERGUNTA"; }
}
