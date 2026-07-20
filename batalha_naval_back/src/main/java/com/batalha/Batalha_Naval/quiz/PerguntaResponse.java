package com.batalha.Batalha_Naval.quiz;

import java.util.List;

public class PerguntaResponse {
    private final String pergunta;
    private final List<String> opcoes;
    private final int segundos;
    private final int indice;
    private final int total;
    private final String dificuldade;

    public PerguntaResponse(String pergunta, List<String> opcoes, int segundos, int indice, int total, String dificuldade) {
        this.pergunta = pergunta;
        this.opcoes = opcoes;
        this.segundos = segundos;
        this.indice = indice;
        this.total = total;
        this.dificuldade = dificuldade;
    }

    public String getPergunta() { return pergunta; }
    public List<String> getOpcoes() { return opcoes; }
    public int getSegundos() { return segundos; }
    public int getIndice() { return indice; }
    public int getTotal() { return total; }
    public String getDificuldade() { return dificuldade; }
    public String getTipo() { return "PERGUNTA"; }
}
