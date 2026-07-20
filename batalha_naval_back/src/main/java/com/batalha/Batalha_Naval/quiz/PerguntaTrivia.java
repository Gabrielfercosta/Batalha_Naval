package com.batalha.Batalha_Naval.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PerguntaTrivia {
    private final String pergunta;
    private final List<String> opcoes;
    private final String respostaCorreta;

    public boolean estaCorreta(String resposta) {
        return respostaCorreta.equals(resposta);
    }
}
