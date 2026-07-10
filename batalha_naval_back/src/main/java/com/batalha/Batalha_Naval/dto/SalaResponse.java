package com.batalha.Batalha_Naval.dto;

import lombok.Getter;

@Getter
public class SalaResponse {
    private final String gameId;
    private final String nome;
    private final boolean temSenha;
    private final String criador;
    private final int jogadores;

    public SalaResponse(String gameId, String nome, boolean temSenha, String criador, int jogadores) {
        this.gameId = gameId;
        this.nome = nome;
        this.temSenha = temSenha;
        this.criador = criador;
        this.jogadores = jogadores;
    }
}
