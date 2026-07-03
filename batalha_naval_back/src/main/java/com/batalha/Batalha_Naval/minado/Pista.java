package com.batalha.Batalha_Naval.minado;

import lombok.Getter;

@Getter
public class Pista {
    private final int minas;
    private final int navios;

    public Pista(int minas, int navios) {
        this.minas = minas;
        this.navios = navios;
    }
}
