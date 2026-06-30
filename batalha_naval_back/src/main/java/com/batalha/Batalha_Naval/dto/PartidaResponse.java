package com.batalha.Batalha_Naval.dto;

import com.batalha.Batalha_Naval.dominio.Partida;
import com.batalha.Batalha_Naval.dominio.StatusPartida;
import lombok.Getter;

@Getter
public class PartidaResponse {
    private final String gameId;
    private final String jogador1;
    private final String jogador2;
    private final StatusPartida status;
    private final String turnoAtual;

    public PartidaResponse(String gameId, Partida partida) {
        this.gameId = gameId;
        this.jogador1 = partida.getJogador1();
        this.jogador2 = partida.getJogador2();
        this.status = partida.getStatus();
        this.turnoAtual = partida.getTurnoAtual();
    }
}
