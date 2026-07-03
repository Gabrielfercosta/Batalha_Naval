package com.batalha.Batalha_Naval.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CriarPartidaRequest {
    private String jogador;
    private String nome;
    private String senha;
}
