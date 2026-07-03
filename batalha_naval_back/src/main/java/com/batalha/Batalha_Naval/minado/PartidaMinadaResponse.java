package com.batalha.Batalha_Naval.minado;

import lombok.Getter;

@Getter
public class PartidaMinadaResponse {
    private final String gameId;
    private final String jogador1;
    private final String jogador2;
    private final StatusPartidaMinada status;
    private final String turnoAtual;

    public PartidaMinadaResponse(String gameId, PartidaMinada partida) {
        this.gameId = gameId;
        this.jogador1 = partida.getJogador1();
        this.jogador2 = partida.getJogador2();
        this.status = partida.getStatus();
        this.turnoAtual = partida.getTurnoAtual();
    }
}
