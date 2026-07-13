package com.batalha.Batalha_Naval.dto;

import com.batalha.Batalha_Naval.dominio.Coordenada;
import com.batalha.Batalha_Naval.dominio.Navio;
import lombok.Getter;

import java.util.List;

@Getter
public class NavioRevelado {
    private final String tipo;
    private final int tamanho;
    private final int linha;
    private final int coluna;
    private final String direcao;

    public NavioRevelado(Navio navio) {
        List<Coordenada> pos = navio.getPosicoes();
        Coordenada ancora = pos.get(0);
        this.tipo = navio.getTipo().name();
        this.tamanho = pos.size();
        this.linha = ancora.getLinha();
        this.coluna = ancora.getColuna();
        this.direcao = pos.get(0).getLinha() == pos.get(1).getLinha() ? "HORIZONTAL" : "VERTICAL";
    }
}
