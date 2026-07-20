package com.batalha.Batalha_Naval.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ResultadoRodadaResponse {
    private final String respostaCorreta;
    private final Map<String, Boolean> acertos;
    private final int indice;

    public String getTipo() {
        return "RESULTADO";
    }
}
