package com.batalha.Batalha_Naval.dominio;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Tabuleiro {

    public static final int TAMANHO = 8;

    private final List<Navio> navios = new ArrayList<>();

    public void posicionarNavio(Navio navio) {
        for (Coordenada pos : navio.getPosicoes()) {
            if (pos.getLinha() < 0 || pos.getLinha() >= TAMANHO
                    || pos.getColuna() < 0 || pos.getColuna() >= TAMANHO) {
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

    public boolean todosAfundados() {
        for (Navio navio : navios) {
            if (!navio.estaAfundado()) {
                return false;
            }
        }
        return true;
    }
}
