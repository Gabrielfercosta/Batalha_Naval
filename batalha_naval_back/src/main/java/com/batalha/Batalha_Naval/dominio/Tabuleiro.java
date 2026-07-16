package com.batalha.Batalha_Naval.dominio;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Tabuleiro {

    public static final int TAMANHO = 10;
    private final List<Navio> navios = new ArrayList<>();
    private final Set<Coordenada> tirosRecebidos = new HashSet<>();


    public void posicionarNavio(Navio navio) {
        for (Navio outro : navios) {
            if (outro.getTipo() == navio.getTipo()) {
                throw new IllegalArgumentException(
                        "Você já posicionou um navio do tipo " + navio.getTipo() + ".");
            }
        }
        for (Coordenada pos : navio.getPosicoes()) {
            if (pos.getLinha() < 0 || pos.getLinha() >= TAMANHO || pos.getColuna() < 0 || pos.getColuna() >= TAMANHO) {
                throw new IllegalArgumentException(
                        "Navio fora do tabuleiro na posição " + pos);
            }
            for (Navio outro : navios) {
                if (outro.ocupaPosicao(pos)) {
                    throw new IllegalArgumentException(
                            "Já existe um navio na posição " + pos);
                }
            }
        }

        navios.add(navio);
    }

    public ResultadoTiro receberTiro(Coordenada tiro) {
        if (tirosRecebidos.contains(tiro)) {
            throw new IllegalArgumentException("Esta posição já foi atacada.");
        }
        tirosRecebidos.add(tiro);

        for (Navio navio : navios) {
            if (navio.ocupaPosicao(tiro)) {
                navio.registrarTiro(tiro);
                if (navio.estaAfundado()) {
                    return ResultadoTiro.AFUNDADO;
                }
                return ResultadoTiro.ACERTO;
            }
        }
        return ResultadoTiro.AGUA;
    }

    public Navio navioEm(Coordenada coordenada) {
        for (Navio navio : navios) {
            if (navio.ocupaPosicao(coordenada)) {
                return navio;
            }
        }
        return null;
    }

    public void limpar() {
        navios.clear();
        tirosRecebidos.clear();
    }

    public boolean todosAfundados() {
        if (navios.isEmpty()) {
            return false;
        }
        for (Navio navio : navios) {
            if (!navio.estaAfundado()) {
                return false;
            }
        }
        return true;
    }
}
