package com.batalha.Batalha_Naval.minado;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CasaRevelada {
    private final int linha;
    private final int coluna;
    private final int minasVizinhas;
    private final int naviosVizinhos;
}
