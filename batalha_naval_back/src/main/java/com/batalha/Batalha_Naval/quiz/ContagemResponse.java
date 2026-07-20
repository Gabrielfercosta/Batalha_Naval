package com.batalha.Batalha_Naval.quiz;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ContagemResponse {
    private final int segundos;

    public String getTipo() {
        return "CONTAGEM";
    }
}
