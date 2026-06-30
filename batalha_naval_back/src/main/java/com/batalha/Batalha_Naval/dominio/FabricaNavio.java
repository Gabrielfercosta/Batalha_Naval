package com.batalha.Batalha_Naval.dominio;

import java.util.ArrayList;
import java.util.List;

public class FabricaNavio {

    public static Navio criar(TipoNavio tipo, int linhaInicial, int colunaInicial, Direcao direcao) {
        List<Coordenada> posicoes = new ArrayList<>();

        for (int i = 0; i < tipo.getTamanho(); i++) {
            if (direcao == Direcao.HORIZONTAL) {
                posicoes.add(new Coordenada(linhaInicial, colunaInicial + i));
            } else {
                posicoes.add(new Coordenada(linhaInicial + i, colunaInicial));
            }
        }

        return new Navio(tipo, posicoes);
    }
}
