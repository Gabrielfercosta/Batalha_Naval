package com.batalha.Batalha_Naval.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PerguntaResponse {
    private final String pergunta;
    private final List<String> opcoes;
    private final int segundos;
    private final int indice;
    private final int total;

    public String getTipo() {
        return "PERGUNTA";
    }
}
