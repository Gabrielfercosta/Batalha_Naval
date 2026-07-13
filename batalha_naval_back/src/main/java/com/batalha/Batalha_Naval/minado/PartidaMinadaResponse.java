package com.batalha.Batalha_Naval.minado;

import com.batalha.Batalha_Naval.dominio.StatusPartida;
import lombok.Getter;

import java.util.List;

@Getter
public class PartidaMinadaResponse {
    private final String gameId;
    private final String jogador1;
    private final String jogador2;
    private final StatusPartida status;
    private final String turnoAtual;
    private final List<int[]> navios1;
    private final List<int[]> minas1;
    private final List<int[]> navios2;
    private final List<int[]> minas2;

    public PartidaMinadaResponse(String gameId, PartidaMinada partida) {
        this.gameId = gameId;
        this.jogador1 = partida.getJogador1();
        this.jogador2 = partida.getJogador2();
        this.status = partida.getStatus();
        this.turnoAtual = partida.getTurnoAtual();
        if (partida.getStatus() == StatusPartida.FINALIZADA) {
            this.navios1 = partida.getTabuleiro1().posicoesDe(EstadoCasa.NAVIO);
            this.minas1 = partida.getTabuleiro1().posicoesDe(EstadoCasa.MINA);
            this.navios2 = partida.getTabuleiro2().posicoesDe(EstadoCasa.NAVIO);
            this.minas2 = partida.getTabuleiro2().posicoesDe(EstadoCasa.MINA);
        } else {
            this.navios1 = null;
            this.minas1 = null;
            this.navios2 = null;
            this.minas2 = null;
        }
    }
}
