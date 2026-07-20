package com.batalha.Batalha_Naval.quiz;

import com.batalha.Batalha_Naval.dominio.StatusPartida;
import com.batalha.Batalha_Naval.dto.NavioRevelado;
import lombok.Getter;

import java.util.List;

@Getter
public class PartidaQuizResponse {
    private final String gameId;
    private final String jogador1;
    private final String jogador2;
    private final StatusPartida status;
    private final List<NavioRevelado> navios1;
    private final List<NavioRevelado> navios2;

    public PartidaQuizResponse(String gameId, PartidaQuiz partida) {
        this.gameId = gameId;
        this.jogador1 = partida.getJogador1();
        this.jogador2 = partida.getJogador2();
        this.status = partida.getStatus();
        if (partida.getStatus() == StatusPartida.FINALIZADA) {
            this.navios1 = partida.getTabuleiro1().getNavios().stream().map(NavioRevelado::new).toList();
            this.navios2 = partida.getTabuleiro2().getNavios().stream().map(NavioRevelado::new).toList();
        } else {
            this.navios1 = null;
            this.navios2 = null;
        }
    }
}
