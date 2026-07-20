package com.batalha.Batalha_Naval.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class PlacarResponse {
    private final Map<String, Integer> acertos;
    private final String proximoAtirador;

    public String getTipo() {
        return "PLACAR";
    }
}
