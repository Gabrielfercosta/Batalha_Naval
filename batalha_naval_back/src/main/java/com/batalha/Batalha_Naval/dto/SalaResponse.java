package com.batalha.Batalha_Naval.dto;

import lombok.Getter;

@Getter
public class SalaResponse {
    private final String gameId;
    private final String nome;
    private final boolean temSenha;

    public SalaResponse(String gameId, String nome, boolean temSenha) {
        this.gameId = gameId;
        this.nome = nome;
        this.temSenha = temSenha;
    }
}
