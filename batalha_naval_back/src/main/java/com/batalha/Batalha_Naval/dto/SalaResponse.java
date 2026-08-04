package com.batalha.Batalha_Naval.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class SalaResponse {
    private final String gameId;
    private final String nome;
    private final boolean temSenha;
    private final String criador;
    private final int jogadores;
}
