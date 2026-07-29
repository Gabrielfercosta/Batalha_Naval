package com.batalha.Batalha_Naval.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SalaResponse {
    private final String gameId;
    private final String nome;
    private final boolean temSenha;
    private final String criador;
    private final int jogadores;

    @JsonCreator
    public SalaResponse(
            @JsonProperty("gameId") String gameId,
            @JsonProperty("nome") String nome,
            @JsonProperty("temSenha") boolean temSenha,
            @JsonProperty("criador") String criador,
            @JsonProperty("jogadores") int jogadores) {
        this.gameId = gameId;
        this.nome = nome;
        this.temSenha = temSenha;
        this.criador = criador;
        this.jogadores = jogadores;
    }
}
