package com.batalha.Batalha_Naval.dominio;

import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Navio {

    private final TipoNavio tipo;
    private final List<Coordenada> posicoes;
    private final Set<Coordenada> atingidas = new HashSet<>();

    public Navio(TipoNavio tipo, List<Coordenada> posicoes) {
        this.tipo = tipo;
        this.posicoes = posicoes;
    }

    public boolean ocupaPosicao(Coordenada coordenada) {
        return posicoes.contains(coordenada);
    }

    public void registrarTiro(Coordenada coordenada) {
        atingidas.add(coordenada);
    }

    public boolean estaAfundado() {
        return atingidas.size() == posicoes.size();
    }
}
